package com.ngstudio.maze.levelgen

import com.ngstudio.maze.core.Analyzer
import com.ngstudio.maze.core.Difficulty
import com.ngstudio.maze.core.Generator
import com.ngstudio.maze.core.HandmadeLevels
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.LevelCodec
import com.ngstudio.maze.core.Mechanic
import com.ngstudio.maze.core.Physics
import com.ngstudio.maze.core.Solver
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.random.Random

/**
 *   ./gradlew :levelgen:run                    -> generate everything (deterministic, same output every run)
 *   ./gradlew :levelgen:run --args="sample 300"        -> quick sample of every 300th level (prints metrics, writes nothing)
 *   ./gradlew :levelgen:run --args="sample 5 11 80"    -> every 5th level from 11 to 80
 *   ./gradlew :levelgen:run --args="explore ..." -> R&D helper, see Explore.kt
 */
const val MASTER_SEED = 0x5EED_2026L
const val DAILY_COUNT = 366

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "explore" -> explore(args.drop(1))
        "sample" -> sample(args.getOrNull(1)?.toInt() ?: 250, args.getOrNull(2)?.toInt() ?: (Difficulty.HANDMADE + 1), args.getOrNull(3)?.toInt() ?: Difficulty.TOTAL_LEVELS)
        else -> generateAll()
    }
}

class GenResult(val level: Level, val target: Double, val attempts: Int, val salt: Int)

fun seedFor(n: Int, salt: Int, daily: Boolean = false): Long =
    MASTER_SEED * 31 + n * 1_000_003L + salt * 7_919L + (if (daily) 0x0DA1_1L else 0L)

/** Generates one level close to [target]. Deterministic for (n, salt, daily). */
fun generateLevel(n: Int, target: Double, salt: Int, daily: Boolean = false): GenResult {
    val rng = Random(seedFor(n, salt, daily))
    val intro = if (daily) null else Difficulty.introOf(n)
    val mechanics = if (daily) Difficulty.unlockedMechanics(400) else Difficulty.unlockedMechanics(n)
    // "Size factor" fed to the parameter mapping; nudged toward the target after every candidate.
    var s = ((target - 0.22) / 0.6).coerceIn(0.0, 1.0)
    var best: Generator.Candidate? = null
    var attempts = 0
    val tolerance = 0.02
    while (attempts < 400) {
        attempts++
        val p = Generator.params(s, rng, mechanics, intro)
        val c = Generator.attempt(rng, p, intro, seedFor(n, salt, daily) + attempts) ?: continue
        val err = c.metrics.score - target
        if (best == null || abs(err) < abs(best.metrics.score - target)) best = c
        if (abs(err) <= tolerance) break
        if (attempts >= 60 && abs(best.metrics.score - target) <= 0.05) break
        if (attempts >= 150 && abs(best.metrics.score - target) <= 0.08) break
        s = (s - err * 0.6).coerceIn(0.0, 1.0)
    }
    val level = best!!.level
    level.intro = intro
    return GenResult(level, target, attempts, salt)
}

fun handmade(): List<Level> = HandmadeLevels.all().map { l ->
    val a = Analyzer.analyze(l)
    check(a.valid) { "hand-made level invalid: ${Analyzer.problems(l)}" }
    val sol = Solver.solve(l, a, exactMaxGroups = 62)!!
    l.par = sol.moves.size
    l.solution = ByteArray(sol.moves.size) { sol.moves[it].ordinal.toByte() }
    l.difficulty = Difficulty.metrics(l, a, l.par).score.toFloat()
    l
}

