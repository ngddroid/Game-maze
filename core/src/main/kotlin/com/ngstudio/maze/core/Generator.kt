package com.ngstudio.maze.core

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Knobs for one generation attempt. */
class GenParams(
    val regionW: Int,
    val regionH: Int,
    val steps: Int,
    /** Chance a carved slide is made reversible (wall locked behind it). Lower = more one-way moves. */
    val reversibleChance: Double,
    val stoppers: Int = 0,
    val oneWays: Int = 0,
    val deflectors: Int = 0,
    val portalPairs: Int = 0,
    val maxW: Int = 12,
    val maxH: Int = 17,
    val minFloor: Int = 8,
)

/**
 * Builds levels by "reverse carving": start from solid wall, move a virtual ball with random slides,
 * carving the floor it passes and locking the wall cell that stops it. Locked walls are never carved,
 * so every carving slide stays valid and the carving walk itself paints the whole level.
 * Special tiles are then added by mutation, and every candidate must pass [Analyzer] (no traps,
 * full coverage, no loops) before it is scored.
 */
object Generator {

    fun carve(rng: Random, p: GenParams): Level? {
        val w = p.regionW
        val h = p.regionH
        val n = w * h
        val tiles = ByteArray(n)
        val locked = BooleanArray(n)
        val start = rng.nextInt(n)
        tiles[start] = Tile.FLOOR.toByte()
        var pos = start
        var last: Dir? = null
        var done = 0
        var attempts = 0
        while (done < p.steps && attempts < p.steps * 12) {
            attempts++
            val d = pickDir(rng, last)
            var maxLen = 0
            var x = pos % w + d.dx
            var y = pos / w + d.dy
            while (x in 0 until w && y in 0 until h && !locked[y * w + x]) { maxLen++; x += d.dx; y += d.dy }
            if (maxLen == 0) continue
            val len = pickLen(rng, maxLen)
            val stepOffset = d.dx + d.dy * w
            val end = pos + len * stepOffset
            val ax = end % w + d.dx
            val ay = end / w + d.dy
            val after = if (ax in 0 until w && ay in 0 until h) ay * w + ax else -1
            if (after >= 0 && tiles[after].toInt() != Tile.WALL) continue // ball would not stop at `end`
            for (k in 1..len) tiles[pos + k * stepOffset] = Tile.FLOOR.toByte()
            if (after >= 0) locked[after] = true
            if (rng.nextDouble() < p.reversibleChance) {
                val bx = pos % w - d.dx
                val by = pos / w - d.dy
                if (bx in 0 until w && by in 0 until h && tiles[by * w + bx].toInt() == Tile.WALL) locked[by * w + bx] = true
            }
            pos = end
            last = d
            done++
        }
        return crop(Level(w, h, tiles, start), p)
    }

    private fun pickDir(rng: Random, last: Dir?): Dir {
        if (last == null) return Dir.of(rng.nextInt(4))
        // Prefer turning 90 degrees: it makes corridors and corners instead of long straight runs.
        val r = rng.nextDouble()
        return when {
            r < 0.78 -> if (last.isHorizontal) (if (rng.nextBoolean()) Dir.UP else Dir.DOWN) else (if (rng.nextBoolean()) Dir.LEFT else Dir.RIGHT)
            r < 0.90 -> last.opposite
            else -> last
        }
    }

    private fun pickLen(rng: Random, maxLen: Int): Int {
        // Mostly 2..5 cells, sometimes the full run.
        val r = rng.nextDouble()
        val len = when {
            r < 0.12 -> 1
            r < 0.80 -> 2 + rng.nextInt(4)
            else -> maxLen
        }
        return len.coerceIn(1, maxLen)
    }

