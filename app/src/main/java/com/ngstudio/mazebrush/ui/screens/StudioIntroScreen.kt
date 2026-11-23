package com.ngstudio.mazebrush.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import com.ngstudio.mazebrush.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * NG Studio ident, shown on every launch before the game's own splash.
 *
 * The logo is cut into layers (tools/studio_intro/split.py) so it can build itself up:
 * the N and G fly in and collide, the orbit swoosh sweeps around them with a glowing comet head,
 * the two sparkle stars pop, a shine passes over the mark, then STUDIO and the tagline settle in.
 * All positions below are in the source logo's pixel space (1254 x 1254 artwork).
 */
@Composable
fun StudioIntroScreen(freezeAt: Float = -1f, onDone: () -> Unit) {
    val layers = IntroLayers(
        n = ImageBitmap.imageResource(R.drawable.ngi_n),
        g = ImageBitmap.imageResource(R.drawable.ngi_g),
        swooshLo = ImageBitmap.imageResource(R.drawable.ngi_swoosh_lo),
        swooshHi = ImageBitmap.imageResource(R.drawable.ngi_swoosh_hi),
        starBlue = ImageBitmap.imageResource(R.drawable.ngi_star_b),
        starYellow = ImageBitmap.imageResource(R.drawable.ngi_star_y),
        studio = ImageBitmap.imageResource(R.drawable.ngi_studio),
        lineLeft = ImageBitmap.imageResource(R.drawable.ngi_line_l),
        lineRight = ImageBitmap.imageResource(R.drawable.ngi_line_r),
    )
    val measurer = rememberTextMeasurer()
    val done by rememberUpdatedState(onDone)
    var time by remember { mutableFloatStateOf(freezeAt.coerceAtLeast(0f)) }
    // freezeAt >= 0 holds one moment of the animation (debug contact sheets).
    var finished by remember { mutableStateOf(freezeAt >= 0f) }
    fun finish() {
        if (!finished) { finished = true; done() }
    }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (!finished) {
            val now = withFrameNanos { it }
            time = (now - start) / 1_000_000_000f
            if (time >= TOTAL) finish()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) { detectTapGestures { if (time > 1.2f) finish() } },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawIntro(time, layers, measurer) }
    }
}

private const val TOTAL = 3.9f
private const val TAGLINE = "PLAY  •  CREATE  •  INSPIRE"

private val Navy = Color(0xFF141B3C)
private val BrandBlue = Color(0xFF1E6BFF)
private val BrandCyan = Color(0xFF2FD3FF)
private val BrandOrange = Color(0xFFFF7A00)
private val BrandYellow = Color(0xFFFFC21A)
private val BrandRed = Color(0xFFFF3D1F)

private class IntroLayers(
    val n: ImageBitmap, val g: ImageBitmap, val swooshLo: ImageBitmap, val swooshHi: ImageBitmap,
    val starBlue: ImageBitmap, val starYellow: ImageBitmap, val studio: ImageBitmap,
    val lineLeft: ImageBitmap, val lineRight: ImageBitmap,
)

/** Where each layer sits in logo space (x, y, w, h), from the layer cut. */
private object Box2 {
    val N = Rect(184f, 418f, 184f + 511f, 418f + 462f)
    val G = Rect(646f, 416f, 646f + 496f, 416f + 469f)
    val LO = Rect(87f, 693f, 87f + 675f, 693f + 217f)
    val HI = Rect(596f, 296f, 596f + 570f, 296f + 269f)
    val STAR_B = Rect(476f, 369f, 476f + 117f, 369f + 122f)
    val STAR_Y = Rect(535f, 275f, 535f + 151f, 275f + 144f)
    val STUDIO = Rect(386f, 911f, 386f + 475f, 911f + 62f)
    val LINE_L = Rect(133f, 901f, 133f + 231f, 901f + 51f)
    val LINE_R = Rect(887f, 928f, 887f + 229f, 928f + 25f)
    /** Column span of each STUDIO letter. */
    val LETTERS = listOf(384f to 442f, 466f to 522f, 551f to 608f, 643f to 704f, 738f to 759f, 794f to 862f)
    /** Visual centre of the block (mark + STUDIO + tagline). */
    val CENTER = Offset(626f, 670f)
    const val BLOCK_W = 1120f
    const val BLOCK_H = 820f
    /** Pivot of the orbit sweep and the point where the letters meet. */
    val ORBIT = Offset(640f, 600f)
    val IMPACT = Offset(662f, 640f)
}

