package com.ngstudio.maze.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class PhysicsTest {
    @Test fun slidesUntilWallAndPaintsPath() {
        val l = Level.parse("S...#.")
        val s = Physics.slide(l, l.start, Dir.RIGHT)!!
        assertEquals(3, s.end)
        assertEquals(listOf(1, 2, 3), s.cells.toList())
        assertNull(Physics.slide(l, l.start, Dir.LEFT))
        assertNull(Physics.slide(l, l.start, Dir.UP))
    }

    @Test fun stopperStopsBall() {
        val l = Level.parse("S.o..")
        assertEquals(2, Physics.slide(l, l.start, Dir.RIGHT)!!.end)
    }

    @Test fun oneWayGateBlocksWrongDirection() {
        val l = Level.parse("S.>..")
        assertEquals(4, Physics.slide(l, l.start, Dir.RIGHT)!!.end)
        val back = Level.parse("..<.S")
        // Moving LEFT through a '<' gate is allowed.
        assertEquals(0, Physics.slide(back, back.start, Dir.LEFT)!!.end)
        val blocked = Level.parse("S.<..")
        assertEquals(1, Physics.slide(blocked, blocked.start, Dir.RIGHT)!!.end)
    }

    @Test fun deflectorTurnsBall() {
        // Ball moves right, hits '\' at (2,0) and goes down to (2,2).
        val l = Level.parse(
            """
            S.\
            ##.
            ##.
            """
        )
        val s = Physics.slide(l, l.start, Dir.RIGHT)!!
        assertEquals(l.idx(2, 2), s.end)
    }

    @Test fun portalTeleportsAndKeepsDirection() {
        val l = Level.parse(
            """
            S.P#
            ####
            P..#
            """
        )
        val s = Physics.slide(l, l.start, Dir.RIGHT)!!
        assertEquals(l.idx(2, 2), s.end)
        assertTrue(s.cells.contains(l.idx(0, 2)))
    }

    @Test fun loopIsDetected() {
        // Two deflectors bouncing the ball between portals forever.
        val l = Level.parse(
            """
            S/.\
            .\./
            """
        )
        val anyLoop = Dir.VALUES.any { d -> Physics.slide(l, l.start, d)?.loops == true } ||
            Analyzer.analyze(l).loops
        assertTrue(anyLoop)
    }
}

class ValidatorTest {
    @Test fun handmadeLevelsAreValidAndRising() {
        val levels = HandmadeLevels.all()
        assertEquals(10, levels.size)
        var last = -1.0
        levels.forEachIndexed { i, l ->
            val a = Analyzer.analyze(l)
            assertTrue("L${i + 1}: ${Analyzer.problems(l)}", a.valid)
            assertEquals("L${i + 1} must have no one-way moves", 0.0, a.irreversibleRatio(), 0.0)
            val sol = Solver.solve(l, a, exactMaxGroups = 62)!!
            assertTrue(sol.optimal)
            assertTrue(Physics.isSolvedBy(l, sol.moves))
            val d = Difficulty.metrics(l, a, sol.moves.size).score
            assertTrue("L${i + 1} difficulty $d should be >= $last", d >= last)
            last = d
        }
    }

    @Test fun unpaintableCellIsRejected() {
        // The ball never stops at the junction, so the arm above it can never be painted.
        val l = Level.parse(
            """
            ##.##
            S....
            """
        )
        val a = Analyzer.analyze(l)
        assertFalse(a.valid)
        assertTrue(a.uncoveredCount > 0)
    }

    @Test fun trapIsRejected() {
        // Going down through the one-way gate leaves no way back up to the start: not strongly connected.
        val l = Level.parse(
            """
            S#
            v#
            ..
            """
        )
        val a = Analyzer.analyze(l)
        assertEquals(0, a.uncoveredCount)
        assertFalse(a.stronglyConnected)
        assertFalse(a.valid)
    }
}

