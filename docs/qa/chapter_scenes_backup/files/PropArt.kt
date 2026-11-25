package com.ngstudio.mazebrush.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * Prop painters for chapters 11..100 (see ChapterProps.kt). Conventions, as in ThemeDecor.kt:
 *  - edge props: the screen corner is (0,0);
 *  - floating props: drawn around the canvas centre;
 *  - ground props: stand on y = size.height.
 * Everything is mirrored for the right-hand side by the caller. `t` loops 0..1 every 8 s.
 */

private val S: DrawScope.() -> Float = { size.minDimension }
private fun DrawScope.shade(c: Color, k: Float) = Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f), c.alpha)
private fun DrawScope.drop(p: Path, a: Float = 0.15f) = translate(3f, 5f) { drawPath(p, Color.Black.copy(alpha = a)) }
private fun DrawScope.ball(c: Offset, r: Float, col: Color) {
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(Brush.radialGradient(listOf(shade(col, 1.25f), col, shade(col, 0.75f)), c - Offset(r * 0.35f, r * 0.4f), r * 1.5f), r, c)
    drawCircle(Color.White.copy(alpha = 0.4f), r * 0.22f, c - Offset(r * 0.38f, r * 0.4f))
}
private fun bob(t: Float, k: Float = 1f) = sin(t * TAU * k)

// ============================================================================================ plants
/** A spray of leaves from the corner, any colours (autumn, fern, ivy, bamboo, pine). */
internal fun DrawScope.leafSpray(t: Float, dark: Color, light: Color, slim: Float = 0.2f, n: Int = 7) {
    val s = S()
    val sway = bob(t) * 2f
    rotate(sway, Offset.Zero) {
        for (i in 0 until n) {
            val deg = 8f + i * (80f / (n - 1))
            val len = s * (0.55f + 0.4f * ((i * 37) % 7) / 7f)
            rotate(deg, Offset.Zero) {
                val p = leafPath(len, len * slim)
                drop(p, 0.12f)
                drawPath(p, Brush.linearGradient(listOf(dark, light), Offset.Zero, Offset(len, 0f)))
                drawLine(Color.White.copy(alpha = 0.3f), Offset(len * 0.05f, 0f), Offset(len * 0.9f, 0f), s * 0.008f, StrokeCap.Round)
            }
        }
    }
}

internal fun DrawScope.mushrooms(t: Float, cap: Color, spots: Color = Color.White, glow: Boolean = false) {
    val s = S(); val h = size.height
    for ((x, tall, r) in listOf(Triple(0.30f, 0.34f, 0.20f), Triple(0.62f, 0.22f, 0.13f), Triple(0.12f, 0.16f, 0.09f))) {
        val top = h - s * tall
        if (glow) drawCircle(cap.copy(alpha = 0.25f + 0.1f * bob(t, 2f)), r * s * 1.6f, Offset(x * s, top))
        drawRoundRect(Color(0xFFF6EBD8), Offset(x * s - r * s * 0.32f, top), Size(r * s * 0.64f, h - top), CornerRadius(r * s * 0.3f))
        val capP = Path().apply { addArc(Rect(Offset(x * s, top), r * s), 180f, 180f); close() }
        drop(capP)
        drawPath(capP, Brush.verticalGradient(listOf(shade(cap, 1.2f), cap), top - r * s, top))
        for (k in 0..2) drawCircle(spots, r * s * 0.16f, Offset(x * s + (k - 1) * r * s * 0.45f, top - r * s * (0.45f + (k % 2) * 0.2f)))
    }
}

internal fun DrawScope.tulips(t: Float, cols: List<Color>) {
    val s = S(); val h = size.height
    for ((i, x) in listOf(0.18f, 0.40f, 0.62f, 0.82f).withIndex()) {
        val tall = s * (0.45f + 0.15f * ((i * 3) % 4) / 3f)
        val sw = bob(t + i * 0.2f) * s * 0.02f
        val top = Offset(x * s + sw, h - tall)
        drawLine(Color(0xFF3F8E2A), Offset(x * s, h), top, s * 0.025f, StrokeCap.Round)
        rotate(-20f, Offset(x * s, h - tall * 0.3f)) { drawPath(leafPath(s * 0.22f, s * 0.05f).apply { translate(Offset(x * s, h - tall * 0.3f)) }, Color(0xFF5CAE3A)) }
        val c = cols[i % cols.size]
        val p = Path().apply {
            moveTo(top.x - s * 0.07f, top.y - s * 0.02f); lineTo(top.x - s * 0.07f, top.y - s * 0.12f); lineTo(top.x - s * 0.035f, top.y - s * 0.07f)
            lineTo(top.x, top.y - s * 0.13f); lineTo(top.x + s * 0.035f, top.y - s * 0.07f); lineTo(top.x + s * 0.07f, top.y - s * 0.12f)
            lineTo(top.x + s * 0.07f, top.y - s * 0.02f); cubicTo(top.x + s * 0.06f, top.y + s * 0.05f, top.x - s * 0.06f, top.y + s * 0.05f, top.x - s * 0.07f, top.y - s * 0.02f); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(shade(c, 1.2f), c), top.y - s * 0.13f, top.y + s * 0.04f))
    }
}

internal fun DrawScope.sunflowers(t: Float) {
    val s = S(); val h = size.height
    for ((i, p) in listOf(Triple(0.30f, 0.72f, 0.16f), Triple(0.66f, 0.50f, 0.12f)).withIndex()) {
        val c = Offset(p.first * s + bob(t + i * 0.3f) * s * 0.015f, h - p.second * s)
        drawLine(Color(0xFF4E8E2A), Offset(p.first * s, h), c, s * 0.03f, StrokeCap.Round)
        rotate(30f, Offset(p.first * s, h - p.second * s * 0.4f)) { drawPath(leafPath(s * 0.2f, s * 0.05f).apply { translate(Offset(p.first * s, h - p.second * s * 0.4f)) }, Color(0xFF5EAE3A)) }
        val r = p.third * s
        for (k in 0 until 14) { val a = k * TAU / 14; rotate(a * 180f / PI.toFloat(), c) { drawOval(Color(0xFFFFC72C), Offset(c.x, c.y - r * 0.16f), Size(r * 1.0f, r * 0.32f)) } }
        drawCircle(Color(0xFF6B3E1E), r * 0.5f, c)
        for (k in 0..8) drawCircle(Color(0xFF3E2210), r * 0.06f, c + Offset(cos(k * 0.9f) * r * 0.28f, sin(k * 0.9f) * r * 0.28f))
    }
}

internal fun DrawScope.lavender(t: Float) {
    val s = S(); val h = size.height
    for (i in 0 until 7) {
        val x = s * (0.08f + i * 0.13f)
        val tall = s * (0.45f + 0.25f * ((i * 5) % 7) / 7f)
        val sw = bob(t + i * 0.15f) * s * 0.03f
        drawLine(Color(0xFF6E9A4A), Offset(x, h), Offset(x + sw, h - tall), s * 0.012f, StrokeCap.Round)
        for (k in 0 until 7) {
            val f = k / 7f
            val p = Offset(x + sw * (1 - f * 0.3f), h - tall + f * tall * 0.45f)
            drawOval(if (k % 2 == 0) Color(0xFF9A6ADA) else Color(0xFFB98CF0), Offset(p.x - s * 0.025f, p.y - s * 0.02f), Size(s * 0.05f, s * 0.04f))
        }
    }
}

internal fun DrawScope.pineTrees(t: Float, snow: Boolean = false) {
    val s = S(); val h = size.height
    for ((cx, sc) in listOf(0.34f to 1f, 0.70f to 0.7f)) {
        val x = s * cx
        for (i in 0..3) {
            val top = h - s * (0.95f - i * 0.16f) * sc; val bot = top + s * 0.27f * sc; val w = s * (0.11f + i * 0.05f) * sc
            val p = Path().apply { moveTo(x, top); lineTo(x + w, bot); lineTo(x - w, bot); close() }
            drawPath(p, Brush.horizontalGradient(listOf(Color(0xFF3A8A56), Color(0xFF1E5A36)), x - w, x + w))
            if (snow) drawPath(Path().apply { moveTo(x, top); lineTo(x + w * 0.35f, top + s * 0.09f * sc); lineTo(x - w * 0.35f, top + s * 0.09f * sc); close() }, Color.White)
        }
        drawRect(Color(0xFF6B3E1E), Offset(x - s * 0.03f * sc, h - s * 0.14f * sc), Size(s * 0.06f * sc, s * 0.14f * sc))
    }
}

internal fun DrawScope.reeds(t: Float) {
    val s = S(); val h = size.height
    for (i in 0 until 6) {
        val x = s * (0.1f + i * 0.15f); val tall = s * (0.5f + 0.3f * ((i * 3) % 5) / 5f)
        val sw = bob(t + i * 0.2f) * s * 0.03f
        drawLine(Color(0xFF5E8A3A), Offset(x, h), Offset(x + sw, h - tall), s * 0.014f, StrokeCap.Round)
        if (i % 2 == 0) drawRoundRect(Color(0xFF7A4A24), Offset(x + sw - s * 0.022f, h - tall + s * 0.02f), Size(s * 0.044f, s * 0.14f), CornerRadius(s * 0.02f))
        else rotate(25f, Offset(x, h - tall * 0.5f)) { drawPath(leafPath(s * 0.3f, s * 0.03f).apply { translate(Offset(x, h - tall * 0.5f)) }, Color(0xFF7AAE4A)) }
    }
}

