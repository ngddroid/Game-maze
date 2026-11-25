package com.ngstudio.mazebrush.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.data.GameStore
import com.ngstudio.mazebrush.render.BallSkin
import com.ngstudio.mazebrush.render.BoardTheme
import com.ngstudio.mazebrush.ui.theme.Clean
import com.ngstudio.mazebrush.ui.theme.Lilita
import com.ngstudio.mazebrush.ui.theme.Nunito

/*
 * Clean-style HUD pieces (2026-11-25 redesign): teal rounded-square buttons with white icons, white pills,
 * a paint-drop progress badge and the New Item bar. Layout ideas from the hyper-casual genre; colours, icons
 * and shapes are our own (see ui/theme/Theme.kt Clean).
 */

/** Teal rounded-square button with a white icon and a darker lip underneath; it sinks a little when pressed. */
@Composable
fun TealButton(icon: Icon, label: String, modifier: Modifier = Modifier, size: Dp = 48.dp, color: Color = Clean.teal, enabled: Boolean = true, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sink by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "sink")
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier.size(size).alpha(if (enabled) 1f else 0.45f)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(src, null, enabled = enabled) { tapFeedback(); onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(size - 4.dp).align(Alignment.BottomCenter).background(dark(color), shape))
        Box(
            Modifier.fillMaxWidth().height(size - 4.dp).offset(y = (sink * 3).dp)
                .background(Brush.verticalGradient(listOf(light(color), color)), shape)
                .border(1.dp, Color.White.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center,
        ) { GameIcon(icon, Color.White, size = size * 0.5f) }
    }
}

private fun light(c: Color) = Color((c.red + (1 - c.red) * 0.25f), (c.green + (1 - c.green) * 0.25f), (c.blue + (1 - c.blue) * 0.25f))
private fun dark(c: Color) = Color(c.red * 0.68f, c.green * 0.68f, c.blue * 0.68f)

/** White pill with a leading icon and a bold value (coins, moves). */
@Composable
fun WhitePill(value: String, modifier: Modifier = Modifier, valueColor: Color = Clean.ink, leading: @Composable () -> Unit) {
    Row(
        modifier.height(34.dp).shadow(3.dp, RoundedCornerShape(17.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .background(Clean.pill, RoundedCornerShape(17.dp)).padding(start = 5.dp, end = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(value, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = valueColor))
    }
}

/** Painted share of the level: a paint drop in the level's colour filling up, next to "NN%". */
@Composable
fun PaintBadge(progress: Float, paint: Color, modifier: Modifier = Modifier) {
    val p by animateFloatAsState(progress, tween(250), label = "paint")
    Row(
        modifier.height(32.dp).shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .background(Clean.pill, RoundedCornerShape(16.dp)).padding(start = 6.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width; val h = size.height
            val drop = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                cubicTo(w * 0.62f, h * 0.24f, w * 0.92f, h * 0.48f, w * 0.92f, h * 0.66f)
                cubicTo(w * 0.92f, h * 0.88f, w * 0.72f, h * 0.98f, w * 0.5f, h * 0.98f)
                cubicTo(w * 0.28f, h * 0.98f, w * 0.08f, h * 0.88f, w * 0.08f, h * 0.66f)
                cubicTo(w * 0.08f, h * 0.48f, w * 0.38f, h * 0.24f, w * 0.5f, h * 0.04f); close()
            }
            drawPath(drop, Clean.track)
            clipPath(drop) { drawRect(paint, Offset(0f, h * (1 - p)), Size(w, h * p)) }
            drawPath(drop, Clean.ink.copy(alpha = 0.18f), style = Stroke(w * 0.07f))
            drawCircle(Color.White.copy(alpha = 0.55f), w * 0.08f, Offset(w * 0.36f, h * 0.6f))
        }
        Spacer(Modifier.width(5.dp))
        Text("${(progress * 100).toInt()}%", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Clean.ink))
    }
}

