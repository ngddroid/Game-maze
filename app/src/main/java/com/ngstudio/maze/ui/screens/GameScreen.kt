package com.ngstudio.maze.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.ngstudio.maze.ui.components.rememberPulse
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ngstudio.maze.BuildConfig
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.audio.SoundManager
import com.ngstudio.maze.core.Difficulty
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Mechanic
import com.ngstudio.maze.core.Physics
import com.ngstudio.maze.core.Slide
import com.ngstudio.maze.data.GameStore
import com.ngstudio.maze.game.DebugFlags
import com.ngstudio.maze.game.GameController
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
import com.ngstudio.maze.ui.components.CounterPill
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.EngravedText
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.HSpace
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.PaintProgressBar
import com.ngstudio.maze.ui.components.Panel
import com.ngstudio.maze.ui.components.Ribbon
import com.ngstudio.maze.ui.components.RoundButton
import com.ngstudio.maze.ui.components.Scrim
import com.ngstudio.maze.ui.components.StarIcon
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.components.rememberPopIn
import com.ngstudio.maze.ui.theme.Candy
import com.ngstudio.maze.ui.theme.Pro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** What is being played: a campaign level or today's daily challenge. */
sealed class PlayMode {
    data class Campaign(val level: Int) : PlayMode()
    data class Daily(val day: Long) : PlayMode()
}

private class WinInfo(val stars: Int, val moves: Int, val par: Int, val reward: GameStore.Reward?, val dailyCoins: Int)

const val HINT_PRICE = 60
const val MOVES_PRICE = 80

