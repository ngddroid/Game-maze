package com.ngstudio.maze.levelgen

import com.ngstudio.maze.core.Analyzer
import com.ngstudio.maze.core.Difficulty
import com.ngstudio.maze.core.GenParams
import com.ngstudio.maze.core.Generator
import com.ngstudio.maze.core.HandmadeLevels
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Solver
import kotlin.random.Random

/**
 * R&D helper:  ./gradlew :levelgen:run --args="explore <minPar> <maxPar> <w> <h> <steps> <count> [seed]"
 * Prints small valid candidates (ASCII + par + solution) to pick tutorial levels from,
 * and "check" prints the analysis of the hand-made levels.
 */
fun explore(args: List<String>) {
    if (args.firstOrNull() == "check") {
        HandmadeLevels.all().forEachIndexed { i, l -> describe("L${i + 1}", l) }
        return
    }
    val minPar = args[0].toInt(); val maxPar = args[1].toInt()
    val w = args[2].toInt(); val h = args[3].toInt(); val steps = args[4].toInt(); val count = args[5].toInt()
    val rng = Random(args.getOrNull(6)?.toLong() ?: 42L)
    var shown = 0
    val seen = HashSet<String>()
    var tries = 0
    while (shown < count && tries < 200_000) {
        tries++
        val c = Generator.attempt(rng, GenParams(w, h, steps, 1.0, maxW = w, maxH = h, minFloor = 6), null, 1L) ?: continue
        val l = c.level
        if (l.par !in minPar..maxPar) continue
        if (c.metrics.irreversible > 0.0) continue
        if (!seen.add(Generator.canonicalKey(l))) continue
        describe("cand ${shown + 1}", l)
        shown++
    }
}

fun describe(name: String, l: Level) {
    val a = Analyzer.analyze(l)
    val sol = Solver.solve(l, a)
    val m = if (sol != null) Difficulty.metrics(l, a, sol.moves.size) else null
    println("=== $name  ${l.width}x${l.height}  valid=${a.valid} ${Analyzer.problems(l)}")
    println(l.toAscii())
    println("par=${sol?.moves?.size} optimal=${sol?.optimal} solution=${sol?.moves?.joinToString("") { it.symbol.toString() }}  $m")
}