internal fun DrawScope.grassTuft(t: Float, col: Color = Color(0xFF5EAE3A)) {
    val s = S(); val h = size.height
    for (i in 0 until 12) {
        val x = s * (0.03f + i * 0.08f); val tall = s * (0.22f + 0.2f * ((i * 7) % 9) / 9f)
        val bend = (if (i % 2 == 0) 1 else -1) * s * 0.05f + bob(t + i * 0.1f) * s * 0.02f
        val p = Path().apply { moveTo(x - s * 0.02f, h); cubicTo(x, h - tall * 0.5f, x + bend * 0.6f, h - tall * 0.8f, x + bend, h - tall); cubicTo(x + bend * 0.4f, h - tall * 0.7f, x + s * 0.02f, h - tall * 0.4f, x + s * 0.025f, h); close() }
        drawPath(p, Brush.verticalGradient(listOf(shade(col, 1.3f), shade(col, 0.7f)), h - tall, h))
    }
}

internal fun DrawScope.bamboo(t: Float) {
    val s = S(); val h = size.height
    for ((i, x) in listOf(0.22f, 0.45f, 0.70f).withIndex()) {
        val w = s * 0.09f; val tall = s * (0.95f - i * 0.15f)
        drawRect(Brush.horizontalGradient(listOf(Color(0xFF9ED66A), Color(0xFF5E9E3A)), x * s - w / 2, x * s + w / 2), Offset(x * s - w / 2, h - tall), Size(w, tall))
        var y = h - s * 0.15f
        while (y > h - tall) { drawRect(Color(0xFF4E8A2E), Offset(x * s - w * 0.55f, y), Size(w * 1.1f, s * 0.015f)); y -= s * 0.2f }
        rotate(-35f + bob(t + i * 0.3f) * 4f, Offset(x * s, h - tall * 0.7f)) { drawPath(leafPath(s * 0.24f, s * 0.04f).apply { translate(Offset(x * s, h - tall * 0.7f)) }, Color(0xFF6CBE4A)) }
    }
}

internal fun DrawScope.acacia(t: Float) {
    val s = S(); val h = size.height
    val x = s * 0.42f
    drawPath(Path().apply { moveTo(x - s * 0.03f, h); lineTo(x - s * 0.01f, h - s * 0.45f); lineTo(x - s * 0.15f, h - s * 0.62f); lineTo(x - s * 0.12f, h - s * 0.64f); lineTo(x + s * 0.01f, h - s * 0.5f); lineTo(x + s * 0.14f, h - s * 0.66f); lineTo(x + s * 0.17f, h - s * 0.63f); lineTo(x + s * 0.035f, h - s * 0.44f); lineTo(x + s * 0.04f, h); close() }, Color(0xFF5A3418))
    for ((dx, w) in listOf(-0.18f to 0.32f, 0.06f to 0.40f, 0.20f to 0.26f)) drawOval(Color(0xFF6E8A2E), Offset(x + dx * s - w * s / 2, h - s * 0.74f), Size(w * s, s * 0.13f))
    drawOval(Color(0xFF8AA83A).copy(alpha = 0.6f), Offset(x - s * 0.25f, h - s * 0.75f), Size(s * 0.4f, s * 0.05f))
}

internal fun DrawScope.palmTree(t: Float) {
    val s = S(); val h = size.height
    val x = s * 0.40f
    val trunk = Path().apply { moveTo(x - s * 0.04f, h); cubicTo(x - s * 0.02f, h - s * 0.4f, x + s * 0.06f, h - s * 0.6f, x + s * 0.1f, h - s * 0.78f); lineTo(x + s * 0.14f, h - s * 0.77f); cubicTo(x + s * 0.1f, h - s * 0.6f, x + s * 0.04f, h - s * 0.4f, x + s * 0.04f, h); close() }
    drawPath(trunk, Color(0xFF9A6A3A))
    for (k in 0..6) drawLine(Color(0xFF6E4422), Offset(x - s * 0.03f + k * s * 0.012f, h - k * s * 0.11f), Offset(x + s * 0.04f + k * s * 0.012f, h - k * s * 0.11f - s * 0.015f), s * 0.01f)
    val top = Offset(x + s * 0.12f, h - s * 0.78f)
    for (deg in listOf(-160f, -120f, -70f, -30f, 10f)) rotate(deg + bob(t) * 3f, top) {
        val p = leafPath(s * 0.36f, s * 0.07f).apply { translate(top) }
        drawPath(p, Brush.linearGradient(listOf(Color(0xFF2E8A3A), Color(0xFF7ED957)), top, top + Offset(s * 0.36f, 0f)))
    }
    for (k in 0..2) drawCircle(Color(0xFF6A4220), s * 0.03f, top + Offset((k - 1) * s * 0.035f, s * 0.035f))
}

internal fun DrawScope.fern(t: Float) {
    val s = S(); val h = size.height
    for ((i, deg) in listOf(-80f, -60f, -100f, -40f, -120f).withIndex()) rotate(deg + bob(t + i * 0.2f) * 3f, Offset(s * 0.4f, h)) {
        val len = s * (0.7f - i * 0.07f)
        for (k in 1..10) {
            val f = k / 11f
            for (side in listOf(-1f, 1f)) rotate(side * 55f, Offset(s * 0.4f + len * f, h)) {
                drawPath(leafPath(len * 0.18f * (1 - f * 0.6f), len * 0.03f).apply { translate(Offset(s * 0.4f + len * f, h)) }, if (side < 0) Color(0xFF3E8E3A) else Color(0xFF5EAE4A))
            }
        }
        drawLine(Color(0xFF2E6E2A), Offset(s * 0.4f, h), Offset(s * 0.4f + len, h), s * 0.012f, StrokeCap.Round)
    }
}

// ============================================================================================ fruit & food
internal fun DrawScope.cherries(t: Float) {
    val s = S()
    val sw = bob(t) * 3f
    rotate(sw, Offset(s * 0.4f, 0f)) {
        drawPath(leafPath(s * 0.3f, s * 0.07f).apply { translate(Offset(s * 0.4f, s * 0.08f)) }, Color(0xFF4E9E3A))
        for ((dx, dy) in listOf(-0.12f to 0.48f, 0.12f to 0.56f)) {
            drawLine(Color(0xFF5E7A2A), Offset(s * 0.4f, s * 0.08f), Offset(s * (0.4f + dx), s * dy), s * 0.015f, StrokeCap.Round)
            ball(Offset(s * (0.4f + dx), s * dy + s * 0.07f), s * 0.09f, Color(0xFFD81E3A))
        }
    }
    drawLine(Color(0xFF6B3A1E), Offset(0f, s * 0.08f), Offset(s * 0.6f, s * 0.08f), s * 0.03f, StrokeCap.Round)
}

internal fun DrawScope.citrusBranch(t: Float, fruit: Color) {
    val s = S()
    drawLine(Color(0xFF6B3A1E), Offset(0f, s * 0.05f), Offset(s * 0.85f, s * 0.22f), s * 0.035f, StrokeCap.Round)
    for ((x, deg) in listOf(0.2f to 70f, 0.45f to 40f, 0.7f to 80f, 0.6f to 140f)) rotate(deg + bob(t) * 3f, Offset(x * s, s * (0.05f + x * 0.2f))) {
        drawPath(leafPath(s * 0.25f, s * 0.07f).apply { translate(Offset(x * s, s * (0.05f + x * 0.2f))) }, Color(0xFF3E8E3A))
    }
    ball(Offset(s * 0.32f, s * 0.36f + bob(t) * 2f), s * 0.12f, fruit)
    ball(Offset(s * 0.66f, s * 0.42f), s * 0.10f, fruit)
}

internal fun DrawScope.fruitSlice(c: Offset, r: Float, rind: Color, flesh: Color, seg: Color = Color.White) {
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(rind, r, c); drawCircle(seg, r * 0.9f, c); drawCircle(flesh, r * 0.82f, c)
    for (k in 0 until 8) { val a = k * TAU / 8; drawLine(seg, c, c + Offset(cos(a), sin(a)) * (r * 0.8f), r * 0.07f) }
    drawCircle(seg, r * 0.12f, c)
}

internal fun DrawScope.citrusPile(t: Float, rind: Color, flesh: Color) {
    val s = S(); val h = size.height
    ball(Offset(s * 0.60f, h - s * 0.14f), s * 0.14f, rind)
    fruitSlice(Offset(s * 0.30f, h - s * 0.20f), s * 0.18f, rind, flesh)
    drawOval(rind, Offset(s * 0.70f, h - s * 0.30f), Size(s * 0.06f, s * 0.04f))
}

