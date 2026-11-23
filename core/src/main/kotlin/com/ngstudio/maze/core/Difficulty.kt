package com.ngstudio.maze.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

class Metrics(
    val floor: Int,
    val nodes: Int,
    val edges: Int,
    val par: Int,
    val irreversible: Double,
    val branching: Double,
    val specials: Int,
    /** Average moves a random swiper needs divided by par (how confusing the maze is). */
    val confusion: Double,
    val score: Double,
) {
    override fun toString() =
        "floor=$floor nodes=$nodes par=$par irr=${"%.2f".format(irreversible)} br=${"%.2f".format(branching)} " +
            "sp=$specials conf=${"%.1f".format(confusion)} D=${"%.3f".format(score)}"
}

object Difficulty {

    /** Hand-made tutorial levels. Generated levels start after these. */
    const val HANDMADE = 10
    const val TOTAL_LEVELS = 5000

    /** Level (1-based) where each mechanic first appears; that level and the next 2 are gentle intros. */
    val INTRO_LEVEL = mapOf(
        Mechanic.STOPPER to 21,
        Mechanic.ONE_WAY to 41,
        Mechanic.DEFLECTOR to 71,
        Mechanic.PORTAL to 111,
    )

    /**
     * Hand-made level 10 scores about 0.25. Generated levels jump well above it (players found the old curve
     * too easy) and reach the hardest band by about level 2000, then stay there with variety.
     */
    const val CURVE_START = 0.52
    const val CURVE_RANGE = 0.33
    private const val CURVE_FULL_AT = 2000

    /** Small sawtooth inside each block of 10: gentle opener, build-up, boss on every 10th level. */
    private val BLOCK_MOD = doubleArrayOf(-0.05, -0.03, -0.015, 0.0, 0.01, 0.02, -0.02, 0.025, 0.04, 0.07)

    /** Smooth 0..1 base curve: rises quickly at first, then keeps climbing slowly to level 5000. */
    fun base(n: Int): Double {
        val m = (n - HANDMADE).coerceAtLeast(0).toDouble()
        val early = (1 - exp(-3.6 * (m / (CURVE_FULL_AT - HANDMADE)).coerceAtMost(1.0))) / (1 - exp(-3.6))
        val late = m / (TOTAL_LEVELS - HANDMADE)
        // Fast climb to level 2000, then a slow but steady rise to level 5000 (never a flat plateau).
        return 0.92 * early + 0.08 * late
    }

    /** Target difficulty for level n (1-based, n > 10). Sawtooth inside each block of 10; every 10th is a boss. */
    fun target(n: Int): Double {
        val b = base(n)
        var m = BLOCK_MOD[(n - 1) % 10]
        if (introOf(n) != null) m = minOf(m, -0.04) // a brand-new tile: keep the level gentle
        return (CURVE_START + CURVE_RANGE * b + m * (0.5 + 0.5 * b)).coerceIn(0.0, 1.0)
    }

    /**
     * Moves allowed on campaign level n (null = unlimited, for the tutorial levels 1-10). The allowance above par
     * shrinks as the game goes on. Always >= par, so the stored (verified) solution always fits.
     */
    fun moveLimit(n: Int, par: Int): Int? = if (n <= HANDMADE) null else par + slack(n)

    fun slack(n: Int): Int = when {
        n <= 30 -> 5
        n <= 100 -> 4
        n <= 300 -> 3
        else -> 2
    }

    /** Daily Challenge allowance. */
    fun dailyMoveLimit(par: Int): Int = par + 3

    /** Mechanic introduced at level n (that level or the 2 after it), if any. */
    fun introOf(n: Int): Mechanic? = INTRO_LEVEL.entries.firstOrNull { n in it.value..it.value + 2 }?.key

    fun unlockedMechanics(n: Int): List<Mechanic> = INTRO_LEVEL.filter { n >= it.value }.keys.toList()

    fun metrics(level: Level, a: Analysis, par: Int, seed: Long = 7L, runs: Int = 48): Metrics {
        val conf = confusion(a, par, Random(seed), runs)
        val floor = level.floorCount
        val irr = a.irreversibleRatio()
        val br = a.branching()
        val sp = level.specialCount()
        val sFloor = ((floor - 6) / 110.0).coerceIn(0.0, 1.0)
        val sPar = ((par - 1) / 34.0).coerceIn(0.0, 1.0)
        val sConf = (ln(conf.coerceAtLeast(1.0)) / ln(40.0)).coerceIn(0.0, 1.0)
        val sIrr = (irr / 0.45).coerceIn(0.0, 1.0)
        val sBr = ((br - 1.5) / 2.0).coerceIn(0.0, 1.0)
        val sSp = (sp / 8.0).coerceIn(0.0, 1.0)
        // Puzzle difficulty is mostly "how confusing" and "how costly a wrong move is", not just size.
        val score = 0.26 * sPar + 0.14 * sFloor + 0.30 * sConf + 0.18 * sIrr + 0.04 * sBr + 0.08 * sSp
        return Metrics(floor, a.nodeCount, a.edgeCount, par, irr, br, sp, conf, score)
    }

    /** Random swiper: picks any moving swipe uniformly until everything is painted (capped). */
    fun confusion(a: Analysis, par: Int, rng: Random, runs: Int): Double {
        if (par <= 0) return 1.0
        val level = a.level
        val floorTotal = level.floorCount
        val cap = par * 60 + 200
        var total = 0L
        val painted = BooleanArray(level.size)
        val options = IntArray(4)
        repeat(runs) {
            painted.fill(false)
            painted[level.start] = true
            var count = 1
            var node = 0
            var steps = 0
            while (count < floorTotal && steps < cap) {
                var k = 0
                for (d in 0 until 4) if (a.edges[node][d] != null) options[k++] = d
                val e = a.edges[node][options[rng.nextInt(k)]]!!
                for (c in e.cells) if (!painted[c]) { painted[c] = true; count++ }
                node = e.to
                steps++
            }
            total += steps
        }
        return total.toDouble() / runs / par
    }
}
