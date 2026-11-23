package com.ngstudio.maze.ui.screens

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
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.render.WorldTheme
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.BottomLeaves
import com.ngstudio.maze.ui.components.TopLeaves
import com.ngstudio.maze.ui.components.SurfaceBackground
import com.ngstudio.maze.ui.components.CandyButton
import com.ngstudio.maze.ui.components.CoinIcon
import com.ngstudio.maze.ui.components.CounterPill
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.EngravedText
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.HSpace
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.MiniBoard
import com.ngstudio.maze.ui.components.Panel
import com.ngstudio.maze.ui.components.PlayOrb
import com.ngstudio.maze.ui.components.WorldBadge
import com.ngstudio.maze.ui.components.RoundButton
import com.ngstudio.maze.ui.components.StarIcon
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.components.rememberPulse
import com.ngstudio.maze.ui.components.tapFeedback
import com.ngstudio.maze.ui.theme.Candy
import com.ngstudio.maze.ui.theme.Pro

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
    SurfaceBackground(theme) {
        TopLeaves()
        BottomLeaves()
        Column(Modifier.fillMaxSize()) {
            // top bar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CounterPill(store.totalStars.toString()) { StarIcon(true, size = 26.dp) }
                Spacer(Modifier.weight(1f))
                CounterPill(store.coins.toString()) { CoinIcon(size = 26.dp) }
                HSpace(8.dp)
                RoundButton(Icon.GEAR, size = 44.dp) { onSettings() }
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
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.fillMaxWidth().height(64.dp)
                .shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = Candy.plumDark, spotColor = Candy.plumDark)
                .background(Brush.verticalGradient(listOf(Pro.chrome, Pro.chromeDark)), RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.16f), 0.35f to Color.Transparent), RoundedCornerShape(26.dp))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Pro.chromeLip.copy(alpha = 0.5f))), RoundedCornerShape(26.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (t in HomeTab.values()) {
                Box(Modifier.weight(1f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { if (t != tab) { tapFeedback(); onTab(t) } }, contentAlignment = Alignment.Center) {
                    if (t != HomeTab.HOME) {
                        val sel = t == tab
                        val s by animateFloatAsState(if (sel) 1.12f else 1f, label = "tabScale")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(s)) {
                            Box {
                                GameIcon(t.icon, if (sel) Pro.gold else Pro.creamDim, size = 28.dp)
                                val dot = (t == HomeTab.DAILY && (store.calendarReady || !store.dailyDoneToday))
                                if (dot) Box(Modifier.align(Alignment.TopEnd).offset(4.dp, (-2).dp).size(10.dp).background(Candy.pink, CircleShape).border(1.5.dp, Color.White, CircleShape))
                            }
                            BodyText(t.label, size = 12.sp, color = if (sel) Pro.gold else Pro.creamDim, shadow = false)
                        }
                    }
                }
            }
        }
        // raised centre PLAY tab
        val sel = tab == HomeTab.HOME
        PlayOrb(
            if (sel) Icon.PLAY else Icon.HOME,
            Modifier.offset(y = (-24).dp).size(80.dp)
                .semantics { contentDescription = "Play" }
                .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) { tapFeedback(); onTab(HomeTab.HOME) },
        )
    }
}

