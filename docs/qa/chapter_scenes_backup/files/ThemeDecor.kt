package com.ngstudio.mazebrush.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ngstudio.mazebrush.render.Decor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Code-drawn corner decorations for each chapter theme (see render/Visuals.kt Backdrop). Chapter 1 keeps the
 * swaying leaves (Decor.kt); every other theme has its own props. Top painters draw with the screen corner at
 * (0,0); bottom painters stand on the ground at y = size.height. The right-hand side is mirrored, and each
 * painter gets `right` so the two sides can hold different things.
 */

internal const val TAU = (2 * PI).toFloat()

@Composable
private fun loopClock(periodMs: Int = 8000): State<Float> =
    rememberInfiniteTransition(label = "decor").animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "t")

@Composable
private fun CornerCanvas(modifier: Modifier, size: Dp, right: Boolean, paint: DrawScope.(t: Float, right: Boolean) -> Unit) {
    val t = loopClock()
    Canvas(modifier.size(size)) {
        if (right) scale(-1f, 1f) { paint(t.value, true) } else paint(t.value, false)
    }
}

/** Decorations in the two top corners (call before the content so it stays on top). */
@Composable
fun BoxScope.TopDecor() {
    val d = LocalBackdrop.current.decor
    if (d == Decor.LEAVES) { TopLeaves(); return }
    if (d == Decor.PROPS) {
        val cd = chapterDecor(LocalBackdrop.current.index) ?: return
        for ((prop, right) in listOf(cd.tl to false, cd.tr to true)) {
            if (prop == null) continue
            val m = if (prop.floating) Modifier.statusBarsPadding().padding(top = 46.dp) else Modifier
            CornerCanvas(Modifier.align(if (right) Alignment.TopEnd else Alignment.TopStart).then(m), if (prop.floating) 104.dp else 130.dp, right) { t, _ -> prop.draw(this, t) }
        }
        return
    }
    val p = topPainter(d) ?: return
    // Things that hang from the edge (icicles, palms, branches) sit in the very corner; floating things
    // (moon, sun, fish, balloon) sit just below the top bar, beside the title, so the buttons don't hide them.
    val floating = d == Decor.NIGHT || d == Decor.DESERT || d == Decor.SEA || d == Decor.CLOUDS
    val m = if (floating) Modifier.statusBarsPadding().padding(top = 46.dp) else Modifier
    val size = if (floating) 104.dp else 130.dp
    CornerCanvas(Modifier.align(Alignment.TopStart).then(m), size, false, p)
    CornerCanvas(Modifier.align(Alignment.TopEnd).then(m), size, true, p)
}

/** Decorations standing in the two bottom corners, behind the nav bar. */
@Composable
fun BoxScope.BottomDecor() {
    val d = LocalBackdrop.current.decor
    if (d == Decor.LEAVES) { BottomLeaves(); return }
    if (d == Decor.PROPS) {
        val cd = chapterDecor(LocalBackdrop.current.index) ?: return
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)) {
            cd.bl?.let { p -> CornerCanvas(Modifier.align(Alignment.BottomStart).offset(y = 4.dp), 150.dp, false) { t, _ -> p.draw(this, t) } }
            cd.br?.let { p -> CornerCanvas(Modifier.align(Alignment.BottomEnd).offset(y = 4.dp), 150.dp, true) { t, _ -> p.draw(this, t) } }
        }
        return
    }
    val p = bottomPainter(d) ?: return
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(150.dp)) {
        CornerCanvas(Modifier.align(Alignment.BottomStart).offset(y = 4.dp), 150.dp, false, p)
        CornerCanvas(Modifier.align(Alignment.BottomEnd).offset(y = 4.dp), 150.dp, true, p)
    }
}

/** Gentle full-screen motion for menus (snow, bubbles, petals, twinkles, butterflies). Not used in the game. */
@Composable
fun BoxScope.AmbientDecor() {
    val d = LocalBackdrop.current.decor
    if (d == Decor.PROPS) {
        val cd = chapterDecor(LocalBackdrop.current.index) ?: return
        if (cd.ambient == Amb.NONE) return
        val t = loopClock(when (cd.ambient) { Amb.SNOW, Amb.PETALS, Amb.LEAVES, Amb.LANTERNS -> 16000; Amb.RAIN -> 6000; else -> 10000 })
        Canvas(Modifier.fillMaxSize()) { ambient(cd.ambient, t.value, cd.tint) }
        return
    }
    val period = when (d) { Decor.SNOW, Decor.SAKURA -> 16000; Decor.SEA -> 12000; else -> 10000 }
    val t = loopClock(period)
    val draw: (DrawScope.(Float) -> Unit) = when (d) {
        Decor.SNOW -> { k -> snowfall(k) }
        Decor.SEA -> { k -> bubbles(k) }
        Decor.SAKURA -> { k -> petals(k) }
        Decor.NIGHT -> { k -> twinkles(k) }
        Decor.MEADOW -> { k -> butterflies(k) }
        else -> return
    }
    Canvas(Modifier.fillMaxSize()) { draw(t.value) }
}

private fun topPainter(d: Decor): (DrawScope.(Float, Boolean) -> Unit)? = when (d) {
    Decor.BEACH -> { t, r -> palmFronds(t, r) }
    Decor.SNOW -> { t, r -> icicles(t, r) }
    Decor.MEADOW -> { t, r -> flowerCorner(t, r) }
    Decor.NIGHT -> { t, r -> if (r) moon(t) else bigStars(t) }
    Decor.CANDY -> { t, r -> candyTop(t, r) }
    Decor.DESERT -> { t, r -> if (r) desertBirds(t) else sun(t) }
    Decor.SEA -> { t, r -> fish(t, r) }
    Decor.SAKURA -> { t, r -> blossomBranch(t, r) }
    Decor.CLOUDS -> { t, r -> if (r) balloon(t) else cloudPuff(t, top = true) }
    Decor.LEAVES, Decor.PROPS -> null
}

private fun bottomPainter(d: Decor): (DrawScope.(Float, Boolean) -> Unit)? = when (d) {
    Decor.BEACH -> { t, r -> beachBottom(t, r) }
    Decor.SNOW -> { t, r -> if (r) snowPine(t) else snowman(t) }
    Decor.MEADOW -> { t, r -> grassFlowers(t, r) }
    Decor.NIGHT -> { t, r -> if (r) smallPlanet(t) else ringedPlanet(t) }
    Decor.CANDY -> { t, r -> lollipops(t, r) }
    Decor.DESERT -> { t, r -> if (r) pricklyPear(t) else saguaro(t) }
    Decor.SEA -> { t, r -> reef(t, r) }
    Decor.SAKURA -> { t, r -> petalPile(t, r) }
    Decor.CLOUDS -> { t, _ -> cloudPuff(t, top = false) }
    Decor.LEAVES, Decor.PROPS -> null
}

// ============================================================================================ shared shapes
internal fun DrawScope.shadowed(dx: Float = 3f, dy: Float = 5f, block: DrawScope.(Boolean) -> Unit) {
    translate(dx, dy) { block(true) }
    block(false)
}

internal fun leafPath(len: Float, w: Float) = Path().apply {
    moveTo(0f, 0f)
    cubicTo(len * 0.25f, -w * 1.1f, len * 0.7f, -w * 0.95f, len, 0f)
    cubicTo(len * 0.7f, w * 0.95f, len * 0.25f, w * 1.1f, 0f, 0f)
    close()
}

