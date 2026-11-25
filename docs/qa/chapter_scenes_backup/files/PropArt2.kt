package com.ngstudio.mazebrush.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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

/* More prop painters for chapters 11..100 (water, earth, sweets, fun). Same conventions as PropArt.kt. */

private fun DrawScope.sz() = size.minDimension
private fun dk(c: Color, k: Float) = Color((c.red * k).coerceIn(0f, 1f), (c.green * k).coerceIn(0f, 1f), (c.blue * k).coerceIn(0f, 1f), c.alpha)
private fun DrawScope.drop2(p: Path) = translate(3f, 5f) { drawPath(p, Color.Black.copy(alpha = 0.15f)) }
private fun wave(t: Float, k: Float = 1f) = sin(t * TAU * k)
private fun DrawScope.glossBall(c: Offset, r: Float, col: Color) {
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(Brush.radialGradient(listOf(dk(col, 1.25f), col, dk(col, 0.75f)), c - Offset(r * 0.35f, r * 0.4f), r * 1.5f), r, c)
    drawCircle(Color.White.copy(alpha = 0.4f), r * 0.22f, c - Offset(r * 0.38f, r * 0.4f))
}

// ============================================================================================ water
internal fun DrawScope.jellyfish(t: Float, col: Color) {
    val s = sz(); val pulse = 0.9f + 0.1f * wave(t, 2f)
    val c = Offset(s * 0.45f, s * 0.36f + wave(t) * 6f)
    for (k in 0..4) {
        val x = c.x + (k - 2) * s * 0.06f
        val p = Path().apply { moveTo(x, c.y); for (j in 1..6) lineTo(x + sin(j * 1.1f + t * TAU * 2 + k) * s * 0.03f, c.y + j * s * 0.07f) }
        drawPath(p, col.copy(alpha = 0.7f), style = Stroke(s * 0.016f, cap = StrokeCap.Round))
    }
    drawCircle(col.copy(alpha = 0.25f), s * 0.28f * pulse, c)
    drawArc(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), col), c.y - s * 0.2f, c.y), 180f, 180f, true, c - Offset(s * 0.2f * pulse, s * 0.2f), Size(s * 0.4f * pulse, s * 0.4f))
    for (k in listOf(-1f, 1f)) drawCircle(Color(0xFF3A1E4A), s * 0.018f, c + Offset(k * s * 0.06f, -s * 0.06f))
}

internal fun DrawScope.treasureChest(t: Float) {
    val s = sz(); val h = size.height
    val x = s * 0.40f; val w = s * 0.5f; val top = h - s * 0.3f
    drawCircle(Color(0x55FFD23F), s * (0.32f + 0.03f * wave(t, 2f)), Offset(x, top))
    drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(x - w / 2 + 3, top + 5), Size(w, h - top), CornerRadius(s * 0.02f))
    drawRoundRect(Color(0xFF8A5028), Offset(x - w / 2, top), Size(w, h - top), CornerRadius(s * 0.02f))
    // open lid
    drawPath(Path().apply { moveTo(x - w / 2, top); lineTo(x - w * 0.45f, top - s * 0.2f); lineTo(x + w * 0.45f, top - s * 0.2f); lineTo(x + w / 2, top); close() }, Color(0xFF6A3A1E))
    for (k in 0..6) drawCircle(Color(0xFFFFD23F), s * 0.04f, Offset(x - w * 0.36f + k * w * 0.12f, top - s * 0.01f - (k % 2) * s * 0.03f))
    drawCircle(Color(0xFFE53950), s * 0.03f, Offset(x - w * 0.1f, top - s * 0.05f)); drawCircle(Color(0xFF3AC8E8), s * 0.025f, Offset(x + w * 0.15f, top - s * 0.06f))
    for (k in listOf(-0.32f, 0.32f)) drawRect(Color(0xFFD8A83A), Offset(x + k * w - s * 0.02f, top), Size(s * 0.04f, h - top))
    drawRoundRect(Color(0xFFD8A83A), Offset(x - s * 0.04f, top + s * 0.05f), Size(s * 0.08f, s * 0.09f), CornerRadius(s * 0.015f))
}

internal fun DrawScope.seagulls(t: Float) {
    val s = sz()
    for (i in 0..1) {
        val c = Offset(s * (0.35f + i * 0.3f), s * (0.4f + i * 0.2f) + wave(t + i * 0.3f) * 5f)
        val f = 0.5f + 0.5f * wave(t, 5f); val w = s * (0.16f - i * 0.05f)
        val p = Path().apply { moveTo(c.x - w, c.y - w * 0.35f * f); cubicTo(c.x - w * 0.6f, c.y - w * 0.7f * f, c.x - w * 0.2f, c.y - w * 0.3f, c.x, c.y); cubicTo(c.x + w * 0.2f, c.y - w * 0.3f, c.x + w * 0.6f, c.y - w * 0.7f * f, c.x + w, c.y - w * 0.35f * f) }
        drawPath(p, Color.White, style = Stroke(s * 0.03f, cap = StrokeCap.Round))
        drawPath(p, Color(0xFF6A7A8A), style = Stroke(s * 0.01f, cap = StrokeCap.Round))
    }
}