internal fun DrawScope.strawberries(t: Float) {
    val s = S(); val h = size.height
    for ((x, r) in listOf(0.30f to 0.17f, 0.62f to 0.12f, 0.82f to 0.08f)) {
        val c = Offset(x * s, h - r * s * 1.1f)
        val body = Path().apply { moveTo(c.x - r * s, c.y - r * s * 0.5f); cubicTo(c.x - r * s, c.y + r * s * 0.6f, c.x - r * s * 0.2f, c.y + r * s * 1.05f, c.x, c.y + r * s * 1.05f); cubicTo(c.x + r * s * 0.2f, c.y + r * s * 1.05f, c.x + r * s, c.y + r * s * 0.6f, c.x + r * s, c.y - r * s * 0.5f); cubicTo(c.x + r * s * 0.5f, c.y - r * s * 0.95f, c.x - r * s * 0.5f, c.y - r * s * 0.95f, c.x - r * s, c.y - r * s * 0.5f); close() }
        drop(body); drawPath(body, Brush.radialGradient(listOf(Color(0xFFFF6A6A), Color(0xFFD81E3A)), c - Offset(r * s * 0.3f, r * s * 0.3f), r * s * 1.6f))
        for (k in 0..7) drawOval(Color(0xFFFFE680), Offset(c.x + ((k * 0.37f) % 1f - 0.5f) * r * s * 1.2f, c.y + ((k * 0.61f) % 1f - 0.4f) * r * s), Size(r * s * 0.1f, r * s * 0.16f))
        for (k in 0..4) rotate(k * 72f - 90f, Offset(c.x, c.y - r * s * 0.7f)) { drawOval(Color(0xFF3E9E3A), Offset(c.x, c.y - r * s * 0.78f), Size(r * s * 0.45f, r * s * 0.16f)) }
    }
}

internal fun DrawScope.watermelonSlices(t: Float) {
    val s = S(); val h = size.height
    for ((x, r, rot) in listOf(Triple(0.32f, 0.26f, -8f), Triple(0.72f, 0.17f, 10f))) {
        val c = Offset(x * s, h - s * 0.04f)
        rotate(rot, c) {
            drawArc(Color(0xFF3E9E3A), 180f, 180f, true, c - Offset(r * s, r * s), Size(r * s * 2, r * s * 2))
            drawArc(Color(0xFFEFFFE0), 180f, 180f, true, c - Offset(r * s * 0.9f, r * s * 0.9f), Size(r * s * 1.8f, r * s * 1.8f))
            drawArc(Color(0xFFFF4A6A), 180f, 180f, true, c - Offset(r * s * 0.82f, r * s * 0.82f), Size(r * s * 1.64f, r * s * 1.64f))
            for (k in 0..5) { val a = PI.toFloat() * (1.15f + k * 0.14f); drawOval(Color(0xFF2A1414), Offset(c.x + cos(a) * r * s * 0.5f, c.y + sin(a) * r * s * 0.5f), Size(r * s * 0.07f, r * s * 0.11f)) }
        }
    }
}

internal fun DrawScope.watermelonFloat(t: Float) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.5f + bob(t) * 4f)
    rotate(-20f + bob(t, 0.5f) * 6f, c) {
        val r = s * 0.32f
        drawArc(Color(0xFF3E9E3A), 0f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
        drawArc(Color(0xFFEFFFE0), 0f, 180f, true, c - Offset(r * 0.9f, r * 0.9f), Size(r * 1.8f, r * 1.8f))
        drawArc(Color(0xFFFF4A6A), 0f, 180f, true, c - Offset(r * 0.82f, r * 0.82f), Size(r * 1.64f, r * 1.64f))
        for (k in 0..4) { val a = PI.toFloat() * (0.2f + k * 0.15f); drawOval(Color(0xFF2A1414), Offset(c.x + cos(a) * r * 0.5f, c.y + sin(a) * r * 0.5f), Size(r * 0.07f, r * 0.11f)) }
    }
}

internal fun DrawScope.lemonadeGlass(t: Float) {
    val s = S(); val h = size.height
    val x = s * 0.36f; val w = s * 0.26f; val top = h - s * 0.55f
    val glass = Path().apply { moveTo(x - w / 2, top); lineTo(x + w / 2, top); lineTo(x + w * 0.42f, h - s * 0.02f); lineTo(x - w * 0.42f, h - s * 0.02f); close() }
    drop(glass)
    drawPath(glass, Color.White.copy(alpha = 0.5f))
    drawPath(Path().apply { moveTo(x - w * 0.47f, top + s * 0.08f); lineTo(x + w * 0.47f, top + s * 0.08f); lineTo(x + w * 0.42f, h - s * 0.02f); lineTo(x - w * 0.42f, h - s * 0.02f); close() }, Color(0xFFFFE45C))
    drawLine(Color(0xFFFF5C8A), Offset(x + w * 0.1f, h - s * 0.2f), Offset(x + w * 0.4f, top - s * 0.14f), s * 0.025f, StrokeCap.Round)
    fruitSlice(Offset(x - w * 0.45f, top + s * 0.02f), s * 0.08f, Color(0xFFF6D21E), Color(0xFFFFF08A))
    for (k in 0..2) drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(x - w * 0.2f + k * w * 0.15f, top + s * (0.18f + k * 0.08f)), Size(s * 0.05f, s * 0.05f), CornerRadius(s * 0.01f))
    fruitSlice(Offset(s * 0.75f, h - s * 0.10f), s * 0.10f, Color(0xFFF6D21E), Color(0xFFFFF08A))
}

internal fun DrawScope.iceCream(t: Float, scoops: List<Color>) {
    val s = S(); val h = size.height
    for ((i, x) in listOf(0.32f, 0.70f).withIndex()) {
        val sc = if (i == 0) 1f else 0.75f
        val tip = Offset(x * s, h - s * 0.02f); val cw = s * 0.15f * sc; val top = h - s * 0.42f * sc
        val cone = Path().apply { moveTo(tip.x, tip.y); lineTo(tip.x - cw, top); lineTo(tip.x + cw, top); close() }
        drop(cone); drawPath(cone, Color(0xFFE0A860))
        for (k in -2..2) drawLine(Color(0xFFB87A3A), Offset(tip.x + k * cw * 0.35f, top), Offset(tip.x - k * cw * 0.1f, tip.y - s * 0.05f), s * 0.008f)
        for (k in 0 until (if (i == 0) 2 else 1)) ball(Offset(tip.x, top - s * (0.08f + k * 0.15f) * sc), cw * 1.05f, scoops[(i + k) % scoops.size])
        if (i == 0) drawCircle(Color(0xFFE0203A), s * 0.035f, Offset(tip.x, top - s * 0.38f * sc))
    }
}

internal fun DrawScope.cupcakes(t: Float, frost: List<Color>) {
    val s = S(); val h = size.height
    for ((i, x) in listOf(0.32f, 0.70f).withIndex()) {
        val sc = if (i == 0) 1f else 0.72f; val w = s * 0.3f * sc; val top = h - s * 0.22f * sc
        val cup = Path().apply { moveTo(x * s - w / 2, top); lineTo(x * s + w / 2, top); lineTo(x * s + w * 0.38f, h); lineTo(x * s - w * 0.38f, h); close() }
        drop(cup); drawPath(cup, Color(0xFF8AD8F0))
        for (k in -2..2) drawLine(Color(0xFF5AB0D0), Offset(x * s + k * w * 0.18f, top), Offset(x * s + k * w * 0.14f, h), s * 0.01f)
        val f = frost[i % frost.size]
        for ((dy, rr) in listOf(0.0f to 0.55f, 0.11f to 0.42f, 0.2f to 0.26f)) drawCircle(f, w * rr * 0.75f, Offset(x * s, top - s * dy * sc - w * 0.12f))
        drawCircle(Color(0xFFE0203A), s * 0.03f * sc, Offset(x * s, top - s * 0.3f * sc))
        for (k in 0..5) drawRoundRect(listOf(Color(0xFFFFE45C), Color(0xFF5CC8FF), Color.White)[k % 3], Offset(x * s + ((k * 0.37f) % 1f - 0.5f) * w * 0.7f, top - s * (0.05f + (k * 0.23f) % 0.15f) * sc), Size(s * 0.025f, s * 0.008f), CornerRadius(2f))
    }
}

internal fun DrawScope.donut(c: Offset, r: Float, icing: Color, rot: Float = 0f) {
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(Color(0xFFE0A060), r, c)
    val ic = Path().apply { for (k in 0..24) { val a = k * TAU / 24; val rr = r * (0.84f + 0.06f * sin(a * 6 + rot)); if (k == 0) moveTo(c.x + cos(a) * rr, c.y + sin(a) * rr) else lineTo(c.x + cos(a) * rr, c.y + sin(a) * rr) }; close() }
    drawPath(ic, icing)
    for (k in 0..9) { val a = k * 0.63f + rot; val rr = r * (0.52f + (k % 3) * 0.1f); drawLine(listOf(Color.White, Color(0xFFFFE45C), Color(0xFF5CC8FF))[k % 3], c + Offset(cos(a) * rr, sin(a) * rr), c + Offset(cos(a + 0.12f) * rr, sin(a + 0.12f) * rr), r * 0.06f, StrokeCap.Round) }
    drawCircle(Color(0xFFFFE6C8), r * 0.3f, c); drawCircle(Color(0xFFD08A50), r * 0.3f, c, style = Stroke(r * 0.05f))
}