internal fun starPath(cx: Float, cy: Float, ro: Float, ri: Float, rot: Float, points: Int = 5) = Path().apply {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) ro else ri
        val a = rot + i * PI.toFloat() / points - PI.toFloat() / 2
        val x = cx + r * cos(a); val y = cy + r * sin(a)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

internal fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y - r); cubicTo(c.x + r * 0.12f, c.y - r * 0.12f, c.x + r * 0.12f, c.y - r * 0.12f, c.x + r, c.y)
        cubicTo(c.x + r * 0.12f, c.y + r * 0.12f, c.x + r * 0.12f, c.y + r * 0.12f, c.x, c.y + r)
        cubicTo(c.x - r * 0.12f, c.y + r * 0.12f, c.x - r * 0.12f, c.y + r * 0.12f, c.x - r, c.y)
        cubicTo(c.x - r * 0.12f, c.y - r * 0.12f, c.x - r * 0.12f, c.y - r * 0.12f, c.x, c.y - r)
        close()
    }
    drawCircle(color.copy(alpha = 0.25f), r * 0.55f, c)
    drawPath(p, color)
}

/** Five rounded petals round a centre. */
internal fun DrawScope.flower(c: Offset, r: Float, petal: Color, centre: Color, rot: Float = 0f, petals: Int = 5) {
    for (i in 0 until petals) {
        val a = rot + i * TAU / petals
        drawCircle(petal, r * 0.52f, c + Offset(cos(a) * r * 0.55f, sin(a) * r * 0.55f))
    }
    drawCircle(Color.White.copy(alpha = 0.25f), r * 0.5f, c + Offset(-r * 0.15f, -r * 0.15f))
    drawCircle(centre, r * 0.32f, c)
}

internal fun DrawScope.cloudShape(c: Offset, w: Float, shade: Color = Color(0xFFCFE3F5)) {
    val puffs = listOf(Triple(-0.32f, 0.10f, 0.26f), Triple(-0.08f, -0.10f, 0.34f), Triple(0.22f, -0.02f, 0.28f), Triple(0.40f, 0.12f, 0.20f), Triple(0.02f, 0.14f, 0.26f))
    for ((x, y, r) in puffs) drawCircle(shade, r * w, c + Offset(x * w, y * w + w * 0.05f))
    for ((x, y, r) in puffs) drawCircle(Color.White, r * w * 0.94f, c + Offset(x * w, y * w))
    drawCircle(Color.White.copy(alpha = 0.7f), w * 0.12f, c + Offset(-w * 0.14f, -w * 0.22f))
}

// ============================================================================================ BEACH
internal fun DrawScope.palmFronds(t: Float, right: Boolean) {
    val s = size.minDimension
    val sway = sin(t * TAU + if (right) 1.7f else 0f) * 2.5f
    val angles = if (right) listOf(18f, 46f, 76f) else listOf(14f, 42f, 70f)
    rotate(sway, Offset.Zero) {
        for ((k, deg) in angles.withIndex()) {
            val len = s * (if (k == 1) 1.02f else 0.86f)
            rotate(deg, Offset.Zero) {
                // leaflets either side of a gently drooping spine
                for (i in 2..15) {
                    val f = i / 16f
                    val x = len * f
                    val droop = len * 0.12f * f * f
                    val ll = s * 0.26f * (1f - f * 0.7f)
                    for (side in listOf(-1f, 1f)) {
                        translate(x, droop) {
                            rotate(side * (62f - f * 20f), Offset.Zero) {
                                val p = leafPath(ll, ll * 0.13f)
                                translate(2f, 4f) { drawPath(p, Color.Black.copy(alpha = 0.10f)) }
                                drawPath(p, Brush.linearGradient(listOf(Color(0xFF237A2E), if (side < 0) Color(0xFF6CCB4B) else Color(0xFF4DAE3B)), Offset.Zero, Offset(ll, 0f)))
                            }
                        }
                    }
                }
                val spine = Path().apply { moveTo(0f, 0f); cubicTo(len * 0.35f, 0f, len * 0.7f, len * 0.05f, len, len * 0.12f) }
                drawPath(spine, Color(0xFF6B8E23), style = Stroke(s * 0.018f, cap = StrokeCap.Round))
            }
        }
    }
    // coconuts in the corner
    for ((x, y) in listOf(0.10f to 0.06f, 0.18f to 0.12f, 0.07f to 0.16f)) {
        drawCircle(Color(0xFF5A3214), s * 0.065f, Offset(x * s, y * s))
        drawCircle(Color(0xFF8A5528), s * 0.04f, Offset(x * s - s * 0.015f, y * s - s * 0.015f))
    }
}

internal fun DrawScope.starfish(c: Offset, r: Float, rot: Float, body: Color) {
    val p = starPath(c.x, c.y, r, r * 0.45f, rot)
    val rounded = Stroke(r * 0.28f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    translate(3f, 5f) { drawPath(p, Color.Black.copy(alpha = 0.16f)); drawPath(p, Color.Black.copy(alpha = 0.16f), style = rounded) }
    drawPath(p, body); drawPath(p, body, style = rounded)
    drawPath(p, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), c - Offset(r * 0.3f, r * 0.3f), r), style = rounded)
    for (i in 0 until 5) {
        val a = rot + i * TAU / 5 - PI.toFloat() / 2
        for (j in 1..2) drawCircle(Color(0xFFFFE0C8), r * 0.07f, c + Offset(cos(a) * r * 0.3f * j, sin(a) * r * 0.3f * j))
    }
}

internal fun DrawScope.scallop(c: Offset, r: Float, color: Color, rot: Float) {
    rotate(rot, c) {
        val fan = Path().apply {
            moveTo(c.x, c.y + r * 0.55f)
            for (i in 0..8) {
                val a = PI.toFloat() * (1.05f + i * 0.1125f)
                val a2 = a + 0.056f
                lineTo(c.x + cos(a) * r, c.y + sin(a) * r * 0.95f)
                lineTo(c.x + cos(a2) * r * 1.04f, c.y + sin(a2) * r)
            }
            close()
        }
        translate(3f, 5f) { drawPath(fan, Color.Black.copy(alpha = 0.15f)) }
        drawPath(fan, Brush.verticalGradient(listOf(color, Color(0xFFE08A6A)), c.y - r, c.y + r))
        for (i in 0..8) {
            val a = PI.toFloat() * (1.08f + i * 0.1125f)
            drawLine(Color(0xFFB8634A).copy(alpha = 0.55f), Offset(c.x, c.y + r * 0.5f), Offset(c.x + cos(a) * r * 0.92f, c.y + sin(a) * r * 0.88f), r * 0.04f, StrokeCap.Round)
        }
        drawOval(Color(0xFFE6A486), Offset(c.x - r * 0.28f, c.y + r * 0.38f), Size(r * 0.56f, r * 0.3f))
    }
}