internal fun DrawScope.sailboat(t: Float, sail: Color, hull: Color) {
    val s = sz(); val h = size.height
    val rock = wave(t) * 3f
    rotate(rock, Offset(s * 0.4f, h - s * 0.1f)) {
        val hullP = Path().apply { moveTo(s * 0.08f, h - s * 0.2f); lineTo(s * 0.74f, h - s * 0.2f); lineTo(s * 0.62f, h - s * 0.06f); lineTo(s * 0.18f, h - s * 0.06f); close() }
        drop2(hullP); drawPath(hullP, hull)
        drawLine(Color(0xFF6B4A2A), Offset(s * 0.4f, h - s * 0.2f), Offset(s * 0.4f, h - s * 0.85f), s * 0.02f)
        drawPath(Path().apply { moveTo(s * 0.42f, h - s * 0.82f); lineTo(s * 0.72f, h - s * 0.26f); lineTo(s * 0.42f, h - s * 0.26f); close() }, sail)
        drawPath(Path().apply { moveTo(s * 0.38f, h - s * 0.72f); lineTo(s * 0.16f, h - s * 0.28f); lineTo(s * 0.38f, h - s * 0.28f); close() }, dk(sail, 0.9f))
        drawPath(Path().apply { moveTo(s * 0.4f, h - s * 0.85f); lineTo(s * 0.52f, h - s * 0.82f); lineTo(s * 0.4f, h - s * 0.79f); close() }, Color(0xFFE53950))
    }
    drawPath(Path().apply { moveTo(0f, h - s * 0.06f); for (k in 0..8) quadraticTo(k * s * 0.11f + s * 0.055f, h - s * (0.1f + 0.02f * wave(t + k * 0.1f)), (k + 1) * s * 0.11f, h - s * 0.06f); lineTo(s, h); lineTo(0f, h); close() }, Color(0xFF5AB8E8).copy(alpha = 0.8f))
}

internal fun DrawScope.lighthouse(t: Float) {
    val s = sz(); val h = size.height
    val x = s * 0.38f; val top = h - s * 0.82f
    drawOval(Color(0xFF7A6A5A), Offset(s * 0.05f, h - s * 0.14f), Size(s * 0.7f, s * 0.2f))
    val tower = Path().apply { moveTo(x - s * 0.13f, h - s * 0.06f); lineTo(x - s * 0.08f, top + s * 0.12f); lineTo(x + s * 0.08f, top + s * 0.12f); lineTo(x + s * 0.13f, h - s * 0.06f); close() }
    drawPath(tower, Color.White)
    for (k in 0..2) { val y0 = h - s * (0.15f + k * 0.2f); drawPath(Path().apply { moveTo(x - s * (0.125f - k * 0.016f), y0); lineTo(x - s * (0.115f - k * 0.016f), y0 - s * 0.08f); lineTo(x + s * (0.115f - k * 0.016f), y0 - s * 0.08f); lineTo(x + s * (0.125f - k * 0.016f), y0); close() }, Color(0xFFE53950)) }
    val beam = 0.5f + 0.5f * wave(t, 2f)
    drawPath(Path().apply { moveTo(x, top + s * 0.06f); lineTo(x + s * 0.6f, top - s * 0.06f); lineTo(x + s * 0.6f, top + s * 0.18f); close() }, Brush.horizontalGradient(listOf(Color(0xAAFFF4B0).copy(alpha = 0.6f * beam), Color.Transparent), x, x + s * 0.6f))
    drawRect(Color(0xFFFFE45C), Offset(x - s * 0.06f, top + s * 0.02f), Size(s * 0.12f, s * 0.1f))
    drawRect(Color(0xFF3A3A4A), Offset(x - s * 0.1f, top + s * 0.11f), Size(s * 0.2f, s * 0.025f))
    drawPath(Path().apply { moveTo(x - s * 0.09f, top + s * 0.02f); lineTo(x, top - s * 0.07f); lineTo(x + s * 0.09f, top + s * 0.02f); close() }, Color(0xFFE53950))
}

internal fun DrawScope.crab(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.42f + wave(t) * s * 0.04f, h - s * 0.14f)
    for (k in listOf(-1f, 1f)) for (j in 0..2) drawLine(Color(0xFFD84A2A), c + Offset(k * s * 0.1f, s * 0.02f), c + Offset(k * s * (0.2f + j * 0.03f), s * (0.08f + j * 0.02f) - (if (j == 1) wave(t, 4f) * s * 0.01f else 0f)), s * 0.018f, StrokeCap.Round)
    for (k in listOf(-1f, 1f)) {
        drawLine(Color(0xFFE85A3A), c + Offset(k * s * 0.12f, -s * 0.03f), c + Offset(k * s * 0.2f, -s * 0.13f), s * 0.03f, StrokeCap.Round)
        val pc = c + Offset(k * s * 0.22f, -s * 0.17f)
        drawCircle(Color(0xFFE85A3A), s * 0.055f, pc)
        drawLine(Color(0xFFFFE6C8), pc, pc + Offset(k * s * 0.05f, -s * 0.04f * (0.6f + 0.4f * wave(t, 4f))), s * 0.015f)
    }
    drawOval(Brush.verticalGradient(listOf(Color(0xFFFF7A4A), Color(0xFFD8402A)), c.y - s * 0.1f, c.y + s * 0.06f), Offset(c.x - s * 0.15f, c.y - s * 0.1f), Size(s * 0.3f, s * 0.17f))
    for (k in listOf(-1f, 1f)) { drawLine(Color(0xFFD84A2A), c + Offset(k * s * 0.04f, -s * 0.08f), c + Offset(k * s * 0.05f, -s * 0.15f), s * 0.012f); drawCircle(Color.White, s * 0.025f, c + Offset(k * s * 0.05f, -s * 0.16f)); drawCircle(Color.Black, s * 0.012f, c + Offset(k * s * 0.05f, -s * 0.16f)) }
}

