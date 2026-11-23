package com.ngstudio.mazebrush.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Physics
import com.ngstudio.maze.core.Slide

/**
 * Rules-side state of one level being played (main thread). The renderer only animates;
 * this class decides what is painted, counts moves against the move limit, handles the swipe buffer,
 * undo, out-of-moves and the win.
 */
class GameController(val level: Level, moveLimit: Int?, private val callbacks: Callbacks) {

    interface Callbacks {
        fun playSlide(slide: Slide)
        fun playUndo(slide: Slide, unpaint: IntArray)
        fun onBlocked(dir: Dir)
        fun onWin(moves: Int, stars: Int)
        fun onOutOfMoves()
    }

    private class UndoEntry(val slide: Slide, val newly: IntArray)

    private val painted = BooleanArray(level.size)
    private var paintedCount = 0
    val floorTotal = level.floorCount
    var position = level.start
        private set
    private val undoStack = ArrayDeque<UndoEntry>()
    private var busy = false
    private var queued: Dir? = null
    var finished = false
        private set

    /** The limit the level started with (stars are measured against it); null = unlimited. */
    val baseLimit: Int? = moveLimit

    /** Observable HUD values. */
    var moves by mutableIntStateOf(0)
        private set
    var limit by mutableStateOf(moveLimit)
        private set
    var outOfMoves by mutableStateOf(false)
        private set
    var visualPainted by mutableIntStateOf(1)
        private set
    var canUndo by mutableStateOf(false)
        private set

    init {
        painted[level.start] = true
        paintedCount = 1
    }

    val progress: Float get() = visualPainted.toFloat() / floorTotal
    val movesLeft: Int? get() = limit?.let { (it - moves).coerceAtLeast(0) }
    val tilesLeft: Int get() = floorTotal - paintedCount

    /** Directions played so far (for hints). */
    val history: List<Dir> get() = undoStack.map { it.slide.dir }

    fun swipe(d: Dir) {
        if (finished || outOfMoves) return
        if (busy) {
            if (queued == null) queued = d
            return
        }
        execute(d)
    }

    private fun execute(d: Dir) {
        val lim = limit
        if (lim != null && moves >= lim) { outOfMoves = true; callbacks.onOutOfMoves(); return }
        val s = Physics.slide(level, position, d)
        if (s == null || s.loops) {
            callbacks.onBlocked(d)
            return
        }
        val newly = s.cells.filter { !painted[it] }.distinct().toIntArray()
        for (c in newly) painted[c] = true
        paintedCount += newly.size
        position = s.end
        moves++
        undoStack.addLast(UndoEntry(s, newly))
        if (undoStack.size > 400) undoStack.removeFirst()
        canUndo = true
        busy = true
        callbacks.playSlide(s)
    }

    /** Renderer painted one more cell (keeps the HUD bar in sync with the animation). */
    fun onCellPainted() {
        if (visualPainted < floorTotal) visualPainted++
    }

    fun onMoveFinished() {
        busy = false
        if (paintedCount >= floorTotal && !finished) {
            finished = true
            queued = null
            visualPainted = floorTotal
            canUndo = false
            callbacks.onWin(moves, starsFor(moves))
            return
        }
        val lim = limit
        if (lim != null && moves >= lim) {
            queued = null
            outOfMoves = true
            callbacks.onOutOfMoves()
            return
        }
        val q = queued
        queued = null
        if (q != null) execute(q)
    }

    fun undo(): Boolean {
        if (busy || finished || undoStack.isEmpty()) return false
        val e = undoStack.removeLast()
        for (c in e.newly) painted[c] = false
        paintedCount -= e.newly.size
        visualPainted = (visualPainted - e.newly.size).coerceAtLeast(1)
        position = e.slide.from
        moves = (moves - 1).coerceAtLeast(0)
        outOfMoves = false
        canUndo = undoStack.isNotEmpty()
        busy = true
        callbacks.playUndo(e.slide, e.newly)
        return true
    }

    /** Extra moves (coins or rewarded ad) after running out. */
    fun addMoves(n: Int) {
        limit = limit?.plus(n)
        outOfMoves = false
    }

    /** The undo animation finished. */
    fun onUndoFinished() { busy = false }

    val isBusy get() = busy

    /** What the Hint button should suggest from here (see [Hints.plan]). */
    fun hint(): Hints.Advice = Hints.plan(level, position, painted, history, movesLeft)

    fun starsFor(m: Int): Int {
        val par = level.par.coerceAtLeast(1)
        val lim = baseLimit
        return when {
            m <= par -> 3
            lim != null -> if (m <= par + (lim - par + 1) / 2) 2 else 1
            m <= (par * 3 + 1) / 2 -> 2
            else -> 1
        }
    }
}
