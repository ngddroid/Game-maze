package com.ngstudio.maze.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Physics
import com.ngstudio.maze.render.BallSkin
import com.ngstudio.maze.render.GameGLView
import com.ngstudio.maze.render.GameRenderer
import com.ngstudio.maze.render.PaintFinish
import com.ngstudio.maze.render.RenderListener
import com.ngstudio.maze.render.SceneConfig
import com.ngstudio.maze.render.TrailStyle
import com.ngstudio.maze.render.WorldTheme
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.CandyButton
import com.ngstudio.maze.ui.components.CoinIcon
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.EngravedText
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.HSpace
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.components.starPath
import com.ngstudio.maze.ui.components.tapFeedback
import com.ngstudio.maze.ui.theme.Candy
import com.ngstudio.maze.ui.theme.Pro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ShopTab(val label: String) { BALLS("Balls"), TRAILS("Trails"), PAINT("Paint") }

private val previewLevel = Level.parse(
    """
    S......
    .#####.
    .......
    """
)

/** One shop entry, whatever its kind. */
private class ShopItem(val tab: ShopTab, val id: Int, val name: String, val price: Int, val owned: Boolean, val equipped: Boolean)

@Composable
fun SkinsTab() {
    val app = MazeApp.instance
    val store = app.store
    var tab by remember { mutableStateOf(ShopTab.BALLS) }
    // What the live preview shows. It starts on what is equipped; tapping ANY card previews it, owned or not.
    var pBall by remember { mutableIntStateOf(store.ballSkin) }
    var pTrail by remember { mutableIntStateOf(store.trail) }
    var pFinish by remember { mutableIntStateOf(store.finish) }
    var noMoney by remember { mutableStateOf(false) }
    var adFor by remember { mutableStateOf<ShopItem?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val item: ShopItem = when (tab) {
        ShopTab.BALLS -> BallSkin.byId(pBall).let { ShopItem(tab, it.id, it.name, it.price, it.id in store.ownedBalls, store.ballSkin == it.id) }
        ShopTab.TRAILS -> TrailStyle.byId(pTrail).let { ShopItem(tab, it.id, it.name, it.price, it.id in store.ownedTrails, store.trail == it.id) }
        ShopTab.PAINT -> PaintFinish.byId(pFinish).let { ShopItem(tab, it.id, it.name, it.price, it.id in store.ownedFinishes, store.finish == it.id) }
    }

    fun equip(i: ShopItem) = when (i.tab) {
        ShopTab.BALLS -> store.selectBall(i.id)
        ShopTab.TRAILS -> store.selectTrail(i.id)
        ShopTab.PAINT -> store.selectFinish(i.id)
    }
    fun buy(i: ShopItem): Boolean = when (i.tab) {
        ShopTab.BALLS -> store.buyBall(i.id)
        ShopTab.TRAILS -> store.buyTrail(i.id)
        ShopTab.PAINT -> store.buyFinish(i.id)
    }
    fun notEnough() = scope.launch {
        noMoney = true
        app.sound.bonk()
        for (k in 0 until 6) shake.animateTo(if (k % 2 == 0) 12f else -12f, tween(45))
        shake.animateTo(0f, tween(45))
        delay(1600); noMoney = false
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        EngravedText("SKINS", 34.sp, WorldTheme.forLevel(store.currentLevel).ink.color(), Modifier.align(Alignment.CenterHorizontally))
        VSpace(6.dp)
        Box {
            SkinPreview(
                Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(22.dp)).border(2.dp, Pro.border, RoundedCornerShape(22.dp)),
                pBall, pTrail, pFinish,
            )
            Box(
                Modifier.align(Alignment.TopStart).padding(10.dp).background(Pro.glass, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
            ) { BodyText(if (item.equipped) "${item.name}  ·  equipped" else "Preview: ${item.name}", size = 12.sp, color = Pro.cream) }
        }
        VSpace(8.dp)
        // action bar for the previewed item
        Row(Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                item.equipped -> CandyButton("EQUIPPED", Modifier.weight(1f), height = 50.dp, textSize = 20.sp, icon = Icon.CHECK, enabled = false) {}
                item.owned -> CandyButton("EQUIP", Modifier.weight(1f), height = 50.dp, textSize = 20.sp, icon = Icon.CHECK) { equip(item); app.sound.pop() }
                else -> {
                    CandyButton("${item.price}", Modifier.weight(1f), height = 50.dp, textSize = 20.sp, icon = Icon.COIN,
                        color = Pro.gold, dark = Pro.goldDark) {
                        if (buy(item)) { app.sound.unlock(); equip(item); app.telemetry.spend("${item.tab.name.lowercase()}_${item.id}", item.price) } else notEnough()
                    }
                    if (Ads.rewarded.available) CandyButton("WATCH AD", Modifier.weight(1f), height = 50.dp, textSize = 19.sp, icon = Icon.VIDEO,
                        color = Candy.pink, dark = Candy.pinkDark) { adFor = item }
                }
            }
        }
        if (noMoney) BodyText("Not enough coins - play levels to earn more!", Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp), color = WorldTheme.forLevel(store.currentLevel).ink.color(), size = 14.sp)
        VSpace(8.dp)
        Row(
            Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)).background(Pro.glass, RoundedCornerShape(20.dp)).padding(4.dp),
        ) {
            for (t in ShopTab.values()) {
                val sel = t == tab
                Box(
                    Modifier.weight(1f).height(38.dp).background(if (sel) Candy.mint else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { tapFeedback(); tab = t },
                    contentAlignment = Alignment.Center,
                ) { GameText(t.label, 18.sp, color = if (sel) Color.White else Pro.creamDim, outline = if (sel) Candy.mintDark else Color.Transparent, outlineWidth = if (sel) 0.06f else 0f) }
            }
        }
        VSpace(8.dp)
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tab) {
                ShopTab.BALLS -> items(BallSkin.ALL) { s ->
                    ShopCard(s.name, s.price, s.id in store.ownedBalls, store.ballSkin == s.id, pBall == s.id, { SkinSwatch(s, Modifier.fillMaxSize(0.78f)) }) { pBall = s.id }
                }
                ShopTab.TRAILS -> items(TrailStyle.ALL) { t ->
                    ShopCard(t.name, t.price, t.id in store.ownedTrails, store.trail == t.id, pTrail == t.id, { TrailSwatch(t.id) }) { pTrail = t.id }
                }
                ShopTab.PAINT -> items(PaintFinish.ALL) { f ->
                    ShopCard(f.name, f.price, f.id in store.ownedFinishes, store.finish == f.id, pFinish == f.id, { PaintSwatch(f.id) }) { pFinish = f.id }
                }
            }
        }
    }
    adFor?.let { i ->
        FakeAdOverlay {
            when (i.tab) {
                ShopTab.BALLS -> store.grantBall(i.id)
                ShopTab.TRAILS -> store.grantTrail(i.id)
                ShopTab.PAINT -> store.grantFinish(i.id)
            }
            equip(i)
            app.sound.unlock()
            app.telemetry.rewardedAd("skin")
            adFor = null
        }
    }
}