internal fun DrawScope.beachBottom(t: Float, right: Boolean) {
    val s = size.minDimension; val h = size.height
    val bob = sin(t * TAU) * 1.5f
    if (!right) {
        starfish(Offset(s * 0.30f, h - s * 0.27f + bob), s * 0.20f, 0.2f, Color(0xFFFF7A4D))
        scallop(Offset(s * 0.66f, h - s * 0.12f), s * 0.13f, Color(0xFFFFD2B8), -12f)
    } else {
        scallop(Offset(s * 0.30f, h - s * 0.22f), s * 0.20f, Color(0xFFFFC7D2), 10f)
        starfish(Offset(s * 0.68f, h - s * 0.12f - bob), s * 0.11f, 0.7f, Color(0xFFFFB347))
    }
}

// ============================================================================================ SNOW
internal fun DrawScope.icicles(t: Float, right: Boolean) {
    val s = size.minDimension
    val lens = if (right) listOf(0.30f, 0.46f, 0.22f, 0.38f, 0.16f) else listOf(0.42f, 0.26f, 0.50f, 0.20f, 0.33f)
    val drip = (t * 2f) % 1f
    for ((i, l) in lens.withIndex()) {
        val x = s * (0.10f + i * 0.17f)
        val w = s * 0.075f
        val tip = s * (0.12f + l)
        val p = Path().apply { moveTo(x - w, s * 0.10f); lineTo(x + w, s * 0.10f); cubicTo(x + w * 0.4f, tip * 0.7f, x + w * 0.1f, tip * 0.95f, x, tip); cubicTo(x - w * 0.1f, tip * 0.95f, x - w * 0.4f, tip * 0.7f, x - w, s * 0.10f); close() }
        drawPath(p, Brush.horizontalGradient(listOf(Color(0xFFE9F6FF), Color(0xFF9CCBEF)), x - w, x + w))
        drawLine(Color.White.copy(alpha = 0.85f), Offset(x - w * 0.35f, s * 0.12f), Offset(x - w * 0.08f, tip * 0.82f), s * 0.012f, StrokeCap.Round)
        if (i == 2) drawCircle(Color(0xFFBFE3FF).copy(alpha = 1f - drip), s * 0.018f, Offset(x, tip + s * 0.25f * drip))
    }
    // snow cap along the top edge
    val cap = Path().apply {
        moveTo(0f, 0f); lineTo(s, 0f); lineTo(s, s * 0.06f)
        var x = s
        var k = 0
        while (x > 0) { val nx = x - s * 0.17f; cubicTo(x - s * 0.04f, s * (0.16f + (k % 2) * 0.03f), nx + s * 0.04f, s * (0.16f + (k % 2) * 0.03f), nx, s * 0.10f); x = nx; k++ }
        lineTo(0f, s * 0.3f); close()
    }
    translate(0f, 4f) { drawPath(cap, Color(0xFF8FB8DA).copy(alpha = 0.45f)) }
    drawPath(cap, Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF4FD)), 0f, s * 0.2f))
    drawCircle(Color.White, s * 0.2f, Offset(0f, s * 0.04f))
}

internal fun DrawScope.snowMound(h: Float, s: Float) {
    val p = Path().apply { moveTo(0f, h); lineTo(0f, h - s * 0.30f); cubicTo(s * 0.35f, h - s * 0.42f, s * 0.75f, h - s * 0.30f, s, h - s * 0.12f); lineTo(s, h); close() }
    drawPath(p, Brush.verticalGradient(listOf(Color.White, Color(0xFFD7E9F8)), h - s * 0.4f, h))
}

internal fun DrawScope.snowman(t: Float) {
    val s = size.minDimension; val h = size.height
    snowMound(h, s)
    val cx = s * 0.40f
    val wob = sin(t * TAU) * 1.5f
    rotate(wob, Offset(cx, h - s * 0.25f)) {
        val body = listOf(Triple(h - s * 0.36f, 0.20f, 0), Triple(h - s * 0.62f, 0.15f, 1), Triple(h - s * 0.83f, 0.11f, 2))
        for ((y, r, _) in body) {
            drawCircle(Color(0xFF8FB8DA).copy(alpha = 0.5f), r * s, Offset(cx + 3f, y + 4f))
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFD5E7F6)), Offset(cx - r * s * 0.35f, y - r * s * 0.4f), r * s * 1.4f), r * s, Offset(cx, y))
        }
        // arms
        drawLine(Color(0xFF6B3E1E), Offset(cx - s * 0.12f, h - s * 0.64f), Offset(cx - s * 0.32f, h - s * 0.76f), s * 0.022f, StrokeCap.Round)
        drawLine(Color(0xFF6B3E1E), Offset(cx + s * 0.12f, h - s * 0.64f), Offset(cx + s * 0.30f, h - s * 0.80f), s * 0.022f, StrokeCap.Round)
        // scarf
        drawRoundRect(Color(0xFFE53950), Offset(cx - s * 0.12f, h - s * 0.75f), Size(s * 0.24f, s * 0.05f), androidx.compose.ui.geometry.CornerRadius(s * 0.025f))
        drawRoundRect(Color(0xFFC62840), Offset(cx + s * 0.04f, h - s * 0.74f), Size(s * 0.05f, s * 0.13f), androidx.compose.ui.geometry.CornerRadius(s * 0.02f))
        // face
        val fy = h - s * 0.84f
        drawCircle(Color(0xFF2A2A35), s * 0.015f, Offset(cx - s * 0.04f, fy - s * 0.02f))
        drawCircle(Color(0xFF2A2A35), s * 0.015f, Offset(cx + s * 0.04f, fy - s * 0.02f))
        drawPath(Path().apply { moveTo(cx, fy + s * 0.005f); lineTo(cx + s * 0.11f, fy + s * 0.025f); lineTo(cx, fy + s * 0.04f); close() }, Color(0xFFFF8A2B))
        for (i in 0..2) drawCircle(Color(0xFF2A2A35), s * 0.014f, Offset(cx, h - s * (0.56f + i * 0.05f) + s * 0.02f))
        // hat
        drawRect(Color(0xFF2E3550), Offset(cx - s * 0.13f, fy - s * 0.12f), Size(s * 0.26f, s * 0.035f))
        drawRect(Color(0xFF2E3550), Offset(cx - s * 0.085f, fy - s * 0.27f), Size(s * 0.17f, s * 0.16f))
        drawRect(Color(0xFFE53950), Offset(cx - s * 0.085f, fy - s * 0.16f), Size(s * 0.17f, s * 0.03f))
    }
}

internal fun DrawScope.snowPine(t: Float) {
    val s = size.minDimension; val h = size.height
    snowMound(h, s)
    val cx = s * 0.38f
    for (i in 0..3) {
        val top = h - s * (0.95f - i * 0.15f); val bot = top + s * 0.26f; val w = s * (0.12f + i * 0.055f)
        val p = Path().apply { moveTo(cx, top); lineTo(cx + w, bot); lineTo(cx - w, bot); close() }
        drawPath(p, Brush.horizontalGradient(listOf(Color(0xFF2F7A4A), Color(0xFF1C5233)), cx - w, cx + w))
        val snowP = Path().apply { moveTo(cx - w * 0.95f, bot - s * 0.01f); cubicTo(cx - w * 0.5f, bot - s * 0.06f, cx + w * 0.5f, bot - s * 0.02f, cx + w * 0.95f, bot - s * 0.015f); lineTo(cx + w, bot + s * 0.01f); lineTo(cx - w, bot + s * 0.01f); close() }
        drawPath(snowP, Color.White)
    }
    drawRect(Color(0xFF6B3E1E), Offset(cx - s * 0.03f, h - s * 0.32f), Size(s * 0.06f, s * 0.08f))
    sparkle(Offset(cx, h - s * 0.97f), s * 0.06f * (0.85f + 0.15f * sin(t * TAU * 2)), Color(0xFFFFE066))
}

