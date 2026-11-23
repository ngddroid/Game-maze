package com.ngstudio.maze.core

/**
 * Result of one swipe. [cells] are the cells entered, in order (the start cell is not included).
 * Two consecutive cells that are not neighbours mean a portal jump. [end] is where the ball rests.
 */
class Slide(val from: Int, val dir: Dir, val end: Int, val cells: IntArray, val loops: Boolean)

/**
 * The single source of truth for movement. Used by the app while playing and by the
 * validator/solver/generator, so a verified level always plays exactly as it was verified.
 *
 * Rules: the ball moves cell by cell in the swipe direction and stops before a wall, the grid edge,
 * or a one-way gate pointing another way. Entering a stopper stops it on that cell. Entering a
 * deflector turns it 90 degrees. Entering a portal moves it to the partner portal, same direction.
 */
object Physics {

    /** Returns null when the ball cannot move in [dir]. A looping slide has [Slide.loops] = true. */
    fun slide(level: Level, from: Int, dir: Dir): Slide? {
        var pos = from
        var d = dir
        var cells = IntArray(16)
        var n = 0
        // (cell, direction) states seen during this slide; a repeat means an endless loop.
        val seen = HashSet<Int>()
        var loops = false
        while (true) {
            val next = level.step(pos, d)
            if (next < 0) break
            val t = level.tile(next)
            if (t == Tile.WALL) break
            if (Tile.isOneWay(t) && Tile.oneWayDir(t) != d) break
            pos = next
            if (n + 2 >= cells.size) cells = cells.copyOf(cells.size * 2)
            cells[n++] = pos
            if (!seen.add(pos * 4 + d.ordinal)) { loops = true; break }
            when {
                t == Tile.STOPPER -> break
                Tile.isDeflector(t) -> d = Tile.deflect(t, d)
                t == Tile.PORTAL -> {
                    val partner = level.portalPartner(pos)
                    if (partner >= 0) {
                        pos = partner
                        cells[n++] = pos
                        if (!seen.add(pos * 4 + d.ordinal)) { loops = true; break }
                    }
                }
            }
        }
        if (n == 0) return null
        return Slide(from, dir, pos, cells.copyOf(n), loops)
    }

    /** Applies a whole move list from the start; returns the painted mask, or null if a move is illegal. */
    fun replay(level: Level, moves: List<Dir>): BooleanArray? {
        val painted = BooleanArray(level.size)
        painted[level.start] = true
        var pos = level.start
        for (m in moves) {
            val s = slide(level, pos, m) ?: return null
            if (s.loops) return null
            for (c in s.cells) painted[c] = true
            pos = s.end
        }
        return painted
    }

    fun isSolvedBy(level: Level, moves: List<Dir>): Boolean {
        val painted = replay(level, moves) ?: return false
        for (i in 0 until level.size) if (level.isFloor(i) && !painted[i]) return false
        return true
    }
}