internal fun DrawScope.donutFloat(t: Float, icing: Color) { val s = S(); donut(Offset(s * 0.45f, s * 0.48f + bob(t) * 4f), s * 0.28f, icing, t * TAU) }
internal fun DrawScope.donutStack(t: Float) {
    val s = S(); val h = size.height
    for ((i, ic) in listOf(Color(0xFFFF8AB8), Color(0xFF7A4A2A), Color(0xFF8AD8FF)).withIndex()) {
        val c = Offset(s * 0.38f, h - s * (0.1f + i * 0.13f)); val r = s * 0.18f
        drawOval(Color(0xFFE0A060), Offset(c.x - r, c.y - r * 0.4f), Size(r * 2, r * 0.8f))
        drawOval(ic, Offset(c.x - r * 0.9f, c.y - r * 0.42f), Size(r * 1.8f, r * 0.62f))
        drawOval(Color(0xFFD08A50), Offset(c.x - r * 0.25f, c.y - r * 0.2f), Size(r * 0.5f, r * 0.2f))
    }
    donut(Offset(s * 0.78f, h - s * 0.14f), s * 0.13f, Color(0xFFFFF0F4))
}

internal fun DrawScope.chocolateBar(t: Float) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.48f + bob(t) * 3f)
    rotate(-18f, c) {
        val w = s * 0.5f; val h = s * 0.32f
        drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(c.x - w / 2 + 3, c.y - h / 2 + 5), Size(w, h), CornerRadius(s * 0.03f))
        drawRoundRect(Color(0xFF6A3A1E), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(s * 0.03f))
        for (i in 0..3) for (j in 0..1) drawRoundRect(Color(0xFF8A5030), Offset(c.x - w / 2 + s * 0.02f + i * w / 4, c.y - h / 2 + s * 0.02f + j * h / 2), Size(w / 4 - s * 0.03f, h / 2 - s * 0.03f), CornerRadius(s * 0.015f))
        drawRect(Color(0xFFE84A4A), Offset(c.x, c.y - h / 2 - s * 0.01f), Size(w / 2 + s * 0.01f, h + s * 0.02f))
        drawRect(Color(0xFFFFD23F), Offset(c.x + s * 0.02f, c.y - h / 2 - s * 0.01f), Size(s * 0.02f, h + s * 0.02f))
    }
}

internal fun DrawScope.truffles(t: Float) {
    val s = S(); val h = size.height
    for ((x, col) in listOf(0.25f to Color(0xFF5A2E14), 0.50f to Color(0xFF8A5030), 0.75f to Color(0xFFF2E0C8))) {
        val c = Offset(x * s, h - s * 0.12f)
        drawRoundRect(Color(0xFFC8A060), Offset(c.x - s * 0.12f, c.y), Size(s * 0.24f, s * 0.1f), CornerRadius(s * 0.02f))
        ball(c, s * 0.11f, col)
        drawLine(if (col.red > 0.8f) Color(0xFF6A3A1E) else Color(0xFFF2E0C8), c + Offset(-s * 0.06f, -s * 0.03f), c + Offset(s * 0.06f, -s * 0.05f), s * 0.012f, StrokeCap.Round)
    }
}

internal fun DrawScope.cookie(c: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFEAB86A), Color(0xFFC88A42)), c - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, c)
    for (k in 0..6) drawCircle(Color(0xFF4A2410), r * 0.11f, c + Offset(cos(k * 1.7f) * r * (0.25f + (k % 3) * 0.2f), sin(k * 1.7f) * r * (0.25f + (k % 3) * 0.2f)))
}
internal fun DrawScope.cookieFloat(t: Float) { val s = S(); rotate(t * 40f, Offset(s * 0.45f, s * 0.48f)) { cookie(Offset(s * 0.45f, s * 0.48f + bob(t) * 3f), s * 0.26f) } }
internal fun DrawScope.cookieJar(t: Float) {
    val s = S(); val h = size.height
    val x = s * 0.36f; val w = s * 0.36f; val top = h - s * 0.52f
    drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(x - w / 2 + 3, top + 5), Size(w, h - top), CornerRadius(s * 0.06f))
    drawRoundRect(Color(0xFFDCEFFF).copy(alpha = 0.85f), Offset(x - w / 2, top), Size(w, h - top), CornerRadius(s * 0.06f))
    for (k in 0..2) cookie(Offset(x + (k - 1) * w * 0.25f, h - s * (0.1f + (k % 2) * 0.12f)), s * 0.09f)
    drawRoundRect(Color(0xFFE85A7A), Offset(x - w * 0.45f, top - s * 0.06f), Size(w * 0.9f, s * 0.08f), CornerRadius(s * 0.03f))
    drawCircle(Color(0xFFE85A7A), s * 0.04f, Offset(x, top - s * 0.08f))
    drawRect(Color.White.copy(alpha = 0.5f), Offset(x - w * 0.38f, top + s * 0.06f), Size(s * 0.03f, s * 0.3f))
    cookie(Offset(s * 0.78f, h - s * 0.09f), s * 0.09f)
}

internal fun DrawScope.honeyPot(t: Float) {
    val s = S(); val h = size.height
    val c = Offset(s * 0.36f, h - s * 0.22f)
    drawCircle(Color.Black.copy(alpha = 0.12f), s * 0.22f, c + Offset(3f, 5f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF0B85A), Color(0xFFB8702A)), c - Offset(s * 0.06f, s * 0.08f), s * 0.3f), s * 0.22f, c)
    drawRoundRect(Color(0xFFB8702A), Offset(c.x - s * 0.15f, c.y - s * 0.27f), Size(s * 0.3f, s * 0.08f), CornerRadius(s * 0.03f))
    val drip = Path().apply { moveTo(c.x - s * 0.15f, c.y - s * 0.2f); lineTo(c.x + s * 0.15f, c.y - s * 0.2f); cubicTo(c.x + s * 0.12f, c.y - s * 0.08f, c.x + s * 0.06f, c.y - s * 0.16f, c.x + s * 0.03f, c.y - s * 0.06f); cubicTo(c.x, c.y - s * 0.14f, c.y.let { c.x - s * 0.06f }, c.y - s * 0.1f, c.x - s * 0.15f, c.y - s * 0.2f); close() }
    drawPath(drip, Color(0xFFFFC72C))
    drawRoundRect(Color(0xFFFFF0C8), Offset(c.x - s * 0.1f, c.y - s * 0.02f), Size(s * 0.2f, s * 0.1f), CornerRadius(s * 0.02f))
}

internal fun DrawScope.beehive(t: Float) {
    val s = S()
    drawLine(Color(0xFF6B3A1E), Offset(0f, s * 0.06f), Offset(s * 0.7f, s * 0.06f), s * 0.035f, StrokeCap.Round)
    val c = Offset(s * 0.42f, s * 0.36f)
    val sway = bob(t) * 2f
    rotate(sway, Offset(s * 0.42f, s * 0.06f)) {
        drawLine(Color(0xFF6B3A1E), Offset(s * 0.42f, s * 0.06f), Offset(s * 0.42f, s * 0.14f), s * 0.02f)
        for ((i, w) in listOf(0.18f, 0.26f, 0.30f, 0.26f, 0.18f).withIndex()) drawRoundRect(if (i % 2 == 0) Color(0xFFF2B43A) else Color(0xFFE09A2A), Offset(c.x - w * s / 2, c.y - s * 0.22f + i * s * 0.09f), Size(w * s, s * 0.1f), CornerRadius(s * 0.05f))
        drawOval(Color(0xFF5A3010), Offset(c.x - s * 0.05f, c.y + s * 0.06f), Size(s * 0.1f, s * 0.08f))
    }
}

internal fun DrawScope.beeAt(c: Offset, r: Float, t: Float) {
    val flap = 0.6f + 0.4f * abs(sin(t * TAU * 16))
    drawOval(Color.White.copy(alpha = 0.75f), Offset(c.x - r * 0.6f, c.y - r * 1.25f * flap), Size(r * 0.8f, r * 1.0f * flap))
    drawOval(Color.White.copy(alpha = 0.75f), Offset(c.x - r * 0.1f, c.y - r * 1.15f * flap), Size(r * 0.8f, r * 0.9f * flap))
    drawOval(Color(0xFFFFC72C), Offset(c.x - r, c.y - r * 0.7f), Size(r * 2, r * 1.4f))
    for (k in 0..1) drawRect(Color(0xFF2A1E14), Offset(c.x - r * 0.35f + k * r * 0.55f, c.y - r * 0.66f), Size(r * 0.25f, r * 1.32f))
    drawCircle(Color(0xFF2A1E14), r * 0.12f, c + Offset(r * 0.65f, -r * 0.15f))
}
internal fun DrawScope.bees(t: Float) {
    val s = S()
    beeAt(Offset(s * (0.45f + 0.15f * sin(t * TAU)), s * (0.45f + 0.1f * sin(t * TAU * 2))), s * 0.11f, t)
    beeAt(Offset(s * (0.25f + 0.08f * cos(t * TAU)), s * (0.75f + 0.06f * sin(t * TAU * 3))), s * 0.07f, t + 0.3f)
}

