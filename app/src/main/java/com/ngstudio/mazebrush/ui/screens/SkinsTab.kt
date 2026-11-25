package com.ngstudio.mazebrush.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Physics
import com.ngstudio.mazebrush.data.GameStore
import com.ngstudio.mazebrush.render.BallSkin
import com.ngstudio.mazebrush.render.BoardTheme
import com.ngstudio.mazebrush.render.GameGLView
import com.ngstudio.mazebrush.render.GameRenderer
import com.ngstudio.mazebrush.render.PaintColor
import com.ngstudio.mazebrush.render.PaintFinish
import com.ngstudio.mazebrush.render.RenderListener
import com.ngstudio.mazebrush.render.SceneConfig
import com.ngstudio.mazebrush.render.TrailStyle
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.CoinIcon
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.starPath
import com.ngstudio.mazebrush.ui.components.tapFeedback
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Clean
import com.ngstudio.mazebrush.ui.theme.Lilita
import com.ngstudio.mazebrush.ui.theme.Nunito
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Skins shop (2026-11-25 clean redesign): a live preview on top, then a white panel with Ball / Trail / Paint / Maze
 * tabs and a paged 3x2 grid. Tapping an owned item equips it; tapping a locked one previews it and shows how to get
 * it (coins, or a number of finished levels for paint colours). Layout idea from the genre; colours and art are ours.
 */
private enum class ShopTab(val label: String, val icon: Icon) {
    BALL("Ball", Icon.BALL), TRAIL("Trail", Icon.TRAIL), PAINT("Paint", Icon.DROP)   // no Maze tab: the board follows the level
}

private enum class Kind { BALL, TRAIL, COLOR, FINISH, BOARD }

/** One card in the grid, whatever it sells. [levels] > 0: unlocks free after that many finished levels. */
private class Entry(val kind: Kind, val id: Int, val name: String, val price: Int, val levels: Int, val owned: Boolean, val equipped: Boolean, val tint: Color)

private const val PER_PAGE = 6

private val previewLevel = Level.parse(
    """
    S......
    .#####.
    .......
    """
)

private val trailTints = listOf(
    Color(0xFFB8C4CC), Color(0xFFFFD86B), Color(0xFF8AD8FF), Color(0xFFFF8A5C), Color(0xFFB98CFF), Color(0xFFFF7FB2), Color(0xFF5CE0B8), Color(0xFFFFC93C),
    Color(0xFF8AD8FF), Color(0xFFFF8FB8), Color(0xFFFFD86B), Color(0xFFC9CCD6), Color(0xFFFF9A5C), Color(0xFF8FDBFF), Color(0xFFB98CFF), Color(0xFF8BD46A),
    Color(0xFFFF9AD0), Color(0xFFB98CFF), Color(0xFF6FB2FF), Color(0xFF9A7BFF), Color(0xFF7A5BD6), Color(0xFF9BD3F5), Color(0xFFFF9AC2), Color(0xFF6FE0D0),
)

private fun entries(tab: ShopTab, s: GameStore, board: BoardTheme): List<Entry> = when (tab) {
    ShopTab.BALL -> BallSkin.ALL.map { Entry(Kind.BALL, it.id, it.name, it.price, 0, it.id in s.ownedBalls, s.ballSkin == it.id, it.a.color()) }
    ShopTab.TRAIL -> TrailStyle.ALL.map { Entry(Kind.TRAIL, it.id, it.name, it.price, 0, it.id in s.ownedTrails, s.trail == it.id, trailTints[it.id % trailTints.size]) }
    ShopTab.PAINT -> PaintColor.ALL.map { Entry(Kind.COLOR, it.id, it.name, 0, it.levels, s.ownsPaint(it.id), s.paintColor == it.id, it.rgb.color()) } +
        PaintFinish.ALL.map { Entry(Kind.FINISH, it.id, it.name, it.price, 0, it.id in s.ownedFinishes, s.finish == it.id, Color(0xFFFFC93C)) }
}

private fun label(size: TextUnit, color: Color) = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Black, fontSize = size, color = color)

/**
 * What the shop shows: the open tab, and the look being previewed (it starts on what is equipped; tapping ANY card
 * previews it, owned or not). Shared by the Skins tab (3D preview on top) and the in-game skin sheet (the live board
 * is the preview).
 */
