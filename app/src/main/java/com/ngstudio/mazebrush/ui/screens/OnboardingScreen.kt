package com.ngstudio.mazebrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Physics
import com.ngstudio.mazebrush.render.BallSkin
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.SurfaceBackground
import com.ngstudio.mazebrush.ui.components.CoinIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.MiniBoard
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.StarIcon
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.rememberPulse
import com.ngstudio.mazebrush.ui.theme.Candy
import kotlinx.coroutines.launch
import kotlin.math.sin

private class Page(val title: String, val body: String)

private val pages = listOf(
    Page("Swipe to Glide", "Swipe up, down, left or right. Your ball slides until it bumps into a wall."),
    Page("Paint Every Tile", "Every tile you roll over gets painted. Fill the whole maze to win!"),
    Page("Stars, Skins & Worlds", "Finish in fewer moves for 3 stars. Earn coins for new balls, trails and paint, and travel through 10 worlds."),
)

/** First-launch onboarding. No Skip button: the player goes through with Next. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    BackHandler(pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
    SurfaceBackground(WorldTheme.ALL[0]) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            VSpace(20.dp)
            EngravedText("MAZE BRUSH", 32.sp, WorldTheme.ALL[0].ink.color())
            HorizontalPager(pager, Modifier.weight(1f), userScrollEnabled = false) { page ->
                Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        when (page) {
                            0 -> DemoSwipe()
                            1 -> DemoPaint()
                            else -> DemoRewards()
                        }
                    }
                    VSpace(14.dp)
                    EngravedText(pages[page].title, 36.sp, WorldTheme.ALL[0].ink.color())
                    VSpace(8.dp)
                    BodyText(pages[page].body, size = 17.sp, color = WorldTheme.ALL[0].ink.color(), align = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in pages.indices) {
                    val w by animateFloatAsState(if (i == pager.currentPage) 30f else 12f, label = "dot")
                    Box(Modifier.height(12.dp).width(w.dp).background(if (i == pager.currentPage) Candy.mintDark else Candy.plum.copy(alpha = 0.25f), RoundedCornerShape(6.dp)))
                }
            }
            VSpace(18.dp)
            val last = pager.currentPage == pages.size - 1
            CandyButton(
                if (last) "LET'S PLAY!" else "NEXT",
                Modifier.fillMaxWidth(0.78f).scale(if (last) rememberPulse() else 1f),
                color = if (last) Candy.mint else Candy.orange, dark = if (last) Candy.mintDark else Candy.orangeDark,
                icon = if (last) Icon.PLAY else null, height = 66.dp, textSize = 30.sp,
            ) {
                if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            }
        }
    }
}

private val demoLevel = Level.parse(
    """
    S....
    .###.
    .#...
    .#.##
    .....
    """
)

/** Scripted ball movement over the demo level; returns ball position and painted amounts at time t. */
private class DemoPlayback(val level: Level, val moves: List<Dir>) {
    val points = ArrayList<Pair<Int, Int>>() // (cell, moveIndex)
    init {
        var pos = level.start
        points.add(pos to -1)
        moves.forEachIndexed { mi, d ->
            val s = Physics.slide(level, pos, d) ?: return@forEachIndexed
            for (c in s.cells) points.add(c to mi)
            pos = s.end
        }
    }

    /** t in cells travelled. */
    fun ball(t: Float): Offset {
        val i = t.toInt().coerceIn(0, points.size - 1)
        val j = (i + 1).coerceAtMost(points.size - 1)
        val f = (t - i).coerceIn(0f, 1f)
        val a = points[i].first; val b = points[j].first
        return Offset(level.x(a) + 0.5f + (level.x(b) - level.x(a)) * f, level.y(a) + 0.5f + (level.y(b) - level.y(a)) * f)
    }

    fun painted(t: Float, cell: Int): Float {
        val idx = points.indexOfFirst { it.first == cell }
        if (idx < 0) return 0f
        return (t - idx + 0.6f).coerceIn(0f, 1f)
    }
}

@Composable
private fun DemoSwipe() {
    val theme = WorldTheme.ALL[0]
    val play = remember { DemoPlayback(demoLevel, listOf(Dir.RIGHT, Dir.DOWN, Dir.LEFT, Dir.UP)) }
    val t by rememberInfiniteTransition(label = "d1").animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "t")
    val travel = (t * (play.points.size + 3)).coerceAtMost(play.points.size - 1f)
    Box(contentAlignment = Alignment.Center) {
        MiniBoard(demoLevel, theme, Modifier.fillMaxSize(0.92f), painted = { play.painted(travel, it) }, paint = theme.paintFor(1).color(), ball = play.ball(travel))
        // swipe arrows showing the direction of the current move
        val move = play.points.getOrNull(travel.toInt().coerceAtMost(play.points.size - 1))?.second ?: 0
        val d = listOf(Dir.RIGHT, Dir.DOWN, Dir.LEFT, Dir.UP).getOrElse(move.coerceAtLeast(0)) { Dir.RIGHT }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).size(54.dp).background(Color.White, RoundedCornerShape(27.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.graphicsLayer { rotationZ = when (d) { Dir.RIGHT -> 0f; Dir.DOWN -> 90f; Dir.LEFT -> 180f; Dir.UP -> 270f } }) {
                com.ngstudio.mazebrush.ui.components.GameIcon(Icon.PLAY, Candy.pink, size = 30.dp)
            }
        }
    }
}

@Composable
private fun DemoPaint() {
    val theme = WorldTheme.ALL[0]
    val play = remember { DemoPlayback(demoLevel, listOf(Dir.DOWN, Dir.RIGHT, Dir.UP, Dir.RIGHT, Dir.UP, Dir.LEFT)) }
    val t by rememberInfiniteTransition(label = "d2").animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "t")
    val travel = (t * (play.points.size + 6)).coerceAtMost(play.points.size - 1f)
    val done = travel >= play.points.size - 1f
    Box(contentAlignment = Alignment.Center) {
        MiniBoard(demoLevel, theme, Modifier.fillMaxSize(0.92f), painted = { play.painted(travel, it) }, paint = theme.paintFor(2).color(), ball = play.ball(travel))
        if (done) GameText("100%", 52.sp, color = Candy.lemon, outline = Candy.pinkDark, modifier = Modifier.scale(rememberPulse(500, 0.1f)))
    }
}

@Composable
private fun DemoRewards() {
    val bob by rememberInfiniteTransition(label = "d3").animateFloat(0f, 6.283f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "b")
    Panel(Modifier.fillMaxWidth(0.9f)) {
        Row(verticalAlignment = Alignment.Bottom) {
            for (i in 1..3) StarIcon(true, size = if (i == 2) 74.dp else 58.dp, modifier = Modifier.padding(horizontal = 4.dp).graphicsLayer { translationY = sin(bob + i) * 8f })
        }
        VSpace(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(size = 36.dp)
            HSpace(8.dp)
            GameText("+20", 34.sp, color = Candy.gold, outline = Candy.goldDark)
        }
        VSpace(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (id in listOf(4, 12, 8, 13)) {
                val skin = BallSkin.byId(id)
                Box(Modifier.size(52.dp).graphicsLayer { translationY = sin(bob * 2 + id) * 5f }) { SkinSwatch(skin, Modifier.fillMaxSize()) }
            }
        }
    }
}
