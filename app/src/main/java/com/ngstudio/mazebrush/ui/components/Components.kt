package com.ngstudio.mazebrush.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.R
import com.ngstudio.mazebrush.render.Rgb
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.render.Backdrop
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Lilita
import com.ngstudio.mazebrush.ui.theme.Nunito
import com.ngstudio.mazebrush.ui.theme.Pro
import kotlin.math.sin

fun Rgb.color() = Color(argb)

/** Display text with a thin dark outline and soft drop shadow (used on buttons and colourful spots). */
@Composable
fun GameText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    outline: Color = Candy.plum,
    outlineWidth: Float = 0.09f,
    align: TextAlign = TextAlign.Center,
) {
    val base = TextStyle(fontFamily = Lilita, fontSize = size, textAlign = align, letterSpacing = 0.5.sp)
    Box(modifier) {
        if (outlineWidth > 0f && outline != Color.Transparent) Text(
            text, Modifier.clearAndSetSemantics {}, style = base.copy(color = outline, drawStyle = Stroke(width = size.value * outlineWidth * 2.2f, join = StrokeJoin.Round),
                shadow = Shadow(Color.Black.copy(alpha = 0.22f), Offset(0f, size.value * 0.12f), 3f)),
        )
        Text(text, style = base.copy(color = color))
    }
}

/** Title "carved" into the surface: dark ink with a light lower edge, no outline. */
@Composable
fun EngravedText(text: String, size: TextUnit, color: Color, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Center) {
    val light = color.luminanceIsDark()
    Text(
        text, modifier,
        style = TextStyle(
            fontFamily = Lilita, fontSize = size, textAlign = align, letterSpacing = 0.6.sp, color = color,
            shadow = if (light) Shadow(Color.White.copy(alpha = 0.55f), Offset(0f, size.value * 0.07f), 1.5f)
            else Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, size.value * 0.07f), 2f),
        ),
    )
}

private fun Color.luminanceIsDark() = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Body text in Nunito. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, size: TextUnit = 15.sp, color: Color = Candy.ink, weight: FontWeight = FontWeight.Bold, align: TextAlign = TextAlign.Start, shadow: Boolean = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue) > 0.7f) {
    Text(text, modifier, style = TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size, color = color, textAlign = align, lineHeight = size * 1.3f,
        shadow = if (shadow) Shadow(Candy.plumDark.copy(alpha = 0.55f), Offset(0f, 2f), 4f) else null))
}

fun tapFeedback() {
    MazeApp.instance.sound.tap()
    MazeApp.instance.haptics.light()
}

/** Glossy 3D button: top face over a darker lip; sinks when pressed and springs back. */
@Composable
fun CandyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Candy.mint,
    dark: Color = Candy.mintDark,
    height: Dp = 64.dp,
    textSize: TextUnit = 26.sp,
    icon: Icon? = null,
    enabled: Boolean = true,
    sound: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "press")
    val top = if (enabled) color else Candy.grey
    val lip = if (enabled) dark else Candy.greyDark
    val lipH = 6.dp
    val shape = RoundedCornerShape(height / 2.2f)
    Box(
        modifier
            .height(height + lipH)
            .scale(1f - 0.03f * press)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClickLabel = text) { if (sound) tapFeedback(); onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(height).offset(y = lipH).shadow(5.dp, shape, clip = false).background(lip, shape))
        Box(
            Modifier
                .fillMaxWidth().height(height).offset(y = lipH * press)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(lerp(top, Color.White, 0.30f), top, lerp(top, lip, 0.28f))))
                .drawBehind {
                    // glossy band across the top half
                    val gh = size.height * 0.40f
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.10f)), size.height * 0.07f, size.height * 0.07f + gh),
                        Offset(size.width * 0.035f, size.height * 0.07f), Size(size.width * 0.93f, gh), CornerRadius(gh / 2f),
                    )
                    // soft darker bottom edge
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, lip.copy(alpha = 0.35f)), size.height * 0.75f, size.height), Offset(0f, size.height * 0.75f), Size(size.width, size.height * 0.25f))
                }
                .border(1.5.dp, Brush.verticalGradient(listOf(lerp(top, Color.White, 0.55f), lerp(top, lip, 0.4f))), shape),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    GameIcon(icon, Color.White, size = height * 0.40f)
                    Spacer(Modifier.width(10.dp))
                }
                GameText(text, textSize, outline = lerp(lip, Color.Black, 0.3f), outlineWidth = 0.06f)
            }
        }
    }
}

