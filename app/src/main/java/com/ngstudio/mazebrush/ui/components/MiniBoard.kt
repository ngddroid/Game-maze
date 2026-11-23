package com.ngstudio.mazebrush.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.drawscope.clipPath
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.mazebrush.render.Material
import com.ngstudio.mazebrush.render.WorldTheme
import kotlin.math.sin

/**
 * Lightweight 2D drawing of a level for menus (Home card, Daily card, onboarding demos), in the same
 * carved style as the OpenGL playfield: a surface tile with the maze cut into it.
 */
@Composable
fun MiniBoard(
    level: Level,
    theme: WorldTheme,
    modifier: Modifier = Modifier,
    painted: (Int) -> Float = { if (it == level.start) 1f else 0f },
    paint: Color = theme.paintFor(1).color(),
    ball: Offset? = Offset(level.x(level.start) + 0.5f, level.y(level.start) + 0.5f),
    ballColor: Color = Color.White,
    ballStripe: Color = Color(0xFFFF4F9A),
    /** On a cream card in world 1, show a rich polished-wood tile with warm brown channels instead of the pale surface. */
    cardWood: Boolean = false,
) {
    val rich = cardWood && theme.index == 0
    val surface = androidx.compose.ui.graphics.ImageBitmap.imageResource(if (rich) com.ngstudio.mazebrush.R.drawable.board_wood else surfaceRes(theme))
    Canvas(modifier) {
        val margin = if (rich) 0.7f else 0.45f
        val cs = minOf(size.width / (level.width + margin * 2), size.height / (level.height + margin * 2))
        val gw = level.width * cs; val gh = level.height * cs
        val ox = (size.width - gw) / 2f
        val oy = (size.height - gh) / 2f
        val tl = Offset(ox - margin * cs, oy - margin * cs)
        val tileSize = Size(gw + margin * 2 * cs, gh + margin * 2 * cs)
        val r = CornerRadius(cs * (if (rich) 0.6f else 0.4f))
        val surfB = if (rich) Color(0xFFB5652A) else theme.matB.color()
        // surface tile: a piece of the world's real surface image
        drawRoundRect(Color.Black.copy(alpha = 0.28f), tl + Offset(0f, cs * 0.14f), tileSize, r)
        val tilePath = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(tl.x, tl.y, tl.x + tileSize.width, tl.y + tileSize.height, r))
        }
        clipPath(tilePath) {
            val aspect = tileSize.width / tileSize.height
            var srcW = surface.width * 0.62f
            var srcH = srcW / aspect
            if (srcH > surface.height * 0.9f) { srcH = surface.height * 0.9f; srcW = srcH * aspect }
            val sx = ((surface.width - srcW) / 2f).toInt()
            val sy = (surface.height * 0.32f).toInt().coerceAtMost((surface.height - srcH).toInt())
            drawImage(
                surface, androidx.compose.ui.unit.IntOffset(sx, sy), androidx.compose.ui.unit.IntSize(srcW.toInt(), srcH.toInt()),
                androidx.compose.ui.unit.IntOffset(tl.x.toInt(), tl.y.toInt()), androidx.compose.ui.unit.IntSize(tileSize.width.toInt(), tileSize.height.toInt()),
            )
        }
        drawRoundRect(Color.White.copy(alpha = 0.22f), tl, tileSize, r, style = androidx.compose.ui.graphics.drawscope.Stroke(cs * 0.04f))
        if (rich) drawRoundRect(Color(0xFF6A3210).copy(alpha = 0.55f), tl, tileSize, r, style = androidx.compose.ui.graphics.drawscope.Stroke(cs * 0.07f))
        val floorA = if (rich) Color(0xFF5A2A10) else theme.floorA.color()
        val floorB = if (rich) Color(0xFF3E1C09) else theme.floorB.color()
        fun isFloor(i: Int) = i >= 0 && level.isFloor(i)
        for (i in 0 until level.size) {
            if (!level.isFloor(i)) continue
            val x = ox + level.x(i) * cs; val y = oy + level.y(i) * cs
            drawRect(floorB, Offset(x - 0.5f, y - 0.5f), Size(cs + 1f, cs + 1f))
        }
        for (i in 0 until level.size) {
            if (!level.isFloor(i)) continue
            val x = ox + level.x(i) * cs; val y = oy + level.y(i) * cs
            val p = painted(i)
            val base = if (p > 0f) lerp(floorA, paint, p) else floorA
            drawRect(base, Offset(x, y), Size(cs, cs))
            // inner shadow from the walls above and to the left
            if (!isFloor(level.step(i, Dir.UP))) drawRect(Color.Black.copy(alpha = 0.30f), Offset(x, y), Size(cs, cs * 0.28f))
            if (!isFloor(level.step(i, Dir.LEFT))) drawRect(Color.Black.copy(alpha = 0.20f), Offset(x, y), Size(cs * 0.22f, cs))
            // visible wall face at the top edge
            if (!isFloor(level.step(i, Dir.UP))) drawRect(lerp(surfB, Color.Black, 0.12f), Offset(x, y), Size(cs, cs * 0.14f))
            // tile grid
            if (!rich) drawRect(Color.Black.copy(alpha = 0.10f), Offset(x, y), Size(cs, cs), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            if (p > 0f) drawRect(Color.White.copy(alpha = 0.18f * p), Offset(x + cs * 0.12f, y + cs * 0.22f), Size(cs * 0.45f, cs * 0.1f))
        }
        if (ball != null) {
            val c = Offset(ox + ball.x * cs, oy + ball.y * cs)
            val br = cs * 0.38f
            drawCircle(Color.Black.copy(alpha = 0.28f), br * 1.05f, c + Offset(br * 0.12f, br * 0.3f))
            drawCircle(Brush.radialGradient(listOf(Color.White, ballColor, lerp(ballColor, Color.Black, 0.28f)), c - Offset(br * 0.35f, br * 0.4f), br * 1.6f), br, c)
            drawRect(ballStripe, Offset(c.x - br * 0.98f, c.y - br * 0.2f), Size(br * 1.96f, br * 0.4f))
            drawCircle(Color.White.copy(alpha = 0.9f), br * 0.22f, c - Offset(br * 0.38f, br * 0.42f))
        }
    }
}
