package com.ngstudio.maze.core

/** Swipe / travel direction. Screen coordinates: y grows downward. */
enum class Dir(val dx: Int, val dy: Int, val symbol: Char) {
    UP(0, -1, 'U'), RIGHT(1, 0, 'R'), DOWN(0, 1, 'D'), LEFT(-1, 0, 'L');

    val opposite: Dir get() = VALUES[(ordinal + 2) and 3]
    val isHorizontal: Boolean get() = dx != 0

    companion object {
        val VALUES = values()
        fun of(ordinal: Int) = VALUES[ordinal and 3]
    }
}

/**
 * Tile codes (one per cell, 4 bits in the binary format).
 * Everything except [WALL] is floor the ball can roll over and must paint.
 */
object Tile {
    const val WALL = 0
    const val FLOOR = 1
    /** Ball stops on this pad. */
    const val STOPPER = 2
    /** One-way gates 3..6: can only be entered while moving in that direction (UP, RIGHT, DOWN, LEFT). */
    const val ONEWAY_UP = 3
    const val ONEWAY_RIGHT = 4
    const val ONEWAY_DOWN = 5
    const val ONEWAY_LEFT = 6
    /** Deflector shaped like '/': RIGHT->UP, UP->RIGHT, LEFT->DOWN, DOWN->LEFT. */
    const val DEFLECT_SLASH = 7
    /** Deflector shaped like '\': RIGHT->DOWN, DOWN->RIGHT, LEFT->UP, UP->LEFT. */
    const val DEFLECT_BACKSLASH = 8
    /** Portal: entering it moves the ball to its partner, same direction. */
    const val PORTAL = 9

    fun isFloor(t: Int) = t != WALL
    fun isSpecial(t: Int) = t >= STOPPER
    fun isOneWay(t: Int) = t in ONEWAY_UP..ONEWAY_LEFT
    fun oneWay(d: Dir) = ONEWAY_UP + d.ordinal
    fun oneWayDir(t: Int) = Dir.of(t - ONEWAY_UP)
    fun isDeflector(t: Int) = t == DEFLECT_SLASH || t == DEFLECT_BACKSLASH

    fun deflect(t: Int, d: Dir): Dir = if (t == DEFLECT_SLASH) when (d) {
        Dir.RIGHT -> Dir.UP; Dir.UP -> Dir.RIGHT; Dir.LEFT -> Dir.DOWN; Dir.DOWN -> Dir.LEFT
    } else when (d) {
        Dir.RIGHT -> Dir.DOWN; Dir.DOWN -> Dir.RIGHT; Dir.LEFT -> Dir.UP; Dir.UP -> Dir.LEFT
    }

    /** Mechanic family, used for intro tips and the difficulty schedule. */
    fun mechanic(t: Int): Mechanic? = when {
        t == STOPPER -> Mechanic.STOPPER
        isOneWay(t) -> Mechanic.ONE_WAY
        isDeflector(t) -> Mechanic.DEFLECTOR
        t == PORTAL -> Mechanic.PORTAL
        else -> null
    }
}

enum class Mechanic(val code: Int) {
    STOPPER(1), ONE_WAY(2), DEFLECTOR(3), PORTAL(4);

    companion object {
        fun of(code: Int) = values().firstOrNull { it.code == code }
    }
}

/**
 * One puzzle. Cells are indexed row-major: i = y * width + x. Outside the grid counts as wall.
 * [portals] holds pairs (a0, b0, a1, b1, ...). [solution] holds Dir ordinals of a verified solution
 * whose length is [par].
 */