/** Moves left, turns red and pulses when 3 or fewer remain. */
@Composable
private fun MovesPill(left: Int) {
    val low = left <= 3
    val pulse = rememberPulse(500, 0.08f)
    Row(
        Modifier.scale(if (low) pulse else 1f).height(34.dp).shadow(3.dp, RoundedCornerShape(17.dp))
            .background(if (low) Candy.pinkDark else Pro.glass, RoundedCornerShape(17.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(17.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText("MOVES", size = 11.sp, color = Pro.creamDim, shadow = false)
        HSpace(6.dp)
        GameText("$left", 22.sp, color = Pro.cream, outline = Color.Transparent, outlineWidth = 0f)
    }
}

@Composable
private fun OutOfMoves(store: GameStore, tilesLeft: Int, canUndo: Boolean, onUndo: () -> Unit, onBuy: () -> Unit, onAd: () -> Unit, onRetry: () -> Unit) {
    Scrim(alpha = 0.5f) {
        val pop = rememberPopIn()
        Panel(Modifier.width(320.dp).scale(pop)) {
            Ribbon("OUT OF MOVES", color = Candy.pink, dark = Candy.pinkDark)
            VSpace(12.dp)
            BodyText(if (tilesLeft == 1) "Only 1 tile left to paint!" else "$tilesLeft tiles left to paint.", size = 17.sp, align = TextAlign.Center)
            BodyText("Plan a shorter route, or get more moves.", size = 13.sp, color = Candy.greyDark, align = TextAlign.Center)
            VSpace(14.dp)
            if (Ads.rewarded.available) {
                CandyButton("+5 MOVES", Modifier.fillMaxWidth(), color = Candy.pink, dark = Candy.pinkDark, icon = Icon.VIDEO) { onAd() }
                VSpace(8.dp)
            }
            CandyButton("+5 MOVES  $MOVES_PRICE", Modifier.fillMaxWidth(), enabled = store.coins >= MOVES_PRICE, color = Pro.gold, dark = Pro.goldDark, icon = Icon.COIN) { onBuy() }
            VSpace(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CandyButton("UNDO", Modifier.weight(1f), height = 50.dp, textSize = 20.sp, enabled = canUndo, color = Candy.sky, dark = Candy.skyDark, icon = Icon.UNDO) { onUndo() }
                CandyButton("RETRY", Modifier.weight(1f), height = 50.dp, textSize = 20.sp, icon = Icon.RESTART) { onRetry() }
            }
        }
    }
}

@Composable
fun GameScreen(mode: PlayMode, onHome: () -> Unit) {
    val app = MazeApp.instance
    val store = app.store
    val sound = app.sound
    val haptics = app.haptics
    val isDaily = mode is PlayMode.Daily
    var levelNumber by remember { mutableIntStateOf((mode as? PlayMode.Campaign)?.level ?: 0) }
    var attempt by remember { mutableIntStateOf(0) }
    val level: Level = remember(levelNumber, attempt) {
        if (mode is PlayMode.Daily) app.levels.dailyLevel(mode.day) else app.levels.level(levelNumber)
    }
    val theme = remember(levelNumber) {
        if (mode is PlayMode.Daily) WorldTheme.ALL[(mode.day % WorldTheme.ALL.size).toInt()] else WorldTheme.forLevel(levelNumber)
    }
    var paused by remember { mutableStateOf(false) }
    var win by remember { mutableStateOf<WinInfo?>(null) }
    var winVisible by remember { mutableStateOf(false) }
    var showBuyHint by remember { mutableStateOf(false) }
    var introDone by remember { mutableStateOf(false) }
    var swiped by remember { mutableStateOf(false) }
    var coachDismissed by remember(levelNumber) { mutableStateOf(false) }

    var lastMoveAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nudge by remember { mutableStateOf(false) }
    var adForHint by remember { mutableStateOf(false) }
    var undoTip by remember { mutableStateOf(false) }
    var adForMoves by remember { mutableStateOf(false) }
    // The HUD coin counter lags behind the real balance so it can roll up when the flying coins land.
    val coinShown = remember { Animatable(store.coins.toFloat()) }
    var coinTarget by remember { mutableStateOf(Offset.Zero) }
    var flight by remember { mutableStateOf<CoinFlightSpec?>(null) }
    var flightId by remember { mutableIntStateOf(0) }

    val controllerHolder = remember { arrayOfNulls<GameController>(1) }
    val renderer = remember {
        GameRenderer(object : RenderListener {
            override fun onCellPainted(cell: Int, indexInMove: Int) {
                controllerHolder[0]?.onCellPainted()
                sound.paint(indexInMove)
                haptics.tick()
            }
            override fun onMoveFinished(endCell: Int, cells: Int) {
                val c = controllerHolder[0] ?: return
                if (c.isBusy && c.position == endCell) {
                    // end of a forward move or an undo
                    c.onUndoFinished()
                    c.onMoveFinished()
                }
            }
            override fun onImpact(strength: Float) { sound.thud(strength); haptics.impact(strength) }
            override fun onTeleport() = sound.teleport()
            override fun onIntroDone() { introDone = true; lastMoveAt = System.currentTimeMillis() }
            override fun onWinShown() { winVisible = true }
        })
    }

    val controller = remember(level, attempt) {
        val limit = if (mode is PlayMode.Daily) Difficulty.dailyMoveLimit(level.par) else Difficulty.moveLimit(levelNumber, level.par)
        GameController(level, limit, object : GameController.Callbacks {
            override fun playSlide(slide: Slide) { renderer.playSlide(slide); swiped = true; lastMoveAt = System.currentTimeMillis(); nudge = false; undoTip = false }
            override fun playUndo(slide: Slide, unpaint: IntArray) { renderer.playUndo(slide, unpaint); sound.undo() }
            override fun onBlocked(dir: Dir) { sound.bonk(); haptics.light() }
            override fun onOutOfMoves() { sound.bonk(); haptics.impact(0.6f) }
            override fun onWin(moves: Int, stars: Int) {
                if (BuildConfig.DEBUG) android.util.Log.i("MazeQA", "WIN level=${if (isDaily) "daily" else levelNumber} moves=$moves par=${level.par} stars=$stars")
                sound.win(); haptics.win()
                renderer.playWin()
                val reward = if (mode is PlayMode.Campaign) store.completeLevel(levelNumber, stars, level.floorCount, moves) else null
                val daily = if (isDaily) store.completeDaily() else 0
                win = WinInfo(stars, moves, level.par, reward, daily)
            }
        }).also { controllerHolder[0] = it }
    }

    // push scene whenever the level (or attempt) changes
    LaunchedEffect(level, attempt) {
        win = null; winVisible = false; introDone = false
        if (BuildConfig.DEBUG) android.util.Log.i("MazeQA", "START level=${if (isDaily) "daily" else levelNumber} par=${level.par} size=${level.width}x${level.height}")
        renderer.setScene(
            SceneConfig(
                level, levelNumber, theme, BallSkin.byId(store.ballSkin), TrailStyle.byId(store.trail), PaintFinish.byId(store.finish),
                theme.paintFor(if (isDaily) 3 else levelNumber), theme.paintFor((if (isDaily) 3 else levelNumber) + 2),
            )
        )
        sound.whoosh()
    }
    LaunchedEffect(Unit) { sound.music(SoundManager.Music.GAME) }

    // debug autoplay: play the stored solution
    LaunchedEffect(level, attempt, introDone, DebugFlags.autoplay) {
        if (!BuildConfig.DEBUG || !DebugFlags.autoplay || !introDone) return@LaunchedEffect
        delay(300)
        for (d in level.solutionDirs()) {
            while (controller.isBusy) delay(30)
            controller.swipe(d)
            delay(60)
        }
    }

    // lifecycle for the GL view
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val glView = remember { arrayOfNulls<GameGLView>(1) }
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> glView[0]?.onResume()
                Lifecycle.Event.ON_PAUSE -> { glView[0]?.onPause(); if (win == null) paused = true }
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    BackHandler {
        if (showBuyHint) showBuyHint = false
        else if (win != null) onHome()
        else paused = !paused
    }

    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    LaunchedEffect(topInset, bottomInset) {
        with(density) { renderer.setSafeArea(topInset + 150.dp.toPx(), bottomInset + 118.dp.toPx()) }
    }

    val ctrl by rememberUpdatedState(controller)
    val scope = rememberCoroutineScope()

    // "Stuck?" nudge: from level 11 on, 15 s without a move makes the Hint button pulse with a tip.
    LaunchedEffect(level, attempt) {
        while (true) {
            delay(1000)
            nudge = !isDaily && levelNumber > 10 && introDone && !paused && win == null && !showBuyHint &&
                System.currentTimeMillis() - lastMoveAt > 15_000
        }
    }
    LaunchedEffect(store.coins, win) { if (win == null && flight == null) coinShown.snapTo(store.coins.toFloat()) }

    fun useHint() {
        val c = ctrl
        if (c.finished || c.isBusy) return
        when (val advice = c.hint()) {
            is com.ngstudio.maze.game.Hints.Advice.Undo -> { undoTip = true; sound.hint(); return }
            is com.ngstudio.maze.game.Hints.Advice.None -> return
            is com.ngstudio.maze.game.Hints.Advice.Move -> Unit
        }
        if (store.hints <= 0) { showBuyHint = true; return }
        val d = (c.hint() as? com.ngstudio.maze.game.Hints.Advice.Move)?.dir ?: return
        val s = Physics.slide(level, c.position, d) ?: return
        undoTip = false
        store.useHint()
        sound.hint()
        renderer.showHint(s.cells, d)
        lastMoveAt = System.currentTimeMillis(); nudge = false
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx -> GameGLView(ctx, renderer).also { glView[0] = it } }, modifier = Modifier.fillMaxSize())

        // swipe input
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                val threshold = 22.dp.toPx()
                var acc = Offset.Zero
                var fired = false
                detectDragGestures(
                    onDragStart = { acc = Offset.Zero; fired = false },
                    onDragEnd = { fired = false },
                    onDragCancel = { fired = false },
                ) { change, drag ->
                    change.consume()
                    if (fired || paused || win != null) return@detectDragGestures
                    acc += drag
                    if (abs(acc.x) > threshold || abs(acc.y) > threshold) {
                        fired = true
                        val d = if (abs(acc.x) > abs(acc.y)) (if (acc.x > 0) Dir.RIGHT else Dir.LEFT) else (if (acc.y > 0) Dir.DOWN else Dir.UP)
                        ctrl.swipe(d)
                    }
                }
            },
        )

        // ---- top HUD
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Icon.PAUSE, size = 48.dp) { paused = true }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EngravedText(if (isDaily) "DAILY" else "LEVEL $levelNumber", 36.sp, theme.ink.color())
                        BodyText(if (isDaily) "Daily Challenge" else theme.name, size = 14.sp, color = theme.ink.color().copy(alpha = 0.8f), align = TextAlign.Center)
                    }
                }
                CounterPill(coinShown.value.toInt().toString(), Modifier.onGloballyPositioned { coinTarget = it.boundsInRoot().center }) { CoinIcon(size = 26.dp) }
            }
            VSpace(10.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PaintProgressBar(controller.progress, theme.paintFor(if (isDaily) 3 else levelNumber).color(), Modifier.weight(1f))
                HSpace(10.dp)
                val left = controller.movesLeft
                if (left == null) EngravedText("${(controller.progress * 100).toInt()}%", 18.sp, theme.ink.color())
                else MovesPill(left)
            }
            VSpace(6.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                val m = controller.moves
                val par = level.par
                for (k in 3 downTo 1) {
                    val lit = controller.starsFor(m.coerceAtLeast(0)) >= k
                    StarIcon(lit, size = 18.dp, modifier = Modifier.alpha(if (lit) 1f else 0.7f))
                }
                HSpace(8.dp)
                BodyText(if (controller.limit == null) "Moves $m  ·  Par $par" else "Par $par  ·  ${controller.tilesLeft} tiles left", size = 15.sp, color = theme.ink.color())
            }
        }

        // ---- bottom buttons
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val undoPulse = rememberPulse(600, 0.12f)
                RoundButton(Icon.UNDO, Modifier.scale(if (undoTip) undoPulse else 1f), size = 58.dp, iconColor = if (controller.canUndo) Pro.cream else Pro.cream.copy(alpha = 0.35f)) { undoTip = false; ctrl.undo() }
                BodyText("Undo", size = 12.sp, color = theme.ink.color())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundButton(Icon.RESTART, size = 58.dp) { if (!ctrl.isBusy || ctrl.finished) attempt++ }
                BodyText("Restart", size = 12.sp, color = theme.ink.color())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pulse = rememberPulse(600, 0.12f)
                RoundButton(Icon.HINT, Modifier.scale(if (nudge) pulse else 1f), size = 58.dp, color = Pro.gold, iconColor = Pro.text, dark = Pro.goldDark,
                    badge = if (store.hints > 0) store.hints.toString() else "+", badgeColor = if (store.hints > 0) Candy.pink else Candy.mint) { useHint() }
                BodyText("Hint", size = 12.sp, color = theme.ink.color())
            }
        }

        if (undoTip) Box(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(bottom = 122.dp, start = 14.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp)).background(Pro.glass, RoundedCornerShape(14.dp))
                .border(1.dp, Pro.gold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        ) { BodyText("Off the best route - undo a few moves", size = 14.sp, color = Pro.cream) }

        if (nudge) Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 122.dp, end = 14.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp)).background(Pro.glass, RoundedCornerShape(14.dp))
                .border(1.dp, Pro.gold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        ) { BodyText("Stuck? Try a hint", size = 14.sp, color = Pro.cream) }

        // ---- tutorial hand on the first two levels
        if (!isDaily && levelNumber <= 2 && introDone && !swiped && controller.moves == 0) {
            val dir = level.solutionDirs().firstOrNull() ?: Dir.RIGHT
            TutorialHand(dir, if (levelNumber == 1) "Swipe to glide!" else "Paint every tile!")
        }

        // ---- coach mark for a new tile type
        val intro = level.intro
        if (!isDaily && intro != null && introDone && !coachDismissed && controller.moves == 0) {
            CoachMark(intro) { coachDismissed = true }
        }

        if (paused && win == null) PauseOverlay(
            store = store,
            onResume = { paused = false },
            onRestart = { paused = false; attempt++ },
            onHome = onHome,
        )

        if (controller.outOfMoves && win == null && !adForMoves) OutOfMoves(
            store = store, tilesLeft = controller.tilesLeft, canUndo = controller.canUndo,
            onUndo = { ctrl.undo() },
            onBuy = { if (store.spend(MOVES_PRICE)) { sound.coin(); ctrl.addMoves(5) } },
            onAd = { adForMoves = true },
            onRetry = { attempt++ },
        )
        if (adForMoves) FakeAdOverlay { adForMoves = false; ctrl.addMoves(5); sound.unlock() }

        if (showBuyHint) BuyHintDialog(store, onClose = { showBuyHint = false }, onAd = { showBuyHint = false; adForHint = true }) {
            if (store.buyHint(HINT_PRICE)) { sound.coin(); showBuyHint = false; useHint() }
        }
        if (adForHint) FakeAdOverlay { store.addHint(1); adForHint = false; sound.unlock(); useHint() }

        val w = win
        AnimatedVisibility(w != null && winVisible, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
            if (w != null) LevelComplete(
                info = w, isDaily = isDaily, levelNumber = levelNumber, theme = theme,
                onNext = {
                    if (isDaily || levelNumber >= app.levels.count) onHome()
                    else { levelNumber += 1; attempt = 0; swiped = false }
                },
                onReplay = { attempt++ },
                onHome = onHome,
                onCoinsFrom = { from -> flight = CoinFlightSpec(from, coinTarget, ++flightId) },
            )
        }
        flight?.let { f ->
            CoinFlight(f, onArrive = { sound.coin(); scope.launch { coinShown.animateTo(store.coins.toFloat(), tween(550)) } }) {
                if (flight === f) flight = null
            }
        }
    }
}