class ShopPreview(store: GameStore, private val boardId: Int = BoardTheme.forLevel(store.currentLevel).id) {
    var tab by mutableIntStateOf(0)
    var ball by mutableIntStateOf(store.ballSkin)
    var trail by mutableIntStateOf(store.trail)
    var finish by mutableIntStateOf(store.finish)
    var paint by mutableIntStateOf(store.paintColor)
    /** The board shown under the preview (the current level's; not chosen in the shop). */
    var board by mutableIntStateOf(boardId)
    /** The card last tapped: Kind ordinal to id. */
    var tapped by mutableStateOf<Pair<Int, Int>?>(null)

    fun reset(store: GameStore, boardId: Int = this.boardId) {
        ball = store.ballSkin; trail = store.trail; finish = store.finish; paint = store.paintColor; board = boardId; tapped = null
    }
}

private class PanelState(val list: List<Entry>, val pending: Entry?)

private fun panelState(pv: ShopPreview, store: GameStore): PanelState {
    val list = entries(ShopTab.values()[pv.tab], store, BoardTheme.byId(pv.board))
    // the locked item the player last tapped (it drives the buy button); null once it is owned
    val pending = pv.tapped?.let { t -> list.firstOrNull { it.kind.ordinal == t.first && it.id == t.second } }?.takeIf { !it.owned }
    return PanelState(list, pending)
}

/** Name of the previewed locked item, or of the equipped item in the open tab. */
fun previewName(pv: ShopPreview, store: GameStore): String? {
    val s = panelState(pv, store)
    return (s.pending ?: s.list.firstOrNull { it.equipped })?.name
}
fun previewIsLocked(pv: ShopPreview, store: GameStore) = panelState(pv, store).pending != null