internal fun DrawScope.butterflyAt(c: Offset, r: Float, t: Float, col: Color) {
    val flap = 0.35f + 0.65f * abs(sin(t * TAU * 10))
    for (side in listOf(-1f, 1f)) {
        val up = Size(r * 1.4f * flap, r * 1.1f); val lo = Size(r * 1.0f * flap, r * 0.8f)
        val uc = c + Offset(side * r * 0.72f * flap, -r * 0.42f); val lc = c + Offset(side * r * 0.52f * flap, r * 0.42f)
        drawOval(col, Offset(uc.x - up.width / 2, uc.y - up.height / 2), up)
        drawOval(shade(col, 0.85f), Offset(lc.x - lo.width / 2, lc.y - lo.height / 2), lo)
        drawCircle(Color.White.copy(alpha = 0.7f), r * 0.16f, uc)
    }
    drawLine(Color(0xFF4A2C1A), c + Offset(0f, -r * 0.8f), c + Offset(0f, r * 0.8f), r * 0.14f, StrokeCap.Round)
}
internal fun DrawScope.butterflyFloat(t: Float, col: Color) { val s = S(); butterflyAt(Offset(s * 0.45f, s * 0.45f + bob(t) * 6f), s * 0.18f, t, col) }

internal fun DrawScope.dragonfly(t: Float) {
    val s = S(); val c = Offset(s * (0.45f + 0.08f * sin(t * TAU)), s * 0.45f + bob(t, 2f) * 4f)
    val flap = 0.7f + 0.3f * abs(sin(t * TAU * 18))
    for (k in listOf(-1f, 1f)) for (j in listOf(0f, 1f)) rotate(k * (25f + j * 30f) * flap, c) { drawOval(Color(0xFFBFF0FF).copy(alpha = 0.7f), Offset(c.x - s * 0.02f, c.y - s * 0.22f + j * s * 0.02f), Size(s * 0.07f, s * 0.22f)) }
    drawLine(Color(0xFF2EA0C8), c, c + Offset(s * 0.32f, s * 0.05f), s * 0.03f, StrokeCap.Round)
    drawCircle(Color(0xFF1E7AA8), s * 0.04f, c - Offset(s * 0.02f, 0f))
}

internal fun DrawScope.frog(t: Float) {
    val s = S(); val h = size.height
    drawOval(Color(0xFF6CC24A), Offset(s * 0.04f, h - s * 0.12f), Size(s * 0.7f, s * 0.14f))
    val c = Offset(s * 0.38f, h - s * 0.2f - abs(bob(t, 0.5f)) * s * 0.02f)
    drawOval(Color(0xFF4EAE3A), Offset(c.x - s * 0.17f, c.y - s * 0.1f), Size(s * 0.34f, s * 0.2f))
    for (k in listOf(-1f, 1f)) { drawCircle(Color(0xFF4EAE3A), s * 0.06f, c + Offset(k * s * 0.08f, -s * 0.1f)); drawCircle(Color.White, s * 0.04f, c + Offset(k * s * 0.08f, -s * 0.11f)); drawCircle(Color(0xFF1A1A1A), s * 0.02f, c + Offset(k * s * 0.08f, -s * 0.11f)) }
    drawArc(Color(0xFF2E6E2A), 20f, 140f, false, c - Offset(s * 0.07f, s * 0.04f), Size(s * 0.14f, s * 0.08f), style = Stroke(s * 0.012f, cap = StrokeCap.Round))
    drawCircle(Color(0xFFFF8AB8).copy(alpha = 0.6f), s * 0.02f, c + Offset(-s * 0.11f, -s * 0.01f)); drawCircle(Color(0xFFFF8AB8).copy(alpha = 0.6f), s * 0.02f, c + Offset(s * 0.11f, -s * 0.01f))
}

internal fun DrawScope.lotus(t: Float) {
    val s = S(); val h = size.height
    drawOval(Color(0xFF5CB84A), Offset(s * 0.06f, h - s * 0.13f), Size(s * 0.75f, s * 0.15f))
    val c = Offset(s * 0.42f, h - s * 0.16f)
    for ((deg, len) in listOf(-60f to 0.2f, -30f to 0.24f, 0f to 0.26f, 30f to 0.24f, 60f to 0.2f)) rotate(deg - 90f, c) {
        drawPath(leafPath(s * len, s * 0.06f).apply { translate(c) }, Brush.linearGradient(listOf(Color(0xFFFFE0EE), Color(0xFFFF8AB8)), c, c + Offset(s * len, 0f)))
    }
    drawCircle(Color(0xFFFFD23F), s * 0.04f, c - Offset(0f, s * 0.05f))
}

// ============================================================================================ seasons
internal fun DrawScope.pumpkin(c: Offset, r: Float, face: Boolean = false) {
    drawOval(Color.Black.copy(alpha = 0.14f), Offset(c.x - r + 3, c.y - r * 0.8f + 5), Size(r * 2, r * 1.6f))
    for (k in listOf(-0.55f, 0.55f, 0f)) drawOval(if (k == 0f) Color(0xFFFF9A2E) else Color(0xFFF07A1A), Offset(c.x + k * r - r * 0.55f, c.y - r * 0.8f), Size(r * 1.1f, r * 1.6f))
    drawRoundRect(Color(0xFF5A7A2A), Offset(c.x - r * 0.08f, c.y - r * 1.05f), Size(r * 0.16f, r * 0.32f), CornerRadius(r * 0.05f))
    if (face) {
        for (k in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(c.x + k * r * 0.35f, c.y - r * 0.35f); lineTo(c.x + k * r * 0.15f, c.y - r * 0.05f); lineTo(c.x + k * r * 0.55f, c.y - r * 0.05f); close() }, Color(0xFF3A1A0A))
        drawPath(Path().apply { moveTo(c.x - r * 0.5f, c.y + r * 0.2f); lineTo(c.x + r * 0.5f, c.y + r * 0.2f); lineTo(c.x + r * 0.3f, c.y + r * 0.45f); lineTo(c.x, c.y + r * 0.32f); lineTo(c.x - r * 0.3f, c.y + r * 0.45f); close() }, Color(0xFF3A1A0A))
    }
}
internal fun DrawScope.pumpkins(t: Float, face: Boolean = false) {
    val s = S(); val h = size.height
    pumpkin(Offset(s * 0.34f, h - s * 0.2f), s * 0.22f, face)
    pumpkin(Offset(s * 0.72f, h - s * 0.12f), s * 0.13f)
    drawPath(leafPath(s * 0.2f, s * 0.06f).apply { translate(Offset(s * 0.06f, h - s * 0.05f)) }, Color(0xFF5A8A2A))
}

internal fun DrawScope.bat(c: Offset, r: Float, t: Float) {
    val f = sin(t * TAU * 8) * 0.35f
    for (side in listOf(-1f, 1f)) {
        val w = Path().apply {
            moveTo(c.x, c.y); cubicTo(c.x + side * r * 0.6f, c.y - r * (0.9f + f), c.x + side * r * 1.4f, c.y - r * (0.7f + f), c.x + side * r * 1.8f, c.y - r * (0.4f + f))
            lineTo(c.x + side * r * 1.4f, c.y - r * 0.1f); lineTo(c.x + side * r * 1.1f, c.y + r * 0.1f); lineTo(c.x + side * r * 0.7f, c.y); close()
        }
        drawPath(w, Color(0xFF2A1A3A))
    }
    drawOval(Color(0xFF2A1A3A), Offset(c.x - r * 0.35f, c.y - r * 0.4f), Size(r * 0.7f, r * 0.8f))
    for (k in listOf(-1f, 1f)) { drawPath(Path().apply { moveTo(c.x + k * r * 0.1f, c.y - r * 0.35f); lineTo(c.x + k * r * 0.3f, c.y - r * 0.7f); lineTo(c.x + k * r * 0.3f, c.y - r * 0.25f); close() }, Color(0xFF2A1A3A)); drawCircle(Color(0xFFFFE45C), r * 0.08f, c + Offset(k * r * 0.13f, -r * 0.15f)) }
}
internal fun DrawScope.bats(t: Float) {
    val s = S()
    bat(Offset(s * 0.42f, s * 0.38f + bob(t) * 6f), s * 0.13f, t)
    bat(Offset(s * 0.72f, s * 0.72f - bob(t) * 4f), s * 0.08f, t + 0.25f)
}