/** Centre line of the orbit swoosh by sweep angle (deg, logo space), traced from the artwork. */
private val ORBIT_PATH: List<Triple<Float, Float, Float>> = listOf(
    Triple(-102f, 611f, 462f), Triple(-94f, 630f, 449f), Triple(-86f, 652f, 435f), Triple(-78f, 679f, 420f),
    Triple(-70f, 713f, 402f), Triple(-62f, 757f, 381f), Triple(-54f, 816f, 359f), Triple(-46f, 900f, 333f),
    Triple(-38f, 1001f, 319f), Triple(-30f, 1094f, 338f), Triple(-22f, 1131f, 401f), Triple(-14f, 1100f, 484f),
    Triple(-6f, 1042f, 550f),
    Triple(86f, 648f, 718f), Triple(94f, 630f, 735f), Triple(102f, 608f, 749f), Triple(110f, 579f, 766f),
    Triple(118f, 539f, 788f), Triple(126f, 483f, 815f), Triple(134f, 400f, 848f), Triple(142f, 285f, 877f),
    Triple(150f, 167f, 873f), Triple(158f, 119f, 812f), Triple(166f, 159f, 724f),
)
private const val SWEEP_FROM = -108f
private const val SWEEP_TO = 172f

private fun orbitPoint(angle: Float): Offset? {
    for (i in 0 until ORBIT_PATH.size - 1) {
        val a = ORBIT_PATH[i]; val b = ORBIT_PATH[i + 1]
        if (angle >= a.first && angle <= b.first) {
            if (b.first - a.first > 10f) return null // behind the G
            val t = (angle - a.first) / (b.first - a.first)
            return Offset(a.second + (b.second - a.second) * t, a.third + (b.third - a.third) * t)
        }
    }
    return null
}

private class Mote(val x: Float, val y: Float, val size: Float, val speed: Float, val phase: Float, val color: Color)

private val MOTES: List<Mote> = run {
    val rnd = java.util.Random(7L)
    List(22) { i ->
        Mote(
            x = rnd.nextFloat(), y = rnd.nextFloat(),
            size = 5f + rnd.nextFloat() * 9f,
            speed = 0.02f + rnd.nextFloat() * 0.04f,
            phase = rnd.nextFloat() * 6.28f,
            color = when (i % 3) { 0 -> BrandBlue; 1 -> BrandOrange; else -> BrandYellow },
        )
    }
}

// ---- easing -------------------------------------------------------------------------------