@Composable
private fun HomeTabContent(theme: WorldTheme, onPlay: (Int) -> Unit, onTab: (HomeTab) -> Unit) {
    val app = MazeApp.instance
    val store = app.store
    val current = store.currentLevel
    val level = remember(current) { app.levels.level(current) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        EngravedText("SPLASHY GLIDE", 36.sp, theme.ink.color())
        WorldBadge("WORLD ${WorldTheme.worldIndexOf(current) + 1}  •  ${theme.name.uppercase()}", theme.accentDark.color())
        VSpace(8.dp)
        Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
            EngravedText("LEVEL $current", 38.sp, Color(0xFF5E2F14))
            VSpace(2.dp)
            MiniBoard(level, theme, Modifier.fillMaxWidth().height(132.dp), paint = theme.paintFor(current).color(), cardWood = true)
            VSpace(4.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BodyText("Par ${level.par} moves", size = 13.sp, color = Candy.greyDark)
                val intro = level.intro
                if (intro != null) {
                    HSpace(8.dp)
                    Box(Modifier.background(Candy.sky, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        BodyText("NEW TILE", size = 11.sp, color = Color.White, shadow = false)
                    }
                }
            }
        }
        VSpace(18.dp)
        CandyButton("PLAY", Modifier.fillMaxWidth(0.82f).scale(rememberPulse()), height = 62.dp, textSize = 34.sp, icon = Icon.PLAY) { onPlay(current) }
        VSpace(18.dp)
        ChestTrack(current)
        VSpace(26.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickTile("Daily", Icon.CALENDAR, Candy.sky, if (!store.dailyDoneToday) "NEW" else null, Modifier.weight(1f)) { onTab(HomeTab.DAILY) }
            QuickTile("Reward", Icon.GIFT, Candy.orange, if (store.calendarReady) "CLAIM" else null, Modifier.weight(1f)) { onTab(HomeTab.DAILY) }
            QuickTile("Skins", Icon.BALL, Candy.mint, null, Modifier.weight(1f)) { onTab(HomeTab.SKINS) }
        }
        VSpace(28.dp)
    }
}

@Composable
internal fun QuickTile(
    label: String, icon: Icon, color: Color, badge: String?, modifier: Modifier,
    badgeColor: Color = Candy.pink, enabled: Boolean = true, onClick: () -> Unit,
) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Pro.chrome, Pro.chromeDark)), RoundedCornerShape(20.dp))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Pro.chromeLip.copy(alpha = 0.5f))), RoundedCornerShape(20.dp))
                .clickable { tapFeedback(); onClick() }
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(40.dp).alpha(if (enabled) 1f else 0.45f).background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.2f), color)), CircleShape), contentAlignment = Alignment.Center) {
                GameIcon(icon, Color.White, size = 24.dp)
            }
            VSpace(4.dp)
            BodyText(label, size = 14.sp, color = Pro.cream, align = TextAlign.Center)
        }
        if (badge != null) Box(
            Modifier.align(Alignment.TopEnd).offset(6.dp, (-8).dp).scale(rememberPulse(700, 0.08f))
                .background(badgeColor, RoundedCornerShape(10.dp)).border(2.dp, Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
        ) { BodyText(badge, size = 11.sp, color = Color.White) }
    }
}

/** Progress through the current block of 10 levels; a chest waits at the end. */
@Composable
fun ChestTrack(current: Int) {
    val store = MazeApp.instance.store
    val blockStart = ((current - 1) / 10) * 10 + 1
    Panel(Modifier.fillMaxWidth(), padding = 10.dp, corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 10) {
                val n = blockStart + i
                val done = store.isCompleted(n)
                val isCur = n == current
                if (i == 9) {
                    Box(Modifier.size(38.dp).scale(if (isCur) rememberPulse(800, 0.1f) else 1f), contentAlignment = Alignment.Center) {
                        GameIcon(Icon.CHEST, if (done) Candy.gold else Candy.orange, size = 34.dp)
                    }
                } else {
                    Box(
                        Modifier.size(if (isCur) 22.dp else 18.dp)
                            .background(if (done) Candy.mint else if (isCur) Candy.pink else Candy.grey.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (done) GameIcon(Icon.CHECK, Color.White, size = 12.dp) }
                    if (i < 8) Box(Modifier.width(6.dp).height(4.dp).background(if (done) Candy.mint else Candy.grey.copy(alpha = 0.4f)))
                    else Box(Modifier.width(6.dp))
                }
            }
        }
        VSpace(4.dp)
        val left = blockStart + 9 - current + 1
        BodyText(if (left <= 1) "Chest on this level!" else "Chest in $left levels", size = 13.sp, color = Candy.orangeDark)
    }
}