internal fun DrawScope.turtle(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.42f, h - s * 0.16f - wave(t) * s * 0.01f)
    for ((dx, dy) in listOf(-0.16f to 0.06f, 0.16f to 0.06f, -0.13f to -0.07f, 0.13f to -0.07f)) drawOval(Color(0xFF6CC27A), Offset(c.x + dx * s - s * 0.05f, c.y + dy * s - s * 0.03f), Size(s * 0.1f, s * 0.06f))
    drawCircle(Color(0xFF6CC27A), s * 0.06f, c + Offset(s * 0.22f, -s * 0.03f))
    drawCircle(Color.Black, s * 0.01f, c + Offset(s * 0.24f, -s * 0.045f))
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - s * 0.17f + 3, c.y - s * 0.13f + 5), Size(s * 0.34f, s * 0.22f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFF5AA84A), Color(0xFF2E7A3A)), c.y - s * 0.13f, c.y + s * 0.09f), Offset(c.x - s * 0.17f, c.y - s * 0.13f), Size(s * 0.34f, s * 0.22f))
    for ((dx, dy) in listOf(0f to -0.03f, -0.08f to 0f, 0.08f to 0f)) drawCircle(Color(0xFF8AC85A), s * 0.035f, c + Offset(dx * s, dy * s))
}

internal fun DrawScope.oyster(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.4f, h - s * 0.12f)
    val open = 0.6f + 0.4f * wave(t, 0.5f).let { abs(it) }
    drawArc(Color(0xFFC8A8C8), 0f, 180f, true, Offset(c.x - s * 0.22f, c.y - s * 0.12f), Size(s * 0.44f, s * 0.24f))
    rotate(-20f * open, Offset(c.x + s * 0.22f, c.y)) { drawArc(Brush.verticalGradient(listOf(Color(0xFFE8D0E8), Color(0xFFB898B8)), c.y - s * 0.2f, c.y), 180f, 180f, true, Offset(c.x - s * 0.22f, c.y - s * 0.12f), Size(s * 0.44f, s * 0.24f)) }
    glossBall(c + Offset(0f, -s * 0.03f), s * 0.07f, Color(0xFFFFF6FA))
    sparkle(c + Offset(s * 0.05f, -s * 0.09f), s * 0.04f * open, Color.White)
}

// ============================================================================================ earth & fire
internal fun DrawScope.volcano(t: Float, big: Boolean) {
    val s = sz(); val h = size.height; val sc = if (big) 1f else 0.7f
    val x = s * 0.42f; val top = h - s * 0.55f * sc
    val p = Path().apply { moveTo(x - s * 0.42f * sc, h); lineTo(x - s * 0.1f * sc, top); lineTo(x + s * 0.1f * sc, top); lineTo(x + s * 0.42f * sc, h); close() }
    drop2(p); drawPath(p, Brush.horizontalGradient(listOf(Color(0xFF6A3A2A), Color(0xFF3A1E14)), x - s * 0.4f, x + s * 0.4f))
    drawPath(Path().apply { moveTo(x - s * 0.1f * sc, top); lineTo(x - s * 0.04f * sc, top + s * 0.2f * sc); lineTo(x + s * 0.02f * sc, top + s * 0.08f * sc); lineTo(x + s * 0.07f * sc, top + s * 0.25f * sc); lineTo(x + s * 0.1f * sc, top); close() }, Color(0xFFFF6A1E))
    for (k in 0..4) { val p2 = (t * 1.5f + k * 0.2f) % 1f; drawCircle(Color(0xFFFFA03A).copy(alpha = 1 - p2), s * 0.03f * sc, Offset(x + (k - 2) * s * 0.04f * p2 * 3, top - p2 * s * 0.3f * sc)) }
    for (k in 0..2) { val p3 = (t + k / 3f) % 1f; drawCircle(Color(0xFF8A7A7A).copy(alpha = 0.5f * (1 - p3)), s * (0.06f + p3 * 0.1f) * sc, Offset(x + p3 * s * 0.1f, top - s * 0.05f - p3 * s * 0.4f * sc)) }
}

internal fun DrawScope.rocks(t: Float, col: Color = Color(0xFF9A8A7A)) {
    val s = sz(); val h = size.height
    for ((x, w) in listOf(0.30f to 0.36f, 0.62f to 0.24f, 0.12f to 0.16f)) {
        val c = Offset(x * s, h - w * s * 0.25f)
        drawOval(Brush.verticalGradient(listOf(dk(col, 1.2f), dk(col, 0.75f)), c.y - w * s * 0.35f, c.y + w * s * 0.3f), Offset(c.x - w * s / 2, c.y - w * s * 0.35f), Size(w * s, w * s * 0.65f))
        drawOval(Color.White.copy(alpha = 0.18f), Offset(c.x - w * s * 0.3f, c.y - w * s * 0.3f), Size(w * s * 0.35f, w * s * 0.15f))
    }
}