private class CoinFlightSpec(val from: Offset, val to: Offset, val id: Int)

/** Coins fly from the win panel into the HUD coin counter. */
@Composable
private fun CoinFlight(spec: CoinFlightSpec, onArrive: () -> Unit, onDone: () -> Unit) {
    val n = 9
    val anims = remember(spec.id) { List(n) { Animatable(0f) } }
    LaunchedEffect(spec.id) {
        launch { delay(560); onArrive() }
        anims.forEachIndexed { i, a -> launch { delay(i * 55L); a.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) } }
        delay(n * 55L + 560)
        onDone()
    }
    val density = LocalDensity.current
    val sizePx = with(density) { 26.dp.toPx() }
    Box(Modifier.fillMaxSize()) {
        anims.forEachIndexed { i, a ->
            val t = a.value
            if (t > 0f && t < 1f) {
                val start = spec.from + Offset((i - n / 2) * 14f, ((i * 37) % 5 - 2) * 8f)
                val ctrl = Offset((start.x + spec.to.x) / 2f + (i - n / 2) * 26f, minOf(start.y, spec.to.y) - 180f)
                val u = 1f - t
                val p = start * (u * u) + ctrl * (2f * u * t) + spec.to * (t * t)
                CoinIcon(Modifier.offset { IntOffset((p.x - sizePx / 2).toInt(), (p.y - sizePx / 2).toInt()) }.scale(1f - 0.35f * t), size = 26.dp)
            }
        }
    }
}