class Level(
    val width: Int,
    val height: Int,
    val tiles: ByteArray,
    val start: Int,
    val portals: IntArray = IntArray(0),
    var par: Int = 0,
    var solution: ByteArray = ByteArray(0),
    /** 0..1 difficulty score from [Difficulty]. */
    var difficulty: Float = 0f,
    /** Mechanic introduced by this level (coach-mark tip in the app), or null. */
    var intro: Mechanic? = null,
) {
    val size get() = width * height

    fun x(i: Int) = i % width
    fun y(i: Int) = i / width
    fun idx(x: Int, y: Int) = y * width + x
    fun inBounds(x: Int, y: Int) = x in 0 until width && y in 0 until height
    fun tile(i: Int): Int = tiles[i].toInt()
    fun isFloor(i: Int) = tiles[i].toInt() != Tile.WALL

    /** Neighbor index in direction d, or -1 when it leaves the grid. */
    fun step(i: Int, d: Dir): Int {
        val nx = x(i) + d.dx
        val ny = y(i) + d.dy
        return if (inBounds(nx, ny)) idx(nx, ny) else -1
    }

    fun portalPartner(i: Int): Int {
        var k = 0
        while (k + 1 < portals.size) {
            if (portals[k] == i) return portals[k + 1]
            if (portals[k + 1] == i) return portals[k]
            k += 2
        }
        return -1
    }

    val floorCount: Int get() = tiles.count { it.toInt() != Tile.WALL }

    fun floorCells(): IntArray = (0 until size).filter { isFloor(it) }.toIntArray()

    fun specialCount(): Int = tiles.count { Tile.isSpecial(it.toInt()) }

    fun mechanics(): Set<Mechanic> = tiles.toList().mapNotNull { Tile.mechanic(it.toInt()) }.toSet()

    fun copy(): Level = Level(width, height, tiles.copyOf(), start, portals.copyOf(), par, solution.copyOf(), difficulty, intro)

    fun solutionDirs(): List<Dir> = solution.map { Dir.of(it.toInt()) }

    /** ASCII form (see [parse] for the legend). Useful for tests, reports and debugging. */
    fun toAscii(): String = buildString {
        val portalLetter = HashMap<Int, Char>()
        var k = 0
        while (k + 1 < portals.size) {
            val c = 'P' + k / 2
            portalLetter[portals[k]] = c; portalLetter[portals[k + 1]] = c
            k += 2
        }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = idx(x, y)
                append(
                    when {
                        i == start -> 'S'
                        else -> when (val t = tile(i)) {
                            Tile.WALL -> '#'
                            Tile.FLOOR -> '.'
                            Tile.STOPPER -> 'o'
                            Tile.ONEWAY_UP -> '^'
                            Tile.ONEWAY_RIGHT -> '>'
                            Tile.ONEWAY_DOWN -> 'v'
                            Tile.ONEWAY_LEFT -> '<'
                            Tile.DEFLECT_SLASH -> '/'
                            Tile.DEFLECT_BACKSLASH -> '\\'
                            Tile.PORTAL -> portalLetter[i] ?: '?'
                            else -> error("tile $t")
                        }
                    }
                )
            }
            if (y < height - 1) append('\n')
        }
    }

    companion object {
        /**
         * Legend: '#' wall, '.' floor, 'S' start (floor), 'o' stopper, '^' '>' 'v' '<' one-way gates,
         * '/' '\' deflectors, 'P'/'Q' portal pairs (two cells with the same letter).
         * Rows may be indented; the grid is cropped to the bounding box of its floor cells.
         */
        fun parse(ascii: String): Level {
            val rows = ascii.trimIndent().lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
            val w0 = rows.maxOf { it.length }
            val h0 = rows.size
            fun ch(x: Int, y: Int) = rows[y].getOrElse(x) { '#' }
            var minX = w0; var minY = h0; var maxX = -1; var maxY = -1
            for (y in 0 until h0) for (x in 0 until w0) if (ch(x, y) != '#' && ch(x, y) != ' ') {
                minX = minOf(minX, x); maxX = maxOf(maxX, x); minY = minOf(minY, y); maxY = maxOf(maxY, y)
            }
            require(maxX >= 0) { "level has no floor" }
            val w = maxX - minX + 1
            val h = maxY - minY + 1
            val tiles = ByteArray(w * h)
            var start = -1
            val portalCells = LinkedHashMap<Char, MutableList<Int>>()
            for (y in 0 until h) for (x in 0 until w) {
                val c = ch(x + minX, y + minY)
                val i = y * w + x
                val t = when (c) {
                    '#', ' ' -> Tile.WALL
                    '.' -> Tile.FLOOR
                    'S' -> { start = i; Tile.FLOOR }
                    'o' -> Tile.STOPPER
                    '^' -> Tile.ONEWAY_UP
                    '>' -> Tile.ONEWAY_RIGHT
                    'v' -> Tile.ONEWAY_DOWN
                    '<' -> Tile.ONEWAY_LEFT
                    '/' -> Tile.DEFLECT_SLASH
                    '\\' -> Tile.DEFLECT_BACKSLASH
                    in 'P'..'R' -> { portalCells.getOrPut(c) { mutableListOf() }.add(i); Tile.PORTAL }
                    else -> error("unknown level char '$c'")
                }
                tiles[i] = t.toByte()
            }
            require(start >= 0) { "level has no start 'S'" }
            val portals = portalCells.values.flatMap {
                require(it.size == 2) { "portal letters must come in pairs" }
                it
            }.toIntArray()
            return Level(w, h, tiles, start, portals)
        }
    }
}