internal fun DrawScope.snowfall(t: Float) {
    val w = size.width; val h = size.height
    for (i in 0 until 34) {
        val seed = (i * 0.6180339f) % 1f
        val speed = 0.6f + (i % 5) * 0.12f
        val y = ((seed + t * speed) % 1f) * (h + 40f) - 20f
        val x = (seed * 1.7f % 1f) * w + sin(t * TAU * 2 + i) * 14f
        val r = 2.5f + (i % 4) * 1.4f
        drawCircle(Color.White.copy(alpha = 0.85f), r, Offset(x, y))
    }
}

// ============================================================================================ MEADOW
internal fun DrawScope.flowerCorner(t: Float, right: Boolean) {
    val s = size.minDimension
    val sway = sin(t * TAU + if (right) 2f else 0f) * 2f
    rotate(sway, Offset.Zero) {
        for ((deg, len) in listOf(20f to 0.62f, 55f to 0.70f, 85f to 0.55f, 38f to 0.45f)) {
            rotate(deg, Offset.Zero) {
                val p = leafPath(s * len, s * len * 0.2f)
                translate(3f, 5f) { drawPath(p, Color.Black.copy(alpha = 0.12f)) }
                drawPath(p, Brush.linearGradient(listOf(Color(0xFF3F8E2A), Color(0xFF8BD14A)), Offset.Zero, Offset(s * len, 0f)))
            }
        }
        if (right) {
            flower(Offset(s * 0.40f, s * 0.30f), s * 0.17f, Color(0xFFFF8FB8), Color(0xFFFFD54A), 0.3f)
            flower(Offset(s * 0.18f, s * 0.55f), s * 0.12f, Color(0xFFB79CFF), Color(0xFFFFE680), 0.9f)
            flower(Offset(s * 0.62f, s * 0.12f), s * 0.10f, Color(0xFFFFE45C), Color(0xFFF08C1E))
        } else {
            flower(Offset(s * 0.34f, s * 0.36f), s * 0.18f, Color.White, Color(0xFFFFC93C), 0.2f, petals = 8)
            flower(Offset(s * 0.62f, s * 0.14f), s * 0.12f, Color(0xFFFF8FB8), Color(0xFFFFE680), 0.6f)
            flower(Offset(s * 0.14f, s * 0.62f), s * 0.10f, Color(0xFFFFE45C), Color(0xFFF08C1E))
        }
    }
}

internal fun DrawScope.grassFlowers(t: Float, right: Boolean) {
    val s = size.minDimension; val h = size.height
    val sway = sin(t * TAU + if (right) 1f else 0f)
    for (i in 0 until 11) {
        val x = s * (0.02f + i * 0.08f)
        val tall = s * (0.30f + ((i * 37) % 11) / 11f * 0.32f) * (1f - i * 0.045f)
        val bend = (if (i % 2 == 0) 1 else -1) * s * 0.06f + sway * s * 0.02f
        val p = Path().apply { moveTo(x - s * 0.025f, h); cubicTo(x, h - tall * 0.5f, x + bend * 0.6f, h - tall * 0.8f, x + bend, h - tall); cubicTo(x + bend * 0.4f, h - tall * 0.75f, x + s * 0.02f, h - tall * 0.4f, x + s * 0.03f, h); close() }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFF8BD14A), Color(0xFF2F7A22)), h - tall, h))
    }
    val stems = if (right) listOf(Triple(0.26f, 0.62f, Color(0xFFFF6F91)), Triple(0.55f, 0.44f, Color.White)) else listOf(Triple(0.22f, 0.58f, Color(0xFFFFE45C)), Triple(0.52f, 0.40f, Color(0xFFB79CFF)))
    for ((x, tall, col) in stems) {
        val top = Offset(s * x + sway * s * 0.02f, h - s * tall)
        drawLine(Color(0xFF3F8E2A), Offset(s * x, h), top, s * 0.022f, StrokeCap.Round)
        flower(top, s * 0.11f, col, Color(0xFFF2A21E), if (col == Color.White) 0f else 0.4f, petals = if (col == Color.White) 8 else 5)
    }
}

internal fun DrawScope.butterflies(t: Float) {
    val w = size.width; val h = size.height
    for (i in 0..1) {
        val a = t * TAU * (if (i == 0) 1f else -1f) + i * 2f
        val c = Offset(w * (0.5f + 0.38f * sin(a) * (if (i == 0) 1f else 0.8f)), h * (0.42f + 0.18f * sin(a * 2f + i) + i * 0.2f))
        val flap = 0.35f + 0.65f * kotlin.math.abs(sin(t * TAU * 14 + i))
        val col = if (i == 0) Color(0xFFFF8FB8) else Color(0xFFFFC93C)
        val r = 13f
        for (side in listOf(-1f, 1f)) {
            val up = Size(r * 1.4f * flap, r * 1.1f); val lo = Size(r * 1.0f * flap, r * 0.8f)
            val uc = c + Offset(side * r * 0.72f * flap, -r * 0.42f); val lc = c + Offset(side * r * 0.52f * flap, r * 0.42f)
            drawOval(col, Offset(uc.x - up.width / 2, uc.y - up.height / 2), up)
            drawOval(col.copy(alpha = 0.85f), Offset(lc.x - lo.width / 2, lc.y - lo.height / 2), lo)
            drawCircle(Color.White.copy(alpha = 0.7f), r * 0.16f, uc)
        }
        drawLine(Color(0xFF4A2C1A), c + Offset(0f, -r * 0.8f), c + Offset(0f, r * 0.8f), 3.5f, StrokeCap.Round)
    }
}

// ============================================================================================ NIGHT
internal fun DrawScope.moon(t: Float) {
    val s = size.minDimension
    val c = Offset(s * 0.42f, s * 0.42f)
    val r = s * 0.24f
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFE9A8), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
    val moon = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c + Offset(r * 0.45f, -r * 0.25f), r * 0.9f)) }
    val crescent = Path.combine(PathOperation.Difference, moon, bite)
    drawPath(crescent, Brush.linearGradient(listOf(Color(0xFFFFF4C8), Color(0xFFFFD36B)), c - Offset(r, r), c + Offset(r, r)))
    drawCircle(Color(0xFFE8B84A).copy(alpha = 0.5f), r * 0.10f, c + Offset(-r * 0.55f, r * 0.1f))
    drawCircle(Color(0xFFE8B84A).copy(alpha = 0.45f), r * 0.07f, c + Offset(-r * 0.35f, r * 0.5f))
    sparkle(Offset(s * 0.82f, s * 0.70f), s * 0.05f * (0.7f + 0.3f * sin(t * TAU * 3)), Color(0xFFFFF4C8))
}

internal fun DrawScope.bigStars(t: Float) {
    val s = size.minDimension
    for ((i, p) in listOf(Triple(0.30f, 0.30f, 0.11f), Triple(0.66f, 0.18f, 0.07f), Triple(0.18f, 0.66f, 0.06f), Triple(0.58f, 0.56f, 0.045f)).withIndex()) {
        val tw = 0.7f + 0.3f * sin(t * TAU * 2 + i * 1.9f)
        sparkle(Offset(p.first * s, p.second * s), p.third * s * tw, if (i % 2 == 0) Color(0xFFFFF0B8) else Color(0xFFDDE8FF))
    }
}