@Composable
private fun ShopCard(name: String, price: Int, owned: Boolean, equipped: Boolean, previewing: Boolean, swatch: @Composable () -> Unit, onClick: () -> Unit) {
    Box {
        Column(
            Modifier.fillMaxWidth().shadow(if (previewing) 10.dp else 4.dp, RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color.White, Candy.cream)), RoundedCornerShape(20.dp))
                .border(if (previewing) 3.dp else 1.5.dp, if (previewing) Pro.gold else Pro.border, RoundedCornerShape(20.dp))
                .clickable { tapFeedback(); onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) { swatch() }
            BodyText(name, size = 12.sp, align = TextAlign.Center, color = Candy.ink)
            VSpace(4.dp)
            Box(
                Modifier.fillMaxWidth().height(26.dp)
                    .background(when { equipped -> Candy.mint; owned -> Candy.sky; else -> Pro.chromeDark }, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    equipped -> BodyText("IN USE", size = 12.sp, color = Color.White)
                    owned -> BodyText("OWNED", size = 12.sp, color = Color.White)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) { CoinIcon(size = 15.dp); HSpace(3.dp); BodyText("$price", size = 12.sp, color = Pro.cream) }
                }
            }
        }
        if (equipped) Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).background(Candy.mint, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) { GameIcon(Icon.CHECK, Color.White, size = 14.dp) }
    }
}