fun generateAll() {
    val t0 = System.currentTimeMillis()
    val threads = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    val pool = Executors.newFixedThreadPool(threads)
    println("Generating ${Difficulty.TOTAL_LEVELS} levels + $DAILY_COUNT daily on $threads threads...")

    val levels = ArrayList<Level>(handmade())
    val targets = ArrayList<Double>(levels.map { it.difficulty.toDouble() })
    val attempts = ArrayList<Int>(levels.map { 0 })

    val ns = (Difficulty.HANDMADE + 1)..Difficulty.TOTAL_LEVELS
    val futures = ns.map { n -> pool.submit(Callable { generateLevel(n, Difficulty.target(n), 0) }) }
    val results = ArrayList<GenResult>()
    for ((k, f) in futures.withIndex()) {
        results += f.get()
        if ((k + 1) % 250 == 0) println("  ${k + 1 + Difficulty.HANDMADE} levels (${(System.currentTimeMillis() - t0) / 1000}s)")
    }

    // Daily pool: medium-hard, Monday easier .. Sunday harder.
    val dailyFutures = (0 until DAILY_COUNT).map { d ->
        val target = 0.42 + 0.035 * (d % 7) + 0.03 * Random(d.toLong()).nextDouble()
        pool.submit(Callable { generateLevel(d + 1, target, 0, daily = true) })
    }
    val dailyResults = dailyFutures.map { it.get() }.toMutableList()

    // Sequential duplicate removal (deterministic): regenerate with the next salt until unique.
    val seen = HashSet<String>()
    levels.forEach { seen.add(Generator.canonicalKey(it)) }
    var regenerated = 0
    for (i in results.indices) {
        var r = results[i]
        while (!seen.add(Generator.canonicalKey(r.level))) {
            r = generateLevel(Difficulty.HANDMADE + 1 + i, r.target, r.salt + 1); regenerated++
        }
        results[i] = r
    }
    for (i in dailyResults.indices) {
        var r = dailyResults[i]
        while (!seen.add(Generator.canonicalKey(r.level))) {
            r = generateLevel(i + 1, r.target, r.salt + 1, daily = true); regenerated++
        }
        dailyResults[i] = r
    }
    pool.shutdown()

    results.forEach { levels += it.level; targets += it.target; attempts += it.attempts }
    val daily = dailyResults.map { it.level }

    // Final proof for every level: valid (no traps, full coverage, no loops) and the stored solution paints all.
    (levels + daily).forEachIndexed { i, l ->
        check(Analyzer.analyze(l).valid) { "level ${i + 1} invalid" }
        check(Physics.isSolvedBy(l, l.solutionDirs())) { "level ${i + 1} stored solution fails" }
        check(l.par == l.solution.size)
    }

    val dir = File("app/src/main/assets/levels").apply { mkdirs() }
    File(dir, "levels.bin").writeBytes(LevelCodec.encode(levels))
    File(dir, "daily.bin").writeBytes(LevelCodec.encode(daily))
    File("docs").mkdirs()
    File("docs/levels_report.html").writeText(Report.html(levels, targets, attempts, daily))

    val secs = (System.currentTimeMillis() - t0) / 1000
    println("Done in ${secs}s. levels.bin=${File(dir, "levels.bin").length() / 1024} KB, daily.bin=${File(dir, "daily.bin").length() / 1024} KB, duplicates regenerated=$regenerated")
    printBuckets(levels, targets)
}

fun printBuckets(levels: List<Level>, targets: List<Double>) {
    println("bucket      avgTarget avgD   avgPar avgFloor  maxW x maxH  mechanics")
    for (b in levels.indices step 250) {
        val sub = levels.subList(b, minOf(levels.size, b + 250))
        val tSub = targets.subList(b, minOf(levels.size, b + 250))
        val mech = Mechanic.values().joinToString(" ") { m -> "${m.name.take(3)}=${sub.count { m in it.mechanics() }}" }
        println(
            "%4d-%-5d   %.3f    %.3f  %5.1f  %6.1f    %2d x %2d   %s".format(
                b + 1, b + sub.size, tSub.average(), sub.map { it.difficulty.toDouble() }.average(),
                sub.map { it.par }.average(), sub.map { it.floorCount }.average(),
                sub.maxOf { it.width }, sub.maxOf { it.height }, mech,
            )
        )
    }
}

/** Fast look at the curve without writing files. */
fun sample(step: Int, from: Int, to: Int) {
    val t0 = System.currentTimeMillis()
    for (n in from..to step step) {
        val t1 = System.currentTimeMillis()
        val r = generateLevel(n, Difficulty.target(n), 0)
        val l = r.level
        println(
            "L%-5d target=%.3f D=%.3f par=%-3d %2dx%-2d floor=%-3d att=%-3d %4dms %s".format(
                n, r.target, l.difficulty, l.par, l.width, l.height, l.floorCount, r.attempts,
                System.currentTimeMillis() - t1, l.mechanics().joinToString(",")
            )
        )
    }
    println("total ${(System.currentTimeMillis() - t0) / 1000}s")
}