/** Rounded-square icon button (like an app icon) with optional badge. */
@Composable
fun RoundButton(
    icon: Icon,
    modifier: Modifier = Modifier,
    color: Color = Pro.chrome,
    iconColor: Color = Pro.cream,
    dark: Color = Pro.chromeLip,
    size: Dp = 54.dp,
    badge: String? = null,
    badgeColor: Color = Candy.pink,
    label: String = icon.name.lowercase().replaceFirstChar { it.uppercase() },
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "rpress")
    val lip = 4.dp
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier.size(size + 6.dp, size + lip + 6.dp)
            .semantics { contentDescription = if (badge != null) "$label, $badge" else label }
            .clickable(interaction, null, role = Role.Button) { tapFeedback(); onClick() },
    ) {
        Box(Modifier.size(size).offset(3.dp, 3.dp + lip).shadow(4.dp, shape).background(dark, shape))
        Box(
            Modifier.size(size).offset(3.dp, 3.dp + lip * press).clip(shape)
                .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.16f), color, lerp(color, Color.Black, 0.12f))))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)), shape),
            contentAlignment = Alignment.Center,
        ) { GameIcon(icon, iconColor, size = size * 0.5f) }
        if (badge != null) {
            Box(
                Modifier.align(Alignment.TopEnd).size(22.dp).background(badgeColor, RoundedCornerShape(11.dp)).border(2.dp, Color.White, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(badge, style = TextStyle(fontFamily = Lilita, fontSize = 12.sp, color = Color.White)) }
        }
    }
}

/** Cream panel used by dialogs and cards. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Candy.cream,
    corner: Dp = 24.dp,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .shadow(12.dp, RoundedCornerShape(corner), ambientColor = Pro.rimDark, spotColor = Pro.chromeLip)
            .clip(RoundedCornerShape(corner))
            .background(Brush.verticalGradient(listOf(Color.White, color, lerp(color, Pro.border, 0.22f))))
            .border(4.dp, Brush.verticalGradient(listOf(Pro.rimLight, Pro.rimDark, Pro.rimLight)), RoundedCornerShape(corner))
            .padding(4.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(corner - 4.dp))
            .padding((padding - 4.dp).coerceAtLeast(0.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** Counter pill (coins, stars, hints). */
@Composable
fun CounterPill(value: String, modifier: Modifier = Modifier, leading: @Composable () -> Unit) {
    Row(
        modifier
            .height(38.dp)
            .shadow(3.dp, RoundedCornerShape(19.dp))
            .background(Brush.verticalGradient(listOf(Pro.chrome, Pro.chromeDark)), RoundedCornerShape(19.dp))
            .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Pro.chromeLip.copy(alpha = 0.6f))), RoundedCornerShape(19.dp))
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(value, style = TextStyle(fontFamily = Lilita, fontSize = 19.sp, color = Pro.cream))
    }
}

/** Surface image resource for a world (also used by MiniBoard). */
fun surfaceRes(theme: WorldTheme): Int = SURFACES[theme.index % SURFACES.size]

private val SURFACES = intArrayOf(
    R.drawable.surface_0, R.drawable.surface_1, R.drawable.surface_2, R.drawable.surface_3, R.drawable.surface_4,
    R.drawable.surface_5, R.drawable.surface_6, R.drawable.surface_7, R.drawable.surface_8, R.drawable.surface_9,
)

/** The wood behind the screen: drives the corner leaves and engraved title colour (see [SurfaceBackground]). */
val LocalBackdrop = androidx.compose.runtime.staticCompositionLocalOf { Backdrop.ALL[0] }

