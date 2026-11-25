package com.ngstudio.maze.tools

import com.ngstudio.maze.tools.MaterialArt.fbm
import com.ngstudio.maze.tools.MaterialArt.fract
import com.ngstudio.maze.tools.MaterialArt.hash
import com.ngstudio.maze.tools.MaterialArt.mix
import com.ngstudio.maze.tools.MaterialArt.noise
import com.ngstudio.maze.tools.MaterialArt.rgb
import com.ngstudio.maze.tools.MaterialArt.smooth
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Chapter backgrounds: each chapter (50 levels) is its own scene. Chapter 1 is the Golden Maple wood
 * (surface_0.jpg); chapters 2..10 are painted here as backdrop_1..9.jpg. The corner decorations
 * (palms, snow, flowers, moon, ...) are drawn in code by the app (ui/components/ThemeDecor.kt).
 * Keep the order in sync with render/Visuals.kt Backdrop.ALL.
 *
 *   ./gradlew :tools:run --args="backdrops"
 */
private typealias Px = (x: Double, y: Double, w: Double, h: Double) -> DoubleArray

object ThemeArt {
    val THEMES: List<Pair<String, Px>> = listOf(
        "Sunny Beach" to ::beach,
        "Snowy Peaks" to ::snow,
        "Flower Meadow" to ::meadow,
        "Starry Night" to ::night,
        "Candy Land" to ::candy,
        "Desert Dunes" to ::desert,
        "Under the Sea" to ::sea,
        "Sakura Garden" to ::sakura,
        "Cloud Kingdom" to ::clouds,
    )

    fun run(outDir: File, width: Int = 900, height: Int = 1950) {
        outDir.mkdirs()
        val jobs = THEMES.withIndex().map { (i, t) ->
            Thread { MaterialArt.renderJpg(File(outDir, "backdrop_${i + 1}.jpg"), width, height, 0.88f) { x, y -> t.second(x, y, width.toDouble(), height.toDouble()) } }
        }
        jobs.forEach { it.start() }; jobs.forEach { it.join() }
        println("  chapters 2..10: ${THEMES.joinToString { it.first }}")
    }