private fun prog(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
private fun outCubic(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
private fun inOutCubic(x: Float) = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).let { it * it * it } / 2f
private fun outBack(x: Float, s: Float = 1.6f): Float {
    val p = x - 1f
    return 1f + (s + 1f) * p * p * p + s * p * p
}

// ---- drawing ------------------------------------------------------------------------------

private fun DrawScope.drawIntro(t: Float, l: IntroLayers, measurer: TextMeasurer) {
    val w = size.width; val h = size.height
    drawBackground(t, w, h)

    val s = min(w * 0.60f / Box2.BLOCK_W, h * 0.63f / Box2.BLOCK_H)
    // A slow push-in once the logo has formed keeps the hold from feeling static.
    val settle = 1f + 0.025f * prog(t, 2.2f, TOTAL)
    withTransform({
        translate(w / 2f, h / 2f)
        scale(s * settle, s * settle, Offset.Zero)
        translate(-Box2.CENTER.x, -Box2.CENTER.y)
    }) {
        drawRays(t)
        drawMark(t, l)
        drawImpact(t)
        drawStars(t, l)
        drawStudio(t, l)
    }
    drawTagline(t, measurer, w, h, s * settle)
}

private fun DrawScope.drawBackground(t: Float, w: Float, h: Float) {
    val glow = outCubic(prog(t, 0.0f, 1.0f))
    drawRect(Color.White)
    drawCircle(
        Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.16f * glow), Color.Transparent), Offset(w * 0.22f, h * 0.45f), w * 0.55f),
        radius = w * 0.55f, center = Offset(w * 0.22f, h * 0.45f),
    )
    drawCircle(
        Brush.radialGradient(listOf(BrandOrange.copy(alpha = 0.15f * glow), Color.Transparent), Offset(w * 0.8f, h * 0.55f), w * 0.55f),
        radius = w * 0.55f, center = Offset(w * 0.8f, h * 0.55f),
    )
    // Floating sparkles in the studio colours.
    val motes = prog(t, 1.0f, 1.8f)
    if (motes > 0f) {
        for (m in MOTES) {
            val y = ((m.y - t * m.speed) % 1f + 1f) % 1f
            val tw = 0.5f + 0.5f * sin(t * 3.2f + m.phase)
            sparkle(Offset(m.x * w, y * h), m.size * (0.6f + 0.4f * tw), m.color.copy(alpha = 0.45f * tw * motes))
        }
    }
}

/** Soft light rays behind the mark, turning slowly once the letters have landed. */
private fun DrawScope.drawRays(t: Float) {
    val a = prog(t, 0.8f, 1.6f)
    if (a <= 0f) return
    val c = Box2.IMPACT
    rotate(t * 9f, c) {
        for (i in 0 until 14) {
            val ang = i * (360f / 14f) * (PI.toFloat() / 180f)
            val half = 0.085f
            val r = 900f
            val p = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + cos(ang - half) * r, c.y + sin(ang - half) * r)
                lineTo(c.x + cos(ang + half) * r, c.y + sin(ang + half) * r)
                close()
            }
            val col = if (i % 2 == 0) BrandCyan else BrandYellow
            drawPath(p, Brush.radialGradient(listOf(col.copy(alpha = 0.11f * a), Color.Transparent), c, r))
        }
    }
}

private fun DrawScope.layer(img: ImageBitmap, r: Rect, alpha: Float = 1f) {
    drawImage(
        img,
        dstOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()),
        dstSize = IntSize(r.width.roundToInt(), r.height.roundToInt()),
        alpha = alpha.coerceIn(0f, 1f),
        filterQuality = FilterQuality.Medium,
    )
}

