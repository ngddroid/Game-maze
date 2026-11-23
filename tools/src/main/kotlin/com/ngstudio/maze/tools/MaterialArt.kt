package com.ngstudio.maze.tools

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * CPU port of Shaders.MATERIAL (surfaceAt) so menus can show the same surface as the game.
 * Writes app/src/main/res/drawable-nodpi/surface_<world>.jpg (900 x 1950).
 * World colours mirror render/Visuals.kt WorldTheme.ALL - keep both lists in sync.
 */
object MaterialArt {
    private class World(val mat: Int, val a: Int, val b: Int, val c: Int)

    private val worlds = listOf(
        World(9, 0xF4D3A3, 0xE5B67C, 0xB97B42),
        World(1, 0xF5F3EF, 0xE2DDD5, 0xA39C93),
        World(2, 0xF9CBDB, 0xEDAFC6, 0xE296B4),
        World(0, 0x8E5E3D, 0x6B4128, 0x4A2A18),
        World(3, 0xE8F6FF, 0xB9DCF5, 0x8FBFE3),
        World(4, 0xF0DCB6, 0xDCC091, 0xC7A36B),
        World(5, 0x86CBAE, 0x5FAE8D, 0x3E8C6C),
        World(5, 0x80889A, 0x636B7D, 0x474E5E),
        World(0, 0xB8644A, 0x934A33, 0x6F3222),
        World(5, 0xE9BC8E, 0xD49F6D, 0xB8814F),
    )

    private fun fract(x: Double) = x - floor(x)
    private fun hash(x: Double, y: Double): Double {
        var px = fract(x * 123.34); var py = fract(y * 456.21)
        val d = px * (px + 45.32) + py * (py + 45.32)
        px += d; py += d
        return fract(px * py)
    }
    private fun noise(x: Double, y: Double): Double {
        val ix = floor(x); val iy = floor(y); val fx = x - ix; val fy = y - iy
        val ux = fx * fx * (3 - 2 * fx); val uy = fy * fy * (3 - 2 * fy)
        val a = hash(ix, iy); val b = hash(ix + 1, iy); val c = hash(ix, iy + 1); val d = hash(ix + 1, iy + 1)
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy
    }
    private fun fbm(x0: Double, y0: Double): Double {
        var s = 0.0; var a = 0.5; var x = x0; var y = y0
        for (i in 0 until 4) { s += a * noise(x, y); x *= 2.03; y *= 2.03; a *= 0.5 }
        return s
    }
    private fun smooth(e0: Double, e1: Double, x: Double): Double { val t = ((x - e0) / (e1 - e0)).coerceIn(0.0, 1.0); return t * t * (3 - 2 * t) }
    private fun mix(a: DoubleArray, b: DoubleArray, t: Double) = DoubleArray(3) { a[it] + (b[it] - a[it]) * t }
    private fun rgb(h: Int) = doubleArrayOf(((h shr 16) and 255) / 255.0, ((h shr 8) and 255) / 255.0, (h and 255) / 255.0)