internal fun DrawScope.ghost(t: Float) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.42f + bob(t) * 6f); val r = s * 0.22f
    val p = Path().apply {
        moveTo(c.x - r, c.y + r * 1.1f); lineTo(c.x - r, c.y); cubicTo(c.x - r, c.y - r * 1.35f, c.x + r, c.y - r * 1.35f, c.x + r, c.y); lineTo(c.x + r, c.y + r * 1.1f)
        val waves = 4
        for (k in 0 until waves) { val x0 = c.x + r - k * 2 * r / waves; val ph = bob(t, 2f) * r * 0.08f; quadraticTo(x0 - r / waves * 0.5f, c.y + r * 0.85f + ph, x0 - r / waves, c.y + r * 1.1f) ; quadraticTo(x0 - r / waves * 1.5f, c.y + r * 1.35f - ph, x0 - 2 * r / waves, c.y + r * 1.1f) }
        close()
    }
    drawPath(p, Color.White.copy(alpha = 0.25f), style = Stroke(r * 0.3f))
    drawPath(p, Brush.verticalGradient(listOf(Color.White, Color(0xFFE6E0F6)), c.y - r, c.y + r))
    for (k in listOf(-1f, 1f)) drawOval(Color(0xFF2A1E3A), Offset(c.x + k * r * 0.35f - r * 0.1f, c.y - r * 0.3f), Size(r * 0.2f, r * 0.3f))
    drawOval(Color(0xFF2A1E3A), Offset(c.x - r * 0.12f, c.y + r * 0.15f), Size(r * 0.24f, r * 0.28f))
}

internal fun DrawScope.ornaments(t: Float) {
    val s = S()
    drawPath(Path().apply { moveTo(0f, s * 0.05f); cubicTo(s * 0.3f, s * 0.12f, s * 0.6f, s * 0.04f, s * 0.95f, s * 0.08f) }, Color(0xFF2E7A3A), style = Stroke(s * 0.06f, cap = StrokeCap.Round))
    for ((i, p) in listOf(Triple(0.18f, 0.32f, Color(0xFFE53950)), Triple(0.46f, 0.46f, Color(0xFFFFC72C)), Triple(0.74f, 0.30f, Color(0xFF3A8AE8))).withIndex()) {
        val top = Offset(p.first * s, s * 0.09f)
        rotate(bob(t + i * 0.3f) * 6f, top) {
            val c = Offset(top.x, top.y + p.second * s)
            drawLine(Color(0xFFD8C8A0), top, c, s * 0.008f)
            drawRect(Color(0xFFD8B040), Offset(c.x - s * 0.03f, c.y - s * 0.12f), Size(s * 0.06f, s * 0.04f))
            ball(c, s * 0.09f, p.third)
            drawLine(Color.White.copy(alpha = 0.5f), c + Offset(-s * 0.08f, s * 0.01f), c + Offset(s * 0.08f, s * 0.01f), s * 0.015f)
        }
    }
}

internal fun DrawScope.xmasTree(t: Float) {
    val s = S(); val h = size.height
    val x = s * 0.40f
    for (i in 0..2) {
        val top = h - s * (0.95f - i * 0.22f); val bot = top + s * 0.36f; val w = s * (0.17f + i * 0.08f)
        drawPath(Path().apply { moveTo(x, top); lineTo(x + w, bot); lineTo(x - w, bot); close() }, Brush.horizontalGradient(listOf(Color(0xFF3AA05A), Color(0xFF1E6A3A)), x - w, x + w))
        drawPath(Path().apply { moveTo(x - w * 0.9f, bot - s * 0.02f); quadraticTo(x, bot - s * 0.1f, x + w * 0.9f, bot - s * 0.02f) }, Color(0xFFFFD23F), style = Stroke(s * 0.012f))
    }
    drawRect(Color(0xFF6B3E1E), Offset(x - s * 0.04f, h - s * 0.1f), Size(s * 0.08f, s * 0.1f))
    val lights = listOf(Color(0xFFFF5C5C), Color(0xFFFFE45C), Color(0xFF5CC8FF), Color(0xFFFF8AD8))
    for (k in 0..9) { val a = 0.5f + 0.5f * sin(t * TAU * 3 + k); drawCircle(lights[k % 4].copy(alpha = 0.5f + 0.5f * a), s * 0.022f, Offset(x + ((k * 0.37f) % 1f - 0.5f) * s * 0.3f * (k / 10f + 0.4f), h - s * (0.8f - k * 0.065f))) }
    drawPath(starPath(x, h - s * 0.97f, s * 0.07f, s * 0.03f, 0f), Color(0xFFFFD23F))
}

internal fun DrawScope.gift(c: Offset, w: Float, box: Color, ribbon: Color) {
    drawRect(Color.Black.copy(alpha = 0.14f), Offset(c.x - w / 2 + 3, c.y - w + 5), Size(w, w))
    drawRect(box, Offset(c.x - w / 2, c.y - w), Size(w, w))
    drawRect(shade(box, 1.15f), Offset(c.x - w * 0.55f, c.y - w * 1.08f), Size(w * 1.1f, w * 0.22f))
    drawRect(ribbon, Offset(c.x - w * 0.08f, c.y - w * 1.08f), Size(w * 0.16f, w * 1.08f))
    for (k in listOf(-1f, 1f)) drawOval(ribbon, Offset(c.x + (if (k < 0) -w * 0.32f else 0f), c.y - w * 1.25f), Size(w * 0.32f, w * 0.2f))
}
internal fun DrawScope.gifts(t: Float) {
    val s = S(); val h = size.height
    gift(Offset(s * 0.32f, h), s * 0.34f, Color(0xFFE53950), Color(0xFFFFD23F))
    gift(Offset(s * 0.68f, h), s * 0.22f, Color(0xFF3A8AE8), Color(0xFFFFFFFF))
    gift(Offset(s * 0.56f, h - s * 0.22f), s * 0.16f, Color(0xFF4EB86A), Color(0xFFE53950))
}

internal fun DrawScope.lightString(t: Float) {
    val s = S()
    val wire = Path().apply { moveTo(0f, s * 0.06f); quadraticTo(s * 0.45f, s * 0.36f, s * 0.95f, s * 0.1f) }
    drawPath(wire, Color(0xFF2E3A2A), style = Stroke(s * 0.012f))
    val cols = listOf(Color(0xFFFF5C5C), Color(0xFFFFE45C), Color(0xFF5CC8FF), Color(0xFF7CFF8A), Color(0xFFFF8AD8))
    for (k in 0..6) {
        val f = (k + 0.5f) / 7f
        val x = s * 0.95f * f; val y = (1 - f) * (1 - f) * s * 0.06f + 2 * (1 - f) * f * s * 0.36f + f * f * s * 0.1f
        val on = 0.55f + 0.45f * sin(t * TAU * 4 + k * 1.3f)
        drawCircle(cols[k % 5].copy(alpha = 0.3f * on), s * 0.06f, Offset(x, y + s * 0.05f))
        drawOval(cols[k % 5].copy(alpha = 0.6f + 0.4f * on), Offset(x - s * 0.022f, y + s * 0.015f), Size(s * 0.044f, s * 0.07f))
        drawRect(Color(0xFF4A5A4A), Offset(x - s * 0.015f, y), Size(s * 0.03f, s * 0.02f))
    }
}

internal fun DrawScope.bunting(t: Float, cols: List<Color>) {
    val s = S()
    val sag = s * (0.22f + 0.02f * bob(t))
    drawPath(Path().apply { moveTo(0f, s * 0.05f); quadraticTo(s * 0.5f, sag + s * 0.1f, s, s * 0.08f) }, Color(0xFF8A6A4A), style = Stroke(s * 0.01f))
    for (k in 0..5) {
        val f = (k + 0.5f) / 6f
        val x = s * f; val y = (1 - f) * (1 - f) * s * 0.05f + 2 * (1 - f) * f * (sag + s * 0.1f) + f * f * s * 0.08f
        drawPath(Path().apply { moveTo(x - s * 0.06f, y); lineTo(x + s * 0.06f, y); lineTo(x, y + s * 0.15f); close() }, cols[k % cols.size])
    }
}

internal fun DrawScope.snowflakeAt(c: Offset, r: Float, rot: Float, col: Color = Color.White) {
    rotate(rot, c) {
        for (k in 0 until 6) {
            val a = k * TAU / 6; val e = c + Offset(cos(a), sin(a)) * r
            drawLine(col, c, e, r * 0.12f, StrokeCap.Round)
            for (j in listOf(-1f, 1f)) { val m = c + Offset(cos(a), sin(a)) * (r * 0.6f); drawLine(col, m, m + Offset(cos(a + j * 0.7f), sin(a + j * 0.7f)) * (r * 0.32f), r * 0.1f, StrokeCap.Round) }
        }
        drawCircle(col, r * 0.14f, c)
    }
}
internal fun DrawScope.snowflakes(t: Float) {
    val s = S()
    snowflakeAt(Offset(s * 0.42f, s * 0.42f + bob(t) * 4f), s * 0.22f, t * 60f, Color(0xFFEAF6FF))
    snowflakeAt(Offset(s * 0.78f, s * 0.78f), s * 0.1f, -t * 90f, Color(0xFFD6ECFF))
}