/** Round icon button with a caption under it (Undo / Restart / Hint) and an optional count badge. */
@Composable
fun LabeledTealButton(icon: Icon, label: String, modifier: Modifier = Modifier, color: Color = Clean.teal, badge: String? = null, badgeColor: Color = Clean.coral, enabled: Boolean = true, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            TealButton(icon, label, size = 56.dp, color = color, enabled = enabled, onClick = onClick)
            if (badge != null) Box(
                Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp).size(22.dp).background(badgeColor, CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(badge, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)) }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Clean.inkSoft))
    }
}

/** The next free item: a white card with a progress track (k / NEW_ITEM_EVERY) and the item's swatch. */
@Composable
fun NewItemBar(store: GameStore, modifier: Modifier = Modifier) {
    val item = store.nextNewItem ?: return
    val k = store.itemProgress
    val n = com.ngstudio.mazebrush.data.NEW_ITEM_EVERY
    val f by animateFloatAsState(k / n.toFloat(), tween(400), label = "item")
    Row(
        modifier.height(46.dp).shadow(4.dp, RoundedCornerShape(23.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .background(Clean.pill, RoundedCornerShape(23.dp)).padding(start = 16.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New item", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Clean.ink))
                Spacer(Modifier.weight(1f))
                Text("$k/$n", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Clean.inkSoft))
            }
            Spacer(Modifier.height(3.dp))
            Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)).background(Clean.track)) {
                Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(Clean.tealLight, Clean.teal)), RoundedCornerShape(5.dp)))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(36.dp)) {
            ItemSwatch(item, Modifier.size(36.dp))
            Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).size(16.dp).background(Clean.ink, CircleShape).border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                GameIcon(Icon.LOCK, Color.White, size = 9.dp)
            }
        }
    }
}

/** A small round picture of a shop item: the ball's colours, or a board theme's floor + paint. */
@Composable
fun ItemSwatch(item: GameStore.ShopItem, modifier: Modifier = Modifier) {
    if (item.kind == GameStore.ShopItem.Kind.BALL) { BallIcon(item.id, modifier); return }
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        run {
            val t = BoardTheme.byId(item.id)
            drawCircle(Color(t.sideBottom.argb), r, c)
            drawCircle(Color(t.floor.argb), r * 0.82f, c - Offset(0f, r * 0.06f))
            drawArc(Color(t.paints[0].argb), 90f, 180f, true, Offset(c.x - r * 0.82f, c.y - r * 0.88f), Size(r * 1.64f, r * 1.64f))
        }
    }
}

/** "LEVEL 12" over a small star/par line, in the clean ink colour. */
@Composable
fun LevelTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = TextStyle(fontFamily = Lilita, fontSize = 30.sp, color = Clean.ink, letterSpacing = 0.5.sp))
}


/** Wide 3D action button in the clean style (Home PLAY): a lighter top, a darker lip, white Lilita text. */
@Composable
fun BigCleanButton(text: String, modifier: Modifier = Modifier, icon: Icon? = null, color: Color = Clean.teal, height: Dp = 62.dp, textSize: androidx.compose.ui.unit.TextUnit = 32.sp, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sink by animateFloatAsState(if (pressed) 1f else 0f, tween(90), label = "sink")
    val shape = RoundedCornerShape(height * 0.4f)
    Box(
        modifier.height(height).semantics { contentDescription = text; role = Role.Button }
            .clickable(src, null) { tapFeedback(); onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(height - 5.dp).align(Alignment.BottomCenter).shadow(6.dp, shape, ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow).background(dark(color), shape))
        Row(
            Modifier.fillMaxWidth().height(height - 5.dp).offset(y = (sink * 4).dp)
                .background(Brush.verticalGradient(listOf(light(color), color)), shape)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { GameIcon(icon, Color.White, size = height * 0.46f); Spacer(Modifier.width(10.dp)) }
            Text(text, style = TextStyle(fontFamily = Lilita, fontSize = textSize, color = Color.White, letterSpacing = 1.sp))
        }
    }
}

/** White rounded card with a soft warm shadow (clean-style replacement for the wood/gold Panel). */
@Composable
fun CleanCard(modifier: Modifier = Modifier, corner: Dp = 24.dp, padding: Dp = 14.dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.shadow(8.dp, RoundedCornerShape(corner), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .background(Clean.pill, RoundedCornerShape(corner)).padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally, content = content,
    )
}