    /** Crops to the floor bounding box. Outside the grid counts as wall, so physics is unchanged. */
    fun crop(l: Level, p: GenParams): Level? {
        var minX = l.width; var minY = l.height; var maxX = -1; var maxY = -1
        for (i in 0 until l.size) if (l.isFloor(i)) {
            minX = minOf(minX, l.x(i)); maxX = maxOf(maxX, l.x(i)); minY = minOf(minY, l.y(i)); maxY = maxOf(maxY, l.y(i))
        }
        if (maxX < 0) return null
        val w = maxX - minX + 1
        val h = maxY - minY + 1
        if (w > p.maxW || h > p.maxH || w < 3 || h < 3) return null
        val tiles = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) tiles[y * w + x] = l.tiles[(y + minY) * l.width + x + minX]
        val start = (l.y(l.start) - minY) * w + (l.x(l.start) - minX)
        return Level(w, h, tiles, start)
    }

    // ------------------------------------------------------------------ special tiles

    /** Tries to add [count] tiles of a mechanic; each addition must keep the level valid and get used. */
    fun addSpecials(rng: Random, level: Level, mechanic: Mechanic, count: Int, tries: Int = 30): Level {
        var cur = level
        repeat(count) {
            for (t in 0 until tries) {
                val cand = mutate(rng, cur, mechanic) ?: continue
                if (Analyzer.analyze(cand).valid) { cur = cand; break }
            }
        }
        return cur
    }

    private fun mutate(rng: Random, l: Level, m: Mechanic): Level? {
        val floor = l.floorCells().filter { it != l.start && l.tile(it) == Tile.FLOOR }
        if (floor.isEmpty()) return null
        fun open(i: Int, d: Dir): Boolean { val j = l.step(i, d); return j >= 0 && l.isFloor(j) }
        val c = l.copy()
        when (m) {
            Mechanic.STOPPER -> {
                // In the middle of a straight run so it changes where the ball stops.
                val cands = floor.filter { (open(it, Dir.LEFT) && open(it, Dir.RIGHT)) || (open(it, Dir.UP) && open(it, Dir.DOWN)) }
                if (cands.isEmpty()) return null
                c.tiles[cands[rng.nextInt(cands.size)]] = Tile.STOPPER.toByte()
            }
            Mechanic.ONE_WAY -> {
                val cands = floor.filter { (open(it, Dir.LEFT) && open(it, Dir.RIGHT) && !open(it, Dir.UP) && !open(it, Dir.DOWN)) ||
                    (open(it, Dir.UP) && open(it, Dir.DOWN) && !open(it, Dir.LEFT) && !open(it, Dir.RIGHT)) }
                if (cands.isEmpty()) return null
                val cell = cands[rng.nextInt(cands.size)]
                val horizontal = open(cell, Dir.LEFT)
                val d = if (horizontal) (if (rng.nextBoolean()) Dir.LEFT else Dir.RIGHT) else (if (rng.nextBoolean()) Dir.UP else Dir.DOWN)
                c.tiles[cell] = Tile.oneWay(d).toByte()
            }
            Mechanic.DEFLECTOR -> {
                // Corner cells: exactly two open neighbours at 90 degrees. The deflector carries the ball round the corner.
                val cands = floor.mapNotNull { i ->
                    val o = Dir.VALUES.filter { open(i, it) }
                    if (o.size != 2 || o[0].isHorizontal == o[1].isHorizontal) null else i to o
                }
                if (cands.isEmpty()) return null
                val (cell, o) = cands[rng.nextInt(cands.size)]
                val set = o.toSet()
                val backslash = set == setOf(Dir.LEFT, Dir.DOWN) || set == setOf(Dir.RIGHT, Dir.UP)
                c.tiles[cell] = (if (backslash) Tile.DEFLECT_BACKSLASH else Tile.DEFLECT_SLASH).toByte()
            }
            Mechanic.PORTAL -> {
                if (l.portals.size >= 4) return null
                val a = floor[rng.nextInt(floor.size)]
                val far = floor.filter { abs(l.x(it) - l.x(a)) + abs(l.y(it) - l.y(a)) >= 4 }
                if (far.isEmpty()) return null
                val b = far[rng.nextInt(far.size)]
                c.tiles[a] = Tile.PORTAL.toByte(); c.tiles[b] = Tile.PORTAL.toByte()
                return Level(c.width, c.height, c.tiles, c.start, l.portals + intArrayOf(a, b))
            }
        }
        return c
    }

    // ------------------------------------------------------------------ one finished, scored candidate

    class Candidate(val level: Level, val metrics: Metrics)

    fun params(target: Double, rng: Random, mechanics: List<Mechanic>, intro: Mechanic?): GenParams {
        val t = target.coerceIn(0.0, 1.0)
        val jitter = { a: Double -> (rng.nextDouble() * 2 - 1) * a }
        val regionW = (5 + 7.5 * Math.pow(t, 0.75) + jitter(1.2)).roundToInt().coerceIn(4, 12)
        val regionH = (6 + 11.5 * Math.pow(t, 0.75) + jitter(1.5)).roundToInt().coerceIn(5, 17)
        val steps = (8 + 52 * t + jitter(3.0)).roundToInt().coerceAtLeast(3)
        val reversible = (0.88 - 0.58 * t).coerceIn(0.28, 0.95)
        fun amount(m: Mechanic, max: Int): Int = when {
            m !in mechanics -> 0
            intro == m -> 1
            intro != null -> 0
            else -> if (rng.nextDouble() < 0.18 + 0.4 * t) 1 + rng.nextInt(max) else 0
        }
        return GenParams(
            regionW, regionH, steps, reversible,
            stoppers = amount(Mechanic.STOPPER, 3),
            oneWays = amount(Mechanic.ONE_WAY, 3),
            deflectors = amount(Mechanic.DEFLECTOR, 3),
            portalPairs = amount(Mechanic.PORTAL, 1),
        )
    }

    /** One attempt: carve, add specials, validate, solve, score. Null when anything fails. */
    fun attempt(rng: Random, p: GenParams, required: Mechanic?, solveSeed: Long): Candidate? {
        var level = carve(rng, p) ?: return null
        if (level.floorCount < p.minFloor) return null
        if (!looksGood(level)) return null
        if (!Analyzer.analyze(level).valid) return null
        if (p.stoppers > 0) level = addSpecials(rng, level, Mechanic.STOPPER, p.stoppers)
        if (p.oneWays > 0) level = addSpecials(rng, level, Mechanic.ONE_WAY, p.oneWays)
        if (p.deflectors > 0) level = addSpecials(rng, level, Mechanic.DEFLECTOR, p.deflectors)
        if (p.portalPairs > 0) level = addSpecials(rng, level, Mechanic.PORTAL, p.portalPairs)
        if (required != null && required !in level.mechanics()) return null
        val a = Analyzer.analyze(level)
        if (!a.valid) return null
        val sol = Solver.solve(level, a, solveSeed) ?: return null
        if (!Physics.isSolvedBy(level, sol.moves)) return null
        level.par = sol.moves.size
        level.solution = ByteArray(sol.moves.size) { sol.moves[it].ordinal.toByte() }
        val m = Difficulty.metrics(level, a, level.par, solveSeed)
        level.difficulty = m.score.toFloat()
        return Candidate(level, m)
    }

    /**
     * Visual quality gate: the maze should fill its board (floor density >= 38%), have no huge solid
     * wall blob (no 5x5 all-wall square), and be roughly portrait-friendly (not a thin strip).
     */
    fun looksGood(l: Level): Boolean {
        if (l.size >= 16 && l.floorCount < l.size * 0.38) return false
        if (l.width >= 6 && l.height >= 6 && l.height * 3 < l.width * 2) return false
        // Largest all-wall square via DP.
        val dp = IntArray(l.size)
        for (y in 0 until l.height) for (x in 0 until l.width) {
            val i = l.idx(x, y)
            if (l.isFloor(i)) continue
            dp[i] = if (x == 0 || y == 0) 1 else 1 + minOf(dp[i - 1], dp[i - l.width], dp[i - l.width - 1])
            if (dp[i] >= 5) return false
        }
        return true
    }

    /** Canonical shape hash over the 8 rotations/mirrors, used to reject duplicate layouts. */
    fun canonicalKey(l: Level): String {
        var best: String? = null
        for (t in 0 until 8) {
            val sb = StringBuilder()
            val (w, h) = if (t and 1 == 1) l.height to l.width else l.width to l.height
            sb.append(w).append('x').append(h).append(':')
            for (y in 0 until h) for (x in 0 until w) {
                // Map (x, y) in the transformed grid back to the source grid.
                var sx: Int; var sy: Int
                if (t and 1 == 1) { sx = y; sy = x } else { sx = x; sy = y }
                if (t and 2 == 2) sx = l.width - 1 - sx
                if (t and 4 == 4) sy = l.height - 1 - sy
                val i = sy * l.width + sx
                val tile = l.tile(i)
                sb.append(
                    when {
                        i == l.start -> 'S'
                        tile == Tile.WALL -> '#'
                        tile == Tile.FLOOR -> '.'
                        Tile.isOneWay(tile) -> '>'
                        Tile.isDeflector(tile) -> '/'
                        else -> ('0' + tile)
                    }
                )
            }
            val s = sb.toString()
            if (best == null || s < best) best = s
        }
        return best!!
    }
}