internal fun DrawScope.gems(t: Float) {
    val s = sz(); val h = size.height
    for ((i, p) in listOf(Triple(0.30f, 0.2f, Color(0xFFE53950)), Triple(0.56f, 0.15f, Color(0xFF3AC8E8)), Triple(0.76f, 0.11f, Color(0xFF6CD06A))).withIndex()) {
        val c = Offset(p.first * s, h - p.second * s * 0.9f); val r = p.second * s
        val g = Path().apply { moveTo(c.x - r, c.y - r * 0.3f); lineTo(c.x - r * 0.55f, c.y - r * 0.8f); lineTo(c.x + r * 0.55f, c.y - r * 0.8f); lineTo(c.x + r, c.y - r * 0.3f); lineTo(c.x, c.y + r * 0.8f); close() }
        drop2(g); drawPath(g, p.third)
        drawPath(Path().apply { moveTo(c.x - r * 0.55f, c.y - r * 0.8f); lineTo(c.x - r * 0.25f, c.y - r * 0.3f); lineTo(c.x + r * 0.25f, c.y - r * 0.3f); lineTo(c.x + r * 0.55f, c.y - r * 0.8f); close() }, dk(p.third, 1.3f))
        drawPath(Path().apply { moveTo(c.x - r, c.y - r * 0.3f); lineTo(c.x + r, c.y - r * 0.3f); lineTo(c.x, c.y + r * 0.8f); close() }, dk(p.third, 0.8f))
        if (i == 0) sparkle(c + Offset(-r * 0.4f, -r * 0.6f), r * 0.4f * (0.6f + 0.4f * wave(t, 3f)), Color.White)
    }
}

internal fun DrawScope.lantern(t: Float, col: Color, hang: Float = 0.3f) {
    val s = sz()
    val pivot = Offset(s * 0.4f, 0f)
    rotate(wave(t) * 5f, pivot) {
        val c = Offset(pivot.x, s * (hang + 0.18f))
        drawLine(Color(0xFF3A2A1A), pivot, c - Offset(0f, s * 0.18f), s * 0.01f)
        drawCircle(col.copy(alpha = 0.25f + 0.1f * wave(t, 3f)), s * 0.3f, c)
        drawRect(Color(0xFF3A2A1A), Offset(c.x - s * 0.07f, c.y - s * 0.2f), Size(s * 0.14f, s * 0.04f))
        drawOval(Brush.horizontalGradient(listOf(dk(col, 0.8f), dk(col, 1.25f), dk(col, 0.8f)), c.x - s * 0.15f, c.x + s * 0.15f), Offset(c.x - s * 0.15f, c.y - s * 0.17f), Size(s * 0.3f, s * 0.34f))
        for (k in -1..1) drawLine(dk(col, 0.7f), Offset(c.x + k * s * 0.07f, c.y - s * 0.15f), Offset(c.x + k * s * 0.07f, c.y + s * 0.15f), s * 0.008f)
        drawRect(Color(0xFF3A2A1A), Offset(c.x - s * 0.06f, c.y + s * 0.16f), Size(s * 0.12f, s * 0.03f))
        drawLine(Color(0xFFFFD23F), Offset(c.x, c.y + s * 0.19f), Offset(c.x, c.y + s * 0.28f), s * 0.012f)
    }
}

internal fun DrawScope.lanternsGround(t: Float) {
    val s = sz(); val h = size.height
    for ((x, col) in listOf(0.3f to Color(0xFFE84A3A), 0.66f to Color(0xFFFFA03A))) {
        val c = Offset(x * s, h - s * (0.3f + 0.04f * wave(t + x)))
        drawCircle(col.copy(alpha = 0.3f), s * 0.22f, c)
        drawOval(col, Offset(c.x - s * 0.12f, c.y - s * 0.14f), Size(s * 0.24f, s * 0.28f))
        drawRect(Color(0xFF3A2A1A), Offset(c.x - s * 0.05f, c.y - s * 0.17f), Size(s * 0.1f, s * 0.03f)); drawRect(Color(0xFF3A2A1A), Offset(c.x - s * 0.05f, c.y + s * 0.13f), Size(s * 0.1f, s * 0.03f))
    }
}

internal fun DrawScope.dinoEgg(t: Float) {
    val s = sz(); val h = size.height
    drawOval(Color(0xFFB89A6A), Offset(s * 0.06f, h - s * 0.1f), Size(s * 0.6f, s * 0.12f))
    val c = Offset(s * 0.36f, h - s * 0.28f)
    rotate(wave(t, 2f) * 4f, Offset(c.x, h - s * 0.06f)) {
        drawOval(Brush.radialGradient(listOf(Color(0xFFF6F0DC), Color(0xFFD8CCA8)), c - Offset(s * 0.06f, s * 0.08f), s * 0.3f), Offset(c.x - s * 0.15f, c.y - s * 0.22f), Size(s * 0.3f, s * 0.4f))
        for (k in 0..4) drawCircle(Color(0xFF8AC85A), s * 0.025f, c + Offset(((k * 0.37f) % 1f - 0.5f) * s * 0.2f, ((k * 0.61f) % 1f - 0.5f) * s * 0.3f))
        drawPath(Path().apply { moveTo(c.x - s * 0.14f, c.y - s * 0.02f); lineTo(c.x - s * 0.07f, c.y - s * 0.07f); lineTo(c.x - s * 0.02f, c.y); lineTo(c.x + s * 0.04f, c.y - s * 0.06f); lineTo(c.x + s * 0.1f, c.y); }, Color(0xFF8A7A5A), style = Stroke(s * 0.01f))
    }
    sparkle(Offset(s * 0.6f, h - s * 0.5f), s * 0.04f * (0.6f + 0.4f * wave(t, 3f)), Color(0xFFFFF4C8))
}