/** The scene for the player's chapter: the newest unlocked level (won or ad-unlocked), or the debug override. */
fun currentBackdrop(): Backdrop =
    if (com.ngstudio.mazebrush.BuildConfig.DEBUG && com.ngstudio.mazebrush.game.DebugFlags.backdrop >= 0) Backdrop.forChapter(com.ngstudio.mazebrush.game.DebugFlags.backdrop)
    else Backdrop.forLevel(com.ngstudio.mazebrush.MazeApp.instance.store.latestUnlocked)

/** The wood for a given level (the game screen), or the debug override. */
fun backdropForLevel(level: Int): Backdrop =
    if (com.ngstudio.mazebrush.BuildConfig.DEBUG && com.ngstudio.mazebrush.game.DebugFlags.backdrop >= 0) Backdrop.forChapter(com.ngstudio.mazebrush.game.DebugFlags.backdrop)
    else Backdrop.forLevel(level)

/**
 * Screen background: the chapter's wood (pre-rendered by tools/MaterialArt), cross-fading when a new
 * chapter is reached. Provides [LocalBackdrop] to the content.
 */
@Composable
fun SurfaceBackground(backdrop: Backdrop = currentBackdrop(), modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    androidx.compose.runtime.CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(modifier.fillMaxSize().background(Color(0xFFE5B67C))) {
            androidx.compose.animation.Crossfade(backdrop, animationSpec = tween(600), label = "backdrop") { b ->
                Image(painterResource(b.res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            content()
        }
    }
}

/** Gentle forever pulse (for PLAY buttons and "claim" badges). */
@Composable
fun rememberPulse(period: Int = 1100, amount: Float = 0.05f): Float {
    val p by rememberInfiniteTransition(label = "pulse").animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    return 1f + amount * p
}

/** Pop-in scale animation used by dialogs and stars. */
@Composable
fun rememberPopIn(delayMs: Int = 0): Float {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        a.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    return a.value
}

/** Dimmed full-screen scrim that hosts a centered dialog. */
@Composable
fun Scrim(alpha: Float = 0.55f, onDismiss: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Candy.plumDark.copy(alpha = alpha))
            .clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Paint progress bar with a moving liquid edge. */
@Composable
fun PaintProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 18.dp) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(220), label = "prog")
    val wave by rememberInfiniteTransition(label = "wave").animateFloat(0f, 6.283f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "w")
    Canvas(modifier.height(height)) {
        val r = size.height / 2
        drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(0f, 2f), size, CornerRadius(r))
        drawRoundRect(Pro.glass, Offset.Zero, size, CornerRadius(r))
        val inset = 3.dp.toPx()
        val fw = (size.width - inset * 2) * anim
        if (fw > 1f) {
            val top = inset
            val bottom = size.height - inset
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(inset, top)
            path.lineTo(inset + fw, top)
            val ex = inset + fw
            for (i in 0..12) {
                val yy = top + (bottom - top) * i / 12
                path.lineTo(ex + 2.5f * sin(wave + i * 0.9f) * (if (anim < 0.999f) 1f else 0f), yy)
            }
            path.lineTo(inset, bottom)
            path.close()
            clipPath(androidx.compose.ui.graphics.Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, top, size.width - inset, bottom, CornerRadius(r - inset)))
            }) {
                drawPath(path, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color, lerp(color, Color.Black, 0.12f)), top, bottom))
                drawRoundRect(Color.White.copy(alpha = 0.45f), Offset(inset + 4f, top + 2f), Size((fw - 8f).coerceAtLeast(0f), (bottom - top) * 0.28f), CornerRadius(4f))
            }
        }
    }
}

/** Small spacer helpers. */
@Composable fun VSpace(h: Dp) = Spacer(Modifier.height(h))
@Composable fun HSpace(w: Dp) = Spacer(Modifier.width(w))

/** Section header tag. */
@Composable
fun Ribbon(text: String, color: Color = Candy.mint, dark: Color = Candy.mintDark, modifier: Modifier = Modifier) {
    Box(
        modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.18f), color)), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 5.dp),
    ) { GameText(text, 20.sp, outline = dark, outlineWidth = 0.06f) }
}

/** Fills the row with [content] centered. */
@Composable
fun CenterColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, content = content)