@Composable
fun SkinsTab() {
    val store = MazeApp.instance.store
    val pv = remember { ShopPreview(store) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        // live preview of the chosen look
        Box(
            Modifier.fillMaxWidth().weight(1f).padding(bottom = 10.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                .clip(RoundedCornerShape(24.dp)).background(Clean.pill),
        ) {
            SkinPreview(Modifier.fillMaxSize(), pv.ball, pv.trail, pv.finish, pv.paint, pv.board)
            PreviewChip(pv, store, Modifier.align(Alignment.BottomCenter).padding(10.dp))
        }
        ShopPanel(pv, Modifier.fillMaxWidth().padding(bottom = 8.dp))
    }
}

/** A small white tag naming what is being previewed (a tick when it is the equipped one). */
@Composable
fun PreviewChip(pv: ShopPreview, store: GameStore, modifier: Modifier = Modifier) {
    val name = previewName(pv, store) ?: return
    Row(
        modifier.shadow(2.dp, RoundedCornerShape(14.dp)).background(Clean.pill, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!previewIsLocked(pv, store)) { GameIcon(Icon.CHECK, Clean.teal, size = 14.dp); Spacer(Modifier.width(5.dp)) }
        Text(name, style = label(13.sp, Clean.ink))
    }
}

/** The shop panel: Ball / Trail / Paint / Maze tabs, the paged grid, page dots and the buy / ad button. */
@Composable
fun ShopPanel(pv: ShopPreview, modifier: Modifier = Modifier) {
    val app = MazeApp.instance
    val store = app.store
    var noMoney by remember { mutableStateOf(false) }
    var adFor by remember { mutableStateOf<Entry?>(null) }
    var coinAd by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val tab = ShopTab.values()[pv.tab]
    val st = panelState(pv, store)
    val list = st.list
    val pending = st.pending

    fun equip(e: Entry) = when (e.kind) {
        Kind.BALL -> store.selectBall(e.id)
        Kind.TRAIL -> store.selectTrail(e.id)
        Kind.COLOR -> store.selectPaint(e.id)
        Kind.FINISH -> store.selectFinish(e.id)
        Kind.BOARD -> store.selectBoard(e.id)
    }
    fun buy(e: Entry): Boolean = when (e.kind) {
        Kind.BALL -> store.buyBall(e.id)
        Kind.TRAIL -> store.buyTrail(e.id)
        Kind.FINISH -> store.buyFinish(e.id)
        Kind.BOARD -> store.buyBoard(e.id)
        Kind.COLOR -> false
    }
    fun preview(e: Entry) = when (e.kind) {
        Kind.BALL -> pv.ball = e.id
        Kind.TRAIL -> pv.trail = e.id
        Kind.COLOR -> pv.paint = e.id
        Kind.FINISH -> pv.finish = e.id
        Kind.BOARD -> pv.board = e.id
    }
    fun notEnough() = scope.launch {
        noMoney = true
        app.sound.bonk()
        for (k in 0 until 6) shake.animateTo(if (k % 2 == 0) 12f else -12f, tween(45))
        shake.animateTo(0f, tween(45))
        delay(1600); noMoney = false
    }

    Column(
        modifier.shadow(8.dp, RoundedCornerShape(26.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .clip(RoundedCornerShape(26.dp)).background(Clean.pill),
    ) {
        Row(Modifier.fillMaxWidth().background(Clean.track)) {
            for (t in ShopTab.values()) {
                val sel = t == tab
                Row(
                    Modifier.weight(1f).height(48.dp)
                        .background(if (sel) Clean.pill else Color.Transparent, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clickable { tapFeedback(); pv.tab = t.ordinal; pv.tapped = null },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(t.icon, if (sel) Clean.teal else Clean.inkSoft.copy(alpha = 0.75f), size = 20.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(t.label, style = label(16.sp, if (sel) Clean.ink else Clean.inkSoft.copy(alpha = 0.8f)))
                }
            }
        }
        val pages = (list.size + PER_PAGE - 1) / PER_PAGE
        key(tab) {
            val ps = rememberPagerState { pages }
            HorizontalPager(ps, Modifier.fillMaxWidth().padding(top = 14.dp)) { page ->
                Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in 0 until 2) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (c in 0 until 3) {
                            val e = list.getOrNull(page * PER_PAGE + r * 3 + c)
                            if (e == null) Spacer(Modifier.weight(1f))
                            else ShopCard(e, store.levelsDone, pv.tapped == (e.kind.ordinal to e.id), Modifier.weight(1f)) {
                                preview(e)
                                pv.tapped = e.kind.ordinal to e.id
                                if (e.owned && !e.equipped) { equip(e); app.sound.pop() }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                if (pages > 1) for (k in 0 until pages) Box(
                    Modifier.padding(horizontal = 4.dp).size(if (k == ps.currentPage) 9.dp else 7.dp)
                        .background(if (k == ps.currentPage) Clean.teal else Clean.track, CircleShape),
                )
            }
        }
        // what to do about the tapped locked item
        Box(
            Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 14.dp, vertical = 10.dp).graphicsLayer { translationX = shake.value },
            contentAlignment = Alignment.Center,
        ) {
            val left = (pending?.levels ?: 0) - store.levelsDone
            when {
                noMoney -> Text("Not enough coins - play levels to earn more!", style = label(14.sp, Clean.coralDark))
                pending != null && pending.levels > 0 -> Text(
                    "Finish $left more level${if (left == 1) "" else "s"} to unlock ${pending.name}",
                    style = label(14.sp, Clean.inkSoft), textAlign = TextAlign.Center,
                )
                pending != null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShopButton("Buy", Clean.teal, Modifier.weight(1f), price = pending.price) {
                        if (buy(pending)) {
                            app.sound.unlock(); equip(pending)
                            app.telemetry.spend("${pending.kind.name.lowercase()}_${pending.id}", pending.price)
                        } else notEnough()
                    }
                    if (Ads.rewarded.available) ShopButton("Watch ad", Clean.coral, Modifier.weight(1f), icon = Icon.VIDEO) { adFor = pending }
                }
                Ads.rewarded.available -> ShopButton("+80 Coins", Clean.coral, Modifier.width(220.dp), icon = Icon.VIDEO) { coinAd = true }
                else -> Text("Tap an item to try it on", style = label(14.sp, Clean.inkSoft))
            }
        }
    }
    adFor?.let { e ->
        FakeAdOverlay {
            when (e.kind) {
                Kind.BALL -> store.grantBall(e.id)
                Kind.TRAIL -> store.grantTrail(e.id)
                Kind.FINISH -> store.grantFinish(e.id)
                Kind.BOARD -> store.grantBoard(e.id)
                Kind.COLOR -> Unit
            }
            equip(e)
            app.sound.unlock()
            app.telemetry.rewardedAd("skin")
            adFor = null
        }
    }
    if (coinAd) FakeAdOverlay {
        store.addCoins(80)
        app.sound.unlock()
        app.telemetry.rewardedAd("shop_coins")
        app.telemetry.earn("coins", 80, "ad_shop")
        coinAd = false
    }
}

/** Rounded 3D button for the shop panel: an optional icon before the text, or a coin price after it. */
@Composable
private fun ShopButton(text: String, color: Color, modifier: Modifier = Modifier, price: Int? = null, icon: Icon? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.height(50.dp).clickable { tapFeedback(); onClick() }) {
        Box(Modifier.fillMaxWidth().height(46.dp).align(Alignment.BottomCenter).background(lerp(color, Color.Black, 0.3f), shape))
        Row(
            Modifier.fillMaxWidth().height(46.dp).background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.22f), color)), shape),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { GameIcon(icon, Color.White, size = 22.dp); Spacer(Modifier.width(8.dp)) }
            Text(text, style = TextStyle(fontFamily = Lilita, fontSize = 21.sp, color = Color.White))
            if (price != null) {
                Spacer(Modifier.width(10.dp)); CoinIcon(size = 22.dp); Spacer(Modifier.width(4.dp))
                Text("$price", style = TextStyle(fontFamily = Lilita, fontSize = 21.sp, color = Color.White))
            }
        }
    }
}