internal fun DrawScope.alienPlants(t: Float) {
    val s = sz(); val h = size.height
    for ((i, p) in listOf(Triple(0.25f, 0.6f, Color(0xFF7AF0C8)), Triple(0.5f, 0.42f, Color(0xFFFF8AD8)), Triple(0.72f, 0.3f, Color(0xFFFFE45C))).withIndex()) {
        val top = Offset(p.first * s + wave(t + i * 0.3f) * s * 0.02f, h - p.second * s)
        drawPath(Path().apply { moveTo(p.first * s, h); quadraticTo(p.first * s + s * 0.08f, h - p.second * s * 0.5f, top.x, top.y) }, Color(0xFF2E9E8A), style = Stroke(s * 0.03f, cap = StrokeCap.Round))
        drawCircle(p.third.copy(alpha = 0.3f + 0.2f * wave(t, 2f + i)), s * 0.1f, top)
        drawCircle(p.third, s * 0.055f, top); drawCircle(Color.White.copy(alpha = 0.6f), s * 0.018f, top - Offset(s * 0.015f, s * 0.015f))
    }
}

internal fun DrawScope.moonFlag(t: Float) {
    val s = sz(); val h = size.height
    drawOval(Color(0xFFB8BCC8), Offset(s * 0.02f, h - s * 0.12f), Size(s * 0.8f, s * 0.16f))
    drawLine(Color(0xFFD8DCE6), Offset(s * 0.35f, h - s * 0.06f), Offset(s * 0.35f, h - s * 0.75f), s * 0.02f)
    val f = wave(t, 2f) * s * 0.02f
    drawPath(Path().apply { moveTo(s * 0.36f, h - s * 0.74f); quadraticTo(s * 0.5f, h - s * 0.74f + f, s * 0.64f, h - s * 0.72f); lineTo(s * 0.64f, h - s * 0.52f); quadraticTo(s * 0.5f, h - s * 0.54f - f, s * 0.36f, h - s * 0.54f); close() }, Color(0xFFE53950))
    drawPath(starPath(s * 0.5f, h - s * 0.63f, s * 0.05f, s * 0.02f, 0f), Color.White)
}

internal fun DrawScope.telescope(t: Float) {
    val s = sz(); val h = size.height
    val base = Offset(s * 0.38f, h - s * 0.3f)
    for (k in listOf(-1f, 0f, 1f)) drawLine(Color(0xFF6B4A2A), base, Offset(base.x + k * s * 0.18f, h), s * 0.02f, StrokeCap.Round)
    rotate(-35f, base) {
        drawRoundRect(Color(0xFF3A5A9A), Offset(base.x - s * 0.08f, base.y - s * 0.06f), Size(s * 0.42f, s * 0.12f), CornerRadius(s * 0.03f))
        drawRoundRect(Color(0xFFD8B040), Offset(base.x + s * 0.3f, base.y - s * 0.08f), Size(s * 0.06f, s * 0.16f), CornerRadius(s * 0.02f))
    }
}

// ============================================================================================ fun & things
internal fun DrawScope.heartFloat(t: Float, col: Color) { val s = sz(); val sc = 1f + 0.06f * wave(t, 2f); heart(Offset(s * 0.45f, s * 0.45f + wave(t) * 4f), s * 0.22f * sc, col) }

internal fun DrawScope.balloonBunch(t: Float) {
    val s = sz(); val h = size.height
    val knot = Offset(s * 0.38f, h - s * 0.05f)
    for ((i, p) in listOf(Triple(-0.14f, 0.62f, Color(0xFFFF5C8A)), Triple(0.06f, 0.78f, Color(0xFF4FC3F7)), Triple(0.2f, 0.56f, Color(0xFFFFD23F)), Triple(-0.02f, 0.46f, Color(0xFF81C784))).withIndex()) {
        val c = Offset(knot.x + p.first * s + wave(t + i * 0.25f) * s * 0.02f, h - p.second * s)
        drawLine(Color(0xFF8A8A8A), knot, c + Offset(0f, s * 0.12f), s * 0.006f)
        drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - s * 0.1f + 3, c.y - s * 0.12f + 5), Size(s * 0.2f, s * 0.24f))
        drawOval(Brush.radialGradient(listOf(dk(p.third, 1.3f), p.third), c - Offset(s * 0.03f, s * 0.05f), s * 0.16f), Offset(c.x - s * 0.1f, c.y - s * 0.12f), Size(s * 0.2f, s * 0.24f))
        drawOval(Color.White.copy(alpha = 0.45f), Offset(c.x - s * 0.06f, c.y - s * 0.09f), Size(s * 0.04f, s * 0.07f))
    }
}

internal fun DrawScope.beachBall(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.4f, h - s * 0.19f - abs(wave(t, 1f)) * s * 0.04f); val r = s * 0.17f
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    rotate(t * 90f, c) {
        val cols = listOf(Color(0xFFE53950), Color.White, Color(0xFF3A8AE8), Color.White, Color(0xFFFFD23F), Color.White)
        for (k in 0..5) drawArc(cols[k], k * 60f, 60f, true, c - Offset(r, r), Size(r * 2, r * 2))
    }
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.3f, c - Offset(r * 0.35f, r * 0.4f))
}

internal fun DrawScope.toyBlocks(t: Float) {
    val s = sz(); val h = size.height; val b = s * 0.2f
    val blocks = listOf(Triple(0.12f, 0f, Color(0xFFFF5C5C)), Triple(0.34f, 0f, Color(0xFF4FA8FF)), Triple(0.23f, 1f, Color(0xFFFFD23F)), Triple(0.58f, 0f, Color(0xFF6CD06A)))
    val letters = listOf("A", "B", "C", "D")
    for ((i, p) in blocks.withIndex()) {
        val o = Offset(p.first * s, h - b * (p.second + 1))
        drawRect(Color.Black.copy(alpha = 0.14f), o + Offset(3f, 5f), Size(b, b))
        drawRect(p.third, o, Size(b, b))
        drawRect(dk(p.third, 1.2f), o + Offset(b * 0.12f, b * 0.12f), Size(b * 0.76f, b * 0.76f))
        drawCircle(Color.White.copy(alpha = 0.8f), b * 0.18f, o + Offset(b / 2, b / 2))
        if (i == 0 && letters.isNotEmpty()) drawCircle(p.third, b * 0.08f, o + Offset(b / 2, b / 2))
    }
}

