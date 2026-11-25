package com.ngstudio.maze.tools

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ball skins (2026-11-25): every ball is painted here on the CPU, all original procedural art.
 *
 *   ./gradlew :tools:run --args="balls"   -> app/src/main/assets/balls/tex_N.png (+ icon_N.png, + docs/qa/balls_preview.png)
 *
 * tex_N.png is an equirectangular albedo map (u = atan2(z, x), v = acos(y); y up, +z faces the viewer at rest) that
 * Shaders.BALL_FS wraps round the 3D ball; its alpha is 255 - glow (glowing lava cracks, stars). icon_N.png is the
 * same ball lit like the game, for the shop and the HUD. The order and materials must match BallSkin.ALL in the app.
 */
object BallArt {
    /** Albedo + glow (0..1). */
    class C(val r: Double, val g: Double, val b: Double, val e: Double = 0.0)

    class Ball(val id: Int, val name: String, val gloss: Double, val metal: Double, val glass: Double, val f: (Double, Double, Double) -> C)

    private const val TW = 512
    private const val TH = 256
    private const val ICON = 224

    // ---------------------------------------------------------------- helpers
    private fun hex(h: Int, e: Double = 0.0) = C(((h shr 16) and 255) / 255.0, ((h shr 8) and 255) / 255.0, (h and 255) / 255.0, e)
    private fun mix(a: C, b: C, t: Double): C { val k = t.coerceIn(0.0, 1.0); return C(a.r + (b.r - a.r) * k, a.g + (b.g - a.g) * k, a.b + (b.b - a.b) * k, a.e + (b.e - a.e) * k) }
    private fun mul(a: C, k: Double) = C(a.r * k, a.g * k, a.b * k, a.e)
    private fun glow(a: C, e: Double) = C(a.r, a.g, a.b, e)
    private fun ss(e0: Double, e1: Double, x: Double): Double { val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0); return t * t * (3 - 2 * t) }
    private fun fract(x: Double) = x - floor(x)

    private fun hash(i: Int, j: Int, k: Int): Double {
        var h = i * 374761393 + j * 668265263 + k * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / 2147483647.0
    }
    private fun hashI(i: Int) = hash(i, i * 7 + 3, i * 13 + 5)

    private fun noise(x: Double, y: Double, z: Double): Double {
        val ix = floor(x).toInt(); val iy = floor(y).toInt(); val iz = floor(z).toInt()
        val fx = x - ix; val fy = y - iy; val fz = z - iz
        val ux = fx * fx * (3 - 2 * fx); val uy = fy * fy * (3 - 2 * fy); val uz = fz * fz * (3 - 2 * fz)
        fun l(a: Double, b: Double, t: Double) = a + (b - a) * t
        return l(
            l(l(hash(ix, iy, iz), hash(ix + 1, iy, iz), ux), l(hash(ix, iy + 1, iz), hash(ix + 1, iy + 1, iz), ux), uy),
            l(l(hash(ix, iy, iz + 1), hash(ix + 1, iy, iz + 1), ux), l(hash(ix, iy + 1, iz + 1), hash(ix + 1, iy + 1, iz + 1), ux), uy), uz,
        )
    }
    private fun fbm(x: Double, y: Double, z: Double, oct: Int = 4): Double {
        var s = 0.0; var a = 0.5; var f = 1.0
        repeat(oct) { s += a * noise(x * f + 17.3, y * f + 3.1, z * f + 9.7); a *= 0.5; f *= 2.03 }
        return s / (1 - 0.5.pow(oct))
    }

    private fun norm(v: DoubleArray): DoubleArray { val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); return doubleArrayOf(v[0] / l, v[1] / l, v[2] / l) }
    private fun ang(x: Double, y: Double, z: Double, p: DoubleArray) = acos((x * p[0] + y * p[1] + z * p[2]).coerceIn(-1.0, 1.0))

    private fun fib(n: Int, seed: Double = 0.0): List<DoubleArray> = List(n) { i ->
        val y = 1 - 2 * (i + 0.5) / n
        val r = sqrt(1 - y * y)
        val a = i * 2.399963229728653 + seed
        doubleArrayOf(cos(a) * r, y, sin(a) * r)
    }

    /** Jittered points for organic cells (lava plates, ice facets, hammered gold). */
    private fun jitter(n: Int, amt: Double, seed: Int) = fib(n).mapIndexed { i, p ->
        norm(doubleArrayOf(p[0] + (hash(i, seed, 1) - 0.5) * amt, p[1] + (hash(i, seed, 2) - 0.5) * amt, p[2] + (hash(i, seed, 3) - 0.5) * amt))
    }

    /** Nearest and second-nearest point (angles), for cells. */
    private class Near(val i: Int, val a1: Double, val a2: Double)
    private fun nearest(x: Double, y: Double, z: Double, pts: List<DoubleArray>, w: DoubleArray? = null): Near {
        var b1 = 9.0; var b2 = 9.0; var bi = 0
        for (k in pts.indices) {
            val a = ang(x, y, z, pts[k]) / (w?.get(k) ?: 1.0)
            if (a < b1) { b2 = b1; b1 = a; bi = k } else if (a < b2) b2 = a
        }
        return Near(bi, b1, b2)
    }

    // the truncated-icosahedron tiling (soccer balls): 12 pentagons at the icosahedron's vertices, 20 hexagons at the dodecahedron's
    private val PHI = (1 + sqrt(5.0)) / 2
    private val soccerPts: List<DoubleArray> = buildList {
        for (a in listOf(-1.0, 1.0)) for (b in listOf(-PHI, PHI)) {
            add(norm(doubleArrayOf(0.0, a, b))); add(norm(doubleArrayOf(a, b, 0.0))); add(norm(doubleArrayOf(b, 0.0, a)))
        }
        for (a in listOf(-1.0, 1.0)) for (b in listOf(-1.0, 1.0)) for (c in listOf(-1.0, 1.0)) add(norm(doubleArrayOf(a, b, c)))
        val ip = 1 / PHI
        for (a in listOf(-PHI, PHI)) for (b in listOf(-ip, ip)) {
            add(norm(doubleArrayOf(0.0, a, b))); add(norm(doubleArrayOf(a, b, 0.0))); add(norm(doubleArrayOf(b, 0.0, a)))
        }
    }
    private val soccerW = DoubleArray(32) { if (it < 12) 0.2876 else 0.3648 }

    /** Soccer panels: [panel] colours panel k (k < 12 = pentagon). */
    private fun soccer(x: Double, y: Double, z: Double, seam: C, panel: (Int) -> C): C {
        val n = nearest(x, y, z, soccerPts, soccerW)
        val edge = (n.a2 - n.a1) * 0.16
        val base = mul(panel(n.i), 1 - 0.13 * ss(0.55, 1.0, n.a1))
        return mix(seam, base, ss(0.006, 0.014, edge))
    }

    /** The two-lobed seam of tennis balls and baseballs, sampled once per ball. */
    private fun curve(a: Double): List<DoubleArray> {
        val b = 1 - a
        return List(900) { i ->
            val t = i * 2 * PI / 900
            doubleArrayOf(a * cos(t) + b * cos(3 * t), a * sin(t) - b * sin(3 * t), 2 * sqrt(a * b) * sin(2 * t))
        }
    }
    private val tennisCurve = curve(0.68)
    private val baseballCurve = curve(0.74)
    private class CurveHit(val d: Double, val t: Double, val side: Double)
    private fun curveHit(x: Double, y: Double, z: Double, c: List<DoubleArray>): CurveHit {
        var best = 9.0; var bi = 0
        for (i in c.indices) {
            val p = c[i]
            val dx = x - p[0]; val dy = y - p[1]; val dz = z - p[2]
            val d = dx * dx + dy * dy + dz * dz
            if (d < best) { best = d; bi = i }
        }
        val p = c[bi]; val q = c[(bi + 1) % c.size]
        val tx = q[0] - p[0]; val ty = q[1] - p[1]; val tz = q[2] - p[2]
        // side of the curve: sign of (p x t) . v
        val cx = p[1] * tz - p[2] * ty; val cy = p[2] * tx - p[0] * tz; val cz = p[0] * ty - p[1] * tx
        val side = if (cx * x + cy * y + cz * z >= 0) 1.0 else -1.0
        return CurveHit(sqrt(best), bi.toDouble() / c.size, side)
    }

    /** A picture drawn with Java2D, stuck on the +z side of the ball (orthographic). */
    private class Decal(val img: BufferedImage, val radius: Double)
    private fun decalAt(x: Double, y: Double, z: Double, d: Decal): C? {
        if (z <= 0) return null
        val u = (x / d.radius * 0.5 + 0.5) * d.img.width
        val v = (-y / d.radius * 0.5 + 0.5) * d.img.height
        if (u < 0 || v < 0 || u >= d.img.width || v >= d.img.height) return null
        val p = d.img.getRGB(u.toInt(), v.toInt())
        val a = (p ushr 24) / 255.0
        if (a <= 0.001) return null
        return C(((p shr 16) and 255) / 255.0, ((p shr 8) and 255) / 255.0, (p and 255) / 255.0, a) // e = coverage
    }
    private fun canvas(s: Int, draw: (java.awt.Graphics2D) -> Unit): BufferedImage {
        val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        draw(g); g.dispose(); return img
    }

    private val eightDecal = Decal(canvas(512) { g ->
        g.color = Color(0xF7F7F2); g.fill(Ellipse2D.Double(56.0, 56.0, 400.0, 400.0))
        g.color = Color(0x15151A); g.font = Font("SansSerif", Font.BOLD, 300)
        val fm = g.fontMetrics; val w = fm.stringWidth("8")
        g.drawString("8", 256 - w / 2, 256 + fm.ascent / 2 - 28)
    }, 0.62)

    private val winkDecal = Decal(canvas(512) { g ->
        val ink = Color(0x5B2A0C)
        g.color = Color(255, 120, 130, 90); g.fill(Ellipse2D.Double(52.0, 262.0, 110.0, 62.0)); g.fill(Ellipse2D.Double(350.0, 262.0, 110.0, 62.0))
        g.color = ink; g.fill(Ellipse2D.Double(128.0, 120.0, 70.0, 104.0))                // open eye
        g.color = Color(255, 255, 255, 230); g.fill(Ellipse2D.Double(146.0, 136.0, 24.0, 30.0))
        g.color = ink; g.stroke = BasicStroke(26f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Arc2D.Double(300.0, 150.0, 96.0, 70.0, 20.0, 140.0, Arc2D.OPEN))            // winking eye
        val mouth = GeneralPath().apply {
            moveTo(118.0, 290.0); curveTo(170.0, 300.0, 342.0, 300.0, 394.0, 290.0)
            curveTo(390.0, 400.0, 122.0, 400.0, 118.0, 290.0); closePath()
        }
        g.fill(mouth)
        g.clip(mouth); g.color = Color(0xE8484F); g.fill(Ellipse2D.Double(190.0, 335.0, 140.0, 90.0))
        g.color = Color(0xFFFFFF); g.fill(java.awt.Rectangle(150, 286, 212, 22))
    }, 0.96)

    private val goldPts = jitter(420, 0.04, 5)
    private val lavaPts = jitter(46, 0.10, 9)
    private val icePts = jitter(70, 0.12, 13)
    private val golfPts = fib(380, 0.4)

    // ---------------------------------------------------------------- the balls (order = BallSkin.ALL ids)
    val ALL: List<Ball> = listOf(
        Ball(0, "Beach Ball", 1.1, 0.0, 0.0) { x, y, z ->
            if (abs(y) > 0.93) return@Ball hex(0xFFFFFF)
            val cols = intArrayOf(0xE53935, 0xFFFFFF, 0xFDD835, 0xFFFFFF, 0x1E88E5, 0xFFFFFF)
            val a = (atan2(z, x) + PI) / (2 * PI) * 6
            val k = floor(a).toInt().mod(6)
            val f = fract(a)
            val c = hex(cols[k])
            mix(mul(c, 0.9), c, ss(0.0, 0.04, f) * ss(1.0, 0.96, f))
        },
        Ball(1, "Basketball", 0.35, 0.0, 0.0) { x, y, z ->
            val pebble = 0.9 + 0.12 * noise(x * 70, y * 70, z * 70)
            val base = mul(mix(hex(0xE8742A), hex(0xC8501A), fbm(x * 2, y * 2, z * 2) * 0.6), pebble)
            val w = 0.022
            val d1 = abs(asin(x.coerceIn(-1.0, 1.0)))
            val d2 = abs(asin(y.coerceIn(-1.0, 1.0)))
            val d3 = abs(acos(abs(x).coerceIn(-1.0, 1.0)) - 0.95)   // the two side loops round the +-x axis
            val d = min(d1, min(d2, d3))
            mix(hex(0x1A1210), base, ss(w - 0.006, w + 0.006, d))
        },
        Ball(2, "Soccer", 0.7, 0.0, 0.0) { x, y, z ->
            soccer(x, y, z, hex(0x3A3A40)) { if (it < 12) hex(0x1C1C22) else hex(0xF6F6F2) }
        },
        Ball(3, "Tennis", 0.15, 0.0, 0.0) { x, y, z ->
            val fuzz = 0.88 + 0.16 * noise(x * 90, y * 90, z * 90) + 0.05 * fbm(x * 6, y * 6, z * 6)
            val felt = mul(hex(0xD4E63A), fuzz)
            val h = curveHit(x, y, z, tennisCurve)
            mix(hex(0xF4F6EC), felt, ss(0.030, 0.042, h.d))
        },
        Ball(4, "Baseball", 0.45, 0.0, 0.0) { x, y, z ->
            val leather = mul(hex(0xF6F1E6), 0.95 + 0.06 * noise(x * 40, y * 40, z * 40))
            val h = curveHit(x, y, z, baseballCurve)
            var c = mix(hex(0xD6CDBE), leather, ss(0.006, 0.012, h.d))
            if (h.d in 0.020..0.074) {
                val u = (h.d - 0.020) / 0.054
                val ph = fract(h.t * 110 + h.side * u * 0.5)
                val st = ss(0.0, 0.08, ph) * ss(0.36, 0.26, ph) * ss(0.0, 0.12, u) * ss(1.0, 0.85, u)
                c = mix(c, hex(0xD3262E), st)
            }
            c
        },
        Ball(5, "Volleyball", 0.5, 0.0, 0.0) { x, y, z ->
            val ax = abs(x); val ay = abs(y); val az = abs(z)
            val m = max(ax, max(ay, az))
            val second = when (m) { ax -> max(ay, az); ay -> max(ax, az); else -> max(ax, ay) }
            val c = when (m) { ax -> y / ax; ay -> z / ay; else -> x / az }
            val strip = abs(abs(c) - 1.0 / 3)
            val seam = min((m - second) * 1.6, strip * m * 0.9)
            val cols = intArrayOf(0xF7F6F0, 0xFFD23F, 0x2F6BD8)
            val face = when (m) { ax -> 0; ay -> 1; else -> 2 }
            val k = floor((c + 1) * 1.5).toInt().coerceIn(0, 2)
            val panel = if (k == 1) hex(cols[face]) else hex(0xF7F6F0)
            mix(hex(0x9E9A90), mul(panel, 0.96 + 0.04 * ss(0.0, 0.08, seam)), ss(0.006, 0.014, seam))
        },
        Ball(6, "Cricket", 0.9, 0.0, 0.0) { x, y, z ->
            val leather = mul(mix(hex(0xB3141E), hex(0x7E0A12), fbm(x * 3, y * 3, z * 3) * 0.7), 0.95 + 0.07 * noise(x * 50, y * 50, z * 50))
            var c = leather
            if (abs(y) < 0.06) c = mul(c, 0.9)
            val phi = (atan2(z, x) + PI) / (2 * PI)
            for (row in doubleArrayOf(-0.045, -0.022, 0.022, 0.045)) {
                val dashOn = ss(0.0, 0.05, fract(phi * 70)) * ss(0.6, 0.5, fract(phi * 70))
                c = mix(c, hex(0xF4EFE4), dashOn * (1 - ss(0.004, 0.008, abs(y - row))))
            }
            c
        },
        Ball(7, "Golf Ball", 0.6, 0.0, 0.0) { x, y, z ->
            val n = nearest(x, y, z, golfPts)
            val r = 0.085
            if (n.a1 < r) {
                val p = golfPts[n.i]
                val tx = x - p[0]; val ty = y - p[1]
                val lit = (-tx * 0.6 + ty * 0.8) / r
                mul(hex(0xF4F4F0), 0.86 + 0.06 * (n.a1 / r) - 0.07 * lit)
            } else hex(0xF6F6F2)
        },
        Ball(8, "Rainbow Swirl", 1.0, 0.0, 0.0) { x, y, z ->
            if (abs(y) > 0.94) return@Ball hex(0xFFFFFF)
            val cols = intArrayOf(0xF44336, 0xFF9800, 0xFFEB3B, 0x4CAF50, 0x2196F3, 0x9C27B0)
            val a = ((atan2(z, x) + PI) / (2 * PI) + y * 0.32) * 6
            val k = floor(a).toInt().mod(6); val f = fract(a)
            val c = hex(cols[k])
            mix(mul(c, 0.88), c, ss(0.0, 0.05, f))
        },
        Ball(9, "Rainbow Stripes", 1.0, 0.0, 0.0) { x, y, z ->
            val cols = intArrayOf(0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x1E88E5, 0x5E35B1, 0x8E24AA)
            val v = acos(y.coerceIn(-1.0, 1.0)) / PI * 7
            val k = floor(v).toInt().coerceIn(0, 6); val f = fract(v)
            val c = hex(cols[k])
            mix(mul(c, 0.9), c, ss(0.0, 0.08, f))
        },
        Ball(10, "Watermelon", 0.8, 0.0, 0.0) { x, y, z ->
            val phi = atan2(z, x); val th = acos(y.coerceIn(-1.0, 1.0))
            val w = sin(phi * 9 + 0.35 * sin(th * 14) + fbm(x * 3, y * 3, z * 3) * 1.4)
            val dark = mul(hex(0x1F6B2A), 0.92 + 0.1 * noise(x * 30, y * 30, z * 30))
            val light = hex(0x76C450)
            val c = mix(dark, light, ss(0.15, 0.35, w) * ss(0.95, 0.75, w))
            if (abs(y) > 0.96) mix(c, hex(0xA8D27A), ss(0.96, 0.99, abs(y))) else c
        },
        Ball(11, "8-Ball", 1.6, 0.0, 0.0) { x, y, z ->
            decalAt(x, y, z, eightDecal)?.let { d -> return@Ball mix(hex(0x111116), C(d.r, d.g, d.b), d.e) }
            hex(0x111116)
        },
        Ball(12, "Bowling Ball", 1.5, 0.0, 0.0) { x, y, z ->
            val swirl = fbm(x * 2.2 + fbm(x * 3, y * 3, z * 3) * 1.5, y * 2.2, z * 2.2, 5)
            var c = mix(hex(0xFF7A1F), hex(0xC4380E), ss(0.45, 0.7, swirl))
            c = mix(c, hex(0xFFB15A), ss(0.62, 0.78, fbm(x * 5, y * 5, z * 5)) * 0.5)
            if (z > 0) {
                for ((hx, hy, r) in listOf(Triple(-0.17, 0.2, 0.085), Triple(0.17, 0.2, 0.085), Triple(0.0, -0.18, 0.1))) {
                    val d = sqrt((x - hx) * (x - hx) + (y - hy) * (y - hy))
                    c = mix(c, mul(c, 0.75), 1 - ss(r, r + 0.03, d))
                    c = mix(c, hex(0x220A02), 1 - ss(r - 0.01, r, d))
                }
            }
            c
        },
        Ball(13, "Earth", 0.9, 0.0, 0.0) { x, y, z ->
            val n = fbm(x * 1.7 + 4.0, y * 1.7, z * 1.7, 5)
            val ocean = mix(hex(0x0F4FB8), hex(0x2A86E8), ss(0.25, 0.52, n))
            var c = if (n > 0.535) {
                val land = mix(hex(0x4FAE3C), hex(0x2E7D32), fbm(x * 6, y * 6, z * 6))
                mix(land, hex(0xB58B4C), ss(0.66, 0.74, n))
            } else ocean
            c = mix(c, hex(0x7FC8F5), ss(0.50, 0.535, n) * (1 - ss(0.535, 0.536, n)) * 0.6)
            val ice = ss(0.86, 0.9, abs(y) + 0.05 * noise(x * 8, y * 8, z * 8))
            mix(c, hex(0xF4FAFF), ice)
        },
        Ball(14, "Winky", 1.0, 0.0, 0.0) { x, y, z ->
            val base = mix(hex(0xFFD43A), hex(0xFFB01F), ss(0.2, -0.9, y))
            decalAt(x, y, z, winkDecal)?.let { d -> return@Ball mix(base, C(d.r, d.g, d.b), d.e) }
            base
        },
        Ball(15, "Candy Soccer", 0.9, 0.0, 0.0) { x, y, z ->
            val cols = intArrayOf(0xFF5252, 0xFFB300, 0x7CDB3E, 0x29B6F6, 0x7E57C2, 0xFF6FB5, 0x26C6A6, 0xFFE14D)
            soccer(x, y, z, hex(0xFFFFFF)) { hex(cols[(it * 5 + it / 3) % cols.size]) }
        },
        Ball(16, "Glass Marble", 1.6, 0.0, 0.35) { x, y, z ->
            val base = mix(hex(0xCDEFFB), hex(0x9CD8F2), fbm(x * 2, y * 2, z * 2))
            val a = atan2(z, x) * 2 + y * 3.2 + fbm(x * 2, y * 2, z * 2) * 1.2
            val band = sin(a)
            val ribbon = ss(0.55, 0.8, band) * (1 - ss(0.7, 0.98, abs(y)))
            val ribbon2 = ss(0.55, 0.8, -band) * (1 - ss(0.5, 0.9, abs(y)))
            var c = mix(base, hex(0xFF8A2A), ribbon)
            c = mix(c, hex(0xFFFFFF), ss(0.82, 0.95, band) * 0.8)
            mix(c, hex(0x1F5FD0), ribbon2 * 0.9)
        },
        Ball(17, "Bubble", 1.8, 0.0, 0.75) { x, y, z ->
            val h = fbm(x * 1.6, y * 1.6, z * 1.6) * 1.6 + y * 0.3
            val r = 0.5 + 0.5 * cos(2 * PI * (h + 0.0)); val g = 0.5 + 0.5 * cos(2 * PI * (h + 0.33)); val b = 0.5 + 0.5 * cos(2 * PI * (h + 0.67))
            mix(hex(0xF2EEFF), C(r, g, b), 0.42)
        },
        Ball(18, "Glitter", 1.3, 0.0, 0.0) { x, y, z ->
            var c = mix(hex(0xD81B8C), hex(0xF050B8), fbm(x * 4, y * 4, z * 4))
            val s = 46.0
            val h = hash(floor(x * s).toInt(), floor(y * s).toInt(), floor(z * s).toInt())
            if (h > 0.72) c = mix(c, if (h > 0.95) hex(0xFFFFFF, 0.6) else hex(0xFF9AD8, 0.2), ss(0.72, 0.8, h))
            c
        },
        Ball(19, "Ice", 1.6, 0.0, 0.35) { x, y, z ->
            val n = nearest(x, y, z, icePts)
            val tint = mix(hex(0xCFF3FF), hex(0x7CCDF0), hashI(n.i) * 0.8)
            val c = mix(tint, hex(0xFFFFFF), 0.35 * fbm(x * 9, y * 9, z * 9))
            mix(hex(0xFFFFFF), c, ss(0.004, 0.016, (n.a2 - n.a1) * 0.5))
        },
        Ball(20, "Disco", 1.3, 0.85, 0.0) { x, y, z ->
            val th = acos(y.coerceIn(-1.0, 1.0)); val phi = atan2(z, x) + PI
            val bands = 18
            val bi = floor(th / PI * bands).toInt().coerceIn(0, bands - 1)
            val thc = (bi + 0.5) / bands * PI
            val segs = max(4, round(2 * bands * sin(thc)).toInt())
            val sp = phi / (2 * PI) * segs
            val si = floor(sp).toInt()
            val fu = fract(th / PI * bands); val fv = fract(sp)
            val gap = min(min(fu, 1 - fu), min(fv, 1 - fv) * (sin(thc) * 2 * bands / segs))
            val h = hash(bi, si, 7)
            val tile = when {
                h > 0.93 -> hex(0xFFFFFF, 0.5)
                h > 0.78 -> hex(0xC9D6FF)
                h > 0.64 -> hex(0xF4C8FF)
                else -> mul(hex(0xD8DCE6), 0.62 + 0.38 * hash(si, bi, 3))
            }
            mix(hex(0x30303A), tile, ss(0.04, 0.09, gap))
        },
        Ball(21, "Gold", 1.2, 1.0, 0.0) { x, y, z ->
            val n = nearest(x, y, z, goldPts)
            val k = 0.78 + 0.24 * hashI(n.i)
            val rim = ss(0.0, 0.02, (n.a2 - n.a1) * 0.5)
            mul(hex(0xF0BE45), k * (0.7 + 0.3 * rim))
        },
        Ball(22, "Lava", 0.6, 0.0, 0.0) { x, y, z ->
            val n = nearest(x, y, z, lavaPts)
            val e = (n.a2 - n.a1) * 0.5
            val rock = mul(mix(hex(0x2E211D), hex(0x16100E), fbm(x * 6, y * 6, z * 6)), 0.9 + 0.2 * noise(x * 40, y * 40, z * 40))
            val heat = 1 - ss(0.0, 0.05, e)
            val crack = 1 - ss(0.006, 0.016, e)
            var c = mix(rock, hex(0x7A1E08, 0.3), heat * 0.6)
            c = mix(c, mix(hex(0xFF6A10, 1.0), hex(0xFFE070, 1.0), 1 - ss(0.0, 0.008, e)), crack)
            c
        },
        Ball(23, "Galaxy", 1.3, 0.0, 0.0) { x, y, z ->
            val n = fbm(x * 2.0, y * 2.0, z * 2.0, 5)
            var c = mix(hex(0x0A0826), hex(0x3B1680), ss(0.35, 0.7, n))
            val arm = sin(atan2(y, x) * 2 + z * 4 + n * 3)
            c = mix(c, hex(0xE05AB8), ss(0.6, 0.95, arm) * ss(0.4, 0.7, n) * 0.8)
            c = mix(c, hex(0x5AA8FF), ss(0.65, 0.85, fbm(x * 4 + 3, y * 4, z * 4)) * 0.45)
            val s = 70.0
            val h = hash(floor(x * s).toInt(), floor(y * s).toInt(), floor(z * s).toInt())
            if (h > 0.965) c = mix(c, hex(0xFFFFFF, 1.0), ss(0.965, 0.985, h))
            c
        },
    )

    // ---------------------------------------------------------------- output
    private fun dirAt(u: Double, v: Double): DoubleArray {
        val phi = (u - 0.5) * 2 * PI; val th = v * PI
        return doubleArrayOf(sin(th) * cos(phi), cos(th), sin(th) * sin(phi))
    }

    private fun texture(b: Ball): BufferedImage {
        val img = BufferedImage(TW, TH, BufferedImage.TYPE_INT_ARGB)
        for (j in 0 until TH) for (i in 0 until TW) {
            var r = 0.0; var g = 0.0; var bl = 0.0; var e = 0.0
            for (sy in 0..1) for (sx in 0..1) {
                val d = dirAt((i + 0.25 + sx * 0.5) / TW, (j + 0.25 + sy * 0.5) / TH)
                val c = b.f(d[0], d[1], d[2])
                r += c.r; g += c.g; bl += c.b; e += c.e
            }
            fun ch(x: Double) = (x / 4 * 255).roundToIntSafe()
            val a = 255 - ch(e)
            img.setRGB(i, j, (a shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(bl))
        }
        return img
    }

    private fun Double.roundToIntSafe() = round(this).toInt().coerceIn(0, 255)

    /** The ball lit like Shaders.BALL_FS (same light, environment, gloss and metal), on a transparent square. */
    private fun icon(b: Ball): BufferedImage {
        val img = BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB)
        val L = norm(doubleArrayOf(-0.45, 0.65, 0.62))
        val envBottom = doubleArrayOf(0xF5 / 255.0, 0xE5 / 255.0, 0xD5 / 255.0)
        val sub = 3
        val rad = 0.84
        for (py in 0 until ICON) for (px in 0 until ICON) {
            var ar = 0.0; var ag = 0.0; var ab = 0.0; var aa = 0.0
            for (sy in 0 until sub) for (sx in 0 until sub) {
                val lx = ((px + (sx + 0.5) / sub) / ICON * 2 - 1) / rad
                val ly = ((py + (sy + 0.5) / sub) / ICON * 2 - 1) / rad
                val r2 = lx * lx + ly * ly
                if (r2 >= 1) continue
                val n = doubleArrayOf(lx, -ly, sqrt(1 - r2))
                // turn the ball a little so the interesting side shows: 20 deg down, 18 deg right
                val ca = cos(0.35); val sa = sin(0.35); val cb = cos(-0.3); val sb = sin(-0.3)
                val y1 = n[1] * ca - n[2] * sa; val z1 = n[1] * sa + n[2] * ca
                val x2 = n[0] * cb + z1 * sb; val z2 = -n[0] * sb + z1 * cb
                val c = b.f(x2, y1, z2)
                val wrap = ((n[0] * L[0] + n[1] * L[1] + n[2] * L[2] + 0.4) / 1.4).coerceIn(0.0, 1.0)
                val shade = 0.38 + 0.72 * wrap + 0.18 * (1 - n[2]).pow(2)
                val R = doubleArrayOf(2 * n[2] * n[0], 2 * n[2] * n[1], 2 * n[2] * n[2] - 1)
                val skyT = (1 - R[1]).coerceIn(0.0, 1.0)
                val sky = doubleArrayOf(0.95 - 0.17 * skyT, 0.97 - 0.13 * skyT, 1.0 - 0.05 * skyT)
                val k = ss(-0.35, 0.35, R[1])
                val win = max(R[0] * L[0] + R[1] * L[1] + R[2] * L[2], 0.0).pow(48.0) * 2.2
                val env = DoubleArray(3) { envBottom[it] * 0.75 * (1 - k) + sky[it] * k + win }
                val fres = 0.05 + 0.95 * (1 - n[2]).pow(4)
                val rl = doubleArrayOf(2 * n[2] * L[0] - L[0], 2 * n[2] * L[1] - L[1], 2 * n[2] * L[2] - L[2])
                val spec = max(rl[2], 0.0).pow(90.0)
                val base = doubleArrayOf(c.r, c.g, c.b)
                val col = DoubleArray(3) { ch ->
                    var v = base[ch] * shade
                    v = v + (env[ch] * (1 - 0.85 + 0.85 * base[ch]) - v) * b.metal
                    v = v + (env[ch] - v) * fres * 0.55 * b.gloss * (1 - b.metal * 0.5)
                    v += 1.6 * spec * b.gloss
                    v *= 1 - 0.22 * ss(0.1, 1.0, -n[1])
                    v + (base[ch] * 1.3 - v) * c.e
                }
                val alpha = 1 + (0.45 + 0.55 * fres + 0.4 * spec - 1) * b.glass
                ar += col[0] * alpha; ag += col[1] * alpha; ab += col[2] * alpha; aa += alpha
            }
            val n = (sub * sub).toDouble()
            // soft contact shadow under the ball
            val sxp = (px + 0.5) / ICON * 2 - 1; val syp = (py + 0.5) / ICON * 2 - 1
            val sd = sqrt((sxp / 0.78) * (sxp / 0.78) + ((syp - 0.86) / 0.13) * ((syp - 0.86) / 0.13))
            val shA = (1 - ss(0.2, 1.0, sd)) * 0.22
            val a = aa / n
            val outA = a + shA * (1 - a)
            if (outA <= 0.002) continue
            fun ch(v: Double) = (v / n / outA * 255).roundToIntSafe()
            img.setRGB(px, py, ((outA * 255).roundToIntSafe() shl 24) or (ch(ar) shl 16) or (ch(ag) shl 8) or ch(ab))
        }
        return img
    }

    fun run(outDir: File, preview: File) {
        outDir.mkdirs()
        val icons = ArrayList<BufferedImage>()
        for (b in ALL) {
            ImageIO.write(texture(b), "png", File(outDir, "tex_${b.id}.png"))
            val ic = icon(b); icons += ic
            ImageIO.write(ic, "png", File(outDir, "icon_${b.id}.png"))
            println("  ${b.id} ${b.name}")
        }
        // a review sheet of all the icons on tinted tiles
        val cols = 6; val cell = ICON + 24
        val sheet = BufferedImage(cols * cell + 24, ((ALL.size + cols - 1) / cols) * (cell + 22) + 24, BufferedImage.TYPE_INT_RGB)
        val g = sheet.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(0xFFF6EC); g.fillRect(0, 0, sheet.width, sheet.height)
        g.font = Font("SansSerif", Font.BOLD, 16)
        for ((k, ic) in icons.withIndex()) {
            val x = 24 + (k % cols) * cell; val y = 24 + (k / cols) * (cell + 22)
            g.color = Color(0xEDE1D3); g.fillRoundRect(x, y, ICON, ICON, 40, 40)
            g.drawImage(ic, x, y, null)
            g.color = Color(0x263A46); g.drawString(ALL[k].name, x + 6, y + ICON + 17)
        }
        g.dispose()
        preview.parentFile.mkdirs()
        ImageIO.write(sheet, "png", preview)
        println("Preview: $preview")
    }
}