    private fun surface(w: World, sx: Double, sy: Double, sw: Double, sh: Double): DoubleArray {
        if (w.mat == 6) return planks(w, sx, sy, sw, sh)
        if (w.mat == 7) return veneer(w, sx, sy, sw, sh)
        if (w.mat == 8) return grain(HONEY_OAK, sx, sy, sw, sh)
        if (w.mat == 9) return grain(LIGHT_MAPLE, sx, sy, sw, sh)
        val px = sx / (sw / 2.4); val py = sy / (sw / 2.4)
        val A = rgb(w.a); val B = rgb(w.b); val C = rgb(w.c)
        var c: DoubleArray
        when (w.mat) {
            0 -> {
                val cx = 1.25 + 0.45 * sin(py * 0.31 + 1.3)
                var rings = sqrt((px - cx) * (px - cx) + ((py - 9.0) * 0.16) * ((py - 9.0) * 0.16)) * 7.0
                rings *= 1.0 + 0.25 * (fbm(px * 0.6 + 4.0, py * 0.6 + 4.0) - 0.5)
                rings += fbm(px * 0.9, py * 0.12) * 2.6
                val r = fract(rings)
                val band = smooth(0.0, 0.22, r) * (1 - smooth(0.62, 1.0, r))
                c = mix(B, A, 0.12 + 0.88 * band)
                val fiber = noise(px * 60, py * 1.6) * 0.6 + noise(px * 150, py * 4) * 0.4
                c = DoubleArray(3) { c[it] * (0.90 + 0.14 * fiber) }
                c = mix(c, C, 0.30 * smooth(0.6, 0.85, fbm(px * 2.2 + 11, py * 0.25 + 11)))
                c = mix(c, C, 0.60 * (1 - smooth(0.0, 0.08, r)))
                // a soft satin varnish sheen across the planks
                val sheen = 0.04 * sin(px * 0.9 + py * 0.25)
                c = DoubleArray(3) { c[it] * (1.0 + sheen) }
            }
            1 -> {
                // flowing marble: domain-warped sine veins + soft grey clouds
                val wx = px + (fbm(px * 0.7, py * 0.7) - 0.5) * 2.2
                val wy = py + (fbm(px * 0.7 + 5.2, py * 0.7 + 1.3) - 0.5) * 2.2
                c = mix(A, B, fbm(px * 1.1 + 2, py * 1.1) * 0.55)
                val vein = abs(sin(wx * 1.5 + wy * 0.8))
                val vein2 = abs(sin(wx * 3.9 - wy * 2.2 + 1.3))
                val mask2 = smooth(0.45, 0.65, fbm(px * 0.9 + 7, py * 0.9 + 3))
                c = mix(c, C, (1 - smooth(0.0, 0.05, vein)) * 0.65)
                c = mix(c, C, (1 - smooth(0.0, 0.025, vein2)) * 0.35 * mask2)
                c = mix(c, B, (1 - smooth(0.0, 0.25, vein)) * 0.25)
            }
            2 -> {
                c = mix(A, B, fbm(px * 1.3, py * 1.3) * 0.7)
                val n = 0.975 + 0.05 * noise(px * 70, py * 70)
                c = DoubleArray(3) { c[it] * n }
                c = mix(c, C, 0.22 * smooth(0.7, 0.9, noise(px * 9, py * 9)))
            }
            3 -> {
                // frosted ice: soft blue depth, two scales of cracks, frost speckles
                c = mix(A, B, fbm(px * 0.9, py * 0.9) * 0.9)
                fun cracks(scale: Double, seed: Double): Double {
                    val gx = floor(px * scale); val gy = floor(py * scale); val fx = fract(px * scale); val fy = fract(py * scale)
                    var d1 = 8.0; var d2 = 8.0
                    for (j in -1..1) for (i in -1..1) {
                        val rx = i + hash(gx + i + seed, gy + j) - fx
                        val ry = j + hash(gx + i + 19.1 + seed, gy + j + 19.1) - fy
                        val d = rx * rx + ry * ry
                        if (d < d1) { d2 = d1; d1 = d } else if (d < d2) d2 = d
                    }
                    return 1 - smooth(0.0, 0.05, sqrt(d2) - sqrt(d1))
                }
                c = mix(c, C, cracks(1.6, 0.0) * 0.75)
                c = mix(c, C, cracks(4.0, 3.3) * 0.35)
                val frost = smooth(0.75, 0.95, noise(px * 60, py * 60))
                c = DoubleArray(3) { c[it] + 0.08 * frost + 0.03 * noise(px * 140, py * 140) }
            }
            4 -> {
                val wv = sin(py * 26 + fbm(px * 1.2, py * 1.2) * 5)
                c = mix(B, A, 0.5 + 0.5 * wv)
                val n = 0.95 + 0.08 * noise(px * 120, py * 120)
                c = DoubleArray(3) { c[it] * n }
            }
            else -> {
                c = mix(A, B, fbm(px * 1.6, py * 1.6))
                c = mix(c, C, smooth(0.55, 0.8, fbm(px * 3.3 + 5, py * 3.3 + 5)) * 0.5)
                val n = 0.95 + 0.10 * noise(px * 80, py * 80)
                c = DoubleArray(3) { c[it] * n }
            }
        }
        val ux = sx / sw - 0.5; val uy = (sy / sh - 0.42) * 1.1
        val vig = smooth(1.3, 0.3, sqrt(ux * ux + uy * uy))
        val k = 0.80 + (1.0 - 0.80) * vig
        return DoubleArray(3) { c[it] * k }
    }