internal fun DrawScope.musicNotes(t: Float) {
    val s = sz()
    for ((i, p) in listOf(Triple(0.35f, 0.42f, Color(0xFFFF5CC8)), Triple(0.68f, 0.68f, Color(0xFF5CC8FF))).withIndex()) {
        val c = Offset(p.first * s, p.second * s + wave(t + i * 0.4f) * 6f)
        val r = s * (0.09f - i * 0.025f)
        rotate(-15f + wave(t + i) * 8f, c) {
            drawOval(p.third, Offset(c.x - r, c.y - r * 0.7f), Size(r * 2, r * 1.4f))
            drawLine(p.third, Offset(c.x + r * 0.85f, c.y), Offset(c.x + r * 0.85f, c.y - r * 3.6f), r * 0.28f)
            drawPath(Path().apply { moveTo(c.x + r * 0.85f, c.y - r * 3.6f); quadraticTo(c.x + r * 2f, c.y - r * 3f, c.x + r * 1.8f, c.y - r * 2f) }, p.third, style = Stroke(r * 0.3f, cap = StrokeCap.Round))
        }
    }
}

internal fun DrawScope.drum(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.38f, h - s * 0.2f); val w = s * 0.44f
    drawRect(Color(0xFFE53950), Offset(c.x - w / 2, c.y - s * 0.12f), Size(w, s * 0.24f))
    for (k in 0..4) drawLine(Color(0xFFFFD23F), Offset(c.x - w / 2 + k * w / 4, c.y - s * 0.12f), Offset(c.x - w / 2 + (k + 0.5f) * w / 4, c.y + s * 0.12f), s * 0.01f)
    drawOval(Color(0xFFF6EEDC), Offset(c.x - w / 2, c.y - s * 0.17f), Size(w, s * 0.1f))
    val hit = abs(wave(t, 4f))
    drawLine(Color(0xFF8A5A2A), Offset(c.x - s * 0.1f, c.y - s * 0.13f - hit * s * 0.06f), Offset(c.x - s * 0.3f, c.y - s * 0.4f), s * 0.02f, StrokeCap.Round)
    drawLine(Color(0xFF8A5A2A), Offset(c.x + s * 0.1f, c.y - s * 0.13f - (1 - hit) * s * 0.06f), Offset(c.x + s * 0.3f, c.y - s * 0.42f), s * 0.02f, StrokeCap.Round)
}

internal fun DrawScope.paintPalette(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.4f, h - s * 0.2f)
    val p = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - s * 0.3f, c.y - s * 0.18f, c.x + s * 0.3f, c.y + s * 0.18f)) }
    val hole = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x + s * 0.12f, c.y + s * 0.02f, c.x + s * 0.22f, c.y + s * 0.1f)) }
    val pal = Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, p, hole)
    drop2(pal); drawPath(pal, Color(0xFFE6C28A))
    for ((i, col) in listOf(Color(0xFFFF5C5C), Color(0xFFFFD23F), Color(0xFF5CB8FF), Color(0xFF6CD06A), Color(0xFFB88AFF)).withIndex()) {
        val a = PI.toFloat() * (0.9f + i * 0.28f)
        drawCircle(col, s * 0.045f, c + Offset(cos(a) * s * 0.19f, sin(a) * s * 0.1f))
    }
    rotate(-30f + wave(t) * 4f, c) {
        drawLine(Color(0xFF8A5A2A), c + Offset(-s * 0.05f, -s * 0.05f), c + Offset(s * 0.35f, -s * 0.35f), s * 0.025f, StrokeCap.Round)
        drawLine(Color(0xFFB0B0B8), c + Offset(-s * 0.05f, -s * 0.05f), c + Offset(-s * 0.1f, 0f), s * 0.035f)
        drawLine(Color(0xFFFF5C5C), c + Offset(-s * 0.1f, 0f), c + Offset(-s * 0.15f, s * 0.05f), s * 0.03f, StrokeCap.Round)
    }
}

internal fun DrawScope.bookStack(t: Float) {
    val s = sz(); val h = size.height
    val books = listOf(Triple(0.46f, Color(0xFF9A3A2E), 0f), Triple(0.40f, Color(0xFF2E5A8A), 3f), Triple(0.44f, Color(0xFF3E7A4A), -4f), Triple(0.34f, Color(0xFFC89A3A), 6f))
    var y = h
    for ((w, col, rot) in books) {
        val bh = s * 0.09f
        rotate(rot, Offset(s * 0.38f, y - bh / 2)) {
            drawRect(Color.Black.copy(alpha = 0.14f), Offset(s * 0.38f - w * s / 2 + 3, y - bh + 4), Size(w * s, bh))
            drawRect(col, Offset(s * 0.38f - w * s / 2, y - bh), Size(w * s, bh))
            drawRect(Color(0xFFF6EEDC), Offset(s * 0.38f + w * s / 2 - s * 0.03f, y - bh + s * 0.012f), Size(s * 0.03f, bh - s * 0.024f))
            drawRect(Color(0xFFFFE08A), Offset(s * 0.38f - w * s * 0.3f, y - bh * 0.6f), Size(w * s * 0.3f, bh * 0.2f))
        }
        y -= bh
    }
}