internal fun DrawScope.iceCrystals(t: Float, col: Color = Color(0xFFBFE6FF)) {
    val s = S(); val h = size.height
    for ((x, tall, deg) in listOf(Triple(0.30f, 0.6f, -10f), Triple(0.46f, 0.42f, 18f), Triple(0.16f, 0.36f, -28f), Triple(0.66f, 0.28f, 30f))) rotate(deg, Offset(x * s, h)) {
        val w = s * 0.07f
        val p = Path().apply { moveTo(x * s - w, h); lineTo(x * s - w, h - tall * s * 0.8f); lineTo(x * s, h - tall * s); lineTo(x * s + w, h - tall * s * 0.8f); lineTo(x * s + w, h); close() }
        drop(p)
        drawPath(p, Brush.horizontalGradient(listOf(shade(col, 1.2f), col, shade(col, 0.7f)), x * s - w, x * s + w))
        drawLine(Color.White.copy(alpha = 0.7f), Offset(x * s - w * 0.4f, h - s * 0.04f), Offset(x * s - w * 0.4f, h - tall * s * 0.78f), s * 0.012f)
    }
    sparkle(Offset(s * 0.44f, h - s * 0.62f), s * 0.05f * (0.7f + 0.3f * bob(t, 3f)), Color.White)
}
internal fun DrawScope.crystalCeiling(t: Float, col: Color) {
    val s = S()
    for ((x, len) in listOf(0.12f to 0.45f, 0.28f to 0.30f, 0.44f to 0.52f, 0.60f to 0.25f, 0.76f to 0.36f)) {
        val w = s * 0.06f
        val p = Path().apply { moveTo(x * s - w, 0f); lineTo(x * s + w, 0f); lineTo(x * s + w * 0.8f, len * s * 0.8f); lineTo(x * s, len * s); lineTo(x * s - w * 0.8f, len * s * 0.8f); close() }
        drawPath(p, Brush.horizontalGradient(listOf(shade(col, 1.25f), col, shade(col, 0.7f)), x * s - w, x * s + w))
    }
    sparkle(Offset(s * 0.44f, s * 0.5f), s * 0.05f * (0.7f + 0.3f * bob(t, 3f)), Color.White)
}

internal fun DrawScope.penguin(t: Float) {
    val s = S(); val h = size.height
    drawOval(Color.White, Offset(s * 0.02f, h - s * 0.1f), Size(s * 0.8f, s * 0.14f))
    val c = Offset(s * 0.38f, h - s * 0.32f); val wob = bob(t) * 4f
    rotate(wob, Offset(c.x, h)) {
        drawOval(Color(0xFF1E2A3A), Offset(c.x - s * 0.15f, c.y - s * 0.25f), Size(s * 0.3f, s * 0.5f))
        drawOval(Color.White, Offset(c.x - s * 0.1f, c.y - s * 0.12f), Size(s * 0.2f, s * 0.36f))
        for (k in listOf(-1f, 1f)) { drawCircle(Color.White, s * 0.035f, c + Offset(k * s * 0.05f, -s * 0.16f)); drawCircle(Color(0xFF1A1A1A), s * 0.017f, c + Offset(k * s * 0.05f, -s * 0.16f)) }
        drawPath(Path().apply { moveTo(c.x - s * 0.03f, c.y - s * 0.11f); lineTo(c.x + s * 0.03f, c.y - s * 0.11f); lineTo(c.x, c.y - s * 0.06f); close() }, Color(0xFFFF9A2E))
        for (k in listOf(-1f, 1f)) drawOval(Color(0xFFFF9A2E), Offset(c.x + k * s * 0.07f - s * 0.05f, c.y + s * 0.22f), Size(s * 0.1f, s * 0.05f))
        drawRoundRect(Color(0xFFE53950), Offset(c.x - s * 0.12f, c.y - s * 0.07f), Size(s * 0.24f, s * 0.04f), CornerRadius(s * 0.02f))
    }
}

internal fun DrawScope.igloo(t: Float) {
    val s = S(); val h = size.height
    val c = Offset(s * 0.42f, h); val r = s * 0.34f
    drawArc(Color(0xFF9ACBEE).copy(alpha = 0.5f), 180f, 180f, true, c - Offset(r - 3f, r - 5f), Size(r * 2, r * 2))
    drawArc(Brush.verticalGradient(listOf(Color.White, Color(0xFFD6EAF8)), c.y - r, c.y), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
    for (k in 1..3) { val rr = r * (1 - k * 0.25f); drawArc(Color(0xFFB8D6EE), 180f, 180f, false, Offset(c.x - r, c.y - rr), Size(r * 2, rr * 2), style = Stroke(s * 0.008f)) }
    drawArc(Color(0xFF3A5A7A), 180f, 180f, true, Offset(c.x + r * 0.2f, c.y - r * 0.45f), Size(r * 0.5f, r * 0.9f))
}

// ============================================================================================ sky & space
internal fun DrawScope.rainCloud(t: Float, dark: Boolean) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.36f + bob(t) * 3f)
    cloudShape(c, s * 0.7f, if (dark) Color(0xFF5A6478) else Color(0xFFB8C8DA))
    if (dark) for ((x, y, r) in listOf(Triple(-0.32f, 0.10f, 0.26f), Triple(-0.08f, -0.10f, 0.34f), Triple(0.22f, -0.02f, 0.28f), Triple(0.40f, 0.12f, 0.20f), Triple(0.02f, 0.14f, 0.26f))) drawCircle(Color(0xFF8A94A8), r * s * 0.7f * 0.94f, c + Offset(x * s * 0.7f, y * s * 0.7f))
    for (k in 0..3) { val p = (t * 3f + k * 0.25f) % 1f; drawLine(Color(0xFF7AB8F0), c + Offset((k - 1.5f) * s * 0.1f, s * 0.15f + p * s * 0.3f), c + Offset((k - 1.5f) * s * 0.1f - s * 0.015f, s * 0.2f + p * s * 0.3f), s * 0.02f, StrokeCap.Round) }
    if (dark && (t * 4f) % 1f < 0.08f) drawPath(Path().apply { moveTo(c.x, c.y + s * 0.12f); lineTo(c.x - s * 0.06f, c.y + s * 0.3f); lineTo(c.x + s * 0.01f, c.y + s * 0.28f); lineTo(c.x - s * 0.04f, c.y + s * 0.45f); lineTo(c.x + s * 0.08f, c.y + s * 0.24f); lineTo(c.x + s * 0.02f, c.y + s * 0.25f); close() }, Color(0xFFFFE45C))
}

internal fun DrawScope.umbrella(t: Float, col: Color) {
    val s = S(); val h = size.height
    val c = Offset(s * 0.40f, h - s * 0.62f)
    rotate(-12f + bob(t) * 3f, Offset(s * 0.4f, h)) {
        drawLine(Color(0xFF4A3A2A), Offset(c.x, c.y), Offset(c.x, h - s * 0.1f), s * 0.02f)
        drawArc(Color(0xFF4A3A2A), 0f, 180f, false, Offset(c.x - s * 0.08f, h - s * 0.16f), Size(s * 0.08f, s * 0.12f), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
        val r = s * 0.32f
        val canopy = Path().apply {
            moveTo(c.x - r, c.y); cubicTo(c.x - r, c.y - r * 0.9f, c.x + r, c.y - r * 0.9f, c.x + r, c.y)
            for (k in 3 downTo 0) quadraticTo(c.x - r + (k + 0.5f) * r / 2, c.y - r * 0.15f, c.x - r + k * r / 2, c.y)
            close()
        }
        drop(canopy); drawPath(canopy, col)
        for (k in listOf(-0.5f, 0f, 0.5f)) drawLine(shade(col, 0.75f), Offset(c.x, c.y - r * 0.68f), Offset(c.x + k * r, c.y), s * 0.01f)
        drawCircle(Color(0xFF4A3A2A), s * 0.015f, Offset(c.x, c.y - r * 0.7f))
    }
}

internal fun DrawScope.rainbowSmall(t: Float) {
    val s = S(); val c = Offset(s * 0.5f, s * 0.7f)
    val cols = listOf(Color(0xFFFF5C5C), Color(0xFFFFA040), Color(0xFFFFE14D), Color(0xFF5CD67A), Color(0xFF4FA8FF), Color(0xFF8E7CFF))
    for ((i, col) in cols.withIndex()) { val r = s * (0.42f - i * 0.045f); drawArc(col, 180f, 180f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(s * 0.046f)) }
    cloudShape(c + Offset(-s * 0.36f, 0f), s * 0.22f); cloudShape(c + Offset(s * 0.36f, 0f), s * 0.22f)
}

internal fun DrawScope.starProp(t: Float, col: Color = Color(0xFFFFD23F), face: Boolean = true) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.45f + bob(t) * 4f)
    rotate(bob(t, 0.5f) * 10f, c) {
        val p = starPath(c.x, c.y, s * 0.3f, s * 0.15f, 0f)
        drop(p)
        drawPath(p, Brush.radialGradient(listOf(shade(col, 1.2f), col), c - Offset(s * 0.08f, s * 0.08f), s * 0.35f))
        drawPath(p, shade(col, 0.8f), style = Stroke(s * 0.02f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        if (face) {
            for (k in listOf(-1f, 1f)) drawCircle(Color(0xFF4A2A10), s * 0.022f, c + Offset(k * s * 0.06f, -s * 0.02f))
            drawArc(Color(0xFF4A2A10), 20f, 140f, false, c + Offset(-s * 0.05f, -s * 0.01f), Size(s * 0.1f, s * 0.07f), style = Stroke(s * 0.015f, cap = StrokeCap.Round))
        }
    }
}

internal fun DrawScope.rocket(t: Float) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.45f + bob(t, 2f) * 3f)
    rotate(35f, c) {
        val flame = 0.8f + 0.2f * sin(t * TAU * 12)
        drawPath(Path().apply { moveTo(c.x - s * 0.06f, c.y + s * 0.2f); lineTo(c.x + s * 0.06f, c.y + s * 0.2f); lineTo(c.x, c.y + s * 0.2f + s * 0.2f * flame); close() }, Color(0xFFFFB03A))
        drawPath(Path().apply { moveTo(c.x - s * 0.035f, c.y + s * 0.2f); lineTo(c.x + s * 0.035f, c.y + s * 0.2f); lineTo(c.x, c.y + s * 0.2f + s * 0.12f * flame); close() }, Color(0xFFFFF08A))
        for (k in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(c.x + k * s * 0.08f, c.y + s * 0.02f); lineTo(c.x + k * s * 0.17f, c.y + s * 0.2f); lineTo(c.x + k * s * 0.07f, c.y + s * 0.18f); close() }, Color(0xFFE53950))
        val body = Path().apply { moveTo(c.x, c.y - s * 0.3f); cubicTo(c.x + s * 0.12f, c.y - s * 0.18f, c.x + s * 0.09f, c.y + s * 0.1f, c.x + s * 0.07f, c.y + s * 0.2f); lineTo(c.x - s * 0.07f, c.y + s * 0.2f); cubicTo(c.x - s * 0.09f, c.y + s * 0.1f, c.x - s * 0.12f, c.y - s * 0.18f, c.x, c.y - s * 0.3f); close() }
        drop(body)
        drawPath(body, Brush.horizontalGradient(listOf(Color.White, Color(0xFFC8D2E0)), c.x - s * 0.1f, c.x + s * 0.1f))
        drawPath(Path().apply { moveTo(c.x, c.y - s * 0.3f); cubicTo(c.x + s * 0.07f, c.y - s * 0.24f, c.x + s * 0.09f, c.y - s * 0.2f, c.x + s * 0.095f, c.y - s * 0.17f); lineTo(c.x - s * 0.095f, c.y - s * 0.17f); cubicTo(c.x - s * 0.09f, c.y - s * 0.2f, c.x - s * 0.07f, c.y - s * 0.24f, c.x, c.y - s * 0.3f); close() }, Color(0xFFE53950))
        drawCircle(Color(0xFF5CC8FF), s * 0.04f, c - Offset(0f, s * 0.05f)); drawCircle(Color(0xFF3A5A7A), s * 0.04f, c - Offset(0f, s * 0.05f), style = Stroke(s * 0.012f))
    }
}