private fun DrawScope.drawMark(t: Float, l: IntroLayers) {
    val pN = prog(t, 0.12f, 0.72f)
    val pG = prog(t, 0.26f, 0.86f)
    if (pN <= 0f) return
    // Little squash when the letters meet.
    val hit = prog(t, 0.78f, 1.12f)
    val bump = 1f + 0.06f * sin(hit * PI.toFloat()) * (1f - hit * 0.5f)

    val pShine = prog(t, 1.9f, 2.55f)
    val shining = pShine > 0f && pShine < 1f
    if (shining) drawIntoCanvas { it.saveLayer(Rect(0f, 200f, 1260f, 1000f), Paint()) }

    scale(bump, bump, Box2.IMPACT) {
        // N flies in from the left, G from the right; both overshoot into each other a little.
        val nx = (1f - outBack(pN)) * -760f
        val nRot = (1f - outCubic(pN)) * -14f
        translate(nx, 0f) {
            rotate(nRot, Box2.N.center) { layer(l.n, Box2.N, pN * 3f) }
        }
        if (pG > 0f) {
            val gx = (1f - outBack(pG)) * 760f
            val gRot = (1f - outCubic(pG)) * 14f
            translate(gx, 0f) {
                rotate(gRot, Box2.G.center) { layer(l.g, Box2.G, pG * 3f) }
            }
        }
        drawSwoosh(t, l)
    }

    if (shining) {
        // A glossy band sweeping across everything drawn so far, clipped to the artwork.
        val cx = -150f + 1500f * inOutCubic(pShine)
        val band = Brush.linearGradient(
            0f to Color.Transparent, 0.45f to Color.White.copy(alpha = 0.0f), 0.5f to Color.White.copy(alpha = 0.75f),
            0.55f to Color.White.copy(alpha = 0.0f), 1f to Color.Transparent,
            start = Offset(cx - 160f, 300f), end = Offset(cx + 160f, 100f),
        )
        drawRect(band, topLeft = Offset(0f, 200f), size = Size(1260f, 800f), blendMode = BlendMode.SrcAtop)
        drawIntoCanvas { it.restore() }
    }
}

private fun DrawScope.drawSwoosh(t: Float, l: IntroLayers) {
    val p = inOutCubic(prog(t, 0.95f, 1.75f))
    if (p <= 0f) return
    if (p >= 1f) {
        layer(l.swooshHi, Box2.HI); layer(l.swooshLo, Box2.LO)
        return
    }
    val angle = SWEEP_FROM + (SWEEP_TO - SWEEP_FROM) * p
    val c = Box2.ORBIT
    val r = 1400f
    val sector = Path().apply {
        moveTo(c.x, c.y)
        arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), SWEEP_FROM, angle - SWEEP_FROM, false)
        close()
    }
    clipPath(sector) {
        layer(l.swooshHi, Box2.HI); layer(l.swooshLo, Box2.LO)
    }
    // Comet head riding the leading edge of the orbit.
    orbitPoint(angle)?.let { head ->
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, BrandYellow.copy(alpha = 0.85f), BrandOrange.copy(alpha = 0.35f), Color.Transparent),
                head, 70f,
            ),
            radius = 70f, center = head,
        )
        sparkle(head, 46f + 10f * sin(t * 40f), Color.White)
    }
}

/** Ring and sparks where the N and G collide. */
private fun DrawScope.drawImpact(t: Float) {
    val p = prog(t, 0.78f, 1.4f)
    if (p <= 0f || p >= 1f) return
    val c = Box2.IMPACT
    val e = outCubic(p)
    drawCircle(
        BrandCyan.copy(alpha = 0.55f * (1f - p)), radius = 60f + 520f * e, center = c,
        style = Stroke(width = 34f * (1f - p) + 2f),
    )
    drawCircle(
        BrandYellow.copy(alpha = 0.5f * (1f - p)), radius = 40f + 380f * e, center = c,
        style = Stroke(width = 22f * (1f - p) + 2f),
    )
    drawCircle(Color.White.copy(alpha = 0.85f * (1f - e)), radius = 90f * (1f - p) + 10f, center = c)
    for (i in 0 until 16) {
        val a = (i * 22.5f + 8f) * (PI.toFloat() / 180f)
        val r0 = 70f + 470f * e
        val r1 = r0 + 70f * (1f - p)
        val col = if (i % 2 == 0) BrandBlue else BrandOrange
        drawLine(
            col.copy(alpha = 1f - p),
            Offset(c.x + cos(a) * r0, c.y + sin(a) * r0), Offset(c.x + cos(a) * r1, c.y + sin(a) * r1),
            strokeWidth = 12f * (1f - p) + 2f, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawStars(t: Float, l: IntroLayers) {
    fun star(img: ImageBitmap, r: Rect, from: Float, phase: Float) {
        val p = prog(t, from, from + 0.42f)
        if (p <= 0f) return
        val pop = outBack(p, 2.6f)
        val twinkle = if (p >= 1f) 1f + 0.07f * sin((t - from) * 5.5f + phase) else 1f
        val rot = (1f - outCubic(p)) * -160f
        val c = r.center
        // Flash behind the star as it appears.
        if (p < 1f) {
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * (1f - p)), Color.Transparent), c, r.width * 1.6f),
                radius = r.width * 1.6f, center = c,
            )
        }
        withTransform({
            rotate(rot, c)
            scale(pop * twinkle, pop * twinkle, c)
        }) { layer(img, r, p * 2f) }
        // Glint: thin cross flare that turns slowly.
        val glint = if (p >= 1f) 0.5f + 0.5f * sin((t - from) * 3.4f + phase) else p
        rotate(t * 40f + phase * 30f, c) {
            sparkle(c, r.width * (0.75f + 0.35f * glint), Color.White.copy(alpha = 0.65f * glint))
        }
    }
    star(l.starBlue, Box2.STAR_B, 1.55f, 0f)
    star(l.starYellow, Box2.STAR_Y, 1.72f, 1.7f)
}