internal fun DrawScope.ringedPlanet(t: Float) {
    val s = size.minDimension; val h = size.height
    val c = Offset(s * 0.36f, h - s * 0.42f + sin(t * TAU) * 3f)
    val r = s * 0.22f
    rotate(-18f, c) {
        drawOval(Color(0xFFFFD9A8).copy(alpha = 0.9f), Offset(c.x - r * 1.7f, c.y - r * 0.32f), Size(r * 3.4f, r * 0.64f), style = Stroke(r * 0.13f))
    }
    drawCircle(Brush.linearGradient(listOf(Color(0xFFFF9E7A), Color(0xFFC0508A)), c - Offset(r, r), c + Offset(r, r)), r, c)
    for (k in -1..1) drawOval(Color.White.copy(alpha = 0.14f), Offset(c.x - r * 0.95f, c.y + k * r * 0.4f - r * 0.07f), Size(r * 1.9f, r * 0.14f))
    drawCircle(Color.White.copy(alpha = 0.25f), r * 0.35f, c - Offset(r * 0.4f, r * 0.4f))
    rotate(-18f, c) {
        // front half of the ring
        val front = Path().apply { addArc(androidx.compose.ui.geometry.Rect(c.x - r * 1.7f, c.y - r * 0.32f, c.x + r * 1.7f, c.y + r * 0.32f), 0f, 180f) }
        drawPath(front, Color(0xFFFFE2B8), style = Stroke(r * 0.13f))
    }
}

internal fun DrawScope.smallPlanet(t: Float) {
    val s = size.minDimension; val h = size.height
    val c = Offset(s * 0.42f, h - s * 0.38f)
    val r = s * 0.16f
    drawCircle(Brush.linearGradient(listOf(Color(0xFF7FD3FF), Color(0xFF3A5BD8)), c - Offset(r, r), c + Offset(r, r)), r, c)
    drawCircle(Color(0xFF4CC38A).copy(alpha = 0.6f), r * 0.35f, c + Offset(-r * 0.3f, r * 0.1f))
    drawCircle(Color(0xFF4CC38A).copy(alpha = 0.6f), r * 0.22f, c + Offset(r * 0.4f, -r * 0.35f))
    drawCircle(Color.White.copy(alpha = 0.22f), r * 0.3f, c - Offset(r * 0.35f, r * 0.4f))
    val a = t * TAU
    drawCircle(Color(0xFFE6E6F0), r * 0.18f, c + Offset(cos(a) * r * 1.6f, sin(a) * r * 0.5f))
    sparkle(Offset(s * 0.78f, h - s * 0.70f), s * 0.04f * (0.7f + 0.3f * sin(a * 3)), Color(0xFFFFF0B8))
}

internal fun DrawScope.twinkles(t: Float) {
    val w = size.width; val h = size.height
    for (i in 0 until 18) {
        val x = ((i * 0.6180339f) % 1f) * w; val y = ((i * 0.3819660f + 0.13f) % 1f) * h
        val a = 0.5f + 0.5f * sin(t * TAU * 3 + i * 2.3f)
        drawCircle(Color.White.copy(alpha = 0.15f + 0.6f * a), 1.5f + 2f * a, Offset(x, y))
    }
    // a shooting star once per loop
    val k = (t * 1.0f) % 1f
    if (k < 0.12f) {
        val p = k / 0.12f
        val start = Offset(w * 0.85f, h * 0.12f); val dir = Offset(-w * 0.55f, h * 0.22f)
        val head = start + dir * p
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * (1 - p))), head - dir * 0.25f, head), head - dir * 0.25f, head, 3f, StrokeCap.Round)
    }
}

// ============================================================================================ CANDY
internal fun DrawScope.candyTop(t: Float, right: Boolean) {
    val s = size.minDimension
    val sway = sin(t * TAU + if (right) 1.4f else 0f) * 3f
    if (!right) {
        // candy cane hooked over the top
        rotate(sway, Offset(s * 0.45f, 0f)) {
            val cane = Path().apply { moveTo(s * 0.45f, s * 0.80f); lineTo(s * 0.45f, s * 0.22f); cubicTo(s * 0.45f, s * 0.02f, s * 0.18f, s * 0.02f, s * 0.18f, s * 0.20f) }
            drawPath(cane, Color.Black.copy(alpha = 0.15f), style = Stroke(s * 0.11f, cap = StrokeCap.Round))
            drawPath(cane, Color.White, style = Stroke(s * 0.10f, cap = StrokeCap.Round))
            drawPath(cane, Color(0xFFE8384F), style = Stroke(s * 0.10f, cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.07f, s * 0.07f))))
            drawPath(cane, Color.White.copy(alpha = 0.4f), style = Stroke(s * 0.025f, cap = StrokeCap.Round))
        }
        wrappedCandy(Offset(s * 0.18f, s * 0.58f), s * 0.11f, Color(0xFF4FC3F7), 25f)
    } else {
        wrappedCandy(Offset(s * 0.40f, s * 0.26f), s * 0.13f, Color(0xFFFFB74D), -20f + sway)
        wrappedCandy(Offset(s * 0.18f, s * 0.62f), s * 0.09f, Color(0xFFBA68C8), 40f)
        heart(Offset(s * 0.70f, s * 0.55f), s * 0.07f, Color(0xFFFF5C8A))
    }
}

internal fun DrawScope.wrappedCandy(c: Offset, r: Float, col: Color, rot: Float) {
    rotate(rot, c) {
        for (side in listOf(-1f, 1f)) {
            val p = Path().apply { moveTo(c.x + side * r * 0.8f, c.y); lineTo(c.x + side * r * 1.7f, c.y - r * 0.6f); lineTo(c.x + side * r * 1.55f, c.y); lineTo(c.x + side * r * 1.7f, c.y + r * 0.6f); close() }
            drawPath(p, col.copy(alpha = 0.85f))
        }
        drawCircle(Color.Black.copy(alpha = 0.12f), r, c + Offset(2f, 4f))
        drawCircle(col, r, c)
        drawArc(Color.White.copy(alpha = 0.55f), 200f, 60f, false, c - Offset(r * 0.7f, r * 0.7f), Size(r * 1.4f, r * 1.4f), style = Stroke(r * 0.18f, cap = StrokeCap.Round))
        for (i in 0..2) drawLine(Color.White.copy(alpha = 0.5f), c + Offset(-r * 0.6f + i * r * 0.5f, r * 0.75f), c + Offset(-r * 0.2f + i * r * 0.5f, -r * 0.75f), r * 0.12f, StrokeCap.Round)
    }
}

internal fun DrawScope.heart(c: Offset, r: Float, col: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r)
        cubicTo(c.x - r * 1.6f, c.y - r * 0.2f, c.x - r * 0.7f, c.y - r * 1.4f, c.x, c.y - r * 0.45f)
        cubicTo(c.x + r * 0.7f, c.y - r * 1.4f, c.x + r * 1.6f, c.y - r * 0.2f, c.x, c.y + r)
        close()
    }
    drawPath(p, col)
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.2f, c + Offset(-r * 0.45f, -r * 0.4f))
}

