package com.ngstudio.maze.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One leaf, in fractions of the cluster size: base point, direction (degrees, 0 = right, 90 = down), length, half-width, shade 0..1. */
private class Leaf(val x: Float, val y: Float, val deg: Float, val len: Float, val w: Float, val shade: Float)

// Back-to-front, radiating from the top-left corner of the canvas.
private val LEAVES = listOf(
    Leaf(0.00f, 0.10f, 12f, 0.70f, 0.13f, 0.0f),
    Leaf(0.08f, 0.00f, 70f, 0.66f, 0.13f, 0.1f),
    Leaf(0.00f, 0.00f, 40f, 0.92f, 0.16f, 0.45f),
    Leaf(0.00f, 0.30f, -4f, 0.52f, 0.11f, 0.6f),
    Leaf(0.24f, 0.00f, 92f, 0.50f, 0.11f, 0.7f),
    Leaf(0.02f, 0.06f, 24f, 0.62f, 0.13f, 0.9f),
    Leaf(0.06f, 0.02f, 58f, 0.56f, 0.12f, 1.0f),
)

private val LEAF_DARK = Color(0xFF2F6E1C)
private val LEAF_MID = Color(0xFF4E9E2C)
private val LEAF_LIGHT = Color(0xFF8BD14A)

private fun DrawScope.leaf(l: Leaf) {
    val s = size.minDimension
    val len = l.len * s; val w = l.w * s
    val path = Path().apply {
        moveTo(0f, 0f)
        cubicTo(len * 0.25f, -w * 1.1f, len * 0.70f, -w * 0.95f, len, 0f)
        cubicTo(len * 0.70f, w * 0.95f, len * 0.25f, w * 1.1f, 0f, 0f)
        close()
    }
    val base = lerp(LEAF_DARK, LEAF_MID, l.shade)
    val tip = lerp(LEAF_MID, LEAF_LIGHT, l.shade)
    translate(l.x * size.width, l.y * size.height) {
        rotate(l.deg, Offset.Zero) {
            translate(3f, 5f) { drawPath(path, Color.Black.copy(alpha = 0.16f)) }
            drawPath(path, Brush.linearGradient(listOf(base, tip), Offset(0f, w), Offset(len * 0.8f, -w)))
            // sunlit upper half
            clipPath(path) { drawRect(Color.White.copy(alpha = 0.10f), Offset(0f, -w * 1.2f), Size(len, w * 1.2f)) }
            drawLine(Color.White.copy(alpha = 0.35f), Offset(len * 0.04f, 0f), Offset(len * 0.92f, 0f), strokeWidth = s * 0.008f, cap = StrokeCap.Round)
            for (i in 1..4) {
                val vx = len * (0.15f + i * 0.17f)
                val vl = w * (0.85f - i * 0.12f)
                drawLine(Color.White.copy(alpha = 0.18f), Offset(vx, 0f), Offset(vx + vl * 0.9f, -vl), strokeWidth = s * 0.005f, cap = StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.12f), Offset(vx, 0f), Offset(vx + vl * 0.9f, vl), strokeWidth = s * 0.005f, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * A cluster of code-drawn leaves growing in from a screen corner, swaying gently.
 * [flipX] mirrors it for the right-hand side, [flipY] for the bottom.
 */
@Composable
fun LeafCluster(modifier: Modifier = Modifier, size: Dp = 130.dp, flipX: Boolean = false, flipY: Boolean = false, phase: Int = 0) {
    val sway by rememberInfiniteTransition(label = "leaves").animateFloat(
        -1.6f, 1.6f, infiniteRepeatable(tween(3200 + phase * 400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway",
    )
    Canvas(modifier.size(size)) {
        scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f) {
            rotate(sway, Offset.Zero) { for (l in LEAVES) leaf(l) }
        }
    }
}

/**
 * Leaves growing up from the bottom corners of the screen. Call it in the full-screen box *before* the
 * content, so the nav bar stays on top and its labels are never covered.
 */
@Composable
fun BoxScope.BottomLeaves() {
    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp)) {
        LeafCluster(Modifier.align(Alignment.BottomStart).offset((-14).dp, 6.dp), size = 140.dp, flipY = true, phase = 1)
        LeafCluster(Modifier.align(Alignment.BottomEnd).offset(14.dp, 6.dp), size = 140.dp, flipX = true, flipY = true, phase = 2)
    }
}

/** Leaves in the two top corners of a full-screen box (drawn behind the content). */
@Composable
fun BoxScope.TopLeaves() {
    LeafCluster(Modifier.align(Alignment.TopStart).offset((-14).dp, (-4).dp), size = 124.dp)
    LeafCluster(Modifier.align(Alignment.TopEnd).offset(14.dp, (-4).dp), size = 124.dp, flipX = true, phase = 3)
}

/** Raised cream badge with bold coloured text (the "WORLD 1 • GOLDEN TEAK" pill). */
@Composable
fun WorldBadge(text: String, color: Color, modifier: Modifier = Modifier, small: Boolean = false) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        modifier
            .shadow(6.dp, shape, ambientColor = Color(0xFF6E3818), spotColor = Color(0xFF6E3818))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFCF4), Color(0xFFF8EBD2), Color(0xFFEBD2A8))), shape)
            .border(1.5.dp, Brush.verticalGradient(listOf(Color.White, Color(0xFFD8B07A))), shape)
            .padding(horizontal = if (small) 14.dp else 24.dp, vertical = if (small) 3.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = com.ngstudio.maze.ui.theme.Nunito, fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                fontSize = if (small) 12.sp else 17.sp, color = color, letterSpacing = 0.3.sp,
                shadow = androidx.compose.ui.graphics.Shadow(Color.White.copy(alpha = 0.9f), Offset(0f, 2f), 1f),
            ),
        )
    }
}

/** Glossy green play orb set in a polished wood ring (centre of the bottom bar). */
@Composable
fun PlayOrb(icon: Icon, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(Color.Black.copy(alpha = 0.30f), r, c + Offset(0f, r * 0.08f))
            // wood ring
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFC07A3E), Color(0xFF8A4A22), Color(0xFF5A2A0E)), c.y - r, c.y + r), r * 0.98f, c)
            drawCircle(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.Transparent), c.y - r, c.y), r * 0.96f, c, style = Stroke(r * 0.04f))
            drawCircle(Color(0xFF3E1A08), r * 0.83f, c)
            // green dome
            drawCircle(Brush.radialGradient(listOf(Color(0xFFA2E874), Color(0xFF5CC93E), Color(0xFF2F8A1F)), c + Offset(-r * 0.15f, -r * 0.35f), r * 1.05f), r * 0.79f, c)
            drawCircle(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1E6A14).copy(alpha = 0.45f)), c.y + r * 0.2f, c.y + r * 0.8f), r * 0.79f, c)
            // gloss
            drawOval(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.0f)), c.y - r * 0.72f, c.y - r * 0.05f),
                Offset(c.x - r * 0.52f, c.y - r * 0.72f), Size(r * 1.04f, r * 0.62f),
            )
        }
        val iconSize = 34.dp
        val nudge = if (icon == Icon.PLAY) 3.dp else 0.dp
        GameIcon(icon, Color(0xFF1E6A14).copy(alpha = 0.55f), size = iconSize, modifier = Modifier.offset(nudge, 2.dp))
        GameIcon(icon, Color.White, size = iconSize, modifier = Modifier.offset(nudge, 0.dp))
    }
}