/** 2D approximation of a ball skin for shop cards (the game uses the real 3D shader). */
@Composable
fun SkinSwatch(s: BallSkin, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2 * 0.92f
        val c = Offset(size.width / 2, size.height / 2)
        val a = s.a.color(); val b = s.b.color(); val cc = s.c.color()
        drawCircle(Color.Black.copy(alpha = 0.18f), r, c + Offset(r * 0.08f, r * 0.14f))
        val ball = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(ball) {
            drawCircle(a, r, c)
            when (s.pattern) {
                1 -> drawRect(b, Offset(c.x - r, c.y - r * 0.28f), Size(r * 2, r * 0.56f))
                2 -> for ((dx, dy) in listOf(-0.45f to -0.4f, 0.4f to -0.3f, 0f to 0.35f, -0.6f to 0.45f, 0.65f to 0.5f)) drawCircle(b, r * 0.2f, c + Offset(dx * r, dy * r))
                3 -> drawRect(b, Offset(c.x, c.y - r), Size(r, r * 2))
                4 -> for (k in 0 until 3) drawArc(b, k * 120f, 60f, true, c - Offset(r, r), Size(r * 2, r * 2))
                5 -> { for (k in 0 until 6) drawArc(listOf(a, b, cc)[k % 3], k * 60f - 90f, 60f, true, c - Offset(r, r), Size(r * 2, r * 2)); drawCircle(Color.White, r * 0.18f, c) }
                6 -> { drawCircle(b.copy(alpha = 0.7f), r * 0.5f, c + Offset(r * 0.35f, r * 0.3f)); drawCircle(b.copy(alpha = 0.5f), r * 0.35f, c + Offset(-r * 0.4f, -r * 0.2f)) }
                7 -> { drawCircle(b.copy(alpha = 0.6f), r * 0.7f, c + Offset(r * 0.2f, r * 0.1f)); for (k in 0 until 9) drawCircle(Color.White, r * 0.04f, c + Offset(cos(k * 2.1f) * r * 0.7f, sin(k * 1.7f) * r * 0.6f)) }
                8 -> for (k in -2..2) drawRect(if (k % 2 == 0) b else a, Offset(c.x - r, c.y + k * r * 0.32f - r * 0.12f), Size(r * 2, r * 0.24f))
                9 -> drawPath(starPath(c.x, c.y - r * 0.05f, r * 0.55f, r * 0.24f), b)
            }
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)), c - Offset(r * 0.3f, r * 0.35f), r * 1.5f), r, c)
        }
        drawCircle(Color.White.copy(alpha = 0.85f), r * 0.18f, c - Offset(r * 0.38f, r * 0.42f))
        drawCircle(Color.White.copy(alpha = 0.35f), r * 0.32f, c - Offset(r * 0.3f, r * 0.34f))
    }
}

@Composable
private fun TrailSwatch(id: Int) {
    Canvas(Modifier.fillMaxSize(0.85f)) {
        val y = size.height * 0.55f
        val colors = listOf(Candy.pink, Candy.lemon, Candy.mint, Candy.sky, Candy.lilac)
        for (k in 0 until 7) {
            val x = size.width * (0.12f + k * 0.1f)
            val a = 0.25f + k * 0.1f
            val r = size.width * (0.03f + k * 0.006f)
            when (id) {
                0 -> Unit
                1, 7 -> drawPath(starPath(x, y + sin(k * 1.3f) * 8f, r * 1.6f, r * 0.5f), (if (id == 7) Color(0xFFFFD86B) else Color.White).copy(alpha = a + 0.2f))
                2 -> drawCircle(Color.White.copy(alpha = a), r * 1.4f, Offset(x, y - k * 3f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                3 -> drawCircle(Candy.pink.copy(alpha = a), r * (0.6f + k * 0.15f), Offset(x, y))
                4 -> drawRect(colors[k % 5].copy(alpha = a + 0.3f), Offset(x, y + sin(k * 2f) * 10f), Size(r * 1.6f, r * 0.9f))
                5 -> drawCircle(colors[k % 5].copy(alpha = a + 0.2f), r * 1.3f, Offset(x, y))
                6 -> drawCircle(Candy.sky.copy(alpha = a + 0.3f), r * 1.1f, Offset(x, y + sin(k * 1.7f) * 9f))
            }
        }
        val br = size.width * 0.15f
        val bc = Offset(size.width * 0.84f, y)
        drawCircle(Color.Black.copy(alpha = 0.18f), br, bc + Offset(3f, 6f))
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFE5F1)), bc - Offset(br * 0.3f, br * 0.4f), br * 1.5f), br, bc)
        drawRect(Candy.pink, Offset(bc.x - br * 0.98f, bc.y - br * 0.2f), Size(br * 1.96f, br * 0.4f))
        if (id == 0) drawLine(Candy.grey, Offset(size.width * 0.15f, y), Offset(size.width * 0.6f, y), 4f)
    }
}