private fun DrawScope.drawStudio(t: Float, l: IntroLayers) {
    val img = l.studio
    val sx = img.width / Box2.STUDIO.width
    Box2.LETTERS.forEachIndexed { i, (x0, x1) ->
        val p = prog(t, 2.0f + i * 0.075f, 2.42f + i * 0.075f)
        if (p <= 0f) return@forEachIndexed
        val e = outBack(p, 1.8f)
        val left = x0 - 3f; val right = x1 + 3f
        val srcX = ((left - Box2.STUDIO.left) * sx).roundToInt().coerceIn(0, img.width - 1)
        val srcW = ((right - left) * sx).roundToInt().coerceAtMost(img.width - srcX)
        val dy = (1f - e) * 46f
        drawImage(
            img,
            srcOffset = IntOffset(srcX, 0), srcSize = IntSize(srcW, img.height),
            dstOffset = IntOffset(left.roundToInt(), (Box2.STUDIO.top + dy).roundToInt()),
            dstSize = IntSize((right - left).roundToInt(), Box2.STUDIO.height.roundToInt()),
            alpha = (p * 2.5f).coerceAtMost(1f),
            filterQuality = FilterQuality.Medium,
        )
    }
    // The blue and orange rules grow outwards from the word.
    val pl = outCubic(prog(t, 2.15f, 2.7f))
    if (pl > 0f) {
        val ll = Box2.LINE_L
        clipRect(ll.right - ll.width * pl, ll.top, ll.right, ll.bottom) { layer(l.lineLeft, ll) }
        val lr = Box2.LINE_R
        clipRect(lr.left, lr.top, lr.left + lr.width * pl, lr.bottom) { layer(l.lineRight, lr) }
    }
}

private fun DrawScope.drawTagline(t: Float, measurer: TextMeasurer, w: Float, h: Float, s: Float) {
    val p = prog(t, 2.6f, 3.2f)
    if (p <= 0f) return
    val e = outCubic(p)
    val size = (30f * s).toSp()
    val layout = measurer.measure(
        TAGLINE,
        TextStyle(
            fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = size,
            letterSpacing = (0.42f - 0.16f * e).em,
        ),
    )
    // Centre under STUDIO.
    val cx = w / 2f + (623f - Box2.CENTER.x) * s
    val top = h / 2f + (1012f - Box2.CENTER.y) * s + (1f - e) * 14f * s
    drawText(
        layout,
        brush = Brush.horizontalGradient(listOf(BrandBlue, Navy, BrandOrange)),
        topLeft = Offset(cx - layout.size.width / 2f, top),
        alpha = e,
    )
}

/** Four-point sparkle, the studio's star motif. */
private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    if (color.alpha <= 0.01f) return
    val k = r * 0.13f
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
        quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
        quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
        quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
}