/** A shop tile: the item on a soft tinted square; a teal frame and tick when in use, a price/levels tag when locked. */
@Composable
private fun ShopCard(e: Entry, levelsDone: Int, tapped: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.aspectRatio(0.88f)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .shadow(if (tapped) 6.dp else 3.dp, shape, ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(lerp(e.tint, Color.White, 0.72f), lerp(e.tint, Color.White, 0.45f))))
                .then(
                    when {
                        e.equipped -> Modifier.border(3.dp, Clean.teal, shape)
                        tapped -> Modifier.border(2.dp, Clean.ink.copy(alpha = 0.35f), shape)
                        else -> Modifier
                    }
                )
                .clickable { tapFeedback(); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) { // soft bubbles behind the item
                val w = size.width
                drawCircle(Color.White.copy(alpha = 0.32f), w * 0.30f, Offset(w * 0.12f, w * 0.18f))
                drawCircle(Color.White.copy(alpha = 0.22f), w * 0.22f, Offset(w * 0.92f, w * 0.80f))
                drawCircle(Color.White.copy(alpha = 0.18f), w * 0.10f, Offset(w * 0.80f, w * 0.16f))
            }
            Box(Modifier.fillMaxSize(0.66f).offset(y = if (e.owned) 0.dp else (-6).dp), contentAlignment = Alignment.Center) {
                when (e.kind) {
                    Kind.BALL -> SkinSwatch(BallSkin.byId(e.id), Modifier.fillMaxSize(1.12f))
                    Kind.TRAIL -> TrailSwatch(e.id)
                    Kind.COLOR -> PaintSwatch(0, listOf(PaintColor.byId(e.id).rgb.color()))
                    Kind.FINISH -> PaintSwatch(e.id)
                    Kind.BOARD -> BoardSwatch(BoardTheme.byId(e.id), Modifier.fillMaxSize())
                }
            }
            if (e.kind == Kind.FINISH) Text(
                e.name, Modifier.align(Alignment.TopCenter).padding(top = 5.dp), style = label(10.sp, Clean.ink.copy(alpha = 0.7f)),
            )
        }
        when {
            e.equipped -> Box(
                Modifier.align(Alignment.BottomCenter).size(26.dp).background(Clean.teal, CircleShape).border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(Icon.CHECK, Color.White, size = 15.dp) }
            !e.owned -> Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.86f).height(26.dp)
                    .shadow(2.dp, RoundedCornerShape(13.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                    .background(Clean.pill, RoundedCornerShape(13.dp)).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (e.levels > 0) {
                    GameIcon(Icon.LOCK, Clean.inkSoft, size = 13.dp); Spacer(Modifier.width(3.dp))
                    Text("${levelsDone.coerceAtMost(e.levels)}/${e.levels} lvls", style = label(12.sp, Clean.ink), maxLines = 1)
                } else {
                    CoinIcon(size = 15.dp); Spacer(Modifier.width(3.dp))
                    Text("${e.price}", style = label(13.sp, Clean.ink))
                }
            }
        }
    }
}

