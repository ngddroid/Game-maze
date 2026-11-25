package com.ngstudio.mazebrush.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text
import com.ngstudio.mazebrush.ui.components.BigCleanButton
import com.ngstudio.mazebrush.ui.components.CleanMiniBoard
import com.ngstudio.mazebrush.ui.components.CleanCard
import com.ngstudio.mazebrush.ui.components.LevelTitle
import com.ngstudio.mazebrush.ui.components.TealButton
import com.ngstudio.mazebrush.ui.components.WhitePill
import com.ngstudio.mazebrush.ui.theme.Nunito
import com.ngstudio.mazebrush.ui.theme.Clean
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.BottomLeaves
import com.ngstudio.mazebrush.ui.components.TopLeaves
import com.ngstudio.mazebrush.ui.components.SurfaceBackground
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.CoinIcon
import com.ngstudio.mazebrush.ui.components.CounterPill
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.MiniBoard
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.PlayOrb
import com.ngstudio.mazebrush.ui.components.WorldBadge
import com.ngstudio.mazebrush.ui.components.RoundButton
import com.ngstudio.mazebrush.ui.components.StarIcon
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.rememberPulse
import com.ngstudio.mazebrush.ui.components.tapFeedback
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Pro

enum class HomeTab(val label: String, val icon: Icon) {
    SKINS("Skins", Icon.BALL), LEVELS("Levels", Icon.LEVELS), HOME("Play", Icon.PLAY), DAILY("Daily", Icon.CALENDAR), TROPHIES("Trophies", Icon.TROPHY)
}

@Composable
fun HomeScreen(
    tab: HomeTab,
    onTab: (HomeTab) -> Unit,
    onPlay: (Int) -> Unit,
    onPlayDaily: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val store = MazeApp.instance.store
    val theme = WorldTheme.forLevel(store.currentLevel)
    SurfaceBackground {
        Column(Modifier.fillMaxSize()) {
            // top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                WhitePill(store.totalStars.toString()) { StarIcon(true, size = 24.dp) }
                Spacer(Modifier.weight(1f))
                WhitePill(store.coins.toString()) { CoinIcon(size = 24.dp) }
                HSpace(10.dp)
                TealButton(Icon.GEAR, "Settings", size = 44.dp) { onSettings() }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(tab, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "tab") { t ->
                    when (t) {
                        HomeTab.HOME -> HomeTabContent(theme, onPlay, onTab)
                        HomeTab.LEVELS -> LevelsTab(onPlay)
                        HomeTab.SKINS -> SkinsTab()
                        HomeTab.DAILY -> DailyTab(onPlayDaily)
                        HomeTab.TROPHIES -> TrophiesTab()
                    }
                }
            }
            BottomBar(tab, onTab)
        }
    }
}

@Composable
private fun BottomBar(tab: HomeTab, onTab: (HomeTab) -> Unit) {
    val store = MazeApp.instance.store
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier.fillMaxWidth().height(66.dp)
                .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Clean.navTop, Clean.navBottom)))
                .border(1.5.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(28.dp)),
        ) {
            // a soft inner highlight along the top edge for a little depth
            Box(Modifier.fillMaxWidth().height(14.dp).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent))))
            // inset so the end tabs never reach the bar's rounded corners
            Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                for (t in HomeTab.values()) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { if (t != tab) { tapFeedback(); onTab(t) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (t != HomeTab.HOME) {
                            val sel = t == tab
                            // smooth fade/lift between tabs
                            val lift by animateFloatAsState(if (sel) 1f else 0f, tween(200, easing = FastOutSlowInEasing), label = "tabLift")
                            val col = lerp(Clean.inkSoft.copy(alpha = 0.6f), Color.White, lift)
                            Box(contentAlignment = Alignment.Center) {
                                // the chip fades in behind the icon+label; sized to the content so it keeps even margins
                                Box(
                                    Modifier.matchParentSize()
                                        .graphicsLayer { alpha = lift }
                                        .shadow(5.dp, RoundedCornerShape(16.dp), ambientColor = Clean.tealDark, spotColor = Clean.tealDark)
                                        .background(Brush.verticalGradient(listOf(Clean.tealLight, Clean.teal, Clean.tealDark)), RoundedCornerShape(16.dp))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.graphicsLayer { translationY = -lift * 2.dp.toPx() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                ) {
                                    Box {
                                        GameIcon(t.icon, col, size = 24.dp)
                                        val dot = (t == HomeTab.DAILY && (store.calendarReady || !store.dailyDoneToday))
                                        if (dot) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-2).dp).size(9.dp).background(Clean.coral, CircleShape).border(1.5.dp, Color.White, CircleShape))
                                    }
                                    VSpace(2.dp)
                                    Text(t.label, maxLines = 1, softWrap = false, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 10.5.sp, color = col))
                                }
                            }
                        }
                    }
                }
            }
        }
        // raised centre PLAY tab: a glossy teal dome in a white ring
        val sel = tab == HomeTab.HOME
        Box(
            Modifier.offset(y = (-24).dp).size(78.dp)
                .shadow(12.dp, CircleShape, ambientColor = Clean.tealDark, spotColor = Clean.tealDark)
                .background(Color.White, CircleShape).padding(5.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Clean.tealLight, Clean.teal, Clean.tealDark)))
                .semantics { contentDescription = "Play" }
                .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) { tapFeedback(); onTab(HomeTab.HOME) },
            contentAlignment = Alignment.Center,
        ) {
            // glossy sphere highlight: a soft elliptical shine near the top, feathered on every side (clipped to the dome)
            Canvas(Modifier.matchParentSize()) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.30f
                val rx = size.width * 0.42f
                val ry = size.height * 0.34f
                withTransform({ scale(1f, ry / rx, pivot = Offset(cx, cy)) }) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.55f),
                            0.55f to Color.White.copy(alpha = 0.16f),
                            1f to Color.Transparent,
                            center = Offset(cx, cy), radius = rx,
                        ),
                        radius = rx, center = Offset(cx, cy),
                    )
                }
            }
            GameIcon(if (sel) Icon.PLAY else Icon.HOME, Color.White, size = 32.dp)
        }
    }
}