internal fun DrawScope.lollipop(base: Offset, tall: Float, r: Float, a: Color, b: Color, spin: Float) {
    val top = base - Offset(0f, tall)
    drawLine(Color(0xFFF3E6D8), base, top, r * 0.16f, StrokeCap.Round)
    drawCircle(Color.Black.copy(alpha = 0.15f), r, top + Offset(3f, 5f))
    drawCircle(a, r, top)
    rotate(spin, top) {
        val sp = Path()
        var first = true
        for (i in 0..60) {
            val k = i / 60f
            val ang = k * TAU * 2.6f
            val rr = r * 0.95f * k
            val pt = top + Offset(cos(ang) * rr, sin(ang) * rr)
            if (first) { sp.moveTo(pt.x, pt.y); first = false } else sp.lineTo(pt.x, pt.y)
        }
        drawPath(sp, b, style = Stroke(r * 0.22f, cap = StrokeCap.Round))
    }
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.3f, top - Offset(r * 0.4f, r * 0.4f))
}

internal fun DrawScope.gumdrop(c: Offset, r: Float, col: Color) {
    val p = Path().apply { moveTo(c.x - r, c.y); cubicTo(c.x - r, c.y - r * 1.5f, c.x + r, c.y - r * 1.5f, c.x + r, c.y); close() }
    drawPath(p, Color.Black.copy(alpha = 0.15f))
    drawPath(p, Brush.verticalGradient(listOf(col.copy(alpha = 0.8f), col), c.y - r * 1.2f, c.y))
    for (i in 0..5) drawCircle(Color.White.copy(alpha = 0.7f), r * 0.07f, c + Offset((i * 0.31f % 1f - 0.5f) * r * 1.3f, -r * (0.2f + (i * 0.47f % 1f) * 0.7f)))
}

internal fun DrawScope.lollipops(t: Float, right: Boolean) {
    val s = size.minDimension; val h = size.height
    val spin = t * 360f
    if (!right) {
        lollipop(Offset(s * 0.30f, h), s * 0.62f, s * 0.19f, Color(0xFFFF5C8A), Color.White, spin)
        lollipop(Offset(s * 0.62f, h), s * 0.36f, s * 0.12f, Color(0xFF4FC3F7), Color(0xFFFFF59D), -spin)
        gumdrop(Offset(s * 0.12f, h - s * 0.02f), s * 0.10f, Color(0xFF81C784))
    } else {
        lollipop(Offset(s * 0.34f, h), s * 0.52f, s * 0.17f, Color(0xFFFFD54F), Color(0xFFFF7043), -spin)
        gumdrop(Offset(s * 0.66f, h - s * 0.02f), s * 0.12f, Color(0xFFBA68C8))
        gumdrop(Offset(s * 0.12f, h - s * 0.02f), s * 0.08f, Color(0xFFFF8A65))
    }
}

// ============================================================================================ DESERT
internal fun DrawScope.sun(t: Float) {
    val s = size.minDimension
    val c = Offset(s * 0.26f, s * 0.26f)
    val r = s * 0.17f
    drawCircle(Brush.radialGradient(listOf(Color(0x88FFD54F), Color.Transparent), c, r * 3f), r * 3f, c)
    rotate(t * 45f, c) {
        for (i in 0 until 12) {
            val a = i * TAU / 12
            val l = if (i % 2 == 0) 1.75f else 1.45f
            drawLine(Color(0xFFFFB300), c + Offset(cos(a), sin(a)) * (r * 1.2f), c + Offset(cos(a), sin(a)) * (r * l), r * 0.16f, StrokeCap.Round)
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107)), c - Offset(r * 0.3f, r * 0.3f), r * 1.3f), r, c)
}

internal fun DrawScope.desertBirds(t: Float) {
    val s = size.minDimension
    for (i in 0..2) {
        val c = Offset(s * (0.25f + i * 0.2f), s * (0.30f + (i % 2) * 0.14f) + sin(t * TAU + i) * 4f)
        val f = 0.5f + 0.5f * sin(t * TAU * 6 + i)
        val w = s * (0.07f - i * 0.01f)
        val p = Path().apply { moveTo(c.x - w, c.y - w * 0.3f * f); cubicTo(c.x - w * 0.5f, c.y - w * 0.6f * f, c.x - w * 0.2f, c.y - w * 0.3f, c.x, c.y); cubicTo(c.x + w * 0.2f, c.y - w * 0.3f, c.x + w * 0.5f, c.y - w * 0.6f * f, c.x + w, c.y - w * 0.3f * f) }
        drawPath(p, Color(0xFF6A3A1E), style = Stroke(s * 0.014f, cap = StrokeCap.Round))
    }
}

internal fun DrawScope.cactusLimb(x: Float, top: Float, bot: Float, w: Float) {
    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF6CC24A), Color(0xFF2F8A3A)), x - w / 2, x + w / 2), Offset(x - w / 2, top), Size(w, bot - top), androidx.compose.ui.geometry.CornerRadius(w / 2))
    for (k in listOf(-0.22f, 0.12f)) drawLine(Color(0xFF256E2E).copy(alpha = 0.5f), Offset(x + k * w, top + w * 0.4f), Offset(x + k * w, bot - w * 0.2f), w * 0.07f, StrokeCap.Round)
}

internal fun DrawScope.rock(c: Offset, w: Float) {
    drawOval(Brush.verticalGradient(listOf(Color(0xFFC08A5A), Color(0xFF8A5A34)), c.y - w * 0.35f, c.y + w * 0.3f), Offset(c.x - w / 2, c.y - w * 0.35f), Size(w, w * 0.65f))
    drawOval(Color.White.copy(alpha = 0.2f), Offset(c.x - w * 0.3f, c.y - w * 0.3f), Size(w * 0.35f, w * 0.15f))
}

internal fun DrawScope.saguaro(t: Float) {
    val s = size.minDimension; val h = size.height
    val x = s * 0.36f; val w = s * 0.15f
    val sway = sin(t * TAU) * 1f
    rotate(sway, Offset(x, h)) {
        cactusLimb(x - w * 1.35f, h - s * 0.62f, h - s * 0.30f, w * 0.75f)
        drawRoundRect(Color(0xFF3E9E40), Offset(x - w * 1.35f, h - s * 0.38f), Size(w * 1.2f, w * 0.7f), androidx.compose.ui.geometry.CornerRadius(w * 0.35f))
        cactusLimb(x + w * 1.25f, h - s * 0.75f, h - s * 0.42f, w * 0.7f)
        drawRoundRect(Color(0xFF3E9E40), Offset(x, h - s * 0.50f), Size(w * 1.25f, w * 0.65f), androidx.compose.ui.geometry.CornerRadius(w * 0.33f))
        cactusLimb(x, h - s * 0.92f, h, w)
        flower(Offset(x, h - s * 0.92f), s * 0.05f, Color(0xFFFF6F91), Color(0xFFFFE45C))
    }
    rock(Offset(s * 0.72f, h - s * 0.06f), s * 0.22f)
}