@Composable
private fun TutorialHand(dir: Dir, text: String) {
    val t by rememberInfiniteTransition(label = "hand").animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ht")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val travel = 90f
        val k = (t * 1.4f).coerceAtMost(1f)
        val dx = dir.dx * travel * k
        val dy = dir.dy * travel * k
        Column(Modifier.padding(top = 260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp)) {
                    val c = center
                    // trail
                    drawLine(Color.White.copy(alpha = 0.55f * (1f - t)), c - Offset(dir.dx * travel, dir.dy * travel) * 0.5f, c + Offset(dx, dy) - Offset(dir.dx * travel, dir.dy * travel) * 0.5f, strokeWidth = 14f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    val hp = c + Offset(dx, dy) - Offset(dir.dx * travel, dir.dy * travel) * 0.5f
                    drawCircle(Color.White.copy(alpha = 0.9f * (1f - t * 0.6f)), 26f, hp)
                    drawCircle(Candy.pink.copy(alpha = 0.9f), 26f, hp, style = Stroke(6f))
                }
            }
            Box(Modifier.shadow(6.dp, RoundedCornerShape(16.dp)).background(Color.White, RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 8.dp)) {
                GameText(text, 22.sp, color = Candy.pink, outline = Color.White, outlineWidth = 0.05f)
            }
        }
    }
}