@Composable
private fun HomeTabContent(theme: WorldTheme, onPlay: (Int) -> Unit, onTab: (HomeTab) -> Unit) {
    val app = MazeApp.instance
    val store = app.store
    val current = store.currentLevel
    val level = remember(current) { app.levels.level(current) }
    val board = com.ngstudio.mazebrush.render.BoardTheme.forLevel(current)
    val paint = com.ngstudio.mazebrush.render.PaintColor.byId(store.paintColor).first(board, current).color()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MAZE BRUSH", style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = 36.sp, color = Clean.ink, letterSpacing = 1.sp))
        VSpace(8.dp)
        CleanCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            LevelTitle("LEVEL $current")
            VSpace(6.dp)
            CleanMiniBoard(level, board, store.ballSkin, Modifier.fillMaxWidth().height(150.dp), paint = paint)
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Par ${level.par} moves", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Clean.inkSoft))
                val intro = level.intro
                if (intro != null) {
                    HSpace(8.dp)
                    Box(Modifier.background(Clean.teal, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("NEW TILE", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White))
                    }
                }
            }
        }
        VSpace(18.dp)
        BigCleanButton("PLAY", Modifier.fillMaxWidth(0.82f).scale(rememberPulse()), icon = Icon.PLAY) { onPlay(current) }
        VSpace(18.dp)
        ChestTrack(current)
        VSpace(26.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("Daily", Icon.CALENDAR, Color(0xFF4FA8FF), if (!store.dailyDoneToday) "NEW" else null, Modifier.weight(1f)) { onTab(HomeTab.DAILY) }
            QuickTile("Reward", Icon.GIFT, Color(0xFFFF9442), if (store.calendarReady) "CLAIM" else null, Modifier.weight(1f)) { onTab(HomeTab.DAILY) }
            QuickTile("Skins", Icon.BALL, Clean.teal, null, Modifier.weight(1f)) { onTab(HomeTab.SKINS) }
        }
        VSpace(28.dp)
    }
}

@Composable
internal fun QuickTile(
    label: String, icon: Icon, color: Color, badge: String?, modifier: Modifier,
    badgeColor: Color = Clean.coral, enabled: Boolean = true, onClick: () -> Unit,
) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                .background(Clean.pill, RoundedCornerShape(22.dp))
                .clickable { tapFeedback(); onClick() }
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(42.dp).alpha(if (enabled) 1f else 0.45f).background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.25f), color)), CircleShape), contentAlignment = Alignment.Center) {
                GameIcon(icon, Color.White, size = 24.dp)
            }
            VSpace(5.dp)
            Text(label, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Clean.ink), textAlign = TextAlign.Center)
        }
        if (badge != null) Box(
            Modifier.align(Alignment.TopEnd).offset(6.dp, (-8).dp).scale(rememberPulse(700, 0.08f))
                .background(badgeColor, RoundedCornerShape(10.dp)).border(2.dp, Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
        ) { Text(badge, style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)) }
    }
}

/** Progress through the current block of 10 levels; a chest waits at the end. */
@Composable
fun ChestTrack(current: Int) {
    val store = MazeApp.instance.store
    val blockStart = ((current - 1) / 10) * 10 + 1
    CleanCard(Modifier.fillMaxWidth(), corner = 22.dp, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 10) {
                val n = blockStart + i
                val done = store.isCompleted(n)
                val isCur = n == current
                if (i == 9) {
                    Box(Modifier.size(38.dp).scale(if (isCur) rememberPulse(800, 0.1f) else 1f), contentAlignment = Alignment.Center) {
                        GameIcon(Icon.CHEST, if (done) Clean.gold else Color(0xFFFF9442), size = 34.dp)
                    }
                } else {
                    Box(
                        Modifier.size(if (isCur) 22.dp else 18.dp)
                            .background(if (done) Clean.teal else if (isCur) Clean.coral else Clean.track, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) GameIcon(Icon.CHECK, Color.White, size = 12.dp) }
                    if (i < 8) Box(Modifier.width(6.dp).height(4.dp).background(if (done) Clean.teal else Clean.track))
                    else Box(Modifier.width(6.dp))
                }
            }
        }
        VSpace(6.dp)
        val left = blockStart + 9 - current + 1
        Text(if (left <= 1) "Chest on this level!" else "Chest in $left levels", style = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Clean.coralDark))
    }
}