@Composable
private fun PaintSwatch(id: Int) {
    Canvas(Modifier.fillMaxSize(0.85f)) {
        val base = Color(0xFFFFD93D)
        val brush = when (id) {
            1 -> Brush.verticalGradient(listOf(lerp(base, Color.White, 0.45f), lerp(base, Color.White, 0.25f)))
            2 -> Brush.radialGradient(listOf(Color(0xFFFF6BEA), Color(0xFFFF2BD6), Color(0xFF7A0F6A)))
            3 -> Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFD9A400), Color(0xFFFFF0A0), Color(0xFFB88700)))
            4 -> Brush.sweepGradient(listOf(Color(0xFFFF5C8A), Color(0xFFFFD93D), Color(0xFF5CF2C5), Color(0xFF4FD8FF), Color(0xFFB98CFF), Color(0xFFFF5C8A)))
            5 -> Brush.verticalGradient(listOf(Color(0xFFFFD93D), Color(0xFFFF5FA8)))
            else -> Brush.verticalGradient(listOf(lerp(base, Color.White, 0.15f), base))
        }
        val blob = Path().apply {
            val c = Offset(size.width / 2, size.height / 2)
            val n = 40
            for (i in 0..n) {
                val ang = i * 2 * PI / n
                val rr = size.minDimension * (0.40f + 0.05f * sin(ang * 5).toFloat())
                val p = c + Offset((cos(ang) * rr).toFloat(), (sin(ang) * rr).toFloat())
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(blob, Color.Black.copy(alpha = 0.15f), style = androidx.compose.ui.graphics.drawscope.Fill)
        drawPath(blob, brush)
        drawCircle(Color.White.copy(alpha = 0.65f), size.minDimension * 0.08f, Offset(size.width * 0.36f, size.height * 0.34f))
    }
}

/** Live 3D preview: the real renderer with the selected ball/trail/paint, rolling round a small ring. */
@Composable
private fun SkinPreview(modifier: Modifier, ball: Int, trail: Int, finish: Int) {
    var finished by remember { mutableIntStateOf(0) }
    val renderer = remember { GameRenderer(object : RenderListener { override fun onMoveFinished(endCell: Int, cells: Int) { finished++ } }) }
    val glView = remember { arrayOfNulls<GameGLView>(1) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) glView[0]?.onResume()
            if (e == Lifecycle.Event.ON_PAUSE) glView[0]?.onPause()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(ball, trail, finish) {
        val theme = WorldTheme.ALL[0]
        renderer.setSafeArea(0f, 0f)
        while (true) {
            renderer.setScene(SceneConfig(previewLevel, 1, theme, BallSkin.byId(ball), TrailStyle.byId(trail), PaintFinish.byId(finish), theme.paintFor(2), theme.paintFor(4), instant = true))
            delay(350)
            var pos = previewLevel.start
            for (d in listOf(Dir.RIGHT, Dir.DOWN, Dir.LEFT, Dir.UP)) {
                val s = Physics.slide(previewLevel, pos, d) ?: continue
                val before = finished
                renderer.playSlide(s)
                pos = s.end
                var waited = 0
                while (finished == before && waited < 3000) { delay(30); waited += 30 }
                delay(220)
            }
            delay(1200)
        }
    }
    AndroidView(factory = { ctx -> GameGLView(ctx, renderer).also { glView[0] = it } }, modifier = modifier)
}