/** A tiny carved board in a theme's colours, half painted. */
@Composable
private fun BoardSwatch(t: BoardTheme, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val o = Offset((size.width - s) / 2, (size.height - s) / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.12f), o + Offset(0f, s * 0.08f), Size(s, s * 0.92f), CornerRadius(s * 0.18f))
        drawRoundRect(t.sideBottom.color(), o + Offset(0f, s * 0.05f), Size(s, s * 0.92f), CornerRadius(s * 0.18f))
        drawRoundRect(t.sideTop.color(), o, Size(s, s * 0.9f), CornerRadius(s * 0.18f))
        clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(o.x, o.y, o.x + s, o.y + s * 0.9f, CornerRadius(s * 0.18f))) }) {
            val ink = lerp(t.sideTop.color(), Color.Black, 0.22f)
            when (t.pattern) {
                BoardTheme.WOOD -> for (k in 0 until 7) {
                    val y = o.y + s * (0.06f + k * 0.13f)
                    val path = Path().apply { moveTo(o.x, y); cubicTo(o.x + s * 0.3f, y - s * 0.05f, o.x + s * 0.6f, y + s * 0.06f, o.x + s, y - s * 0.02f) }
                    drawPath(path, ink.copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.018f))
                }
                BoardTheme.TERRAZZO -> {
                    val chips = listOf(Color(0xFFF27360), Color(0xFF40B3A8), Color(0xFFF5C24D), Color(0xFF8C8580))
                    for (k in 0 until 26) drawCircle(chips[k % 4], s * (0.018f + 0.012f * ((k * 7) % 3)), o + Offset(s * ((k * 0.377f) % 1f), s * 0.9f * ((k * 0.613f + 0.1f) % 1f)))
                }
                BoardTheme.MARBLE -> {
                    val path = Path().apply { moveTo(o.x, o.y + s * 0.2f); cubicTo(o.x + s * 0.35f, o.y + s * 0.05f, o.x + s * 0.5f, o.y + s * 0.7f, o.x + s, o.y + s * 0.55f) }
                    drawPath(path, ink.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.022f))
                }
            }
        }
        val cell = s * 0.16f
        fun ctr(i: Int, j: Int) = o + Offset(s * 0.1f + cell * (i + 0.5f), s * 0.05f + cell * (j + 0.5f))
        // the floor runs of a fixed little maze, the first three painted
        val runs = listOf(intArrayOf(0, 0, 3, 0), intArrayOf(0, 0, 0, 4), intArrayOf(0, 4, 4, 4), intArrayOf(3, 0, 3, 2), intArrayOf(3, 2, 4, 2), intArrayOf(4, 2, 4, 4))
        for (r in runs) drawLine(t.floor.color(), ctr(r[0], r[1]), ctr(r[2], r[3]), cell * 0.86f, StrokeCap.Round)
        for (r in runs.take(3)) drawLine(t.paints[0].color(), ctr(r[0], r[1]), ctr(r[2], r[3]), cell * 0.86f, StrokeCap.Round)
        drawCircle(Color.White, cell * 0.36f, ctr(4, 4))
    }
}

/** A ball skin's picture (the same art the game wraps round the 3D ball). */
@Composable
fun SkinSwatch(s: BallSkin, modifier: Modifier = Modifier) = com.ngstudio.mazebrush.ui.components.BallIcon(s.id, modifier)

