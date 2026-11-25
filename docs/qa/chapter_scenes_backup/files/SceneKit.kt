package com.ngstudio.maze.tools

import com.ngstudio.maze.tools.MaterialArt.fbm
import com.ngstudio.maze.tools.MaterialArt.fract
import com.ngstudio.maze.tools.MaterialArt.hash
import com.ngstudio.maze.tools.MaterialArt.mix
import com.ngstudio.maze.tools.MaterialArt.noise
import com.ngstudio.maze.tools.MaterialArt.rgb
import com.ngstudio.maze.tools.MaterialArt.smooth
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One pixel being painted: position, handy coordinates and the colour so far. */
class Ctx(val x: Double, val y: Double, val w: Double, val h: Double) {
    val u = x / w
    val v = y / h
    /** Aspect-correct coordinates in quarter-widths. */
    val px = x / w * 4
    val py = y / w * 4
    var c = doubleArrayOf(0.0, 0.0, 0.0)
}

typealias Layer = (Ctx) -> Unit

/**
 * Building blocks for chapter scenes (SceneArt). Each function returns a layer that paints over the colour
 * so far; a scene is a list of layers, painted in order. Colours are 0xRRGGBB. Positions u, v are 0..1
 * across / down the screen.
 */
object SceneKit {
    private const val TAU = 2 * PI
    private fun add(c: DoubleArray, k: Double) = DoubleArray(3) { c[it] + k }
    private fun addc(c: DoubleArray, col: DoubleArray, k: Double) = DoubleArray(3) { c[it] + col[it] * k }
    private fun mul(c: DoubleArray, k: Double) = DoubleArray(3) { c[it] * k }
    private fun segDist(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax; val dy = by - ay
        val t = (((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        val ex = px - ax - dx * t; val ey = py - ay - dy * t
        return sqrt(ex * ex + ey * ey)
    }
    /** (edge distance, nearest cell hash) of a Voronoi pattern. */
    private fun voronoi(px: Double, py: Double, seed: Double): Pair<Double, Double> {
        val gx = floor(px); val gy = floor(py); val fx = px - gx; val fy = py - gy
        var d1 = 8.0; var d2 = 8.0; var id = 0.0
        for (j in -1..1) for (i in -1..1) {
            val rx = i + hash(gx + i + seed, gy + j) - fx
            val ry = j + hash(gx + i + 19.1 + seed, gy + j + 19.1) - fy
            val d = rx * rx + ry * ry
            if (d < d1) { d2 = d1; d1 = d; id = hash(gx + i + 7.3, gy + j + seed) } else if (d < d2) d2 = d
        }
        return Pair(sqrt(d2) - sqrt(d1), id)
    }
    /** Distance in screen widths (aspect-correct) from (u, v) to (cu, cv). */
    private fun dist(k: Ctx, cu: Double, cv: Double): Double { val dx = k.u - cu; val dy = (k.v - cv) * k.h / k.w; return sqrt(dx * dx + dy * dy) }

    // ------------------------------------------------------------------------------------------ base
    fun grad(vararg stops: Int): Layer = { k ->
        val t = k.v.coerceIn(0.0, 1.0) * (stops.size - 1)
        val i = min(floor(t).toInt(), stops.size - 2)
        k.c = mix(rgb(stops[i]), rgb(stops[i + 1]), smooth(0.0, 1.0, t - i))
    }
    fun flat(col: Int): Layer = { k -> k.c = rgb(col) }
    fun glow(cu: Double, cv: Double, col: Int, r: Double, a: Double): Layer = { k ->
        val d = dist(k, cu, cv); k.c = mix(k.c, rgb(col), a * exp(-(d / r) * (d / r)))
    }
    fun mottle(col: Int, scale: Double, a: Double): Layer = { k -> k.c = mix(k.c, rgb(col), a * smooth(0.45, 0.75, fbm(k.px * scale, k.py * scale))) }
    fun grain(a: Double = 0.06): Layer = { k -> k.c = mul(k.c, 1 - a / 2 + a * noise(k.x * 0.9, k.y * 0.9)) }
    fun vig(a: Double = 0.12): Layer = { k ->
        val vx = k.u - 0.5; val vy = (k.v - 0.45) * 1.1
        k.c = mul(k.c, 1 - a + a * smooth(1.25, 0.25, sqrt(vx * vx + vy * vy)))
    }

    // ------------------------------------------------------------------------------------------ sky
    fun sunDisc(cu: Double, cv: Double, r: Double, col: Int, halo: Int = col, stripes: Boolean = false): Layer = { k ->
        val d = dist(k, cu, cv)
        k.c = mix(k.c, rgb(halo), 0.45 * exp(-((d / (r * 2.4)).pow(2))))
        var m = 1 - smooth(r * 0.97, r, d)
        if (stripes && k.v > cv) { val s = fract((k.v - cv) / (r * k.w / k.h) * 5); if (s > 0.62 + (k.v - cv) * 2) m = 0.0 }
        k.c = mix(k.c, mix(rgb(col), rgb(0xFFFFFF), 0.25 * (1 - smooth(0.0, r, d + (k.v - cv)))), m)
    }
    fun stars(cell: Double, prob: Double, rad: Double, tint: Int = 0xEAF2FF, bright: Double = 0.9, vMax: Double = 1.0): Layer = { k ->
        if (k.v < vMax) {
            val cx = floor(k.x / cell); val cy = floor(k.y / cell)
            if (hash(cx + cell, cy) > prob) {
                val ox = (cx + 0.2 + hash(cx, cy + cell) * 0.6) * cell; val oy = (cy + 0.2 + hash(cx + 2.2, cy) * 0.6) * cell
                val d = sqrt((k.x - ox) * (k.x - ox) + (k.y - oy) * (k.y - oy))
                val tw = 0.6 + 0.4 * hash(cx + 9.1, cy)
                k.c = mix(k.c, rgb(tint), (1 - smooth(rad * 0.5, rad * 1.6, d)) * bright * tw)
                if (cell > 50) k.c = add(k.c, 0.08 * exp(-d * d / (rad * rad * 16)))
            }
        }
    }
    fun nebula(a: Int, b: Int, amt: Double, scale: Double = 0.8): Layer = { k ->
        val wx = k.px + (fbm(k.px * 0.5, k.py * 0.5) - 0.5) * 1.6; val wy = k.py + (fbm(k.px * 0.5 + 5, k.py * 0.5 + 2) - 0.5) * 1.6
        k.c = mix(k.c, rgb(a), amt * smooth(0.45, 0.75, fbm(wx * scale, wy * scale)))
        k.c = mix(k.c, rgb(b), amt * 0.7 * smooth(0.5, 0.8, fbm(wx * scale + 7, wy * scale)))
    }
    fun swirl(cu: Double, cv: Double, a: Int, b: Int, amt: Double): Layer = { k ->
        val dx = k.u - cu; val dy = (k.v - cv) * k.h / k.w
        val r = sqrt(dx * dx + dy * dy); val ang = atan2(dy, dx)
        val arm = 0.5 + 0.5 * sin(ang * 2 + r * 18 + fbm(k.px, k.py) * 2)
        val m = arm.pow(3.0) * exp(-r * 2.2) * amt
        k.c = mix(k.c, mix(rgb(a), rgb(b), smooth(0.0, 0.5, r)), m)
        k.c = add(k.c, 0.35 * exp(-r * r * 90))
    }
    fun aurora(a: Int, b: Int, amt: Double = 0.8): Layer = { k ->
        if (k.v < 0.6) {
            val xw = k.u * 3 + (fbm(k.u * 2, k.v * 0.8 + 3) - 0.5) * 2.2
            val band = (0.5 + 0.5 * sin(xw * 5)).pow(4.0)
            val vert = smooth(0.04, 0.22, k.v) * smooth(0.58, 0.28, k.v)
            val streak = 0.6 + 0.4 * noise(k.u * 120, k.v * 3)
            k.c = addc(k.c, mix(rgb(a), rgb(b), smooth(0.1, 0.5, k.v)), amt * band * vert * streak)
        }
    }
    fun clouds(lit: Int, shade: Int, bands: List<Double>, alpha: Double = 0.92, scale: Double = 0.55): Layer = { k ->
        for ((i, bv) in bands.withIndex()) {
            val bandY = bv * k.h / k.w * 4
            fun dens(qx: Double, qy: Double): Double { val band = exp(-((qy - bandY) * (qy - bandY)) * 1.6); return fbm(qx * scale + i * 3.1, qy * 1.1 + i * 1.7) * 1.25 * band + 0.15 * band }
            val d = dens(k.px, k.py)
            val m = smooth(0.50, 0.56, d)
            if (m > 0) {
                val litK = smooth(0.62, 0.45, dens(k.px, k.py - 0.12))
                k.c = mix(k.c, mix(rgb(shade), rgb(lit), 0.35 + 0.65 * litK), m * alpha)
            }
        }
    }
    fun rainbow(cu: Double, cv: Double, r: Double, width: Double, alpha: Double = 0.55): Layer = { k ->
        val d = dist(k, cu, cv)
        val t = (d - (r - width)) / width
        if (t in 0.0..1.0 && k.v < cv + 0.02) {
            val cols = intArrayOf(0x8E7CFF, 0x4FA8FF, 0x5CD67A, 0xFFE14D, 0xFFA040, 0xFF5C5C)
            val i = min((t * cols.size).toInt(), cols.size - 1)
            k.c = mix(k.c, rgb(cols[i]), alpha * smooth(0.0, 0.08, t) * smooth(1.0, 0.92, t))
        }
    }
    fun rays(col: Int, amt: Double, n: Double = 13.0): Layer = { k ->
        val r = (0.5 + 0.5 * sin(k.u * n + k.v * 2.5 + fbm(k.u * 2, k.v * 0.5) * 3)).pow(6.0) + (0.5 + 0.5 * sin(k.u * n * 0.55 - k.v * 1.5 + 2)).pow(8.0)
        k.c = addc(k.c, rgb(col), amt * r * (1 - k.v) * (1 - k.v))
    }
    fun sunburst(cu: Double, cv: Double, col: Int, n: Int, alpha: Double): Layer = { k ->
        val a = atan2((k.v - cv) * k.h / k.w, k.u - cu)
        val s = 0.5 + 0.5 * sin(a * n)
        k.c = mix(k.c, rgb(col), alpha * smooth(0.45, 0.55, s))
    }
    fun meteors(col: Int, n: Int): Layer = { k ->
        for (i in 0 until n) {
            val sx = hash(i.toDouble(), 3.3) * 1.2; val sy = hash(i.toDouble(), 7.7) * 0.6
            val len = 0.10 + 0.12 * hash(i.toDouble(), 1.1)
            val ex = sx - len; val ey = sy + len * 0.55
            val d = segDist(k.u, k.v * k.h / k.w, sx, sy * k.h / k.w, ex, ey * k.h / k.w)
            val along = ((k.u - ex) / (sx - ex)).coerceIn(0.0, 1.0)
            k.c = mix(k.c, rgb(col), (1 - smooth(0.0, 0.004, d)) * (1 - along) * 0.9)
        }
    }
    fun lightning(cu: Double, top: Double, bottom: Double, col: Int): Layer = { k ->
        if (k.v in top..bottom) {
            val t = (k.v - top) / (bottom - top)
            val zig = cu + 0.05 * sin(t * 18) + 0.03 * sin(t * 41 + 1) + (fbm(t * 6, 2.0) - 0.5) * 0.08
            val d = abs(k.u - zig) * k.w
            k.c = mix(k.c, rgb(col), (1 - smooth(1.5, 4.0, d)))
            k.c = addc(k.c, rgb(col), 0.25 * exp(-d * d / 900))
        }
    }

    // ------------------------------------------------------------------------------------------ land
    fun hills(base: Double, amp: Double, freq: Double, phase: Double, top: Int, bottom: Int, rim: Int? = null): Layer = { k ->
        val crest = base - amp * (0.5 + 0.5 * sin(k.u * freq * TAU + phase)) - 0.012 * (fbm(k.u * 6 + phase, phase) - 0.5)
        if (k.v > crest) {
            var col = mix(rgb(top), rgb(bottom), smooth(crest, crest + 0.28, k.v))
            col = mul(col, 0.96 + 0.06 * noise(k.x * 0.05, k.y * 0.05))
            if (rim != null) col = mix(col, rgb(rim), 1 - smooth(0.0, 0.006, k.v - crest))
            k.c = col
        }
    }
    fun mountains(base: Double, amp: Double, kf: Double, ph: Double, rock: Int, cap: Int?, haze: Int? = null): Layer = { k ->
        val t = fract(k.u * kf + ph)
        val tri = 1 - abs(t * 2 - 1)
        val top = base - amp * (tri + 0.12 * (fbm(k.u * 9 + ph, ph) - 0.5))
        if (k.v > top) {
            val depth = k.v - top
            var m = if (haze != null) mix(rgb(rock), rgb(haze), smooth(0.0, 0.3, depth)) else rgb(rock)
            val ridge = (t - 0.5) / (kf * 0.5)
            val faceT = smooth(-0.015, 0.015, ridge)
            m = mul(m, 1.0 + (0.06 - 0.13 * faceT) * (1 - smooth(0.0, 0.09, depth)))
            if (cap != null) { val cl = 0.04 + 0.02 * fbm(k.u * 20 + ph, k.v * 10); m = mix(m, mul(rgb(cap), 1 - 0.08 * faceT), 1 - smooth(cl, cl + 0.01, depth)) }
            k.c = m
        }
    }
    fun treeline(base: Double, height: Double, col: Int, kf: Double, seed: Double = 0.0, snow: Boolean = false): Layer = { k ->
        if (k.v > base) k.c = rgb(col)
        else {
            val ci = floor(k.u * kf)
            for (i in -1..1) {
                val id = ci + i
                val cx = (id + 0.5 + (hash(id, seed) - 0.5) * 0.5) / kf
                val th = height * (0.55 + 0.45 * hash(id + 3.1, seed))
                val top = base - th
                if (k.v < top) continue
                val hw = (k.v - top) / th * (0.75 / kf) * (1 + 0.15 * sin((k.v - top) / th * 18))
                if (abs(k.u - cx) < hw) {
                    var tc = mul(rgb(col), if (k.u < cx) 1.08 else 0.94)
                    if (snow && (k.v - top) / th < 0.25 + 0.1 * sin(k.u * 200)) tc = rgb(0xF4FAFF)
                    k.c = tc
                }
            }
        }
    }
    fun skyline(base: Double, col: Int, win: Int, kf: Double, maxH: Double = 0.22, seed: Double = 0.0, winProb: Double = 0.55): Layer = { k ->
        val ci = floor(k.u * kf)
        val bh = maxH * (0.35 + 0.65 * hash(ci, seed))
        val top = base - bh
        if (k.v > top) {
            k.c = mul(rgb(col), 0.92 + 0.12 * hash(ci + 1.7, seed))
            val wx = fract(k.u * kf * 4); val wy = fract((k.v - top) * k.h / (k.w / kf / 4) )
            val cellId = floor(k.u * kf * 4) * 131 + floor((k.v - top) * k.h / (k.w / kf / 4))
            if (wx in 0.3..0.7 && wy in 0.3..0.7 && hash(cellId, seed + 2) < winProb && k.v < base + 0.5) k.c = rgb(win)
        }
    }
    fun ground(v0: Double, a: Int, b: Int, wob: Double = 0.012): Layer = { k ->
        val edge = v0 + wob * sin(k.u * 6 + 1) + wob * 0.6 * (fbm(k.u * 8, 3.0) - 0.5)
        if (k.v > edge) k.c = mix(rgb(a), rgb(b), smooth(edge, 1.0, k.v))
    }
    fun ripples(col: Int, freq: Double, amt: Double, vMin: Double = 0.0): Layer = { k ->
        if (k.v > vMin) {
            val r = sin(k.py * freq + k.px * 0.9 + fbm(k.px * 0.7, k.py * 0.7) * 5)
            k.c = mul(k.c, 1 + 0.045 * r * amt); k.c = mix(k.c, rgb(col), 0.3 * amt * smooth(0.82, 1.0, r))
        }
    }
    fun grass(amt: Double = 0.12, vMin: Double = 0.0): Layer = { k ->
        if (k.v > vMin) k.c = mul(k.c, 1 - amt * 0.6 + amt * (noise(k.x * 0.20, k.y * 0.035) * 0.6 + noise(k.x * 0.5, k.y * 0.08) * 0.4))
    }
    fun dunes(top: Double, tones: IntArray): Layer = { k ->
        val n = tones.size
        for (i in 0 until n) {
            val base = top + (1 - top) * i / n
            val cx = k.u * (2.0 + (i % 3) * 0.4) * PI + i * 1.7
            val crest = base - 0.05 * sin(cx) - 0.01 * (fbm(k.u * 5 + i, i * 1.7) - 0.5)
            if (k.v < crest) continue
            var col = mul(rgb(tones[i]), 1.0 + 0.10 * (1 - smooth(0.0, 0.09, k.v - crest)) * cos(cx))
            col = mix(col, rgb(0xFFF1D6), (1 - smooth(0.0, 0.004, k.v - crest)) * 0.5)
            k.c = col
        }
    }
    fun strata(top: Double, cols: IntArray): Layer = { k ->
        val edge = top + 0.04 * sin(k.u * 5 + 2) + 0.03 * (fbm(k.u * 4, 1.0) - 0.5)
        if (k.v > edge) {
            val t = (k.v - edge) * 18 + fbm(k.u * 3, k.v * 2) * 1.5
            k.c = mul(rgb(cols[(floor(t).toInt()).mod(cols.size)]), 0.93 + 0.1 * noise(k.x * 0.1, k.y * 0.02))
        }
    }
    fun mesas(base: Double, col: Int): Layer = { k ->
        val ci = floor(k.u * 3.0)
        val cx = (ci + 0.5) / 3.0; val hw = 0.10 + 0.07 * hash(ci, 4.0); val ht = 0.08 + 0.06 * hash(ci, 8.0)
        val top = base - ht
        val slope = (k.v - top) * 0.8
        if (k.v > top && abs(k.u - cx) < hw + slope) k.c = mul(rgb(col), if (k.u < cx) 1.05 else 0.92)
        if (k.v > base) k.c = rgb(col)
    }
    fun pyramids(base: Double, col: Int): Layer = { k ->
        for ((cx, hw) in listOf(0.30 to 0.16, 0.62 to 0.22, 0.85 to 0.10)) {
            val top = base - hw * 0.9 * k.w / k.h
            if (k.v > top && k.v < base) {
                val half = (k.v - top) / (base - top) * hw
                if (abs(k.u - cx) < half) k.c = mul(rgb(col), if (k.u < cx) 1.06 else 0.86)
            }
        }
    }
    fun pool(cu: Double, cv: Double, rw: Double, rh: Double, water: Int, edge: Int): Layer = { k ->
        val e = ((k.u - cu) / rw).pow(2) + ((k.v - cv) / rh).pow(2) + 0.08 * (fbm(k.u * 6, k.v * 6) - 0.5)
        if (e < 1.08) k.c = mix(k.c, rgb(edge), 1 - smooth(1.0, 1.08, e))
        if (e < 1.0) { k.c = mix(rgb(water), rgb(0xFFFFFF), 0.25 * smooth(0.85, 0.95, noise(k.x * 0.05, k.y * 0.3))) }
    }
    fun craters(v0: Double, a: Int, b: Int): Layer = { k ->
        if (k.v > v0 - 0.02 * sin(k.u * 4)) {
            k.c = mix(rgb(a), rgb(b), smooth(v0, 1.0, k.v)); k.c = mul(k.c, 0.94 + 0.1 * fbm(k.px * 2, k.py * 2))
            val (e, id) = voronoi(k.px * 2.2, k.py * 3.5, 4.0)
            if (id > 0.6) k.c = mul(k.c, 0.85 + 0.15 * smooth(0.0, 0.25, e))
        }
    }
    fun terraces(top: Double, a: Int, b: Int): Layer = { k ->
        if (k.v > top) {
            val t = (k.v - top) * 22 + sin(k.u * 4 + k.v * 3) * 0.8
            val s = fract(t)
            k.c = mul(mix(rgb(a), rgb(b), smooth(top, 1.0, k.v)), 0.88 + 0.16 * smooth(0.0, 0.2, s) - 0.06 * smooth(0.85, 1.0, s))
        }
    }
    fun fields(top: Double, cols: IntArray): Layer = { k ->
        if (k.v > top) {
            // patchwork in perspective: bands get taller towards the viewer, split into fields of random width
            val t = ((k.v - top) / (1 - top)).pow(0.75) * 9
            val band = floor(t + 0.25 * sin(k.u * 3 + floor(t)))
            val segs = 2.0 + band * 0.5
            val sx = k.u * segs + hash(band * 1.31 + 0.17, 4.7)
            val seg = floor(sx)
            val id = hash(band * 2.17 + 0.3, seg * 1.53 + 0.9)
            k.c = mul(rgb(cols[(id * cols.size).toInt().coerceAtMost(cols.size - 1)]), 0.94 + 0.08 * noise(k.x * 0.4, k.y * 0.06))
            // crop rows inside each field
            k.c = mul(k.c, 1.0 - 0.06 * smooth(0.6, 1.0, sin((t - band) * 40 + sx * 2)))
            val edge = min(min(fract(t + 0.25 * sin(k.u * 3 + floor(t))), 1 - fract(t + 0.25 * sin(k.u * 3 + floor(t)))) * 30, min(fract(sx), 1 - fract(sx)) * 25)
            k.c = mix(k.c, rgb(0x4E7A2A), 1 - smooth(0.0, 0.25 + band * 0.04, edge))
        }
    }
    fun rows(top: Double, a: Int, b: Int): Layer = { k ->
        if (k.v > top) {
            val d = (k.v - top) / (1 - top)
            val x = (k.u - 0.5) / (0.15 + d)
            val s = 0.5 + 0.5 * sin(x * 22)
            k.c = mix(rgb(b), rgb(a), smooth(0.3, 0.7, s)); k.c = mul(k.c, 0.9 + 0.15 * noise(k.x * 0.3, k.y * 0.3))
        }
    }
    fun mowed(a: Int, b: Int, n: Int): Layer = { k ->
        k.c = if (floor(k.v * n).toInt() % 2 == 0) rgb(a) else rgb(b)
        k.c = mul(k.c, 0.95 + 0.08 * noise(k.x * 0.25, k.y * 0.05))
    }
    fun castle(base: Double, col: Int): Layer = { k ->
        val towers = listOf(0.15 to 0.13, 0.36 to 0.09, 0.50 to 0.17, 0.64 to 0.09, 0.85 to 0.13)
        if (k.v > base - 0.05 && k.v < base + 0.2) k.c = rgb(col)
        for ((cx, ht) in towers) {
            val hw = 0.045; val top = base - ht
            if (abs(k.u - cx) < hw && k.v > top) {
                val notch = k.v < top + 0.012 && fract((k.u - cx + hw) / (hw * 2) * 3.5) > 0.5
                if (!notch) k.c = mul(rgb(col), if (k.u < cx) 1.06 else 0.92)
            }
            // roof cone
            val rt = top - 0.05
            if (k.v in rt..top && abs(k.u - cx) < (k.v - rt) / 0.05 * hw * 1.2) k.c = mul(rgb(col), 0.8)
        }
    }

    // ------------------------------------------------------------------------------------------ water
    fun waterTop(shore: Double, deep: Int, shallow: Int): Layer = { k ->
        val s = shore + 0.022 * sin(k.u * 7 + 0.6) + 0.012 * sin(k.u * 17 + 2) + 0.02 * (fbm(k.u * 3, 1.3) - 0.5)
        k.c = mix(k.c, mul(k.c, 0.86), smooth(s + 0.06, s, k.v) * 0.6)
        val water = smooth(s - 0.004, s - 0.012, k.v)
        k.c = mix(k.c, add(mix(rgb(shallow), rgb(deep), smooth(s, 0.0, k.v)), smooth(0.78, 0.95, noise(k.x * 0.06, k.y * 0.25)) * 0.25), water)
        k.c = mix(k.c, rgb(0xFFFFFF), (1 - smooth(0.0, 0.012, abs(k.v - s + 0.006 + 0.004 * sin(k.u * 60)))) * 0.85)
    }
    fun seaBand(top: Double, bottom: Double, a: Int, b: Int): Layer = { k ->
        if (k.v in top..bottom) {
            k.c = mix(rgb(a), rgb(b), (k.v - top) / (bottom - top))
            val wl = sin(k.u * 40 + sin(k.v * 300) * 0.5 + k.v * 180)
            k.c = mix(k.c, rgb(0xFFFFFF), 0.25 * smooth(0.92, 1.0, wl) * (0.5 + 0.5 * noise(k.x * 0.05, k.y * 0.2)))
        }
    }
    fun caustics(amt: Double, fade: Double = 0.75): Layer = { k ->
        val wx = k.px * 1.6 + (fbm(k.px * 0.8, k.py * 0.8) - 0.5) * 1.4; val wy = k.py * 2.2 + (fbm(k.px * 0.8 + 4, k.py * 0.8) - 0.5) * 1.4
        val e1 = 1 - smooth(0.0, 0.05, voronoi(wx, wy, 0.0).first); val e2 = 1 - smooth(0.0, 0.05, voronoi(wx * 1.4 + 3, wy * 1.4, 5.0).first)
        k.c = add(k.c, (0.07 * e1 + 0.05 * e2) * amt * smooth(fade, 0.0, k.v))
    }
    fun lilyPads(col: Int, flower: Int): Layer = { k ->
        val cs = k.w / 3.2
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy) < 0.45) continue
            val ox = (hx + 0.2 + hash(hx + 1.3, hy) * 0.6) * cs; val oy = (hy + 0.2 + hash(hx, hy + 2.9) * 0.6) * cs
            val r = cs * (0.22 + 0.12 * hash(hx + 5.0, hy))
            val dx = k.x - ox; val dy = k.y - oy; val d = sqrt(dx * dx + dy * dy)
            val a = atan2(dy, dx); val notch = abs(((a - hash(hx, hy + 4.0) * TAU + PI).mod(TAU)) - PI) < 0.25
            if (d < r && !notch) {
                k.c = mul(rgb(col), 0.85 + 0.2 * (1 - d / r))
                if (abs(sin(a * 7)) < 0.05 && d > r * 0.15) k.c = mul(k.c, 0.85)
                if (hash(hx + 8.0, hy) > 0.75 && d < r * 0.28) k.c = mix(rgb(flower), rgb(0xFFFFFF), d / (r * 0.28) * 0.5)
            }
            if (d > r && d < r + 6 && !notch) k.c = mul(k.c, 0.85)
        }
    }
    fun floes(top: Double, bottom: Double, ice: Int, water: Int): Layer = { k ->
        if (k.v in top..bottom) {
            k.c = mix(rgb(water), mul(rgb(water), 0.85), (k.v - top) / (bottom - top))
            val (e, id) = voronoi(k.px * 1.8, k.py * 2.6, 3.0)
            if (id > 0.35 && e > 0.06) k.c = mix(rgb(ice), rgb(0xFFFFFF), 0.4 * smooth(0.06, 0.3, e))
            else if (id > 0.35) k.c = mix(rgb(water), rgb(ice), 0.5)
        }
    }
    fun kelp(col: Int, n: Double): Layer = { k ->
        val x = k.u * n + sin(k.v * 9 + floor(k.u * n) * 2) * 0.25
        val f = fract(x); val id = floor(x)
        val topV = 0.25 + 0.4 * hash(id, 1.0)
        if (k.v > topV && abs(f - 0.5) < 0.07 + 0.04 * sin(k.v * 40 + id)) k.c = mix(k.c, mul(rgb(col), 0.85 + 0.3 * f), 0.75)
    }

