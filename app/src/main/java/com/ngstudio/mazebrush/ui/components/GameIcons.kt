package com.ngstudio.mazebrush.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Original icon set, drawn in code on a 24x24 grid (no icon fonts or third-party art). */
enum class Icon {
    PLAY, PAUSE, UNDO, RESTART, HINT, HOME, GEAR, STAR, LOCK, COIN, TROPHY, CALENDAR, BALL, LEVELS, SOUND, MUSIC,
    VIBRATE, CLOSE, CHECK, BACK, SHARE, RATE, DOC, SHIELD, CHEST, FLAME, VIDEO, GIFT, NEXT, PAINT, TRAIL
}

private object Paths {
    private val cache = HashMap<String, Path>()
    fun get(d: String): Path = cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
}

@Composable
fun GameIcon(icon: Icon, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) { drawIcon(icon, tint) }
    }
}

private fun DrawScope.fillPath(d: String, c: Color) = drawPath(Paths.get(d), c)
private fun DrawScope.strokePath(d: String, c: Color, w: Float = 2.6f) =
    drawPath(Paths.get(d), c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))

fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawIcon(icon: Icon, c: Color) {
    when (icon) {
        Icon.PLAY -> fillPath("M8 5.2v13.6c0 .8.9 1.3 1.6.85l10-6.8c.6-.4.6-1.3 0-1.7l-10-6.8C8.9 3.9 8 4.4 8 5.2z", c)
        Icon.NEXT -> { fillPath("M5 5.5v13c0 .8.9 1.2 1.5.75l8.3-6.5c.5-.4.5-1.1 0-1.5L6.5 4.75C5.9 4.3 5 4.7 5 5.5z", c); drawRoundRect(c, Offset(16f, 5f), Size(3.2f, 14f), CornerRadius(1.6f)) }
        Icon.PAUSE -> { drawRoundRect(c, Offset(6f, 5f), Size(4.2f, 14f), CornerRadius(1.8f)); drawRoundRect(c, Offset(13.8f, 5f), Size(4.2f, 14f), CornerRadius(1.8f)) }
        Icon.UNDO -> { strokePath("M9 6.5L4.5 11 9 15.5", c, 2.8f); strokePath("M5 11h9a5 5 0 0 1 0 10h-2.5", c, 2.8f) }
        Icon.RESTART -> { strokePath("M19.2 12.5a7.2 7.2 0 1 1-2.2-5.6", c, 2.8f); fillPath("M20.5 3.8v5.6c0 .6-.5 1-1 1h-5.6c-.9 0-1.3-1.1-.7-1.7l5.6-5.6c.6-.6 1.7-.2 1.7.7z", c) }
        Icon.HINT -> {
            fillPath("M12 2.5a6.5 6.5 0 0 0-4 11.6c.7.6 1.1 1.4 1.1 2.3v.6h5.8v-.6c0-.9.4-1.7 1.1-2.3A6.5 6.5 0 0 0 12 2.5z", c)
            drawRoundRect(c, Offset(9.2f, 18f), Size(5.6f, 1.9f), CornerRadius(0.9f))
            drawRoundRect(c, Offset(10f, 20.4f), Size(4f, 1.8f), CornerRadius(0.9f))
        }
        Icon.HOME -> fillPath("M3.2 11.4L11.3 4.3c.4-.35 1-.35 1.4 0l8.1 7.1c.6.55.2 1.6-.6 1.6H19V20a1 1 0 0 1-1 1h-3.5v-5.5h-5V21H6a1 1 0 0 1-1-1v-7H3.8c-.8 0-1.2-1.05-.6-1.6z", c)
        Icon.GEAR -> {
            val p = Path()
            val teeth = 8
            for (i in 0 until teeth * 2) {
                val a = i * PI / teeth
                val r = if (i % 2 == 0) 10.2f else 7.6f
                val a1 = a - PI / teeth / 2 * 0.75; val a2 = a + PI / teeth / 2 * 0.75
                val x1 = 12 + (r * cos(a1)).toFloat(); val y1 = 12 + (r * sin(a1)).toFloat()
                val x2 = 12 + (r * cos(a2)).toFloat(); val y2 = 12 + (r * sin(a2)).toFloat()
                if (i == 0) p.moveTo(x1, y1) else p.lineTo(x1, y1)
                p.lineTo(x2, y2)
            }
            p.close()
            drawPath(p, c, style = androidx.compose.ui.graphics.drawscope.Fill)
            drawPath(p, c, style = Stroke(1.6f, join = StrokeJoin.Round))
            drawCircle(Color.Black.copy(alpha = 0.35f), 3.4f, Offset(12f, 12f))
        }
        Icon.STAR -> drawPath(starPath(12f, 12.6f, 10.5f, 4.8f), c, style = Stroke(2.2f, join = StrokeJoin.Round)).also { drawPath(starPath(12f, 12.6f, 10.5f, 4.8f), c) }
        Icon.LOCK -> {
            strokePath("M7.5 11V8a4.5 4.5 0 0 1 9 0v3", c, 2.8f)
            drawRoundRect(c, Offset(4.5f, 10.5f), Size(15f, 11f), CornerRadius(3f))
            drawCircle(Color.Black.copy(alpha = 0.25f), 1.6f, Offset(12f, 15.4f))
            drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(11.3f, 15.5f), Size(1.4f, 3f), CornerRadius(0.7f))
        }
        Icon.COIN -> {
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB100))), 10.5f, Offset(12f, 12f))
            drawCircle(Color(0xFFE08A00), 10.5f, Offset(12f, 12f), style = Stroke(1.4f))
            drawCircle(Color(0xFFFFD24D), 7.2f, Offset(12f, 12f))
            drawPath(starPath(12f, 12.4f, 4.6f, 2.0f), Color(0xFFFFF3B0))
            drawPath(starPath(12f, 12.4f, 4.6f, 2.0f), Color(0xFFE08A00), style = Stroke(0.8f, join = StrokeJoin.Round))
        }
        Icon.TROPHY -> {
            fillPath("M7 3.5h10v5.2a5 5 0 0 1-10 0z", c)
            strokePath("M7 5.5H4.5c0 3 1.2 4.6 3.2 5.2 M17 5.5h2.5c0 3-1.2 4.6-3.2 5.2", c, 2f)
            drawRoundRect(c, Offset(10.8f, 13f), Size(2.4f, 4.5f), CornerRadius(0.6f))
            drawRoundRect(c, Offset(7f, 17.5f), Size(10f, 3.6f), CornerRadius(1.4f))
        }
        Icon.CALENDAR -> {
            drawRoundRect(c, Offset(3.5f, 5f), Size(17f, 16f), CornerRadius(3f))
            drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(5.5f, 10f), Size(13f, 9f), CornerRadius(1.5f))
            drawRoundRect(c, Offset(7.2f, 2.8f), Size(2.2f, 4.6f), CornerRadius(1.1f))
            drawRoundRect(c, Offset(14.6f, 2.8f), Size(2.2f, 4.6f), CornerRadius(1.1f))
            for (i in 0..2) for (j in 0..1) drawCircle(c, 1.05f, Offset(8.5f + i * 3.5f, 12.8f + j * 3.4f))
        }
        Icon.BALL, Icon.PAINT, Icon.TRAIL -> {
            if (icon == Icon.TRAIL) for (k in 0..2) drawCircle(c.copy(alpha = 0.35f + 0.15f * k), 1.6f + k * 0.5f, Offset(3f + k * 3.2f, 15.5f - k * 1.2f))
            if (icon == Icon.PAINT) drawRoundRect(c.copy(alpha = 0.45f), Offset(2f, 16f), Size(20f, 6f), CornerRadius(3f))
            val center = if (icon == Icon.TRAIL) Offset(15f, 11f) else Offset(12f, 11f)
            drawCircle(c, 8.2f, center)
            drawArc(Color.White.copy(alpha = 0.55f), 200f, 70f, false, Offset(center.x - 5.5f, center.y - 5.5f), Size(11f, 11f), style = Stroke(2.2f, cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.9f), 1.6f, Offset(center.x - 3.4f, center.y - 3.6f))
        }
        Icon.LEVELS -> for (i in 0..1) for (j in 0..1) drawRoundRect(c, Offset(3.5f + i * 9f, 3.5f + j * 9f), Size(8f, 8f), CornerRadius(2.4f))
        Icon.SOUND -> { fillPath("M4 9.5c0-.6.4-1 1-1h3l4.3-3.7c.6-.5 1.7-.1 1.7.8v12.8c0 .9-1.1 1.3-1.7.8L8 15.5H5c-.6 0-1-.4-1-1z", c); strokePath("M16.5 9a4.2 4.2 0 0 1 0 6 M19 6.5a7.8 7.8 0 0 1 0 11", c, 2.2f) }
        Icon.MUSIC -> fillPath("M9 6.2v9.5a3 3 0 1 0 2 2.8V9.6l7-1.6v5.7a3 3 0 1 0 2 2.8V4.2c0-.65-.6-1.1-1.2-.97l-8.6 2c-.7.15-1.2.3-1.2.97z", c)
        Icon.VIBRATE -> { drawRoundRect(c, Offset(7.5f, 3f), Size(9f, 18f), CornerRadius(2.4f)); drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(9.3f, 5.5f), Size(5.4f, 11f), CornerRadius(1f)); strokePath("M4 8.5l-1.5 1.75L4 12l-1.5 1.75L4 15.5 M20 8.5l1.5 1.75L20 12l1.5 1.75L20 15.5", c, 1.6f) }
        Icon.CLOSE -> strokePath("M6.5 6.5l11 11 M17.5 6.5l-11 11", c, 3.2f)
        Icon.CHECK -> strokePath("M5 12.5l4.5 4.5L19 7.5", c, 3.4f)
        Icon.BACK -> strokePath("M15 4.8L7.8 12l7.2 7.2", c, 3.4f)
        Icon.SHARE -> { drawCircle(c, 3f, Offset(17.5f, 6f)); drawCircle(c, 3f, Offset(6.5f, 12f)); drawCircle(c, 3f, Offset(17.5f, 18f)); strokePath("M8.8 10.8l6.4-3.6 M8.8 13.2l6.4 3.6", c, 2f) }
        Icon.RATE -> { drawPath(starPath(12f, 12.6f, 10.5f, 4.8f), c); drawPath(starPath(12f, 12.6f, 10.5f, 4.8f), c, style = Stroke(2.2f, join = StrokeJoin.Round)) }
        Icon.DOC -> { fillPath("M6 2.5h8.5l4.5 4.5v13.5c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-17c0-.6.4-1 1-1z", c); for (k in 0..2) drawRoundRect(Color.White.copy(alpha = 0.9f), Offset(8f, 10f + k * 3.4f), Size(8f, 1.7f), CornerRadius(0.85f)) }
        Icon.SHIELD -> { fillPath("M12 2.5l7.8 3v6c0 4.9-3.3 8.8-7.8 10-4.5-1.2-7.8-5.1-7.8-10v-6z", c); strokePath("M8.6 12.2l2.4 2.4 4.4-4.6", Color.White, 2.2f) }
        Icon.CHEST -> {
            fillPath("M4 10h16v9.5c0 .8-.7 1.5-1.5 1.5h-13C4.7 21 4 20.3 4 19.5z", c)
            fillPath("M5.5 4h13c.8 0 1.5.7 1.5 1.5V9H4V5.5C4 4.7 4.7 4 5.5 4z", c.copy(alpha = 0.85f))
            drawRoundRect(Color(0xFFFFD24D), Offset(10.3f, 8f), Size(3.4f, 5f), CornerRadius(1f))
        }
        Icon.FLAME -> fillPath("M12 2.5c.9 3.3 5.5 5.8 5.5 11a5.5 5.5 0 0 1-11 0c0-2.7 1.4-4 2.1-6 .8 1.3 1.5 2 2.2 2.3 0-2.6.4-4.9 1.2-7.3z", c)
        Icon.VIDEO -> { drawRoundRect(c, Offset(3.5f, 5.5f), Size(17f, 13f), CornerRadius(3.4f), style = Stroke(2.4f)); fillPath("M10 9v6c0 .5.5.8 1 .5l4.6-3c.4-.3.4-.8 0-1L11 8.5c-.5-.3-1 0-1 .5z", c) }
        Icon.GIFT -> {
            drawRoundRect(c, Offset(3.5f, 8f), Size(17f, 4.5f), CornerRadius(1.4f))
            drawRoundRect(c, Offset(5f, 12f), Size(14f, 9f), CornerRadius(1.6f))
            drawRect(Color.White.copy(alpha = 0.85f), Offset(10.8f, 8f), Size(2.4f, 13f))
            strokePath("M12 8C10.5 4.5 7 4 7 6s3 2 5 2c2 0 5 0 5-2s-3.5-1.5-5 2", c, 1.8f)
        }
    }
}

/** Gold coin for counters. */
@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = 22.dp) = GameIcon(Icon.COIN, Color.Unspecified, modifier, size)

/** Filled or empty star with a darker outline. */
@Composable
fun StarIcon(filled: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val fill = remember(filled) { if (filled) Brush.verticalGradient(listOf(Color(0xFFFFEA7A), Color(0xFFFFB300))) else Brush.verticalGradient(listOf(Color(0xFFE6DDF0), Color(0xFFC7BAD9))) }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            val p = starPath(12f, 12.8f, 10.8f, 5f)
            translate(0f, 1.2f) { drawPath(p, Color.Black.copy(alpha = 0.18f)) }
            drawPath(p, fill)
            drawPath(p, if (filled) Color(0xFFE08A00) else Color(0xFFA796BE), style = Stroke(1.6f, join = StrokeJoin.Round))
            if (filled) drawCircle(Color.White.copy(alpha = 0.7f), 1.6f, Offset(9.2f, 9.6f))
        }
    }
}