/** A trail on a little dark lane behind the equipped ball, drawn the way the game shows it. */
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
                8 -> drawCircle((if (k % 2 == 0) Color(0xFF3FA9F5) else Color(0xFF1E7BE0)).copy(alpha = a + 0.3f), r * (0.7f + k * 0.12f), Offset(x, y + sin(k * 1.9f) * 7f))
                9 -> heart(Offset(x, y + sin(k * 1.4f) * 9f), r * 1.5f, Color(0xFFFF4F8B).copy(alpha = a + 0.35f))
                10 -> drawPath(starPath(x, y + sin(k * 1.3f) * 8f, r * 1.7f, r * 0.75f), Color(0xFFFFC21A).copy(alpha = a + 0.35f))
                11 -> drawCircle(Color(0xFFA9AEBC).copy(alpha = 0.14f + k * 0.06f), r * (1.7f + k * 0.12f), Offset(x, y - k * 1.5f))
                12 -> drawCircle(lerp(Color(0xFFFF3D12), Color(0xFFFFD23F), k / 6f).copy(alpha = a + 0.35f), r * (0.9f + k * 0.14f), Offset(x, y - sin(k * 1.1f) * 4f))
                13 -> shard(Offset(x, y + sin(k * 1.7f) * 7f), r * 2.1f, r * 0.8f, if (k % 2 == 0) Color(0xFF4FB8F0) else Color(0xFF8FDBFF), a + 0.35f)
                15 -> leaf(Offset(x, y + sin(k * 1.5f) * 9f), r * 2f, r * 0.9f, k * 47f, if (k % 2 == 0) Color(0xFF4CAF50) else Color(0xFF7BC74D), a + 0.35f)
                16 -> flower(Offset(x, y + sin(k * 1.4f) * 8f), r * 1.3f, if (k % 2 == 0) Color(0xFFFF6FB5) else Color(0xFFE84FA0), a + 0.35f)
                17 -> if (k % 2 == 1) note(Offset(x, y + sin(k * 1.2f) * 9f), r * 1.6f, Color(0xFF8A4DE8).copy(alpha = a + 0.4f))
                18 -> shard(Offset(x, y + sin(k * 2.1f) * 8f), r * 2.6f, r * 1.0f, if (k % 2 == 0) Color(0xFF2A6BE0) else Color(0xFF6FB2FF), a + 0.4f)
                19 -> drawCircle((if (k % 2 == 0) Color(0xFF00C8E5) else Color(0xFFFF4FD8)).copy(alpha = a + 0.35f), r * 1.6f, Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                20 -> {
                    drawCircle(Color(0xFF7A3BD6).copy(alpha = 0.2f + k * 0.06f), r * (1.6f + k * 0.1f), Offset(x, y))
                    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.28f, Offset(x + r * 0.6f, y - r * (if (k % 2 == 0) 0.9f else -0.8f)))
                }
                21 -> snowflake(Offset(x, y + sin(k * 1.6f) * 9f), r * 1.4f, Color(0xFF6FB4E8).copy(alpha = a + 0.4f))
                22 -> petal(Offset(x, y + sin(k * 1.8f) * 9f), r * 1.6f, k * 55f, (if (k % 2 == 0) Color(0xFFFF6FA8) else Color(0xFFF2588F)).copy(alpha = a + 0.35f))
                23 -> drawCircle(lerp(Color(0xFF3FD8E0), Color(0xFFB06CFF), k / 6f).copy(alpha = 0.2f + k * 0.07f), r * 1.9f, Offset(x, y + sin(k * 1.1f) * 9f))
            }
        }
        if (id == 14) {   // electric: a violet zigzag bolt with a white core
            val pts = listOf(0.1f to 0.0f, 0.24f to -0.12f, 0.36f to 0.08f, 0.5f to -0.1f, 0.62f to 0.06f, 0.72f to -0.02f)
            val path = Path().apply { pts.forEachIndexed { i, (px, py) -> val o = Offset(size.width * px, y + size.height * py); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) } }
            drawPath(path, Color(0xFF8A4DFF).copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * 0.06f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawPath(path, Color(0xFFE9D8FF), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * 0.018f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
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
private fun PaintSwatch(id: Int, colors: List<Color>? = null) {
    Canvas(Modifier.fillMaxSize(0.8f)) {
        val base = Color(0xFFFFD93D)
        val brush = if (colors != null) {
            if (colors.size == 1) Brush.verticalGradient(listOf(lerp(colors[0], Color.White, 0.18f), colors[0]))
            else Brush.sweepGradient(colors + colors[0])
        } else when (id) {
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

/** Live 3D preview: the real renderer with the selected ball/trail/paint/maze, rolling round a small ring. */
@Composable
private fun SkinPreview(modifier: Modifier, ball: Int, trail: Int, finish: Int, paint: Int, boardId: Int) {
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
    LaunchedEffect(ball, trail, finish, paint, boardId) {
        val theme = WorldTheme.ALL[0]
        val board = BoardTheme.byId(boardId)
        val pc = PaintColor.byId(paint)
        renderer.setSafeArea(0f, 0f)
        while (true) {
            renderer.setScene(SceneConfig(previewLevel, 1, theme, BallSkin.byId(ball), TrailStyle.byId(trail), PaintFinish.byId(finish), pc.first(board, 2), pc.second(board, 2), instant = true, board = board))
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

// ---- little shapes for the trail pictures
private fun androidx.compose.ui.graphics.drawscope.DrawScope.heart(c: Offset, r: Float, col: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.3f, c.y + r * 0.1f, c.x - r * 0.9f, c.y - r * 1.0f, c.x, c.y - r * 0.35f)
        cubicTo(c.x + r * 0.9f, c.y - r * 1.0f, c.x + r * 1.3f, c.y + r * 0.1f, c.x, c.y + r * 0.9f); close()
    }
    drawPath(p, col)
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.shard(c: Offset, len: Float, w: Float, col: Color, a: Float) {
    val top = Path().apply { moveTo(c.x - len / 2, c.y); lineTo(c.x, c.y - w / 2); lineTo(c.x + len / 2, c.y); close() }
    val bottom = Path().apply { moveTo(c.x - len / 2, c.y); lineTo(c.x, c.y + w / 2); lineTo(c.x + len / 2, c.y); close() }
    drawPath(top, lerp(col, Color.White, 0.35f).copy(alpha = a.coerceAtMost(1f)))
    drawPath(bottom, col.copy(alpha = a.coerceAtMost(1f)))
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.leaf(c: Offset, len: Float, w: Float, deg: Float, col: Color, a: Float) {
    rotate(deg, c) {
        drawOval(col.copy(alpha = a.coerceAtMost(1f)), Offset(c.x - len / 2, c.y - w / 2), Size(len, w))
        drawLine(lerp(col, Color.Black, 0.25f).copy(alpha = a.coerceAtMost(1f)), Offset(c.x - len * 0.45f, c.y), Offset(c.x + len * 0.45f, c.y), 1.5f)
    }
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.flower(c: Offset, r: Float, col: Color, a: Float) {
    for (i in 0 until 5) {
        val ang = i * 72f * PI.toFloat() / 180f
        drawCircle(col.copy(alpha = a.coerceAtMost(1f)), r * 0.5f, c + Offset(cos(ang) * r * 0.55f, sin(ang) * r * 0.55f))
    }
    drawCircle(Color(0xFFFFD84A), r * 0.32f, c)
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.note(c: Offset, r: Float, col: Color) {
    drawOval(col, Offset(c.x - r * 0.75f, c.y + r * 0.2f), Size(r * 0.8f, r * 0.6f))
    drawLine(col, Offset(c.x, c.y + r * 0.45f), Offset(c.x, c.y - r * 0.9f), r * 0.16f)
    drawLine(col, Offset(c.x, c.y - r * 0.9f), Offset(c.x + r * 0.5f, c.y - r * 0.5f), r * 0.16f, StrokeCap.Round)
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.snowflake(c: Offset, r: Float, col: Color) {
    for (i in 0 until 6) {
        val ang = i * PI.toFloat() / 3f
        val e = c + Offset(cos(ang) * r, sin(ang) * r)
        drawLine(col, c, e, 2.2f, StrokeCap.Round)
        val m = c + Offset(cos(ang) * r * 0.55f, sin(ang) * r * 0.55f)
        drawLine(col, m, m + Offset(cos(ang + 0.7f) * r * 0.3f, sin(ang + 0.7f) * r * 0.3f), 1.6f, StrokeCap.Round)
        drawLine(col, m, m + Offset(cos(ang - 0.7f) * r * 0.3f, sin(ang - 0.7f) * r * 0.3f), 1.6f, StrokeCap.Round)
    }
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.petal(c: Offset, r: Float, deg: Float, col: Color) {
    rotate(deg, c) {
        val p = Path().apply {
            moveTo(c.x - r * 0.8f, c.y)
            cubicTo(c.x - r * 0.2f, c.y - r * 0.7f, c.x + r * 0.7f, c.y - r * 0.5f, c.x + r * 0.8f, c.y)
            cubicTo(c.x + r * 0.7f, c.y + r * 0.5f, c.x - r * 0.2f, c.y + r * 0.7f, c.x - r * 0.8f, c.y); close()
        }
        drawPath(p, col)
    }
}