internal fun DrawScope.pricklyPear(t: Float) {
    val s = size.minDimension; val h = size.height
    val pads = listOf(Triple(0.36f, 0.20f, 0.17f), Triple(0.22f, 0.46f, 0.13f), Triple(0.50f, 0.50f, 0.14f), Triple(0.36f, 0.72f, 0.11f))
    for ((x, y, r) in pads) {
        val c = Offset(s * x, h - s * y)
        drawOval(Brush.linearGradient(listOf(Color(0xFF7ED957), Color(0xFF2F8A3A)), c - Offset(r * s, r * s), c + Offset(r * s, r * s)), Offset(c.x - r * s * 0.8f, c.y - r * s), Size(r * s * 1.6f, r * s * 2f))
        for (k in 0..4) drawCircle(Color(0xFFFFF3C4), s * 0.008f, c + Offset(((k * 0.37f) % 1f - 0.5f) * r * s, ((k * 0.61f) % 1f - 0.5f) * r * s * 1.4f))
    }
    val bloom = 0.9f + 0.1f * sin(t * TAU * 2)
    flower(Offset(s * 0.36f, h - s * 0.84f), s * 0.05f * bloom, Color(0xFFFFD54A), Color(0xFFFF8A2B))
    flower(Offset(s * 0.56f, h - s * 0.64f), s * 0.04f * bloom, Color(0xFFFF6F91), Color(0xFFFFE45C))
    rock(Offset(s * 0.74f, h - s * 0.05f), s * 0.18f)
}

// ============================================================================================ SEA
internal fun DrawScope.fishShape(c: Offset, len: Float, body: Color, fin: Color, stripes: Boolean, t: Float) {
    val wag = sin(t * TAU * 4) * len * 0.08f
    val tail = Path().apply { moveTo(c.x - len * 0.38f, c.y); lineTo(c.x - len * 0.62f, c.y - len * 0.22f + wag); lineTo(c.x - len * 0.58f, c.y + len * 0.22f + wag); close() }
    drawPath(tail, fin)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - len * 0.42f + 2f, c.y - len * 0.22f + 4f), Size(len * 0.84f, len * 0.44f))
    drawOval(body, Offset(c.x - len * 0.42f, c.y - len * 0.22f), Size(len * 0.84f, len * 0.44f))
    if (stripes) for (k in listOf(-0.12f, 0.16f)) drawLine(Color.White, Offset(c.x + k * len, c.y - len * 0.2f), Offset(c.x + k * len - len * 0.03f, c.y + len * 0.2f), len * 0.08f, StrokeCap.Round)
    drawPath(Path().apply { moveTo(c.x - len * 0.05f, c.y - len * 0.2f); lineTo(c.x + len * 0.12f, c.y - len * 0.32f); lineTo(c.x + len * 0.18f, c.y - len * 0.18f); close() }, fin)
    drawCircle(Color.White, len * 0.07f, c + Offset(len * 0.24f, -len * 0.04f))
    drawCircle(Color(0xFF1B1B2A), len * 0.04f, c + Offset(len * 0.26f, -len * 0.04f))
    drawOval(Color.White.copy(alpha = 0.3f), Offset(c.x - len * 0.15f, c.y - len * 0.18f), Size(len * 0.35f, len * 0.1f))
}

internal fun DrawScope.fish(t: Float, right: Boolean) {
    val s = size.minDimension
    val bob = sin(t * TAU + if (right) 2f else 0f) * 5f
    // painted facing right; on the right-hand side the mirror makes it face into the screen too
    if (!right) {
        fishShape(Offset(s * 0.42f, s * 0.42f + bob), s * 0.42f, Color(0xFFFF8A2B), Color(0xFFFF6A00), true, t)
    } else {
        fishShape(Offset(s * 0.42f, s * 0.36f + bob), s * 0.40f, Color(0xFF2F6FE0), Color(0xFFFFD23F), false, t)
        fishShape(Offset(s * 0.30f, s * 0.70f - bob), s * 0.22f, Color(0xFFFFD23F), Color(0xFFFFB300), false, t + 0.3f)
    }
    for (k in 0..2) {
        val p = (t * 1.5f + k / 3f) % 1f
        drawCircle(Color.White.copy(alpha = 0.6f * (1 - p)), s * (0.02f + k * 0.008f), Offset(s * (0.78f - k * 0.05f), s * (0.6f - p * 0.5f)), style = Stroke(2f))
    }
}