@Composable
private fun CoachMark(m: Mechanic, onDismiss: () -> Unit) {
    val (title, body) = when (m) {
        Mechanic.STOPPER -> "Stopper Pad" to "The ball stops right on top of this pad. Use it to turn where there is no wall."
        Mechanic.ONE_WAY -> "One-way Gate" to "You can roll through it only in the arrow's direction. From any other side it is a wall."
        Mechanic.DEFLECTOR -> "Bumper" to "The ball bounces off the bumper and turns 90 degrees without stopping."
        Mechanic.PORTAL -> "Portal" to "Roll into a portal and pop out of its twin, still rolling the same way."
    }
    val pop = rememberPopIn()
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Panel(Modifier.padding(horizontal = 22.dp).scale(pop), padding = 16.dp) {
            Ribbon("NEW TILE!", color = Candy.sky, dark = Candy.skyDark)
            VSpace(10.dp)
            GameText(title, 26.sp, color = Candy.pink, outline = Color.White, outlineWidth = 0.05f)
            VSpace(4.dp)
            BodyText(body, align = TextAlign.Center)
            VSpace(12.dp)
            CandyButton("GOT IT", Modifier.width(180.dp), height = 50.dp, textSize = 22.sp) { onDismiss() }
        }
    }
}

@Composable
private fun PauseOverlay(store: GameStore, onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    Scrim(onDismiss = onResume) {
        val pop = rememberPopIn()
        Panel(Modifier.width(310.dp).scale(pop)) {
            Ribbon("PAUSED")
            VSpace(18.dp)
            CandyButton("RESUME", Modifier.fillMaxWidth(), icon = Icon.PLAY) { onResume() }
            VSpace(10.dp)
            CandyButton("RESTART", Modifier.fillMaxWidth(), color = Candy.sky, dark = Candy.skyDark, icon = Icon.RESTART) { onRestart() }
            VSpace(10.dp)
            CandyButton("HOME", Modifier.fillMaxWidth(), color = Candy.pink, dark = Candy.pinkDark, icon = Icon.HOME) { onHome() }
            VSpace(16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToggleRound(Icon.SOUND, store.sound) { store.toggleSound() }
                ToggleRound(Icon.MUSIC, store.music) { store.toggleMusic(); MazeApp.instance.sound.refreshMusic() }
                ToggleRound(Icon.VIBRATE, store.vibration) { store.toggleVibration() }
            }
        }
    }
}