internal fun DrawScope.crownFloat(t: Float) {
    val s = sz(); val c = Offset(s * 0.45f, s * 0.48f + wave(t) * 4f)
    rotate(wave(t, 0.5f) * 6f, c) {
        val p = Path().apply { moveTo(c.x - s * 0.26f, c.y + s * 0.12f); lineTo(c.x - s * 0.28f, c.y - s * 0.14f); lineTo(c.x - s * 0.13f, c.y - s * 0.02f); lineTo(c.x, c.y - s * 0.2f); lineTo(c.x + s * 0.13f, c.y - s * 0.02f); lineTo(c.x + s * 0.28f, c.y - s * 0.14f); lineTo(c.x + s * 0.26f, c.y + s * 0.12f); close() }
        drop2(p); drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFE0A020)), c.y - s * 0.2f, c.y + s * 0.12f))
        drawRect(Color(0xFFD8901A), Offset(c.x - s * 0.26f, c.y + s * 0.06f), Size(s * 0.52f, s * 0.06f))
        for ((k, col) in listOf(-1 to Color(0xFF3AC8E8), 0 to Color(0xFFE53950), 1 to Color(0xFF6CD06A))) drawCircle(col, s * 0.03f, Offset(c.x + k * s * 0.15f, c.y + s * 0.03f))
        for (k in listOf(-0.28f, 0f, 0.28f)) drawCircle(Color(0xFFFFF4C8), s * 0.025f, Offset(c.x + k * s, c.y - (if (k == 0f) 0.2f else 0.14f) * s))
    }
    sparkle(c + Offset(s * 0.25f, -s * 0.22f), s * 0.05f * (0.6f + 0.4f * wave(t, 3f)), Color.White)
}

internal fun DrawScope.chessPieces(t: Float) {
    val s = sz(); val h = size.height
    for ((x, col, king) in listOf(Triple(0.32f, Color(0xFF3A2A1E), true), Triple(0.66f, Color(0xFFF6EEDC), false))) {
        val sc = if (king) 1f else 0.7f; val cx = x * s
        val body = Path().apply { moveTo(cx - s * 0.14f * sc, h); lineTo(cx + s * 0.14f * sc, h); lineTo(cx + s * 0.1f * sc, h - s * 0.06f * sc); lineTo(cx + s * 0.05f * sc, h - s * 0.32f * sc); lineTo(cx - s * 0.05f * sc, h - s * 0.32f * sc); lineTo(cx - s * 0.1f * sc, h - s * 0.06f * sc); close() }
        drop2(body); drawPath(body, col)
        drawCircle(col, s * 0.09f * sc, Offset(cx, h - s * 0.4f * sc))
        drawRect(col, Offset(cx - s * 0.12f * sc, h - s * 0.34f * sc), Size(s * 0.24f * sc, s * 0.04f * sc))
        if (king) { drawRect(col, Offset(cx - s * 0.015f, h - s * 0.6f), Size(s * 0.03f, s * 0.12f)); drawRect(col, Offset(cx - s * 0.05f, h - s * 0.56f), Size(s * 0.1f, s * 0.03f)) }
        drawCircle(Color.White.copy(alpha = 0.25f), s * 0.03f * sc, Offset(cx - s * 0.03f * sc, h - s * 0.43f * sc))
    }
}

internal fun DrawScope.castleTower(t: Float) {
    val s = sz(); val h = size.height
    val x = s * 0.38f; val w = s * 0.3f; val top = h - s * 0.6f
    drawRect(Brush.horizontalGradient(listOf(Color(0xFFD8D0E6), Color(0xFFA8A0C0)), x - w / 2, x + w / 2), Offset(x - w / 2, top), Size(w, h - top))
    for (k in 0..2) drawRect(Color(0xFFB8B0D0), Offset(x - w / 2 + k * w / 2.5f, top - s * 0.05f), Size(w / 5, s * 0.06f))
    drawPath(Path().apply { moveTo(x - w * 0.6f, top - s * 0.04f); lineTo(x, top - s * 0.34f); lineTo(x + w * 0.6f, top - s * 0.04f); close() }, Color(0xFF6A5AC8))
    drawLine(Color(0xFF6B4A2A), Offset(x, top - s * 0.34f), Offset(x, top - s * 0.5f), s * 0.01f)
    val f = wave(t, 2f) * s * 0.015f
    drawPath(Path().apply { moveTo(x, top - s * 0.5f); quadraticTo(x + s * 0.08f, top - s * 0.48f + f, x + s * 0.14f, top - s * 0.46f); lineTo(x, top - s * 0.42f); close() }, Color(0xFFE53950))
    drawRoundRect(Color(0xFF5A3A2A), Offset(x - s * 0.05f, h - s * 0.15f), Size(s * 0.1f, s * 0.15f), CornerRadius(s * 0.05f, s * 0.05f))
    drawRoundRect(Color(0xFF3A3A5A), Offset(x - s * 0.03f, top + s * 0.1f), Size(s * 0.06f, s * 0.1f), CornerRadius(s * 0.03f))
}