internal fun DrawScope.ufo(t: Float) {
    val s = S(); val c = Offset(s * 0.45f + bob(t) * s * 0.04f, s * 0.42f + bob(t, 2f) * 3f)
    drawPath(Path().apply { moveTo(c.x - s * 0.08f, c.y + s * 0.06f); lineTo(c.x + s * 0.08f, c.y + s * 0.06f); lineTo(c.x + s * 0.2f, c.y + s * 0.45f); lineTo(c.x - s * 0.2f, c.y + s * 0.45f); close() }, Brush.verticalGradient(listOf(Color(0x88B8FF8A), Color.Transparent), c.y, c.y + s * 0.45f))
    drawOval(Color(0xFF9AF0FF).copy(alpha = 0.85f), Offset(c.x - s * 0.12f, c.y - s * 0.17f), Size(s * 0.24f, s * 0.2f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFFD8E0EE), Color(0xFF7A8AA8)), c.y - s * 0.06f, c.y + s * 0.07f), Offset(c.x - s * 0.3f, c.y - s * 0.06f), Size(s * 0.6f, s * 0.14f))
    for (k in -2..2) drawCircle(if ((k + (t * 8).toInt()) % 2 == 0) Color(0xFFFFE45C) else Color(0xFFFF5C8A), s * 0.02f, Offset(c.x + k * s * 0.1f, c.y + s * 0.01f))
}

internal fun DrawScope.comet(t: Float) {
    val s = S(); val head = Offset(s * 0.62f, s * 0.36f + bob(t) * 3f)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, Color(0xFFBFF0FF)), head - Offset(s * 0.55f, -s * 0.3f), head), head - Offset(s * 0.55f, -s * 0.3f), head, s * 0.1f, StrokeCap.Round)
    drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), head - Offset(s * 0.4f, -s * 0.22f), head), head - Offset(s * 0.4f, -s * 0.22f), head, s * 0.04f, StrokeCap.Round)
    drawCircle(Color(0x66BFF0FF), s * 0.1f, head); drawCircle(Color.White, s * 0.06f, head)
}

internal fun DrawScope.kite(t: Float, a: Color, b: Color) {
    val s = S(); val c = Offset(s * 0.45f, s * 0.36f)
    rotate(bob(t) * 10f, c) {
        val p = Path().apply { moveTo(c.x, c.y - s * 0.26f); lineTo(c.x + s * 0.18f, c.y); lineTo(c.x, c.y + s * 0.2f); lineTo(c.x - s * 0.18f, c.y); close() }
        drop(p); drawPath(p, a)
        drawPath(Path().apply { moveTo(c.x, c.y - s * 0.26f); lineTo(c.x + s * 0.18f, c.y); lineTo(c.x, c.y); close() }, b)
        drawPath(Path().apply { moveTo(c.x, c.y + s * 0.2f); lineTo(c.x - s * 0.18f, c.y); lineTo(c.x, c.y); close() }, b)
        val tail = Path().apply { moveTo(c.x, c.y + s * 0.2f); for (k in 1..6) lineTo(c.x + sin(k * 1.2f + t * TAU * 2) * s * 0.05f, c.y + s * 0.2f + k * s * 0.06f) }
        drawPath(tail, Color(0xFF6A4A2A), style = Stroke(s * 0.008f))
        for (k in 1..3) drawPath(Path().apply { val y = c.y + s * 0.2f + k * s * 0.11f; val x = c.x + sin(k * 2f + t * TAU * 2) * s * 0.05f; moveTo(x - s * 0.04f, y - s * 0.02f); lineTo(x + s * 0.04f, y + s * 0.02f); lineTo(x + s * 0.04f, y - s * 0.02f); lineTo(x - s * 0.04f, y + s * 0.02f); close() }, if (k % 2 == 0) a else b)
    }
}

internal fun DrawScope.paperPlane(t: Float, col: Color = Color.White) {
    val s = S(); val c = Offset(s * 0.45f + bob(t) * s * 0.05f, s * 0.42f + bob(t, 2f) * 4f)
    rotate(-15f + bob(t) * 5f, c) {
        val top = Path().apply { moveTo(c.x + s * 0.3f, c.y); lineTo(c.x - s * 0.25f, c.y - s * 0.12f); lineTo(c.x - s * 0.1f, c.y + s * 0.02f); close() }
        val bot = Path().apply { moveTo(c.x + s * 0.3f, c.y); lineTo(c.x - s * 0.1f, c.y + s * 0.02f); lineTo(c.x - s * 0.2f, c.y + s * 0.12f); close() }
        drop(top); drawPath(top, col); drawPath(bot, shade(col, 0.82f))
        drawLine(shade(col, 0.7f), Offset(c.x + s * 0.3f, c.y), Offset(c.x - s * 0.1f, c.y + s * 0.02f), s * 0.008f)
    }
    for (k in 1..3) drawCircle(Color.White.copy(alpha = 0.6f), s * 0.015f, c + Offset(-s * (0.2f + k * 0.08f), s * 0.05f * k))
}

internal fun DrawScope.windmill(t: Float, big: Boolean) {
    val s = S(); val h = size.height; val sc = if (big) 1f else 0.7f
    val x = s * 0.40f; val top = h - s * 0.62f * sc
    drawPath(Path().apply { moveTo(x - s * 0.1f * sc, h); lineTo(x - s * 0.06f * sc, top); lineTo(x + s * 0.06f * sc, top); lineTo(x + s * 0.1f * sc, h); close() }, Brush.horizontalGradient(listOf(Color(0xFFF6EEE0), Color(0xFFC8B8A0)), x - s * 0.1f, x + s * 0.1f))
    drawPath(Path().apply { moveTo(x - s * 0.08f * sc, top); lineTo(x, top - s * 0.1f * sc); lineTo(x + s * 0.08f * sc, top); close() }, Color(0xFFB84A3A))
    drawRoundRect(Color(0xFF6B3E1E), Offset(x - s * 0.03f * sc, h - s * 0.12f * sc), Size(s * 0.06f * sc, s * 0.12f * sc), CornerRadius(s * 0.03f))
    val hub = Offset(x, top + s * 0.02f)
    rotate(t * 360f, hub) {
        for (k in 0..3) rotate(k * 90f, hub) {
            drawLine(Color(0xFF6B4A2A), hub, hub + Offset(0f, -s * 0.36f * sc), s * 0.012f)
            drawRect(Color(0xFFF6F0E6).copy(alpha = 0.9f), Offset(hub.x, hub.y - s * 0.34f * sc), Size(s * 0.07f * sc, s * 0.26f * sc))
        }
    }
    drawCircle(Color(0xFF6B4A2A), s * 0.025f, hub)
}