internal fun DrawScope.seaweed(x: Float, h: Float, tall: Float, w: Float, t: Float, phase: Float, col: Color) {
    val p = Path()
    val n = 18
    for (i in 0..n) {
        val k = i / n.toFloat()
        val px = x + sin(k * 6f + t * TAU + phase) * w * 1.2f * k
        val py = h - tall * k
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    drawPath(p, col, style = Stroke(w, cap = StrokeCap.Round))
}

internal fun DrawScope.coral(base: Offset, s: Float, col: Color) {
    fun branch(from: Offset, ang: Float, len: Float, w: Float, depth: Int) {
        val to = from + Offset(cos(ang), sin(ang)) * len
        drawLine(col, from, to, w, StrokeCap.Round)
        if (depth > 0) {
            branch(to, ang - 0.45f, len * 0.72f, w * 0.78f, depth - 1)
            branch(to, ang + 0.40f, len * 0.68f, w * 0.78f, depth - 1)
        } else drawCircle(col.copy(red = (col.red + 0.15f).coerceAtMost(1f)), w * 0.7f, to)
    }
    branch(base, -PI.toFloat() / 2, s * 0.24f, s * 0.07f, 3)
}

internal fun DrawScope.reef(t: Float, right: Boolean) {
    val s = size.minDimension; val h = size.height
    if (!right) {
        seaweed(s * 0.12f, h, s * 0.70f, s * 0.05f, t, 0f, Color(0xFF2E9E5A))
        coral(Offset(s * 0.42f, h), s, Color(0xFFFF6F7D))
        seaweed(s * 0.70f, h, s * 0.45f, s * 0.04f, t, 1.5f, Color(0xFF3CB371))
    } else {
        seaweed(s * 0.18f, h, s * 0.55f, s * 0.045f, t, 0.8f, Color(0xFF2E9E5A))
        coral(Offset(s * 0.50f, h), s * 0.8f, Color(0xFFB57BFF))
        drawOval(Brush.radialGradient(listOf(Color(0xFFFFB38A), Color(0xFFE07A50)), Offset(s * 0.78f, h - s * 0.08f), s * 0.14f), Offset(s * 0.66f, h - s * 0.16f), Size(s * 0.26f, s * 0.18f))
        for (k in 0..3) drawArc(Color(0xFFB85A38).copy(alpha = 0.5f), 200f, 140f, false, Offset(s * (0.69f + k * 0.012f), h - s * (0.15f - k * 0.025f)), Size(s * (0.20f - k * 0.025f), s * 0.12f), style = Stroke(s * 0.01f))
    }
}

internal fun DrawScope.bubbles(t: Float) {
    val w = size.width; val h = size.height
    for (i in 0 until 16) {
        val seed = (i * 0.6180339f) % 1f
        val speed = 0.7f + (i % 4) * 0.15f
        val y = h - ((seed + t * speed) % 1f) * (h + 40f)
        val x = seed * w + sin(t * TAU * 3 + i) * 10f
        val r = 4f + (i % 5) * 2.5f
        drawCircle(Color.White.copy(alpha = 0.45f), r, Offset(x, y), style = Stroke(2f))
        drawCircle(Color.White.copy(alpha = 0.6f), r * 0.25f, Offset(x - r * 0.35f, y - r * 0.35f))
    }
}

// ============================================================================================ SAKURA
internal fun DrawScope.blossomBranch(t: Float, right: Boolean) {
    val s = size.minDimension
    val sway = sin(t * TAU + if (right) 1.2f else 0f) * 2f
    rotate(sway, Offset.Zero) {
        val main = Path().apply { moveTo(-s * 0.05f, s * 0.05f); cubicTo(s * 0.30f, s * 0.12f, s * 0.55f, s * 0.30f, s * 0.95f, s * 0.38f) }
        val twig = Path().apply { moveTo(s * 0.35f, s * 0.18f); cubicTo(s * 0.40f, s * 0.35f, s * 0.36f, s * 0.55f, s * 0.42f, s * 0.75f) }
        drawPath(main, Color.Black.copy(alpha = 0.12f), style = Stroke(s * 0.065f, cap = StrokeCap.Round))
        drawPath(main, Color(0xFF6B3A2A), style = Stroke(s * 0.055f, cap = StrokeCap.Round))
        drawPath(twig, Color(0xFF6B3A2A), style = Stroke(s * 0.03f, cap = StrokeCap.Round))
        val spots = if (right) listOf(0.22f to 0.14f, 0.52f to 0.24f, 0.80f to 0.34f, 0.40f to 0.48f, 0.44f to 0.74f, 0.66f to 0.40f)
            else listOf(0.18f to 0.10f, 0.46f to 0.22f, 0.70f to 0.30f, 0.92f to 0.40f, 0.37f to 0.42f, 0.42f to 0.70f)
        for ((i, p) in spots.withIndex()) {
            val c = Offset(p.first * s, p.second * s)
            val r = s * (0.09f + (i % 3) * 0.02f)
            for (k in 0 until 5) {
                val a = i * 0.7f + k * TAU / 5
                drawCircle(if (k % 2 == 0) Color(0xFFFFB7CE) else Color(0xFFFFC9D9), r * 0.52f, c + Offset(cos(a) * r * 0.55f, sin(a) * r * 0.55f))
            }
            drawCircle(Color(0xFFFFE3EC), r * 0.32f, c)
            for (k in 0 until 5) { val a = k * TAU / 5 + 0.3f; drawCircle(Color(0xFFD6457A), r * 0.07f, c + Offset(cos(a) * r * 0.22f, sin(a) * r * 0.22f)) }
        }
    }
}

internal fun DrawScope.petal(c: Offset, r: Float, rot: Float, col: Color) {
    rotate(rot, c) { drawOval(col, Offset(c.x - r, c.y - r * 0.55f), Size(r * 2f, r * 1.1f)) }
}

internal fun DrawScope.petalPile(t: Float, right: Boolean) {
    val s = size.minDimension; val h = size.height
    // grass tuft with a few fallen petals and one blossom on top
    for (i in 0 until 8) {
        val x = s * (0.08f + i * 0.09f); val tall = s * (0.18f + ((i * 29) % 7) / 7f * 0.16f)
        drawLine(Color(0xFF6FAE4A), Offset(x, h), Offset(x + (if (i % 2 == 0) 1 else -1) * s * 0.04f + sin(t * TAU + i) * 3f, h - tall), s * 0.025f, StrokeCap.Round)
    }
    for (i in 0 until 7) petal(Offset(s * (0.10f + ((i * 0.37f) % 1f) * 0.7f), h - s * (0.04f + (i % 3) * 0.03f)), s * 0.035f, i * 40f, Color(0xFFFFB7CE))
    val c = Offset(s * (if (right) 0.55f else 0.38f), h - s * 0.30f)
    flower(c, s * 0.12f, Color(0xFFFFB7CE), Color(0xFFFFE3EC), 0.5f)
}

internal fun DrawScope.petals(t: Float) {
    val w = size.width; val h = size.height
    for (i in 0 until 16) {
        val seed = (i * 0.6180339f) % 1f
        val speed = 0.5f + (i % 4) * 0.12f
        val y = ((seed + t * speed) % 1f) * (h + 40f) - 20f
        val x = (seed * 1.9f % 1f) * w + sin(t * TAU * 2 + i * 1.3f) * 30f
        petal(Offset(x, y), 6f + (i % 3) * 2f, t * 360f * (if (i % 2 == 0) 2 else -2) + i * 30f, Color(0xFFF7A1BE).copy(alpha = 0.85f))
    }
}

// ============================================================================================ CLOUDS
internal fun DrawScope.cloudPuff(t: Float, top: Boolean) {
    val s = size.minDimension; val h = size.height
    val drift = sin(t * TAU) * 6f
    if (top) {
        cloudShape(Offset(s * 0.36f + drift, s * 0.30f), s * 0.75f)
        cloudShape(Offset(s * 0.70f - drift * 0.5f, s * 0.70f), s * 0.38f)
    } else {
        cloudShape(Offset(s * 0.40f + drift, h - s * 0.22f), s * 0.95f)
        cloudShape(Offset(s * 0.78f - drift * 0.6f, h - s * 0.48f), s * 0.42f)
    }
}

internal fun DrawScope.balloon(t: Float) {
    val s = size.minDimension
    val c = Offset(s * 0.42f, s * 0.34f + sin(t * TAU) * 5f)
    val r = s * 0.22f
    val env = Path().apply {
        moveTo(c.x, c.y + r * 1.25f)
        cubicTo(c.x - r * 1.35f, c.y + r * 0.3f, c.x - r * 1.1f, c.y - r * 1.15f, c.x, c.y - r * 1.1f)
        cubicTo(c.x + r * 1.1f, c.y - r * 1.15f, c.x + r * 1.35f, c.y + r * 0.3f, c.x, c.y + r * 1.25f)
        close()
    }
    drawPath(env, Color(0xFFFF5C5C))
    val cols = listOf(Color(0xFFFFD23F), Color(0xFF4FC3F7))
    for (k in 0..1) {
        val stripe = Path().apply {
            val ox = (k * 2 - 1) * r * 0.45f
            moveTo(c.x + ox * 0.2f, c.y + r * 1.2f)
            cubicTo(c.x + ox * 1.5f, c.y + r * 0.3f, c.x + ox * 1.4f, c.y - r * 0.9f, c.x + ox * 0.6f, c.y - r * 1.08f)
            lineTo(c.x + ox * 0.15f, c.y - r * 1.1f)
            cubicTo(c.x + ox * 0.7f, c.y - r * 0.9f, c.x + ox * 0.8f, c.y + r * 0.3f, c.x + ox * 0.05f, c.y + r * 1.22f)
            close()
        }
        drawPath(stripe, cols[k])
    }
    drawOval(Color.White.copy(alpha = 0.35f), Offset(c.x - r * 0.75f, c.y - r * 0.8f), Size(r * 0.35f, r * 0.7f))
    val bt = c.y + r * 1.25f
    drawLine(Color(0xFF6B3E1E), Offset(c.x - r * 0.18f, bt), Offset(c.x - r * 0.2f, bt + r * 0.45f), 2f)
    drawLine(Color(0xFF6B3E1E), Offset(c.x + r * 0.18f, bt), Offset(c.x + r * 0.2f, bt + r * 0.45f), 2f)
    drawRoundRect(Color(0xFF9A5B2A), Offset(c.x - r * 0.25f, bt + r * 0.42f), Size(r * 0.5f, r * 0.32f), androidx.compose.ui.geometry.CornerRadius(r * 0.06f))
}
