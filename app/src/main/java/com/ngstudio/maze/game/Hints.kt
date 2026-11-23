package com.ngstudio.maze.game

import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Physics

/**
 * Hint logic. While the player's moves match the level's stored (verified) solution, the hint is the next move
 * of that solution, so following hints always finishes within par. After the player has left that route, the
 * hint is the first move of a greedy route to finish; if that route no longer fits the moves left, the hint says
 * "undo" instead (undo gives the move back).
 */
object Hints {
    sealed class Advice {
        class Move(val dir: Dir) : Advice()
        object Undo : Advice()
        object None : Advice()
    }

    fun plan(level: Level, pos: Int, painted: BooleanArray, history: List<Dir>, movesLeft: Int?): Advice {
        val sol = level.solutionDirs()
        if (history.size < sol.size && history == sol.subList(0, history.size)) return Advice.Move(sol[history.size])
        // off the stored route: greedy finish from here
        val p = painted.copyOf()
        var at = pos
        var first: Dir? = null
        var count = 0
        while (p.indices.any { level.isFloor(it) && !p[it] } && count < 600) {
            val d = next(level, at, p) ?: break
            val s = Physics.slide(level, at, d) ?: break
            for (c in s.cells) p[c] = true
            at = s.end
            if (first == null) first = d
            count++
        }
        if (first == null) return Advice.None
        if (movesLeft != null && count > movesLeft) return if (history.isNotEmpty()) Advice.Undo else Advice.Move(first)
        return Advice.Move(first)
    }

    /** First swipe of the shortest route to a swipe that paints something new. */
    fun next(level: Level, pos: Int, painted: BooleanArray): Dir? {
        val prev = HashMap<Int, Pair<Int, Dir>>()
        val seen = HashSet<Int>()
        val queue = ArrayDeque<Int>()
        seen.add(pos); queue.add(pos)
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            for (d in Dir.VALUES) {
                val s = Physics.slide(level, u, d) ?: continue
                if (s.loops) continue
                if (s.cells.any { !painted[it] }) {
                    var node = u
                    var first = d
                    while (node != pos) {
                        val (p, pd) = prev[node]!!
                        first = pd
                        node = p
                    }
                    return first
                }
                if (seen.add(s.end)) { prev[s.end] = u to d; queue.add(s.end) }
            }
        }
        return null
    }
}