@Composable
fun ToggleRound(icon: Icon, on: Boolean, onClick: () -> Unit) {
    Box {
        RoundButton(icon, size = 52.dp, color = if (on) Candy.mint else Candy.grey, dark = if (on) Candy.mintDark else Candy.greyDark, iconColor = Color.White, onClick = onClick)
        if (!on) Canvas(Modifier.size(58.dp)) {
            drawLine(Color.White, Offset(size.width * 0.25f, size.height * 0.25f), Offset(size.width * 0.75f, size.height * 0.75f), 7f, androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun BuyHintDialog(store: GameStore, onClose: () -> Unit, onAd: () -> Unit, onBuy: () -> Unit) {
    Scrim(onDismiss = onClose) {
        val pop = rememberPopIn()
        Panel(Modifier.width(300.dp).scale(pop)) {
            Ribbon("NEED A HINT?", color = Candy.lemon, dark = Candy.lemonDark)
            VSpace(14.dp)
            GameIcon(Icon.HINT, Candy.lemon, size = 64.dp)
            VSpace(8.dp)
            BodyText("Get 1 hint for $HINT_PRICE coins.\nYou earn a free hint with every chapter chest.", align = TextAlign.Center)
            VSpace(14.dp)
            CandyButton("BUY  $HINT_PRICE", Modifier.fillMaxWidth(), enabled = store.coins >= HINT_PRICE, color = Candy.lemon, dark = Candy.lemonDark, icon = Icon.COIN) { onBuy() }
            if (Ads.rewarded.available) {
                VSpace(8.dp)
                CandyButton("FREE HINT", Modifier.fillMaxWidth(), color = Candy.pink, dark = Candy.pinkDark, icon = Icon.VIDEO) { onAd() }
                BodyText("Watch a short ad", size = 12.sp, color = Candy.greyDark)
            }
            VSpace(8.dp)
            CandyButton("NOT NOW", Modifier.fillMaxWidth(), height = 48.dp, textSize = 20.sp, color = Candy.grey, dark = Candy.greyDark) { onClose() }
        }
    }
}

@Composable
private fun LevelComplete(
    info: WinInfo, isDaily: Boolean, levelNumber: Int, theme: WorldTheme,
    onNext: () -> Unit, onReplay: () -> Unit, onHome: () -> Unit, onCoinsFrom: (Offset) -> Unit,
) {
    val sound = MazeApp.instance.sound
    val store = MazeApp.instance.store
    val coinsTotal = (info.reward?.coins ?: 0) + info.dailyCoins
    val coinAnim = remember { Animatable(0f) }
    var coinPos by remember { mutableStateOf(Offset.Zero) }
    var coinsLanded by remember { mutableStateOf(false) }
    var doubled by remember { mutableStateOf(false) }
    var adPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        for (i in 1..info.stars) { delay(if (i == 1) 380 else 260); sound.star(i) }
        delay(250)
        if (coinsTotal > 0) {
            sound.coin(); coinAnim.animateTo(coinsTotal.toFloat(), tween(600))
            onCoinsFrom(coinPos); coinsLanded = true
        }
        if ((info.reward?.chest ?: 0) > 0) { delay(200); sound.chest() }
    }
    Scrim(alpha = 0.45f) {
        val pop = rememberPopIn()
        Column(Modifier.padding(horizontal = 22.dp).scale(pop), horizontalAlignment = Alignment.CenterHorizontally) {
            val title = when (info.stars) { 3 -> "PERFECT!"; 2 -> "GREAT!"; else -> "NICE!" }
            GameText(title, 54.sp, color = Candy.lemon, outline = Candy.pinkDark)
            VSpace(4.dp)
            Panel(Modifier.fillMaxWidth()) {
                GameText(if (isDaily) "DAILY DONE" else "LEVEL $levelNumber", 26.sp, color = Candy.pink, outline = Color.White, outlineWidth = 0.04f)
                VSpace(10.dp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    for (i in 1..3) {
                        val p = rememberPopIn(380 + (i - 1) * 260)
                        val lit = i <= info.stars
                        StarIcon(lit && p > 0.05f, size = if (i == 2) 76.dp else 60.dp,
                            modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = if (i == 2) 14.dp else 0.dp)
                                .scale(if (lit) p.coerceAtLeast(0.01f) else 1f).rotate(if (lit) (1f - p) * -40f else 0f))
                    }
                }
                VSpace(8.dp)
                BodyText("Moves ${info.moves}  ·  Par ${info.par}", size = 14.sp, color = Candy.greyDark)
                VSpace(12.dp)
                if (coinsTotal > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.onGloballyPositioned { coinPos = it.boundsInRoot().center }) {
                        CoinIcon(size = 34.dp)
                        HSpace(8.dp)
                        GameText("+${coinAnim.value.toInt()}", 34.sp, color = Candy.gold, outline = Candy.goldDark)
                    }
                }
                val streakBonus = info.reward?.streakBonus ?: 0
                if (streakBonus > 0) {
                    VSpace(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarIcon(true, size = 22.dp)
                        HSpace(6.dp)
                        BodyText("Perfect streak x${info.reward?.streak}:  +$streakBonus coins", size = 14.sp, color = Candy.mintDark)
                    }
                }
                val chest = info.reward?.chest ?: 0
                if (chest > 0) {
                    VSpace(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon(Icon.CHEST, Candy.orange, size = 26.dp)
                        HSpace(6.dp)
                        BodyText("Chapter chest: +$chest coins  +1 hint", size = 14.sp, color = Candy.orangeDark)
                    }
                }
                if (isDaily) {
                    VSpace(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon(Icon.FLAME, Candy.orange, size = 24.dp)
                        HSpace(6.dp)
                        BodyText("Daily streak: ${MazeApp.instance.store.dailyStreak}", size = 15.sp, color = Candy.orangeDark)
                    }
                }
                if (Ads.rewarded.available && coinsLanded && !doubled && coinsTotal > 0) {
                    VSpace(12.dp)
                    CandyButton("x2 COINS", Modifier.fillMaxWidth(), height = 52.dp, textSize = 22.sp, color = Candy.pink, dark = Candy.pinkDark, icon = Icon.VIDEO) { adPlaying = true }
                }
                VSpace(16.dp)
                CandyButton(if (isDaily) "HOME" else "NEXT", Modifier.fillMaxWidth(), height = 66.dp, textSize = 30.sp, icon = if (isDaily) Icon.HOME else Icon.NEXT) { onNext() }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    RoundButton(Icon.RESTART, size = 50.dp) { onReplay() }
                    RoundButton(Icon.HOME, size = 50.dp) { onHome() }
                }
            }
        }
    }
    if (adPlaying) FakeAdOverlay {
        store.addCoins(coinsTotal)
        doubled = true
        adPlaying = false
        scope.launch { coinAnim.animateTo(coinsTotal * 2f, tween(600)) }
        onCoinsFrom(coinPos)
    }
}