class CodecAndGeneratorTest {
    @Test fun codecRoundTrip() {
        val levels = HandmadeLevels.all().map { l ->
            val sol = Solver.solve(l, exactMaxGroups = 62)!!
            l.par = sol.moves.size
            l.solution = ByteArray(sol.moves.size) { sol.moves[it].ordinal.toByte() }
            l.difficulty = 0.5f
            l
        } + Level.parse("S.P#o\n####.\nP..>.").also { it.intro = Mechanic.PORTAL }
        val pack = LevelCodec.Pack(LevelCodec.encode(levels))
        assertEquals(levels.size, pack.count)
        levels.forEachIndexed { i, l ->
            val r = pack.level(i)
            assertEquals(l.toAscii(), r.toAscii())
            assertEquals(l.par, r.par)
            assertEquals(l.solution.toList(), r.solution.toList())
            assertEquals(l.intro, r.intro)
            assertEquals(l.portals.toList(), r.portals.toList())
        }
    }

    @Test fun generatorIsDeterministic() {
        var a: Generator.Candidate? = null
        var b: Generator.Candidate? = null
        val r1 = Random(1234); val r2 = Random(1234)
        repeat(200) {
            if (a == null) a = Generator.attempt(r1, Generator.params(0.5, r1, Mechanic.values().toList(), null), null, 3L)
            if (b == null) b = Generator.attempt(r2, Generator.params(0.5, r2, Mechanic.values().toList(), null), null, 3L)
        }
        assertNotNull(a)
        assertEquals(a!!.level.toAscii(), b!!.level.toAscii())
        assertEquals(a!!.level.par, b!!.level.par)
    }
}

/** The "all levels are perfect" proof for the shipped asset files. */
class ShippedLevelsTest {
    private fun pack(name: String): LevelCodec.Pack {
        val dir = System.getProperty("levelsDir") ?: "../app/src/main/assets/levels"
        return LevelCodec.Pack(File(dir, name).readBytes())
    }

    private fun verify(pack: LevelCodec.Pack, label: String) {
        for (i in 0 until pack.count) {
            val l = pack.level(i)
            val a = Analyzer.analyze(l)
            assertTrue("$label ${i + 1}: ${Analyzer.problems(l)}", a.valid)
            assertTrue("$label ${i + 1}: stored solution must paint every tile", Physics.isSolvedBy(l, l.solutionDirs()))
            assertEquals(l.par, l.solution.size)
            assertTrue("$label ${i + 1} too big for the screen", l.width <= 12 && l.height <= 17)
        }
    }

    @Test fun all5000LevelsArePerfect() {
        val p = pack("levels.bin")
        assertEquals(5000, p.count)
        verify(p, "Level")
        // Every level's move limit leaves room for its verified solution; levels 1-10 are unlimited.
        for (i in 0 until p.count) {
            val l = p.level(i)
            val lim = Difficulty.moveLimit(i + 1, l.par)
            if (i < 10) assertEquals(null, lim) else assertTrue("level ${i + 1}: limit $lim < par ${l.par}", lim != null && lim >= l.par && lim <= l.par + 5)
        }
        // Hand-made first 10 are the tutorial set.
        val hand = HandmadeLevels.all()
        for (i in 0 until 10) assertEquals(hand[i].toAscii(), p.level(i).toAscii())
        // Progressive difficulty after level 10: every 250-level bucket is at least as hard as the one before,
        // and no 50-level stretch is noticeably easier than anything before it.
        val d = DoubleArray(5000) { p.level(it).difficulty.toDouble() }
        var prevBucket = 0.0
        for (b in 10 until 5000 step 250) {
            val avg = (b until minOf(5000, b + 250)).map { d[it] }.average()
            assertTrue("250-level average fell at level ${b + 1}: $avg < $prevBucket", avg >= prevBucket - 0.005)
            prevBucket = avg
        }
        var best = 0.0
        for (b in 10 until 5000 step 50) {
            val avg = (b until minOf(5000, b + 50)).map { d[it] }.average()
            assertTrue("difficulty fell at level ${b + 1}: $avg < $best", avg >= best - 0.02)
            best = maxOf(best, avg)
        }
        // Level 11 onward is never easier than level 10 on average.
        assertTrue((10 until 20).map { p.level(it).difficulty }.average() > p.level(9).difficulty)
    }

    @Test fun dailyLevelsArePerfect() {
        val p = pack("daily.bin")
        assertEquals(366, p.count)
        verify(p, "Daily")
    }
}
