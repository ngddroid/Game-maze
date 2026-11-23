package com.ngstudio.maze.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.maze.BuildConfig
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.render.WorldTheme
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.SurfaceBackground
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.PaintProgressBar
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.theme.Candy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

private val letterColors = listOf(Color(0xFFFF4F9A), Color(0xFFFFB13D), Color(0xFFFFD93D), Color(0xFF2EDB9B), Color(0xFF4FD8FF), Color(0xFF9B7BFF))

/** Animated logo splash: letters bounce in, the ball glides under the title painting a streak, levels load. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val words = listOf("SPLASHY", "GLIDE")
    val letters = remember { words.sumOf { it.length } }
    val anims = remember { List(letters) { Animatable(0f) } }
    val ball = remember { Animatable(0f) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            val load = async(Dispatchers.IO) { MazeApp.instance.levels.load() }
            anims.forEachIndexed { i, a ->
                launch { delay(90L * i + 150); a.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)) }
            }
            launch { delay(500); ball.animateTo(1f, tween(1100, easing = LinearEasing)) }
            launch { MazeApp.instance.sound.pop() }
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 1900) {
                progress = ((System.currentTimeMillis() - start) / 1900f).coerceAtMost(if (load.isCompleted) 1f else 0.9f)
                delay(16)
            }
            withContext(Dispatchers.IO) { load.await() }
            progress = 1f
            delay(180)
            onDone()
        }
    }

    SurfaceBackground(WorldTheme.ALL[0]) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            var k = 0
            for ((wi, word) in words.withIndex()) {
                Row {
                    for (ch in word) {
                        val a = anims[k].value
                        val color = letterColors[k % letterColors.size]
                        val idx = k
                        Box(Modifier.graphicsLayer {
                            translationY = (1f - a) * -420f
                            scaleX = 0.6f + 0.4f * a; scaleY = 0.6f + 0.4f * a
                            alpha = a.coerceIn(0f, 1f)
                            rotationZ = (1f - a) * (if (idx % 2 == 0) -25f else 25f)
                        }) {
                            GameText(ch.toString(), if (wi == 0) 62.sp else 76.sp, color = color, outline = Color.White, outlineWidth = 0.09f)
                        }
                        k++
                    }
                }
                if (wi == 0) VSpace(0.dp)
            }
            VSpace(10.dp)
            // ball painting a streak under the title
            Canvas(Modifier.fillMaxWidth(0.8f).height(56.dp)) {
                val t = ball.value
                val y = size.height / 2
                val x0 = size.height * 0.5f
                val x1 = size.width - size.height * 0.5f
                val bx = x0 + (x1 - x0) * t
                if (t > 0f) {
                    drawLine(Brush.horizontalGradient(listOf(Color(0xFFFF4F9A), Color(0xFFFFD93D), Color(0xFF2EDB9B), Color(0xFF4FD8FF)), x0, x1), Offset(x0, y), Offset(bx, y), strokeWidth = size.height * 0.42f, cap = StrokeCap.Round)
                    drawLine(Color.White.copy(alpha = 0.45f), Offset(x0 + 6f, y - size.height * 0.1f), Offset(bx - 6f, y - size.height * 0.1f), strokeWidth = size.height * 0.08f, cap = StrokeCap.Round)
                }
                val r = size.height * 0.36f
                drawCircle(Color.Black.copy(alpha = 0.2f), r, Offset(bx + 4f, y + 8f))
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFF0F7), Color(0xFFE6C9DA)), Offset(bx - r * 0.35f, y - r * 0.4f), r * 1.7f), r, Offset(bx, y))
                val sw = sin(t * 40f) * r * 0.75f
                drawRect(Color(0xFFFF4F9A), Offset(bx - kotlin.math.abs(sw).coerceAtLeast(r * 0.15f), y - r), Size(kotlin.math.abs(sw).coerceAtLeast(r * 0.15f) * 2f, r * 2f), alpha = 0.0f)
                drawCircle(Color(0xFFFF4F9A), r * 0.32f, Offset(bx + sw * 0.6f, y))
                drawCircle(Color.White.copy(alpha = 0.9f), r * 0.25f, Offset(bx - r * 0.38f, y - r * 0.42f))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 48.dp).fillMaxWidth(0.72f), horizontalAlignment = Alignment.CenterHorizontally) {
            PaintProgressBar(progress, Candy.mint, Modifier.fillMaxWidth())
            VSpace(8.dp)
            BodyText("Mixing the paint…", size = 14.sp, color = WorldTheme.ALL[0].ink.color())
            VSpace(4.dp)
            BodyText("v${BuildConfig.VERSION_NAME}", size = 11.sp, color = lerp(Color.White, Candy.plum, 0.3f))
        }
    }
}

@Suppress("unused")
private fun Modifier.wobble(t: Float) = this.rotate(sin(t) * 3f)