    // ------------------------------------------------------------------------------------------ patterns
    fun dots(col: Int, cell: Double, rad: Double, alpha: Double, jitter: Double = 0.0, vMin: Double = 0.0, palette: IntArray? = null): Layer = { k ->
        if (k.v >= vMin) {
            val cx = floor(k.x / cell); val cy = floor(k.y / cell)
            val shift = if (cy.toInt() % 2 == 0) 0.0 else 0.5
            val gx = floor(k.x / cell - shift)
            val ox = (gx + 0.5 + shift + (hash(gx, cy) - 0.5) * jitter) * cell; val oy = (cy + 0.5 + (hash(gx + 3.0, cy) - 0.5) * jitter) * cell
            val d = sqrt((k.x - ox) * (k.x - ox) + (k.y - oy) * (k.y - oy))
            val cc = if (palette != null) palette[(hash(gx + 5.5, cy) * palette.size).toInt().coerceAtMost(palette.size - 1)] else col
            k.c = mix(k.c, rgb(cc), alpha * (1 - smooth(rad - 1.2, rad + 0.8, d)))
        }
    }
    fun sprinkles(palette: IntArray, cellFrac: Double = 1 / 13.0, prob: Double = 0.65, shadow: Int = 0xD9779F, size: Double = 1.0): Layer = { k ->
        val cs = k.w * cellFrac
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy) > prob) continue
            val ox = (hx + 0.25 + hash(hx + 1.3, hy) * 0.5) * cs; val oy = (hy + 0.25 + hash(hx, hy + 2.9) * 0.5) * cs
            val a = hash(hx + 4.4, hy + 8.8) * PI
            val len = cs * 0.16 * size; val rad = cs * 0.055 * size
            val ax = ox - cos(a) * len; val ay = oy - sin(a) * len; val bx = ox + cos(a) * len; val by = oy + sin(a) * len
            k.c = mix(k.c, rgb(shadow), (1 - smooth(rad - 1, rad + 2, segDist(k.x - 2.5, k.y - 3.5, ax, ay, bx, by))) * 0.3)
            val col = rgb(palette[(hash(hx + 7.7, hy) * palette.size).toInt().coerceAtMost(palette.size - 1)])
            val m = 1 - smooth(rad - 0.8, rad + 0.6, segDist(k.x, k.y, ax, ay, bx, by))
            if (m > 0) k.c = mix(k.c, add(col, 0.15 * (1 - smooth(0.0, rad * 0.8, segDist(k.x + rad * 0.35, k.y + rad * 0.35, ax, ay, bx, by)))), m)
        }
    }
    fun confetti(palette: IntArray, cellFrac: Double = 1 / 11.0, prob: Double = 0.55): Layer = { k ->
        val cs = k.w * cellFrac
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        if (hash(cx, cy + 0.7) < prob) {
            val ox = (cx + 0.5) * cs; val oy = (cy + 0.5) * cs
            val a = hash(cx + 2.0, cy) * PI
            val dx = k.x - ox; val dy = k.y - oy
            val rx = dx * cos(a) + dy * sin(a); val ry = -dx * sin(a) + dy * cos(a)
            if (abs(rx) < cs * 0.13 && abs(ry) < cs * 0.07 * (0.6 + 0.4 * abs(sin(a * 3)))) k.c = rgb(palette[(hash(cx, cy + 5.0) * palette.size).toInt().coerceAtMost(palette.size - 1)])
        }
    }
    fun stripes(col: Int, freq: Double, angleDeg: Double, alpha: Double, soft: Double = 0.15): Layer = { k ->
        val a = angleDeg / 180 * PI
        val s = sin((k.u * cos(a) + k.v * k.h / k.w * sin(a)) * freq * TAU)
        k.c = mix(k.c, rgb(col), alpha * smooth(-soft, soft, s))
    }
    fun radialStripes(cu: Double, cv: Double, col: Int, n: Int, alpha: Double): Layer = { k ->
        val a = atan2((k.v - cv) * k.h / k.w, k.u - cu)
        k.c = mix(k.c, rgb(col), alpha * smooth(-0.1, 0.1, sin(a * n)))
    }
    fun checker(col: Int, cell: Double, alpha: Double): Layer = { k ->
        val on = (floor(k.x / cell) + floor(k.y / cell)).toInt() % 2 == 0
        if (on) k.c = mix(k.c, rgb(col), alpha)
    }
    fun gingham(col: Int, cell: Double, alpha: Double): Layer = { k ->
        val a = if (floor(k.x / cell).toInt() % 2 == 0) 1 else 0
        val b = if (floor(k.y / cell).toInt() % 2 == 0) 1 else 0
        k.c = mix(k.c, rgb(col), alpha * (a + b) / 2.0 * (if (a + b == 2) 1.3 else 1.0))
    }
    fun bricks(mortar: Int, cw: Double, ch: Double, tintVar: Double = 0.12): Layer = { k ->
        val row = floor(k.y / ch); val off = if (row.toInt() % 2 == 0) 0.0 else cw / 2
        val col = floor((k.x + off) / cw)
        val fx = (k.x + off) / cw - col; val fy = k.y / ch - row
        k.c = mul(k.c, 1 - tintVar / 2 + tintVar * hash(col, row))
        val edge = min(min(fx, 1 - fx) * cw, min(fy, 1 - fy) * ch)
        k.c = mix(k.c, rgb(mortar), 1 - smooth(1.5, 3.5, edge))
    }
    fun hexes(line: Int, size: Double, alpha: Double, fill: Int? = null): Layer = { k ->
        val qx = k.x / size; val qy = k.y / size
        val r = 2.0 / sqrt(3.0)
        val ax = qx; val ay = qy * r
        val bx = ax - 0.5 * (floor(ay) % 2); val gy = floor(ay); val gx = floor(bx)
        var best = 9.0; var second = 9.0; var bid = 0.0
        for (j in -1..1) for (i in -1..1) {
            val cy = gy + j; val cx = gx + i + 0.5 * (cy % 2) + 0.5
            val dx = ax - cx; val dy = (ay - cy - 0.5) / r
            val d = sqrt(dx * dx + dy * dy)
            if (d < best) { second = best; best = d; bid = hash(cx, cy) } else if (d < second) second = d
        }
        if (fill != null) k.c = mix(k.c, rgb(fill), 0.25 * bid)
        k.c = mix(k.c, rgb(line), alpha * (1 - smooth(0.0, 0.06, second - best)))
    }
    fun bokeh(palette: IntArray, alpha: Double, cellFrac: Double = 1 / 4.5, prob: Double = 0.45): Layer = { k ->
        val bs = k.w * cellFrac
        val bx = floor(k.x / bs); val by = floor(k.y / bs)
        for (j in -1..1) for (i in -1..1) {
            val hx = bx + i; val hy = by + j
            if (hash(hx + 0.3, hy) < 1 - prob) continue
            val ox = (hx + hash(hx, hy + 3.3)) * bs; val oy = (hy + hash(hx + 6.6, hy)) * bs
            val r = bs * (0.15 + 0.25 * hash(hx + 2.0, hy + 9.0))
            val d = sqrt((k.x - ox) * (k.x - ox) + (k.y - oy) * (k.y - oy))
            val col = palette[(hash(hx + 1.7, hy + 2.1) * palette.size).toInt().coerceAtMost(palette.size - 1)]
            k.c = mix(k.c, rgb(col), (1 - smooth(r * 0.82, r, d)) * alpha * (0.6 + 0.4 * hash(hx, hy + 1.1)))
        }
    }
    fun bubbles(col: Int, alpha: Double, cellFrac: Double = 1 / 6.0): Layer = { k ->
        val bs = k.w * cellFrac
        val bx = floor(k.x / bs); val by = floor(k.y / bs)
        for (j in -1..1) for (i in -1..1) {
            val hx = bx + i; val hy = by + j
            if (hash(hx + 0.3, hy) < 0.5) continue
            val ox = (hx + hash(hx, hy + 3.3)) * bs; val oy = (hy + hash(hx + 6.6, hy)) * bs
            val r = bs * (0.12 + 0.3 * hash(hx + 2.0, hy + 9.0))
            val d = sqrt((k.x - ox) * (k.x - ox) + (k.y - oy) * (k.y - oy))
            k.c = mix(k.c, rgb(col), alpha * (1 - smooth(1.0, 3.0, abs(d - r))))
            val hd = sqrt((k.x - ox + r * 0.4).pow(2) + (k.y - oy + r * 0.4).pow(2))
            k.c = mix(k.c, rgb(0xFFFFFF), alpha * (1 - smooth(r * 0.1, r * 0.2, hd)))
        }
    }
    fun petals(a: Int, b: Int, cellFrac: Double = 1 / 9.0, prob: Double = 0.45, alpha: Double = 0.75): Layer = { k ->
        val cs = k.w * cellFrac
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy + 0.5) > prob) continue
            val ox = (hx + 0.2 + hash(hx + 1.1, hy) * 0.6) * cs; val oy = (hy + 0.2 + hash(hx, hy + 1.9) * 0.6) * cs
            val ang = hash(hx + 3.7, hy) * TAU
            val dx = k.x - ox; val dy = k.y - oy
            val rx = dx * cos(ang) + dy * sin(ang); val ry = -dx * sin(ang) + dy * cos(ang)
            val l = cs * 0.13
            val kk = (rx / l).coerceIn(-1.0, 1.0)
            val half = l * 0.55 * sqrt(1 - kk * kk) * (0.75 + 0.25 * kk)
            if (abs(rx) < l && abs(ry) < half) k.c = mix(k.c, mix(rgb(a), rgb(b), (rx / l + 1) / 2), alpha)
        }
    }
    fun leaves(palette: IntArray, cellFrac: Double = 1 / 7.0, prob: Double = 0.45): Layer = { k ->
        val cs = k.w * cellFrac
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy + 0.25) > prob) continue
            val ox = (hx + 0.2 + hash(hx + 1.1, hy) * 0.6) * cs; val oy = (hy + 0.2 + hash(hx, hy + 1.9) * 0.6) * cs
            val ang = hash(hx + 3.7, hy) * TAU
            val dx = k.x - ox; val dy = k.y - oy
            val rx = dx * cos(ang) + dy * sin(ang); val ry = -dx * sin(ang) + dy * cos(ang)
            val l = cs * 0.24
            val kk = (rx / l).coerceIn(-1.0, 1.0)
            val half = l * 0.42 * sqrt(1 - kk * kk) * (1 + 0.25 * sin(rx / l * 9).let { abs(it) })
            val col = rgb(palette[(hash(hx + 6.6, hy) * palette.size).toInt().coerceAtMost(palette.size - 1)])
            if (abs(rx) < l && abs(ry) < half) k.c = if (abs(ry) < 1.2) mul(col, 0.75) else mul(col, 0.92 + 0.12 * (ry / half))
            if (abs(rx + l * 1.15) < l * 0.2 && abs(ry) < 1.4) k.c = mul(col, 0.6)
        }
    }
    fun rain(col: Int, alpha: Double): Layer = { k ->
        val a = 0.25
        val rx = k.x * cos(a) - k.y * sin(a); val ry = k.x * sin(a) + k.y * cos(a)
        val cx = floor(rx / 14); val cy = floor(ry / 60 + hash(cx, 1.0))
        if (hash(cx, cy) > 0.7) {
            val fx = rx / 14 - cx; val fy = ry / 60 + hash(cx, 1.0) - cy
            if (abs(fx - 0.5) < 0.08 && fy in 0.1..0.6) k.c = mix(k.c, rgb(col), alpha)
        }
    }
    fun snowDots(alpha: Double = 0.85): Layer = { k ->
        for ((cell, rad) in listOf(16.0 to 1.6, 34.0 to 2.6)) {
            val cx = floor(k.x / cell); val cy = floor(k.y / cell)
            if (hash(cx + cell, cy) > 0.72) {
                val ox = (cx + 0.2 + hash(cx, cy + cell) * 0.6) * cell; val oy = (cy + 0.2 + hash(cx + 2.2, cy) * 0.6) * cell
                val d = sqrt((k.x - ox) * (k.x - ox) + (k.y - oy) * (k.y - oy))
                k.c = mix(k.c, rgb(0xFFFFFF), alpha * (1 - smooth(rad * 0.6, rad * 1.3, d)))
            }
        }
    }
    fun lavaCracks(rock: Int, glowCol: Int, scale: Double, vMin: Double = 0.0): Layer = { k ->
        if (k.v > vMin) {
            val (e, id) = voronoi(k.px * scale + fbm(k.px, k.py) * 0.4, k.py * scale, 1.0)
            k.c = mul(rgb(rock), 0.85 + 0.3 * id)
            k.c = mix(k.c, rgb(glowCol), 1 - smooth(0.0, 0.06, e))
            k.c = addc(k.c, rgb(glowCol), 0.25 * (1 - smooth(0.0, 0.2, e)))
        }
    }
    fun crystals(a: Int, b: Int, scale: Double, edge: Int = 0xFFFFFF): Layer = { k ->
        val (e, id) = voronoi(k.px * scale, k.py * scale * 1.4, 2.0)
        k.c = mix(rgb(a), rgb(b), id)
        k.c = mul(k.c, 0.9 + 0.2 * smooth(0.0, 0.5, e))
        k.c = mix(k.c, rgb(edge), 0.5 * (1 - smooth(0.0, 0.03, e)))
    }
    fun donuts(dough: Int, icings: IntArray): Layer = { k ->
        val cs = k.w / 3.6
        val row = floor(k.y / cs); val off = if (row.toInt() % 2 == 0) 0.0 else 0.5
        val col = floor(k.x / cs - off)
        val ox = (col + 0.5 + off) * cs; val oy = (row + 0.5) * cs
        val d = sqrt((k.x - ox).pow(2) + (k.y - oy).pow(2)); val r = cs * 0.36
        if (d < r && d > r * 0.36) {
            k.c = rgb(dough)
            val wob = r * (0.80 + 0.06 * sin(atan2(k.y - oy, k.x - ox) * 7))
            if (d < wob && d > r * 0.42) {
                k.c = rgb(icings[(hash(col, row) * icings.size).toInt().coerceAtMost(icings.size - 1)])
                if (hash(floor(k.x / 7), floor(k.y / 7)) > 0.9) k.c = rgb(0xFFFFFF)
            }
            k.c = add(k.c, 0.12 * exp(-((d - r * 0.6) / (r * 0.15)).pow(2)) * (if (k.y < oy) 1.0 else 0.0))
        } else if (d < r + 4 && d > r * 0.3) k.c = mul(k.c, 0.9)
    }
    fun chocoWaves(a: Int, b: Int): Layer = { k ->
        val s = sin(k.v * 28 + sin(k.u * 6 + k.v * 4) * 1.6)
        k.c = mix(rgb(a), rgb(b), 0.5 + 0.5 * s)
        k.c = add(k.c, 0.08 * smooth(0.85, 1.0, s))
    }
    fun splats(palette: IntArray, cellFrac: Double = 1 / 4.0, prob: Double = 0.45): Layer = { k ->
        val cs = k.w * cellFrac
        val cx = floor(k.x / cs); val cy = floor(k.y / cs)
        for (j in -1..1) for (i in -1..1) {
            val hx = cx + i; val hy = cy + j
            if (hash(hx, hy + 0.9) > prob) continue
            val ox = (hx + 0.5) * cs; val oy = (hy + 0.5) * cs
            val dx = k.x - ox; val dy = k.y - oy; val d = sqrt(dx * dx + dy * dy); val a = atan2(dy, dx)
            val r = cs * 0.22 * (1 + 0.25 * sin(a * 5 + hx) + 0.15 * sin(a * 11 + hy))
            if (d < r) k.c = mix(k.c, rgb(palette[(hash(hx + 3.0, hy) * palette.size).toInt().coerceAtMost(palette.size - 1)]), 0.8)
        }
    }
    fun books(palette: IntArray, shelf: Int): Layer = { k ->
        val sh = k.w / 2.6
        val row = floor(k.y / sh); val fy = k.y / sh - row
        if (fy > 0.9) { k.c = mul(rgb(shelf), if (fy > 0.95) 0.75 else 1.0) }
        else {
            var x = 0.0; var i = 0
            val target = k.x
            while (true) { val bw = k.w * (0.045 + 0.04 * hash(i.toDouble(), row)); if (x + bw > target) break; x += bw; i++ }
            val bh = 0.55 + 0.33 * hash(i.toDouble() + 3, row)
            if (fy > 0.9 - bh) {
                k.c = mul(rgb(palette[(hash(i.toDouble(), row + 7) * palette.size).toInt().coerceAtMost(palette.size - 1)]), 0.88 + 0.16 * fract((target - x) / (k.w * 0.06)))
                val t = (fy - (0.9 - bh)) / bh
                if (abs(t - 0.2) < 0.02 || abs(t - 0.8) < 0.02) k.c = mix(k.c, rgb(0xFFE08A), 0.6)
            } else k.c = mul(rgb(shelf), 0.55)
        }
    }
    fun synthGrid(horizon: Double, line: Int): Layer = { k ->
        if (k.v > horizon) {
            val d = (k.v - horizon) / (1 - horizon)
            val z = 1 / (d + 0.04)
            val gx = (k.u - 0.5) * z * 0.9; val gz = z * 0.5
            val wx = 2.2 * z * 0.9 / k.w
            val wz = 2.2 * 0.5 / ((d + 0.04) * (d + 0.04)) / (1 - horizon) / k.h
            val lx = 1 - smooth(wx * 0.5, wx * 1.5, abs(fract(gx + 0.5) - 0.5))
            val lz = 1 - smooth(wz * 0.5, wz * 1.5, abs(fract(gz + 0.5) - 0.5))
            k.c = mix(k.c, rgb(line), max(lx, lz) * smooth(0.0, 0.2, d) * 0.9)
            k.c = addc(k.c, rgb(line), 0.25 * exp(-d * 30))
        }
    }
    fun coins(col: Int, cellFrac: Double = 1 / 7.0): Layer = { k ->
        val cs = k.w * cellFrac
        val cy = floor(k.y / cs); val off = if (cy.toInt() % 2 == 0) 0.0 else 0.5
        val cx = floor(k.x / cs - off)
        val hx = cx * 1.37 + 0.13; val hy = cy * 1.91 + 0.71
        if (hash(hx, hy) > 0.35) {
            val ox = (cx + 0.5 + off + (hash(hx + 2.3, hy) - 0.5) * 0.3) * cs; val oy = (cy + 0.5 + (hash(hx, hy + 4.1) - 0.5) * 0.3) * cs
            val r = cs * (0.26 + 0.1 * hash(hx + 7.7, hy))
            val squash = 0.55 + 0.45 * hash(hx + 1.1, hy + 2.2)
            val dx = (k.x - ox) / squash; val dy = k.y - oy
            val d = sqrt(dx * dx + dy * dy)
            if (d < r) {
                k.c = mul(rgb(col), 0.8 + 0.4 * (1 - (dy + r) / (2 * r)))
                if (d > r * 0.78) k.c = mul(k.c, 0.82)
                if (abs(d - r * 0.5) < 1.5) k.c = mul(k.c, 0.88)
                k.c = add(k.c, 0.35 * exp(-((dx + r * 0.35).pow(2) + (dy + r * 0.35).pow(2)) / (r * r * 0.03)))
            } else if (d < r + 3) k.c = mul(k.c, 0.8)
        }
    }
    fun paperFolds(palette: IntArray, cellFrac: Double = 1 / 5.0): Layer = { k ->
        val cs = k.w * cellFrac
        val gx = k.x / cs; val gy = k.y / (cs * 0.866)
        val row = floor(gy); val fx = gx + (if (row.toInt() % 2 == 0) 0.0 else 0.5)
        val col = floor(fx); val tx = fx - col; val ty = gy - row
        val upper = ty < (if (tx < 0.5) tx * 2 else (1 - tx) * 2)
        val id = hash(col * 2 + (if (upper) 1 else 0), row)
        k.c = mul(rgb(palette[(id * palette.size).toInt().coerceAtMost(palette.size - 1)]), 0.9 + 0.15 * id)
    }
    fun zenRakes(col: Int, stone: Int): Layer = { k ->
        var s = sin(k.y / 9.0 + sin(k.x / 120) * 2)
        var hit = false
        for ((cu, cv, r) in listOf(Triple(0.3, 0.35, 0.10), Triple(0.72, 0.68, 0.08))) {
            val d = dist(k, cu, cv)
            if (d < r * 2.4) s = sin(d * k.w / 9.0)
            if (d < r && !hit) { hit = true; k.c = mul(rgb(stone), 0.8 + 0.35 * (1 - d / r) - 0.25 * smooth(0.0, r, (k.v - cv) * k.h / k.w)) }
        }
        if (!hit) k.c = mix(k.c, rgb(col), 0.35 * smooth(0.6, 1.0, s))
    }
    fun bamboo(col: Int, n: Double, alpha: Double = 0.85): Layer = { k ->
        val x = k.u * n + (hash(floor(k.u * n), 2.0) - 0.5) * 0.2
        val f = fract(x); val id = floor(x)
        val w = 0.16 + 0.05 * hash(id, 3.0)
        if (abs(f - 0.5) < w) {
            val seg = fract(k.v * (6 + 3 * hash(id, 4.0)) + hash(id, 5.0))
            var cc = mul(rgb(col), 0.8 + 0.35 * (0.5 - abs(f - 0.5) / w * 0.5) * 2 * 0.5 + 0.1 * hash(id, 6.0))
            if (seg < 0.04) cc = mul(cc, 0.7)
            k.c = mix(k.c, cc, alpha)
        }
    }
}