    // ------------------------------------------------------------------------------------------ helpers
    private fun add(c: DoubleArray, k: Double) = DoubleArray(3) { c[it] + k }
    private fun mul(c: DoubleArray, k: Double) = DoubleArray(3) { c[it] * k }
    private fun vignette(c: DoubleArray, u: Double, v: Double, amount: Double = 0.14): DoubleArray {
        val vx = u - 0.5; val vy = (v - 0.45) * 1.1
        return mul(c, 1.0 - amount + amount * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy)))
    }
    private fun vgrad(top: Int, bottom: Int, v: Double) = mix(rgb(top), rgb(bottom), v.coerceIn(0.0, 1.0))
    /** Distance from (px,py) to the segment a-b. */
    private fun segDist(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax; val dy = by - ay
        val t = (((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        val ex = px - ax - dx * t; val ey = py - ay - dy * t
        return sqrt(ex * ex + ey * ey)
    }
    /** Edges of a Voronoi cell pattern (1 on the edges), used for caustics. */
    private fun cellEdges(px: Double, py: Double, seed: Double): Double {
        val gx = floor(px); val gy = floor(py); val fx = px - gx; val fy = py - gy
        var d1 = 8.0; var d2 = 8.0
        for (j in -1..1) for (i in -1..1) {
            val rx = i + hash(gx + i + seed, gy + j) - fx
            val ry = j + hash(gx + i + 19.1 + seed, gy + j + 19.1) - fy
            val d = rx * rx + ry * ry
            if (d < d1) { d2 = d1; d1 = d } else if (d < d2) d2 = d
        }
        return sqrt(d2) - sqrt(d1)
    }

    // ------------------------------------------------------------------------------------------ 2 Sunny Beach
    private fun beach(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = mix(rgb(0xFBE7BE), rgb(0xF1D29A), fbm(px * 0.6, py * 0.6))
        // wind ripples in the sand
        val r = sin(py * 7.5 + px * 0.9 + fbm(px * 0.7, py * 0.7) * 5.0)
        c = mul(c, 1.0 + 0.045 * r)
        c = mix(c, rgb(0xE2BC7E), 0.35 * smooth(0.82, 1.0, r))
        // grains and tiny shell fragments
        val g = noise(x * 0.9, y * 0.9)
        c = mul(c, 0.95 + 0.08 * g)
        val cx = floor(x / 26); val cy = floor(y / 26)
        if (hash(cx, cy) > 0.93) {
            val ox = cx * 26 + 6 + hash(cx + 1.1, cy) * 14; val oy = cy * 26 + 6 + hash(cx, cy + 2.3) * 14
            val d = sqrt((x - ox) * (x - ox) + (y - oy) * (y - oy))
            val tint = if (hash(cx + 5.5, cy) > 0.5) rgb(0xFFFFFF) else rgb(0xF6B9A0)
            c = mix(c, tint, (1 - smooth(1.2, 2.6, d)) * 0.7)
        }
        // the sea washing in at the top: turquoise water, then a lacy foam line, then wet sand
        val shore = 0.115 + 0.022 * sin(u * 7.0 + 0.6) + 0.012 * sin(u * 17.0 + 2.0) + 0.02 * (fbm(u * 3.0, 1.3) - 0.5)
        val wet = smooth(shore + 0.06, shore, v)
        c = mix(c, mul(c, 0.86), wet * 0.6)
        val water = smooth(shore - 0.004, shore - 0.012, v)
        val deep = mix(rgb(0x4FD0D8), rgb(0x1FA4C6), smooth(shore, 0.0, v))
        val sparkle = smooth(0.78, 0.95, noise(x * 0.06, y * 0.25)) * 0.25
        c = mix(c, add(deep, sparkle), water)
        val foam = 1 - smooth(0.0, 0.012, abs(v - shore + 0.006 + 0.004 * sin(u * 60.0)))
        c = mix(c, rgb(0xFFFFFF), foam * (0.7 + 0.3 * noise(x * 0.08, y * 0.08)))
        return vignette(c, u, v, 0.10)
    }

    // ------------------------------------------------------------------------------------------ 3 Snowy Peaks
    private fun snow(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0xBFE2F8, 0xEEF8FF, v * 1.2)
        // two ranges of sharp peaks: blue-grey rock with white snow caps, lit from the left
        fun peaks(k: Double, ph: Double, amp: Double): Pair<Double, Double> {
            val t = fract(u * k + ph)
            val tri = 1 - abs(t * 2 - 1)
            return Pair(amp * (tri + 0.12 * (fbm(u * 9 + ph, ph) - 0.5)), if (t < 0.5) 1.0 else -1.0)
        }
        for ((base, k, ph, amp, rock) in listOf(Five(0.40, 1.6, 0.15, 0.16, 0xA9C6E4), Five(0.52, 2.2, 0.55, 0.14, 0x8FB2D6))) {
            val (pk, side) = peaks(k, ph, amp)
            val top = base - pk
            if (v > top) {
                val depth = v - top
                var m = mix(rgb(rock), rgb(0xC6DCF0), smooth(0.0, 0.25, depth))
                // lit / shaded faces meet along a ridge that runs down from the peak and fades out
                val ridge = (fract(u * k + ph) - 0.5) / (k * 0.5)
                val faceT = smooth(-0.015, 0.015, ridge + depth * 0.35 * (if (ridge > 0) -0.2 else 0.2))
                m = mul(m, 1.0 + (0.06 - 0.13 * faceT) * (1 - smooth(0.0, 0.09, depth)))
                val capLine = 0.045 + 0.02 * fbm(u * 20 + ph, v * 10)
                m = mix(m, mul(rgb(0xFFFFFF), 1.0 - 0.08 * faceT), 1 - smooth(capLine, capLine + 0.01, depth))
                c = m
            }
        }
        // soft snow drifts lower down
        val d1 = 0.70 + 0.03 * sin(u * 6.0 + 0.5) + 0.02 * fbm(u * 4, 7.0)
        if (v > d1) c = mix(rgb(0xFFFFFF), rgb(0xD6E9F7), smooth(d1, d1 + 0.18, v) * 0.8)
        // frosty haze and sparkles
        c = mix(c, rgb(0xFFFFFF), 0.25 * smooth(0.5, 0.8, fbm(px * 0.8, py * 0.5)))
        val sx = floor(x / 18); val sy = floor(y / 18)
        if (hash(sx, sy) > 0.94) {
            val ox = sx * 18 + 9; val oy = sy * 18 + 9
            val d = max(abs(x - ox), abs(y - oy)) + min(abs(x - ox), abs(y - oy)) * 3
            c = mix(c, rgb(0xFFFFFF), (1 - smooth(0.5, 4.0, d)) * 0.9)
        }
        return vignette(c, u, v, 0.08)
    }

    // ------------------------------------------------------------------------------------------ 4 Flower Meadow
    private fun meadow(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0xD8F0A6, 0xA9D774, v)
        c = mix(c, rgb(0xC2E58E), 0.5 * smooth(0.45, 0.7, fbm(px * 0.7, py * 0.7)))
        c = mix(c, rgb(0x8FC75E), 0.35 * smooth(0.55, 0.8, fbm(px * 1.4 + 9, py * 1.4)))
        // grass blades: short vertical strokes
        val blade = noise(x * 0.20, y * 0.035) * 0.6 + noise(x * 0.5, y * 0.08) * 0.4
        c = mul(c, 0.92 + 0.12 * blade)
        // scattered little flowers
        val cs = 44.0
        val cx = floor(x / cs); val cy = floor(y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy) < 0.72) continue
            val ox = (hx + 0.2 + hash(hx + 3.3, hy) * 0.6) * cs; val oy = (hy + 0.2 + hash(hx, hy + 7.1) * 0.6) * cs
            val dx = x - ox; val dy = y - oy
            val d = sqrt(dx * dx + dy * dy)
            if (d > 9) continue
            val a = kotlin.math.atan2(dy, dx) + hash(hx, hy + 1.7) * 6.0
            val petal = 5.2 + 1.8 * cos(a * 5)
            val col = when ((hash(hx + 9.9, hy) * 4).toInt()) { 0 -> 0xFFFFFF; 1 -> 0xFFE45C; 2 -> 0xFF9EC2; else -> 0xC9B6FF }
            c = mix(c, rgb(col), (1 - smooth(petal - 0.8, petal + 0.6, d)) * 0.9)
            c = mix(c, rgb(if (col == 0xFFE45C) 0xF08C1E else 0xFFC93C), 1 - smooth(1.6, 2.4, d))
        }
        // sunny glow at the top
        c = add(c, 0.10 * exp(-((u - 0.3) * (u - 0.3) * 3 + v * v * 8)))
        return vignette(c, u, v, 0.12)
    }

    // ------------------------------------------------------------------------------------------ 5 Starry Night
    private fun night(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0x121845, 0x3A2468, v)
        // nebula clouds and a soft milky-way band
        val wx = px + (fbm(px * 0.5, py * 0.5) - 0.5) * 1.6
        val wy = py + (fbm(px * 0.5 + 5, py * 0.5 + 2) - 0.5) * 1.6
        c = mix(c, rgb(0x6A3FB0), 0.40 * smooth(0.45, 0.75, fbm(wx * 0.8, wy * 0.8)))
        c = mix(c, rgb(0x2D78B8), 0.28 * smooth(0.5, 0.8, fbm(wx * 0.9 + 7, wy * 0.9)))
        val band = exp(-((u * 0.9 + v * 0.45 - 0.62) * (u * 0.9 + v * 0.45 - 0.62)) * 40.0)
        c = add(c, 0.10 * band * fbm(px * 2.0, py * 2.0))
        // stars at three sizes
        for ((cell, prob, rad, bright) in listOf(Quad(9.0, 0.82, 0.8, 0.7), Quad(23.0, 0.80, 1.3, 0.9), Quad(70.0, 0.75, 2.0, 1.0))) {
            val cx = floor(x / cell); val cy = floor(y / cell)
            if (hash(cx + cell, cy) < prob) continue
            val ox = (cx + 0.2 + hash(cx, cy + cell) * 0.6) * cell; val oy = (cy + 0.2 + hash(cx + 2.2, cy) * 0.6) * cell
            val dx = abs(x - ox); val dy = abs(y - oy)
            val d = sqrt(dx * dx + dy * dy)
            val tint = if (hash(cx, cy + 4.4) > 0.7) rgb(0xFFE7A8) else rgb(0xEAF2FF)
            c = mix(c, tint, (1 - smooth(rad * 0.5, rad * 1.6, d)) * bright)
            if (cell > 50) {
                val flare = (1 - smooth(0.0, 1.2, min(dx, dy))) * (1 - smooth(rad, rad * 6, max(dx, dy)))
                c = mix(c, tint, flare * 0.7)
                c = add(c, 0.10 * exp(-d * d / (rad * rad * 18)))
            }
        }
        return vignette(c, u, v, 0.18)
    }
    private data class Five(val a: Double, val b: Double, val c: Double, val d: Double, val e: Int)
    private data class Quad(val a: Double, val b: Double, val c: Double, val d: Double)

    // ------------------------------------------------------------------------------------------ 6 Candy Land
    private val SPRINKLES = intArrayOf(0xFF5C8A, 0x4FC3F7, 0xFFD54F, 0x81C784, 0xB39DDB, 0xFFFFFF, 0xFF8A50)
    private fun candy(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h
        var c = vgrad(0xFFE0EC, 0xFFC6DD, v)
        // soft diagonal stripes, like a candy wrapper
        val s = sin((x + y * 0.6) / w * 2 * PI * 5.5)
        c = mix(c, rgb(0xFFF0F6), 0.35 * smooth(0.2, 0.6, s))
        // sprinkles: little rounded rods with a drop shadow
        val cs = w / 13.0
        val cx = floor(x / cs); val cy = floor(y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy) < 0.35) continue
            val ox = (hx + 0.25 + hash(hx + 1.3, hy) * 0.5) * cs; val oy = (hy + 0.25 + hash(hx, hy + 2.9) * 0.5) * cs
            val a = hash(hx + 4.4, hy + 8.8) * PI
            val len = cs * 0.16; val rad = cs * 0.055
            val ax = ox - cos(a) * len; val ay = oy - sin(a) * len; val bx = ox + cos(a) * len; val by = oy + sin(a) * len
            val sh = segDist(x - 2.5, y - 3.5, ax, ay, bx, by)
            c = mix(c, rgb(0xD9779F), (1 - smooth(rad - 1, rad + 2, sh)) * 0.35)
            val d = segDist(x, y, ax, ay, bx, by)
            val col = rgb(SPRINKLES[(hash(hx + 7.7, hy) * SPRINKLES.size).toInt().coerceAtMost(SPRINKLES.size - 1)])
            val m = 1 - smooth(rad - 0.8, rad + 0.6, d)
            if (m > 0) {
                val hi = 1 - smooth(0.0, rad * 0.8, segDist(x + rad * 0.35, y + rad * 0.35, ax, ay, bx, by))
                c = mix(c, add(col, 0.18 * hi), m)
            }
        }
        return vignette(c, u, v, 0.08)
    }

    // ------------------------------------------------------------------------------------------ 7 Desert Dunes
    private fun desert(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h
        // warm sky at the very top
        var c = vgrad(0xFFD9A0, 0xFFE9C4, v * 5)
        val layers = listOf(
            doubleArrayOf(0.17, 0.035, 3.0, 0.4), doubleArrayOf(0.30, 0.05, 2.3, 2.1), doubleArrayOf(0.45, 0.06, 2.8, 4.0),
            doubleArrayOf(0.60, 0.06, 2.0, 1.2), doubleArrayOf(0.76, 0.07, 2.5, 5.3), doubleArrayOf(0.91, 0.06, 1.8, 3.3),
        )
        val tones = intArrayOf(0xF7D6A0, 0xF4C88A, 0xF0BB78, 0xEDAE68, 0xE9A25C, 0xE59752)
        for ((k, l) in layers.withIndex()) {
            val crestX = u * l[2] * PI + l[3]
            val crest = l[0] - l[1] * sin(crestX) - 0.012 * (fbm(u * 5 + k, k * 1.7) - 0.5)
            if (v < crest) continue
            val base = rgb(tones[k])
            // sunlit face on one side of each crest, shadow on the other
            val slope = cos(crestX)
            val below = (v - crest) / 0.09
            val shade = (1 - smooth(0.0, 1.0, below)) * slope
            c = mul(base, 1.0 + 0.10 * shade)
            c = mix(c, rgb(0xFFF1D6), (1 - smooth(0.0, 0.004, v - crest)) * 0.6)  // bright crest line
            // ripples running along the dune
            val rip = sin((v - crest) * 260.0 + u * 14.0 + fbm(u * 6, v * 6) * 4.0)
            c = mul(c, 1.0 + 0.03 * rip * smooth(0.0, 0.02, v - crest))
        }
        c = mul(c, 0.96 + 0.06 * noise(x * 0.8, y * 0.8))
        return vignette(c, u, v, 0.12)
    }

    // ------------------------------------------------------------------------------------------ 8 Under the Sea
    private fun sea(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0x8BE4F0, 0x1E83B8, v.pow(0.85))
        // sun rays from the surface
        val ray = (0.5 + 0.5 * sin(u * 13.0 + v * 2.5 + fbm(u * 2, v * 0.5) * 3.0)).pow(6.0) +
            (0.5 + 0.5 * sin(u * 7.0 - v * 1.5 + 2.0)).pow(8.0)
        c = add(c, 0.10 * ray * (1 - v) * (1 - v))
        // dancing caustics, strongest near the top
        val wx = px * 1.6 + (fbm(px * 0.8, py * 0.8) - 0.5) * 1.4; val wy = py * 2.2 + (fbm(px * 0.8 + 4, py * 0.8) - 0.5) * 1.4
        val e1 = 1 - smooth(0.0, 0.05, cellEdges(wx, wy, 0.0))
        val e2 = 1 - smooth(0.0, 0.05, cellEdges(wx * 1.4 + 3.0, wy * 1.4, 5.0))
        c = add(c, (0.07 * e1 + 0.05 * e2) * smooth(0.75, 0.0, v))
        // sandy sea floor at the bottom
        val floorY = 0.90 + 0.02 * sin(u * 6 + 1) + 0.01 * fbm(u * 8, 3.0)
        if (v > floorY) c = mix(mix(rgb(0xE8D29A), rgb(0xC9AE72), smooth(floorY, 1.0, v)), c, 0.25 * (1 - smooth(floorY, floorY + 0.02, v)))
        // drifting specks
        if (hash(floor(x / 7), floor(y / 7)) > 0.985) c = add(c, 0.15)
        return vignette(c, u, v, 0.16)
    }

    // ------------------------------------------------------------------------------------------ 9 Sakura Garden
    private fun sakura(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0xFFF5F1, 0xFBD9E4, v)
        c = mix(c, rgb(0xFCE3EC), 0.5 * smooth(0.4, 0.75, fbm(px * 0.6, py * 0.6)))
        // soft bokeh circles
        val bs = w / 4.5
        val bx = floor(x / bs); val by = floor(y / bs)
        for (j in -1..1) for (i in -1..1) {
            val hx = bx + i; val hy = by + j
            if (hash(hx + 0.3, hy) < 0.55) continue
            val ox = (hx + hash(hx, hy + 3.3)) * bs; val oy = (hy + hash(hx + 6.6, hy)) * bs
            val r = bs * (0.18 + 0.25 * hash(hx + 2.0, hy + 9.0))
            val d = sqrt((x - ox) * (x - ox) + (y - oy) * (y - oy))
            c = mix(c, rgb(0xFFFFFF), (1 - smooth(r * 0.85, r, d)) * 0.22)
        }
        // scattered falling petals
        val cs = w / 9.0
        val cx = floor(x / cs); val cy = floor(y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy + 0.5) < 0.55) continue
            val ox = (hx + 0.2 + hash(hx + 1.1, hy) * 0.6) * cs; val oy = (hy + 0.2 + hash(hx, hy + 1.9) * 0.6) * cs
            val a = hash(hx + 3.7, hy) * PI * 2
            val dx = x - ox; val dy = y - oy
            val rx = dx * cos(a) + dy * sin(a); val ry = -dx * sin(a) + dy * cos(a)
            val L = cs * 0.13
            // teardrop petal with a notch at the tip
            val k = (rx / L).coerceIn(-1.0, 1.0)
            val half = L * 0.55 * sqrt(1 - k * k) * (0.75 + 0.25 * k)
            val notch = if (rx > L * 0.7) L * 0.18 * (1 - smooth(0.0, L * 0.12, abs(ry))) else 0.0
            val inside = abs(rx) < L && abs(ry) < half && rx < L - notch
            if (inside) {
                val t = (rx / L + 1) / 2
                c = mix(c, mix(rgb(0xF48FB1), rgb(0xFFD6E4), t), 0.75)
            }
        }
        return vignette(c, u, v, 0.08)
    }

    // ------------------------------------------------------------------------------------------ 10 Cloud Kingdom
    private fun clouds(x: Double, y: Double, w: Double, h: Double): DoubleArray {
        val u = x / w; val v = y / h; val px = x / w * 4; val py = y / w * 4
        var c = vgrad(0x6EC3F2, 0xCDEFFF, v)
        // a few layers of puffy clouds; each lit from above, shaded underneath
        fun dens(qx: Double, qy: Double, k: Int): Double {
            val bandY = 1.2 + k * 2.1
            val band = exp(-((qy - bandY) * (qy - bandY)) * 1.4)
            return fbm(qx * 0.55 + k * 3.1, qy * 1.1 + k * 1.7) * 1.25 * band + 0.15 * band
        }
        for (k in 0 until 4) {
            val d = dens(px, py, k)
            val m = smooth(0.50, 0.56, d)
            if (m <= 0) continue
            val above = dens(px, py - 0.12, k)
            val lit = smooth(0.62, 0.45, above)
            val body = mix(rgb(0xC9DDF0), rgb(0xFFFFFF), 0.35 + 0.65 * lit)
            c = mix(c, body, m * (0.85 + 0.15 * k / 3.0))
        }
        // sun glow top-left
        c = add(c, 0.18 * exp(-((u - 0.1) * (u - 0.1) * 6 + (v - 0.02) * (v - 0.02) * 20)))
        return vignette(c, u, v, 0.08)
    }
}