    /**
     * Rich varnished planks laid horizontally: per-plank tone, flowing grain that swirls round knots,
     * rounded plank edges, dark gaps between planks and a soft varnish sheen.
     */
    private fun planks(w: World, x: Double, y: Double, sw: Double, sh: Double): DoubleArray {
        val ph = sw * 0.30
        val k = floor(y / ph)
        val v = (y - k * ph) / ph
        fun rnd(a: Double, b: Double) = fract(sin(a * 127.1 + b * 311.7) * 43758.5453)
        val r1 = rnd(k, 1.0); val r2 = rnd(k, 2.0); val r3 = rnd(k, 3.0)
        val xs = x / sw
        val ar = ph / sw
        // ring field: grain runs along the plank; big cathedral arches; irregular ring spacing
        var t = (v - 0.5 + (r1 - 0.5) * 0.6) * 6.0
        t += fbm(xs * 1.1 + r2 * 10.0, k * 1.7) * 2.6
        val cx = 0.12 + r3 * 0.76
        val arch = (xs - cx) * (xs - cx) * (7.0 + r1 * 11.0) * (if (r2 > 0.5) 1.0 else -1.0)
        t += arch
        // knots: rings swirl around them
        var knot = 0.0
        var knotRim = 0.0
        val nKnots = if (r1 < 0.45) 0 else if (r1 < 0.9) 1 else 2
        for (i in 0 until nKnots) {
            val kx = 0.08 + rnd(k, 10.0 + i) * 0.84
            val ky = 0.30 + rnd(k, 20.0 + i) * 0.40
            val ks = 0.022 + rnd(k, 30.0 + i) * 0.03
            val dx = (xs - kx) / ks
            val dy = (v - ky) * ar / (ks * 0.60)
            val d = sqrt(dx * dx + dy * dy)
            t += 4.5 / (1.0 + d * d * 0.22)
            knot = max(knot, 1.0 - smooth(0.45, 0.95, d))
            knotRim = max(knotRim, (1.0 - smooth(0.9, 1.6, d)) * smooth(0.6, 0.95, d))
        }
        // irregular spacing: warp the ring index by noise of itself
        t += 0.55 * sin(t * 1.7 + fbm(xs * 3.0, k) * 4.0)
        val ring = fract(t)
        val late = 1.0 - smooth(0.0, 0.085, ring)
        val band = smooth(0.10, 0.50, ring) * (1.0 - smooth(0.58, 1.0, ring))
        val fine = 1.0 - smooth(0.0, 0.07, fract(t * 4.0 + 0.37))
        val light = rgb(w.a); val mid = rgb(w.b); val line = rgb(w.c)
        val deep = rgb(0xA24E18)
        var c = mix(mid, light, 0.10 + 0.90 * band)
        c = mix(c, deep, 0.55 * smooth(0.40, 0.78, fbm(xs * 1.3 + k * 1.3, v * 0.9 + k)))
        c = mix(c, line, late * 0.70)
        c = mix(c, line, fine * 0.24)
        // fine fibres along the plank
        val fiber = noise(x * 0.012, y * 0.6 + k * 7.0) * 0.55 + noise(x * 0.035, y * 1.8) * 0.45
        c = DoubleArray(3) { c[it] * (0.90 + 0.15 * fiber) }
        // knot core
        c = mix(c, rgb(0x5A2508), knotRim * 0.6)
        c = mix(c, rgb(0x3A1606), knot * 0.92)
        c = mix(c, rgb(0x7A3A12), knot * smooth(0.6, 1.0, knot) * 0.35)
        // per-plank tone
        val tone = 0.86 + 0.24 * r2
        c = DoubleArray(3) { c[it] * tone }
        // rounded plank edges, top bevel highlight, dark gaps
        val et = v * ph; val eb = (1.0 - v) * ph
        val gap = sw * 0.007
        val edgeShade = (0.70 + 0.30 * smooth(gap, ph * 0.16, et)) * (0.62 + 0.38 * smooth(gap, ph * 0.20, eb))
        c = DoubleArray(3) { c[it] * edgeShade }
        val bevel = smooth(gap, gap * 2.0, et) * (1.0 - smooth(gap * 2.0, gap * 5.0, et))
        c = DoubleArray(3) { c[it] + 0.10 * bevel }
        val inGap = 1.0 - smooth(gap * 0.4, gap, minOf(et, eb * 2.0))
        c = mix(c, rgb(0x1A0B04), inGap)
        // varnish sheen and vignette
        val ux = x / sw; val uy = y / sh
        val sheen = kotlin.math.exp(-((ux - 0.32) * (ux - 0.32) * 5.0 + (uy - 0.28) * (uy - 0.28) * 2.2)) * 0.10
        c = DoubleArray(3) { c[it] + sheen }
        val vx = ux - 0.5; val vy = (uy - 0.45) * 1.1
        val vig = 0.74 + 0.26 * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy))
        return DoubleArray(3) { c[it] * vig }
    }

    /**
     * Teak / walnut veneer, grain running top to bottom: broad caramel and dark-brown streaks, dense wavy
     * fine grain, small dark pore ticks, a few small knots and a satin sheen. No planks or seams.
     */
    private fun veneer(w: World, x: Double, y: Double, sw: Double, sh: Double): DoubleArray {
        val px = x / sw * 4.0
        val py = y / sw * 4.0
        // slow sideways wander so the grain flows and waves down the board
        val warp = (fbm(px * 0.35, py * 0.05) - 0.5) * 1.7 + (fbm(px * 1.3 + 7.0, py * 0.16) - 0.5) * 0.45
        val gx = px + warp
        val light = rgb(w.a); val mid = rgb(w.b); val dark = rgb(w.c)
        // broad vertical tone streaks with fairly crisp edges
        val streak = fbm(gx * 1.25, py * 0.035 + 3.0)
        var c = mix(dark, mid, smooth(0.30, 0.44, streak))
        c = mix(c, light, smooth(0.50, 0.62, streak) * 0.92)
        val streak2 = fbm(gx * 2.6 + 11.0, py * 0.07)
        c = mix(c, mid, smooth(0.55, 0.75, streak2) * 0.35)
        c = mix(c, dark, smooth(0.62, 0.82, fbm(gx * 4.2 + 23.0, py * 0.10)) * 0.45)
        // elongated cathedral flames in a few places
        val ax = floor(px * 0.9)
        val acx = (ax + 0.2 + hash(ax, 5.1) * 0.6) / 0.9
        val acy = hash(ax, 9.3) * 20.0
        val arch = sqrt((gx - acx) * (gx - acx) * 9.0 + ((py - acy) * 0.08) * ((py - acy) * 0.08) * 9.0)
        val archLines = 1.0 - smooth(0.0, 0.12, fract(arch * 3.0))
        val archMask = 1.0 - smooth(0.6, 1.4, arch)
        c = mix(c, dark, archLines * archMask * 0.45)
        // dense wavy fine grain
        val l1 = 1.0 - smooth(0.0, 0.09, fract(gx * 13.0 + fbm(gx * 2.2, py * 0.14) * 4.0))
        val l2 = 1.0 - smooth(0.0, 0.18, fract(gx * 31.0 + fbm(gx * 5.0, py * 0.32) * 5.0))
        val l3 = 1.0 - smooth(0.0, 0.05, fract(gx * 5.0 + fbm(gx * 1.1 + 4.0, py * 0.08) * 3.0))
        c = mix(c, DoubleArray(3) { dark[it] * 0.85 }, l1 * 0.50)
        c = mix(c, dark, l2 * 0.24)
        c = mix(c, DoubleArray(3) { dark[it] * 0.7 }, l3 * 0.55)
        // pore ticks: short dark dashes along the grain
        val cx = floor(gx * 10.0); val cy = floor(py * 1.6)
        if (hash(cx, cy) > 0.93) {
            val ox = 0.3 + hash(cx + 1.7, cy) * 0.4; val oy = 0.2 + hash(cx, cy + 3.1) * 0.6
            val dx = (gx * 10.0 - cx - ox) / 0.09; val dy = (py * 1.6 - cy - oy) / 0.42
            val d = sqrt(dx * dx + dy * dy)
            c = mix(c, DoubleArray(3) { dark[it] * 0.55 }, (1.0 - smooth(0.5, 1.0, d)) * 0.85)
        }
        // a few small knots
        val kx = floor(px * 1.6); val ky = floor(py * 0.35)
        if (hash(kx + 0.5, ky + 0.5) > 0.72) {
            val ox = 0.25 + hash(kx, ky + 7.7) * 0.5; val oy = 0.3 + hash(kx + 3.3, ky) * 0.4
            val dx = (px * 1.6 - kx - ox) / 0.035; val dy = (py * 0.35 - ky - oy) / 0.020
            val d = sqrt(dx * dx + dy * dy)
            c = mix(c, DoubleArray(3) { dark[it] * 0.5 }, (1.0 - smooth(0.55, 1.0, d)) * 0.9)
            c = mix(c, dark, (1.0 - smooth(1.0, 2.2, d)) * smooth(0.8, 1.2, d) * 0.4)
        }
        // fibre and satin sheen
        val fiber = noise(gx * 70.0, py * 1.5) * 0.6 + noise(gx * 160.0, py * 3.0) * 0.4
        c = DoubleArray(3) { c[it] * (0.92 + 0.12 * fiber) }
        val ux = x / sw; val uy = y / sh
        val sheen = kotlin.math.exp(-((ux - 0.35) * (ux - 0.35) * 4.0 + (uy - 0.25) * (uy - 0.25) * 2.0)) * 0.07
        c = DoubleArray(3) { c[it] + sheen }
        val vx = ux - 0.5; val vy = (uy - 0.45) * 1.1
        val vig = 0.78 + 0.22 * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy))
        return DoubleArray(3) { c[it] * vig }
    }

    /**
     * Straight-grain hardwood, grain running top to bottom. Calmer than [veneer]: no knots, soft streaks,
     * optional quarter-sawn ray flecks (oak) and ribbon stripe (mahogany).
     */
    class Grain(
        val light: Int, val mid: Int, val dark: Int,
        val streaks: Double = 0.6, val lines: Double = 0.35, val density: Double = 11.0,
        val wave: Double = 1.0, val rays: Double = 0.0, val ribbon: Double = 0.0, val pores: Double = 0.3,
        val cathedral: Double = 0.0,
    )

    /** Warm honey oak (world 1 before the maple redesign, unused) with soft streaks and a few cathedral flames. */
    private val HONEY_OAK = Grain(0xE0AE6C, 0xBF8545, 0x6B4019, streaks = 0.7, lines = 0.40, density = 9.0, wave = 0.7, cathedral = 0.55, pores = 0.35)

    /** World 1 (current): pale illustrated maple, wide soft wavy grain, low contrast so cards and text read. */
    private val LIGHT_MAPLE = Grain(0xF4D3A3, 0xE5B67C, 0xB97B42, streaks = 0.55, lines = 0.42, density = 4.0, wave = 2.3, cathedral = 0.40, pores = 0.0)

    /** Rich polished teak for the level preview tile on cream cards (board_wood.jpg). */
    private val CARD_TEAK = Grain(0xE8994A, 0xC46F2C, 0x6A300E, streaks = 1.0, lines = 0.55, density = 5.5, wave = 1.1, cathedral = 0.55, pores = 0.35)

    fun grain(g: Grain, x: Double, y: Double, sw: Double, sh: Double): DoubleArray {
        val px = x / sw * 4.0
        val py = y / sw * 4.0
        val warp = ((fbm(px * 0.35, py * 0.05) - 0.5) * 1.5 + (fbm(px * 1.3 + 7.0, py * 0.16) - 0.5) * 0.35) * g.wave
        val gx = px + warp
        val light = rgb(g.light); val mid = rgb(g.mid); val dark = rgb(g.dark)
        // soft broad tone streaks
        val s = fbm(gx * 1.05, py * 0.03 + 3.0)
        var c = mix(mid, light, smooth(0.5 - 0.22 / g.streaks.coerceAtLeast(0.2), 0.5 + 0.22 / g.streaks.coerceAtLeast(0.2), s))
        c = mix(c, mid, smooth(0.55, 0.8, fbm(gx * 2.4 + 11.0, py * 0.06)) * 0.4 * g.streaks)
        c = mix(c, dark, smooth(0.66, 0.86, fbm(gx * 3.6 + 23.0, py * 0.08)) * 0.25 * g.streaks)
        // ribbon stripe: alternating light/dark bands that catch the light differently
        if (g.ribbon > 0) {
            val r = sin(gx * 16.0 + fbm(gx * 1.5, py * 0.08) * 6.0)
            val m = smooth(0.35, 0.65, fbm(gx * 0.9 + 3.0, py * 0.04))
            c = DoubleArray(3) { c[it] * (1.0 + g.ribbon * 0.10 * r * (0.4 + 0.6 * m)) }
        }
        // cathedral arches: nested flame shapes pointing up the board
        if (g.cathedral > 0) {
            val ax = floor(px * 0.55)
            val acx = (ax + 0.25 + hash(ax, 5.1) * 0.5) / 0.55
            val acy = 1.0 + hash(ax, 9.3) * 6.0
            val dx = gx - acx
            val t = dx * dx * 5.0 - (py - acy) * 0.35
            val arch = 1.0 - smooth(0.0, 0.16, fract(t * 2.2 + fbm(gx * 2.0, py * 0.2) * 0.8))
            val mask = (1.0 - smooth(0.5, 1.6, kotlin.math.abs(dx) * 1.4)) * smooth(-0.6, 0.6, py - acy)
            c = mix(c, dark, arch * mask * g.cathedral)
            c = mix(c, light, (1.0 - smooth(0.2, 0.6, fract(t * 2.2 + 0.5))) * mask * g.cathedral * 0.25)
        }
        // fine grain lines
        val l1 = 1.0 - smooth(0.0, 0.10, fract(gx * g.density + fbm(gx * 2.2, py * 0.14) * 4.0))
        val l2 = 1.0 - smooth(0.0, 0.16, fract(gx * g.density * 2.6 + fbm(gx * 5.0, py * 0.3) * 5.0))
        c = mix(c, dark, l1 * g.lines)
        c = mix(c, dark, l2 * g.lines * 0.45)
        // quarter-sawn ray flecks: pale satin flakes lying across the grain
        if (g.rays > 0) {
            val fx = gx * 3.2; val fy = py * 1.3
            val cx = floor(fx); val cy = floor(fy)
            for (j in -1..1) for (i in -1..1) {
                val hx = cx + i; val hy = cy + j
                if (hash(hx, hy) < 0.6) continue
                val ox = hx + 0.5 + (hash(hx + 3.1, hy) - 0.5) * 0.6
                val oy = hy + 0.5 + (hash(hx, hy + 5.7) - 0.5) * 0.6
                val tilt = (hash(hx + 9.2, hy + 1.3) - 0.5) * 0.8
                val dxx = fx - ox; val dyy = fy - oy - dxx * tilt
                val wob = 1.0 + 0.35 * (noise(fx * 6.0, fy * 6.0) - 0.5)
                val d = sqrt((dxx / 0.22) * (dxx / 0.22) + (dyy / 0.05) * (dyy / 0.05)) * wob
                val m = 1.0 - smooth(0.6, 1.0, d)
                if (m > 0) c = mix(c, DoubleArray(3) { light[it] * 1.08 }, m * g.rays)
            }
        }
        // pores: tiny dark dashes along the grain
        if (g.pores > 0) {
            val cx = floor(gx * 14.0); val cy = floor(py * 2.2)
            if (hash(cx, cy) > 0.9) {
                val ox = 0.3 + hash(cx + 1.7, cy) * 0.4; val oy = 0.2 + hash(cx, cy + 3.1) * 0.6
                val dx = (gx * 14.0 - cx - ox) / 0.08; val dy = (py * 2.2 - cy - oy) / 0.35
                c = mix(c, DoubleArray(3) { dark[it] * 0.8 }, (1.0 - smooth(0.5, 1.0, sqrt(dx * dx + dy * dy))) * g.pores)
            }
        }
        val fiber = noise(gx * 70.0, py * 1.5) * 0.6 + noise(gx * 160.0, py * 3.0) * 0.4
        c = DoubleArray(3) { c[it] * (0.94 + 0.09 * fiber) }
        val ux = x / sw; val uy = y / sh
        val sheen = kotlin.math.exp(-((ux - 0.35) * (ux - 0.35) * 4.0 + (uy - 0.25) * (uy - 0.25) * 2.0)) * 0.06
        c = DoubleArray(3) { c[it] + sheen }
        val vx = ux - 0.5; val vy = (uy - 0.45) * 1.1
        val vig = 0.80 + 0.20 * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy))
        return DoubleArray(3) { c[it] * vig }
    }

    /** Figured burl: grain swirls round clusters of small dark eyes. */
    fun burl(light: Int, mid: Int, dark: Int, x: Double, y: Double, sw: Double, sh: Double): DoubleArray {
        val px = x / sw * 4.0; val py = y / sw * 4.0
        val wx = px + (fbm(px * 0.7, py * 0.7) - 0.5) * 2.4
        val wy = py + (fbm(px * 0.7 + 5.2, py * 0.7 + 1.3) - 0.5) * 2.4
        val sc = 1.6
        val gx = floor(wx * sc); val gy = floor(wy * sc)
        var f = fbm(wx * 1.2, wy * 1.2) * 2.5
        var eye = 0.0
        for (j in -1..1) for (i in -1..1) {
            val hx = gx + i; val hy = gy + j
            val ex = (hx + hash(hx, hy)) / sc; val ey = (hy + hash(hx + 7.7, hy + 3.3)) / sc
            val d = sqrt((wx - ex) * (wx - ex) + (wy - ey) * (wy - ey))
            f += 1.1 / (1.0 + d * d * 30.0)
            if (hash(hx + 1.1, hy + 2.2) > 0.35) eye = max(eye, 1.0 - smooth(0.012, 0.03, d))
        }
        val r = fract(f * 2.2)
        val band = smooth(0.0, 0.35, r) * (1.0 - smooth(0.55, 1.0, r))
        val L = rgb(light); val M = rgb(mid); val D = rgb(dark)
        var c = mix(M, L, 0.15 + 0.85 * band)
        c = mix(c, D, (1.0 - smooth(0.0, 0.05, r)) * 0.32)
        c = mix(c, M, smooth(0.45, 0.75, fbm(px * 0.8 + 9.0, py * 0.8)) * 0.35)
        c = mix(c, DoubleArray(3) { D[it] * 0.6 }, eye * 0.9)
        val ux = x / sw; val uy = y / sh
        val sheen = kotlin.math.exp(-((ux - 0.35) * (ux - 0.35) * 4.0 + (uy - 0.25) * (uy - 0.25) * 2.0)) * 0.06
        c = DoubleArray(3) { c[it] + sheen }
        val vx = ux - 0.5; val vy = (uy - 0.45) * 1.1
        val vig = 0.80 + 0.20 * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy))
        return DoubleArray(3) { c[it] * vig }
    }

    /** Side-by-side comparison of candidate woods for world 1, with a mock carved maze on each. */
    fun preview(out: File, width: Int = 900, height: Int = 1950) {
        val cands: List<Pair<String, (Double, Double) -> DoubleArray>> = listOf(
            "1 Dark Walnut" to { x, y -> grain(Grain(0x96643E, 0x6A4329, 0x2C180B, streaks = 0.9, lines = 0.42, density = 10.0, cathedral = 0.45, pores = 0.4), x, y, width.toDouble(), height.toDouble()) },
            "2 Honey Oak" to { x, y -> grain(HONEY_OAK, x, y, width.toDouble(), height.toDouble()) },
            "3 Mahogany" to { x, y -> grain(Grain(0xAE6040, 0x86412A, 0x3E160A, streaks = 0.6, lines = 0.30, density = 14.0, wave = 0.4, ribbon = 1.0, pores = 0.25), x, y, width.toDouble(), height.toDouble()) },
            "4 Smoked Ash" to { x, y -> grain(Grain(0xB8AA98, 0x928472, 0x4A4137, streaks = 0.8, lines = 0.45, density = 8.0, wave = 0.8, cathedral = 0.4, pores = 0.3), x, y, width.toDouble(), height.toDouble()) },
            "6 Light Maple" to { x, y -> grain(LIGHT_MAPLE, x, y, width.toDouble(), height.toDouble()) },
            "5 Amber Burl" to { x, y -> burl(0xD9A062, 0xAC6C36, 0x4A2410, x, y, width.toDouble(), height.toDouble()) },
        )
        val tw = width / 3; val th = height / 3; val gap = 16
        val sheet = BufferedImage(cands.size * (tw + gap) + gap, th + 70, BufferedImage.TYPE_INT_RGB)
        val g2 = sheet.createGraphics()
        g2.color = java.awt.Color(0x1B1B1B); g2.fillRect(0, 0, sheet.width, sheet.height)
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        for ((k, cand) in cands.withIndex()) {
            val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until height) for (x in 0 until width) {
                val c = cand.second(x + 0.5, y + 0.5)
                val r = (c[0].coerceIn(0.0, 1.0) * 255).toInt(); val g = (c[1].coerceIn(0.0, 1.0) * 255).toInt(); val b = (c[2].coerceIn(0.0, 1.0) * 255).toInt()
                img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
            // mock carved maze so the contrast with the espresso channel can be judged
            val gi = img.createGraphics()
            gi.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            gi.stroke = java.awt.BasicStroke(70f, java.awt.BasicStroke.CAP_SQUARE, java.awt.BasicStroke.JOIN_MITER)
            val path = java.awt.geom.Path2D.Double().apply {
                moveTo(180.0, 600.0); lineTo(720.0, 600.0); lineTo(720.0, 900.0); lineTo(300.0, 900.0); lineTo(300.0, 1200.0)
                lineTo(720.0, 1200.0); lineTo(720.0, 1500.0); lineTo(180.0, 1500.0); moveTo(180.0, 600.0); lineTo(180.0, 1100.0)
            }
            gi.color = java.awt.Color(0x1B0D05); gi.draw(path)
            gi.stroke = java.awt.BasicStroke(58f, java.awt.BasicStroke.CAP_SQUARE, java.awt.BasicStroke.JOIN_MITER)
            gi.color = java.awt.Color(0x2B160A); gi.draw(path)
            gi.dispose()
            val ox = gap + k * (tw + gap)
            g2.drawImage(img, ox, gap, tw, th, null)
            g2.color = java.awt.Color(0xF2E6D0); g2.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 22)
            g2.drawString(cand.first, ox + 6, th + gap + 36)
        }
        g2.dispose()
        out.parentFile?.mkdirs()
        ImageIO.write(sheet, "png", out)
        println("  ${out.path}")
    }

    fun run(outDir: File, width: Int = 900, height: Int = 1950) {
        outDir.mkdirs()
        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        for ((i, w) in worlds.withIndex()) {
            val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until height) for (x in 0 until width) {
                val c = surface(w, x + 0.5, y + 0.5, width.toDouble(), height.toDouble())
                val r = (c[0].coerceIn(0.0, 1.0) * 255).toInt(); val g = (c[1].coerceIn(0.0, 1.0) * 255).toInt(); val b = (c[2].coerceIn(0.0, 1.0) * 255).toInt()
                img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
            val f = File(outDir, "surface_$i.jpg")
            f.delete()
            ImageIO.createImageOutputStream(f).use { out ->
                writer.output = out
                val p = writer.defaultWriteParam.apply { compressionMode = ImageWriteParam.MODE_EXPLICIT; compressionQuality = 0.86f }
                writer.write(null, IIOImage(img, null, null), p)
            }
            println("  ${f.path}  ${f.length() / 1024} KB")
        }
        // the level-preview wood used on cream cards
        run {
            val bw = 600; val bh = 900
            val img = BufferedImage(bw, bh, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until bh) for (x in 0 until bw) {
                val c = grain(CARD_TEAK, x + 0.5, y + 0.5, bw.toDouble(), bh.toDouble())
                val r = (c[0].coerceIn(0.0, 1.0) * 255).toInt(); val g = (c[1].coerceIn(0.0, 1.0) * 255).toInt(); val b = (c[2].coerceIn(0.0, 1.0) * 255).toInt()
                img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
            val f = File(outDir, "board_wood.jpg")
            f.delete()
            ImageIO.createImageOutputStream(f).use { out ->
                writer.output = out
                val p = writer.defaultWriteParam.apply { compressionMode = ImageWriteParam.MODE_EXPLICIT; compressionQuality = 0.88f }
                writer.write(null, IIOImage(img, null, null), p)
            }
            println("  ${f.path}  ${f.length() / 1024} KB")
        }
        writer.dispose()
    }
}