internal fun DrawScope.gear(c: Offset, r: Float, col: Color, rot: Float) {
    rotate(rot, c) {
        for (k in 0 until 8) rotate(k * 45f, c) { drawRect(col, Offset(c.x - r * 0.18f, c.y - r * 1.2f), Size(r * 0.36f, r * 0.4f)) }
        drawCircle(col, r, c); drawCircle(dk(col, 0.7f), r * 0.4f, c); drawCircle(Color.White.copy(alpha = 0.25f), r * 0.85f, c, style = Stroke(r * 0.08f))
    }
}
internal fun DrawScope.gearsFloat(t: Float) { val s = sz(); gear(Offset(s * 0.4f, s * 0.42f), s * 0.18f, Color(0xFF8AA0B8), t * 120f); gear(Offset(s * 0.66f, s * 0.66f), s * 0.11f, Color(0xFF2EC8E8), -t * 200f) }
internal fun DrawScope.gearsGround(t: Float) { val s = sz(); val h = size.height; gear(Offset(s * 0.35f, h - s * 0.24f), s * 0.2f, Color(0xFF7A8CA8), t * 90f); gear(Offset(s * 0.68f, h - s * 0.12f), s * 0.11f, Color(0xFFE0A030), -t * 160f) }

internal fun DrawScope.soccerBall(t: Float) {
    val s = sz(); val h = size.height
    val c = Offset(s * 0.4f, h - s * 0.18f - abs(wave(t, 1f)) * s * 0.05f); val r = s * 0.17f
    drawCircle(Color.Black.copy(alpha = 0.14f), r, c + Offset(3f, 5f))
    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFD8DCE0)), c - Offset(r * 0.3f, r * 0.3f), r * 1.4f), r, c)
    rotate(t * 120f, c) {
        drawPath(starPath(c.x, c.y, r * 0.35f, r * 0.3f, 0f), Color(0xFF2A2A2A))
        for (k in 0..4) { val a = k * TAU / 5 - PI.toFloat() / 2; drawCircle(Color(0xFF2A2A2A), r * 0.17f, c + Offset(cos(a), sin(a)) * (r * 0.82f)) }
    }
}

internal fun DrawScope.trophy(t: Float) {
    val s = sz(); val h = size.height
    val x = s * 0.4f
    drawCircle(Color(0x55FFD23F), s * (0.3f + 0.03f * wave(t, 2f)), Offset(x, h - s * 0.5f))
    drawRect(Color(0xFF6B3E1E), Offset(x - s * 0.16f, h - s * 0.12f), Size(s * 0.32f, s * 0.12f))
    drawRect(Color(0xFFD8A030), Offset(x - s * 0.04f, h - s * 0.28f), Size(s * 0.08f, s * 0.16f))
    for (k in listOf(-1f, 1f)) drawArc(Color(0xFFE8B030), if (k < 0) 90f else -90f, 180f, false, Offset(x + k * s * 0.17f - s * 0.08f, h - s * 0.62f), Size(s * 0.16f, s * 0.18f), style = Stroke(s * 0.03f))
    val cup = Path().apply { moveTo(x - s * 0.18f, h - s * 0.66f); lineTo(x + s * 0.18f, h - s * 0.66f); cubicTo(x + s * 0.18f, h - s * 0.38f, x + s * 0.06f, h - s * 0.3f, x, h - s * 0.3f); cubicTo(x - s * 0.06f, h - s * 0.3f, x - s * 0.18f, h - s * 0.38f, x - s * 0.18f, h - s * 0.66f); close() }
    drop2(cup); drawPath(cup, Brush.horizontalGradient(listOf(Color(0xFFFFE680), Color(0xFFE0A020), Color(0xFFB87A10)), x - s * 0.18f, x + s * 0.18f))
    drawPath(starPath(x, h - s * 0.52f, s * 0.06f, s * 0.025f, 0f), Color(0xFFFFF4C8))
}

internal fun DrawScope.firework(t: Float, cols: List<Color>) {
    val s = sz()
    for ((i, c0) in listOf(Offset(0.42f, 0.42f), Offset(0.74f, 0.74f)).withIndex()) {
        val p = (t * 2f + i * 0.5f) % 1f
        val c = Offset(c0.x * s, c0.y * s); val r = s * (0.32f - i * 0.12f) * p
        val col = cols[i % cols.size]
        for (k in 0 until 12) { val a = k * TAU / 12; drawLine(col.copy(alpha = 1 - p), c + Offset(cos(a), sin(a)) * (r * 0.5f), c + Offset(cos(a), sin(a)) * r, s * 0.018f, StrokeCap.Round); drawCircle(Color.White.copy(alpha = 1 - p), s * 0.012f, c + Offset(cos(a), sin(a)) * r) }
    }
}

internal fun DrawScope.paperBoat(t: Float) {
    val s = sz(); val h = size.height
    rotate(wave(t) * 4f, Offset(s * 0.4f, h - s * 0.1f)) {
        val hull = Path().apply { moveTo(s * 0.08f, h - s * 0.26f); lineTo(s * 0.72f, h - s * 0.26f); lineTo(s * 0.58f, h - s * 0.08f); lineTo(s * 0.22f, h - s * 0.08f); close() }
        drop2(hull); drawPath(hull, Color(0xFFFFD6D6))
        drawPath(Path().apply { moveTo(s * 0.24f, h - s * 0.26f); lineTo(s * 0.4f, h - s * 0.58f); lineTo(s * 0.56f, h - s * 0.26f); close() }, Color(0xFFFFE8C2))
        drawLine(Color(0xFFE0B0B0), Offset(s * 0.4f, h - s * 0.58f), Offset(s * 0.4f, h - s * 0.26f), s * 0.008f)
    }
}

internal fun DrawScope.paletteFloat(t: Float) { val s = sz(); for ((i, col) in listOf(Color(0xFFFF5C5C), Color(0xFF5CB8FF), Color(0xFFFFD23F)).withIndex()) drawCircle(col, s * (0.12f - i * 0.025f), Offset(s * (0.3f + i * 0.2f), s * (0.4f + (i % 2) * 0.2f) + wave(t + i * 0.3f) * 5f)) }
