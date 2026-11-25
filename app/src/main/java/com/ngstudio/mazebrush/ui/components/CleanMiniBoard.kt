package com.ngstudio.mazebrush.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import com.ngstudio.maze.core.Level
import com.ngstudio.mazebrush.render.BoardTheme
import kotlin.math.min

/**
 * A flat picture of a level in the clean style (Home card): the board slab in the level's [BoardTheme], the paths as
 * rounded channels, the start cell painted and the equipped ball on it. Matches the in-game board closely enough for
 * a preview; no GL.
 */
@Composable
fun CleanMiniBoard(
    level: Level, board: BoardTheme, ballSkin: Int, modifier: Modifier = Modifier, paint: Color = board.paints[0].color(),
    /** How painted each cell is (0..1); default: only the start cell. */
    painted: ((Int) -> Float)? = null,
    /** Ball centre in cells (x + 0.5, y + 0.5); default: on the start cell. */
    ball: Offset? = null,
    /** A paint stroke through these points (cell units, centres at +0.5), drawn like the game's round brush; overrides [painted]. */
    paintPath: List<Offset>? = null,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }; val hPx = with(density) { maxHeight.toPx() }
        val rim = 0.3f; val lip = 0.16f
        val cs = min(wPx / (level.width + rim * 2), hPx / (level.height + rim * 2 + lip))
        val ox = (wPx - level.width * cs) / 2
        val oy = (hPx - (level.height + lip) * cs) / 2
        Canvas(Modifier.matchParentSize()) {
            val slabTL = Offset(ox - rim * cs, oy - rim * cs)
            val slabSize = Size((level.width + rim * 2) * cs, (level.height + rim * 2) * cs)
            val rad = CornerRadius(cs * (0.5f + rim))
            drawRoundRect(Color.Black.copy(alpha = 0.10f), slabTL + Offset(0f, cs * (lip + 0.18f)), slabSize, rad)
            drawRoundRect(board.sideBottom.color(), slabTL + Offset(0f, cs * lip), slabSize, rad)
            drawRoundRect(board.sideTop.color(), slabTL, slabSize, rad)
            fun c(i: Int) = Offset(ox + (level.x(i) + 0.5f) * cs, oy + (level.y(i) + 0.5f) * cs)
            val floor = board.floor.color()
            // straight runs as round-capped bars, so corners and dead ends come out rounded like the game
            for (y in 0 until level.height) {
                var x = 0
                while (x < level.width) {
                    if (!level.isFloor(y * level.width + x)) { x++; continue }
                    var e = x
                    while (e + 1 < level.width && level.isFloor(y * level.width + e + 1)) e++
                    if (e > x) drawLine(floor, c(y * level.width + x), c(y * level.width + e), cs, StrokeCap.Round)
                    x = e + 1
                }
            }
            for (x in 0 until level.width) {
                var y = 0
                while (y < level.height) {
                    if (!level.isFloor(y * level.width + x)) { y++; continue }
                    var e = y
                    while (e + 1 < level.height && level.isFloor((e + 1) * level.width + x)) e++
                    if (e > y) drawLine(floor, c(y * level.width + x), c(e * level.width + x), cs, StrokeCap.Round)
                    y = e + 1
                }
            }
            for (i in 0 until level.width * level.height) if (level.isFloor(i)) drawCircle(floor, cs / 2, c(i))
            if (paintPath != null) {
                fun pc(o: Offset) = Offset(ox + o.x * cs, oy + o.y * cs)
                if (paintPath.size == 1) drawCircle(paint, cs * 0.5f, pc(paintPath[0]))
                else if (paintPath.size > 1) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pc(paintPath[0]).x, pc(paintPath[0]).y)
                        for (k in 1 until paintPath.size) lineTo(pc(paintPath[k]).x, pc(paintPath[k]).y)
                    }
                    drawPath(path, paint, style = androidx.compose.ui.graphics.drawscope.Stroke(cs, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            } else if (painted == null) drawCircle(paint, cs * 0.5f, c(level.start))
            else for (i in 0 until level.width * level.height) {
                if (!level.isFloor(i)) continue
                val v = painted(i)
                if (v <= 0f) continue
                drawCircle(paint, cs * 0.5f * v.coerceIn(0f, 1f), c(i))
                if (v >= 0.99f) {
                    // join fully painted neighbours so the paint reads as one stroke
                    if (level.x(i) + 1 < level.width && level.isFloor(i + 1) && painted(i + 1) >= 0.99f) drawLine(paint, c(i), c(i + 1), cs)
                    if (level.y(i) + 1 < level.height && level.isFloor(i + level.width) && painted(i + level.width) >= 0.99f) drawLine(paint, c(i), c(i + level.width), cs)
                }
            }
        }
        val bc = ball ?: Offset(level.x(level.start) + 0.5f, level.y(level.start) + 0.5f)
        val ballDp = with(density) { (cs * 0.86f).toDp() }
        val bx = with(density) { (ox + bc.x * cs - cs * 0.43f).toDp() }
        val by = with(density) { (oy + bc.y * cs - cs * 0.43f).toDp() }
        Box(Modifier.offset(bx, by).size(ballDp)) { BallIcon(ballSkin, Modifier.size(ballDp)) }
    }
}
