package com.ngstudio.mazebrush.ui.screens

import androidx.compose.foundation.layout.requiredHeight
import com.ngstudio.mazebrush.ui.components.ItemSwatch
import com.ngstudio.mazebrush.ui.components.LevelTitle
import com.ngstudio.mazebrush.ui.components.NewItemBar
import com.ngstudio.mazebrush.ui.components.LabeledTealButton
import com.ngstudio.mazebrush.ui.components.PaintBadge
import com.ngstudio.mazebrush.ui.components.WhitePill
import com.ngstudio.mazebrush.ui.components.TealButton
import com.ngstudio.mazebrush.ui.theme.Clean
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Shadow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ngstudio.mazebrush.ui.components.tapFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.ngstudio.mazebrush.ui.components.starPath
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import com.ngstudio.mazebrush.ui.components.WorldBadge
import com.ngstudio.mazebrush.ui.components.BottomLeaves
import com.ngstudio.mazebrush.ui.components.TopLeaves
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
import com.ngstudio.mazebrush.ui.components.rememberPulse
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
import com.ngstudio.mazebrush.BuildConfig
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.audio.SoundManager
import com.ngstudio.maze.core.Difficulty
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Mechanic
import com.ngstudio.maze.core.Physics
import com.ngstudio.maze.core.Slide
import com.ngstudio.mazebrush.data.GameStore
import com.ngstudio.mazebrush.game.DebugFlags
import com.ngstudio.mazebrush.game.GameController
import com.ngstudio.mazebrush.render.BallSkin
import com.ngstudio.mazebrush.render.GameGLView
import com.ngstudio.mazebrush.render.GameRenderer
import com.ngstudio.mazebrush.render.PaintFinish
import com.ngstudio.mazebrush.render.RenderListener
import com.ngstudio.mazebrush.render.SceneConfig
import com.ngstudio.mazebrush.render.TrailStyle
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.CoinIcon
import com.ngstudio.mazebrush.ui.components.CounterPill
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.PaintProgressBar
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.Ribbon
import com.ngstudio.mazebrush.ui.components.RoundButton
import com.ngstudio.mazebrush.ui.components.Scrim
import com.ngstudio.mazebrush.ui.components.StarIcon
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.rememberPopIn
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Pro
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
            .background(if (low) Brush.verticalGradient(listOf(Candy.pink, Candy.pinkDark)) else Brush.verticalGradient(listOf(Pro.chrome, Pro.chromeDark)), RoundedCornerShape(17.dp))
            .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Pro.chromeLip.copy(alpha = 0.6f))), RoundedCornerShape(17.dp)).padding(horizontal = 12.dp),
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
    // board colours and paint come from the chosen board theme (shop "Themes")
    // the board's colours follow the level (BoardTheme.forLevel); players don't pick them
    val board = if (BuildConfig.DEBUG && DebugFlags.board >= 0) com.ngstudio.mazebrush.render.BoardTheme.byId(DebugFlags.board)
        else if (isDaily) com.ngstudio.mazebrush.render.BoardTheme.forDay(store.today())
        else com.ngstudio.mazebrush.render.BoardTheme.forLevel(levelNumber)
    val paint = com.ngstudio.mazebrush.render.PaintColor.byId(store.paintColor)
    val paintIndex = if (isDaily) 3 else levelNumber
    var showThemes by remember { mutableStateOf(false) }
    val shopPv = remember { ShopPreview(store, board.id) }
    var sheetPx by remember { mutableIntStateOf(0) }
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
    val telemetry = app.telemetry
    var hintsThisTry by remember { mutableIntStateOf(0) }
    val levelId = if (mode is PlayMode.Daily) mode.day.toInt() else levelNumber
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
            override fun onOutOfMoves() {
                sound.bonk(); haptics.impact(0.6f)
                telemetry.outOfMoves(levelId, isDaily, controllerHolder[0]?.tilesLeft ?: 0)
            }
            override fun onWin(moves: Int, stars: Int) {
                if (BuildConfig.DEBUG) android.util.Log.i("MazeQA", "WIN level=${if (isDaily) "daily" else levelNumber} moves=$moves par=${level.par} stars=$stars")
                sound.win(); haptics.win()
                renderer.playWin()
                val reward = if (mode is PlayMode.Campaign) store.completeLevel(levelNumber, stars, level.floorCount, moves) else null
                val daily = if (isDaily) store.completeDaily() else 0
                win = WinInfo(stars, moves, level.par, reward, daily)
                telemetry.levelComplete(levelId, isDaily, moves, level.par, stars, (reward?.coins ?: 0) + daily, reward?.firstClear ?: false, hintsThisTry)
            }
        }).also { controllerHolder[0] = it }
    }

    // push scene whenever the level (or attempt) changes
    LaunchedEffect(level, attempt) {
        win = null; winVisible = false; introDone = false
        hintsThisTry = 0
        telemetry.levelStart(
            levelId, isDaily, world = theme.index + 1, par = level.par,
            moveLimit = if (mode is PlayMode.Daily) Difficulty.dailyMoveLimit(level.par) else Difficulty.moveLimit(levelNumber, level.par),
            attempt = attempt,
        )
        if (BuildConfig.DEBUG) android.util.Log.i("MazeQA", "START level=${if (isDaily) "daily" else levelNumber} par=${level.par} size=${level.width}x${level.height}")
        renderer.setScene(
            SceneConfig(
                level, levelNumber, theme, BallSkin.byId(store.ballSkin), TrailStyle.byId(store.trail), PaintFinish.byId(store.finish),
                paint.first(board, paintIndex), paint.second(board, paintIndex),
                board = board,
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
    // while the skin sheet is open the board shrinks to fit above it (it is the sheet's live preview)
    LaunchedEffect(topInset, bottomInset, showThemes, sheetPx) {
        with(density) {
            val bottom = if (showThemes && sheetPx > 0) sheetPx + 14.dp.toPx() else bottomInset + 150.dp.toPx()
            renderer.setSafeArea(topInset + 122.dp.toPx(), bottom)
        }
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
            is com.ngstudio.mazebrush.game.Hints.Advice.Undo -> { undoTip = true; sound.hint(); return }
            is com.ngstudio.mazebrush.game.Hints.Advice.None -> return
            is com.ngstudio.mazebrush.game.Hints.Advice.Move -> Unit
        }
        if (store.hints <= 0) { showBuyHint = true; return }
        val d = (c.hint() as? com.ngstudio.mazebrush.game.Hints.Advice.Move)?.dir ?: return
        val s = Physics.slide(level, c.position, d) ?: return
        undoTip = false
        store.useHint()
        hintsThisTry++
        telemetry.hintUsed(levelId, isDaily)
        sound.hint()
        renderer.showHint(s.cells, d)
        lastMoveAt = System.currentTimeMillis(); nudge = false
    }

    /** Restart the level, logging the unfinished try. */
    fun restart(reason: String) {
        if (!ctrl.finished) telemetry.levelQuit(levelId, isDaily, reason, ctrl.moves, ctrl.progress)
        attempt++
    }
    /** Leave for Home, logging the try if it was unfinished. */
    fun quitHome() {
        if (!ctrl.finished) telemetry.levelQuit(levelId, isDaily, "home", ctrl.moves, ctrl.progress)
        onHome()
    }

    // the ball, trail, paint and board can change mid-level (skin sheet) without restarting it; while the sheet is
    // open the board shows what is being previewed, otherwise what is equipped
    val look = if (showThemes) listOf(shopPv.ball, shopPv.trail, shopPv.finish, shopPv.paint, shopPv.board)
        else listOf(store.ballSkin, store.trail, store.finish, store.paintColor, board.id)
    LaunchedEffect(look) {
        val b = com.ngstudio.mazebrush.render.BoardTheme.byId(look[4])
        val pc = com.ngstudio.mazebrush.render.PaintColor.byId(look[3])
        renderer.setLook(BallSkin.byId(look[0]), TrailStyle.byId(look[1]), PaintFinish.byId(look[2]), b, pc.first(b, paintIndex), pc.second(b, paintIndex))
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

        // ---- top HUD: pause | paint badge over the level | board themes; then moves, stars/par, coins
        val paintColor = paint.first(board, paintIndex).color()
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TealButton(Icon.PAUSE, "Pause") { paused = true }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    PaintBadge(controller.progress, paintColor)
                    VSpace(2.dp)
                    LevelTitle(if (isDaily) "DAILY" else "LEVEL $levelNumber")
                }
                TealButton(Icon.PAINT, "Skins") { shopPv.reset(store, board.id); showThemes = true }
            }
            VSpace(8.dp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val left = controller.movesLeft
                val low = left != null && left <= 3
                val pulse = rememberPulse(500, 0.08f)
                WhitePill(left?.toString() ?: controller.moves.toString(), Modifier.scale(if (low) pulse else 1f), valueColor = if (low) Clean.coral else Clean.ink) {
                    Text(if (left != null) "MOVES" else "MOVES", style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, color = Clean.inkSoft), modifier = Modifier.padding(start = 8.dp))
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    val m = controller.moves
                    for (k in 3 downTo 1) {
                        val lit = controller.starsFor(m.coerceAtLeast(0)) >= k
                        StarIcon(lit, size = 16.dp, modifier = Modifier.alpha(if (lit) 1f else 0.55f))
                    }
                    HSpace(6.dp)
                    Text("Par ${level.par}", style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Clean.inkSoft))
                }
                WhitePill(coinShown.value.toInt().toString(), Modifier.onGloballyPositioned { coinTarget = it.boundsInRoot().center }) { CoinIcon(size = 24.dp) }
            }
        }

        // ---- bottom: New Item progress, then Undo / Restart / Hint
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NewItemBar(store, Modifier.fillMaxWidth())
            VSpace(10.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val undoPulse = rememberPulse(600, 0.12f)
                LabeledTealButton(Icon.UNDO, "Undo", Modifier.scale(if (undoTip) undoPulse else 1f), enabled = controller.canUndo) { undoTip = false; ctrl.undo() }
                LabeledTealButton(Icon.RESTART, "Restart", color = Clean.coral) { if (!ctrl.isBusy || ctrl.finished) restart("restart") }
                val pulse = rememberPulse(600, 0.12f)
                LabeledTealButton(Icon.HINT, "Hint", Modifier.scale(if (nudge) pulse else 1f), color = Color(0xFFF2B530),
                    badge = if (store.hints > 0) store.hints.toString() else "+", badgeColor = if (store.hints > 0) Clean.coral else Clean.teal) { useHint() }
            }
        }

        if (undoTip) Box(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(bottom = 156.dp, start = 14.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp)).background(Pro.glass, RoundedCornerShape(14.dp))
                .border(1.dp, Pro.gold.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
        ) { BodyText("Off the best route - undo a few moves", size = 14.sp, color = Pro.cream) }

        if (nudge) Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 156.dp, end = 14.dp)
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

        if (showThemes && win == null) SkinSheet(store, shopPv, onHeight = { sheetPx = it }, onClose = { showThemes = false; sheetPx = 0 })

        if (paused && win == null) PauseOverlay(
            store = store,
            label = if (isDaily) "DAILY CHALLENGE" else "LEVEL $levelNumber",
            progress = controller.progress, movesLeft = controller.movesLeft, par = level.par, tilesLeft = controller.tilesLeft,
            paint = paint.first(board, paintIndex).color(),
            onResume = { paused = false },
            onRestart = { paused = false; restart("restart") },
            onHome = { quitHome() },
        )

        if (controller.outOfMoves && win == null && !adForMoves) OutOfMoves(
            store = store, tilesLeft = controller.tilesLeft, canUndo = controller.canUndo,
            onUndo = { ctrl.undo() },
            onBuy = { if (store.spend(MOVES_PRICE)) { sound.coin(); ctrl.addMoves(5); telemetry.spend("moves_5", MOVES_PRICE) } },
            onAd = { adForMoves = true },
            onRetry = { restart("retry") },
        )
        if (adForMoves) FakeAdOverlay { adForMoves = false; ctrl.addMoves(5); sound.unlock(); telemetry.rewardedAd("moves_5") }

        if (showBuyHint) BuyHintDialog(store, onClose = { showBuyHint = false }, onAd = { showBuyHint = false; adForHint = true }) {
            if (store.buyHint(HINT_PRICE)) { sound.coin(); showBuyHint = false; telemetry.spend("hint", HINT_PRICE); useHint() }
        }
        if (adForHint) FakeAdOverlay { store.addHint(1); adForHint = false; sound.unlock(); telemetry.rewardedAd("hint"); useHint() }

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
private fun PauseOverlay(
    store: GameStore, label: String, progress: Float, movesLeft: Int?, par: Int, tilesLeft: Int, paint: Color,
    onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit,
) {
    // the card drops in from above and settles with a little swing; the plaque pops in on top
    val scrim = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    val plaque = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scrim.animateTo(1f, tween(220)) }
        launch { drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
        delay(200)
        plaque.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = scrim.value }
            .background(Color(0x99263A46))
            .clickable(remember { MutableInteractionSource() }, null) { onResume() },
        contentAlignment = Alignment.Center,
    ) {
        val d = drop.value
        Box(
            Modifier.padding(horizontal = 22.dp)
                .graphicsLayer {
                    translationY = (1f - d) * -420.dp.toPx()
                    rotationZ = (1f - d) * 6f
                    alpha = (d * 3f).coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Panel(Modifier.fillMaxWidth().padding(top = 28.dp).clickable(remember { MutableInteractionSource() }, null) {}, padding = 18.dp) {
                VSpace(30.dp)
                Text(label, style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                    letterSpacing = 1.5.sp, color = Color(0xFF5E7280)), textAlign = TextAlign.Center)
                VSpace(12.dp)
                // progress
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    BodyText("Painted", size = 14.sp, color = Color(0xFF5E7280), shadow = false)
                    HSpace(6.dp)
                    BodyText("${(progress * 100).toInt()}%", size = 18.sp, color = Pro.text, shadow = false)
                    Spacer(Modifier.weight(1f))
                    BodyText(if (tilesLeft == 1) "1 tile left" else "$tilesLeft tiles left", size = 13.sp, color = Color(0xFF5E7280), shadow = false)
                }
                VSpace(6.dp)
                PaintProgressBar(progress, paint, Modifier.fillMaxWidth(), height = 16.dp)
                VSpace(10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip("MOVES LEFT", movesLeft?.toString() ?: "∞", Modifier.weight(1f), warn = movesLeft != null && movesLeft <= 3)
                    InfoChip("PAR", "$par", Modifier.weight(1f))
                }
                VSpace(16.dp)
                // settings as wood tiles
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WoodToggle(Icon.SOUND, "Sound", com.ngstudio.mazebrush.ui.theme.Clean.sky, store.sound, Modifier.weight(1f)) { store.toggleSound() }
                    WoodToggle(Icon.MUSIC, "Music", com.ngstudio.mazebrush.ui.theme.Clean.amber, store.music, Modifier.weight(1f)) { store.toggleMusic(); MazeApp.instance.sound.refreshMusic() }
                    WoodToggle(Icon.VIBRATE, "Vibrate", com.ngstudio.mazebrush.ui.theme.Clean.teal, store.vibration, Modifier.weight(1f)) { store.toggleVibration() }
                }
                VSpace(18.dp)
                CandyButton("RESUME", Modifier.fillMaxWidth(), height = 62.dp, textSize = 30.sp, icon = Icon.PLAY) { onResume() }
                VSpace(10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WoodButton(Icon.RESTART, "Restart", com.ngstudio.mazebrush.ui.theme.Clean.amber, com.ngstudio.mazebrush.ui.theme.Clean.amberDark, Modifier.weight(1f)) { onRestart() }
                    WoodButton(Icon.HOME, "Home", com.ngstudio.mazebrush.ui.theme.Clean.slate, com.ngstudio.mazebrush.ui.theme.Clean.slateDark, Modifier.weight(1f)) { onHome() }
                }
            }
            // wood plaque with the title, sitting on the card's top edge
            val pq = plaque.value
            Row(
                Modifier.graphicsLayer { val sc = 0.5f + 0.5f * pq; scaleX = sc; scaleY = sc; alpha = pq.coerceIn(0f, 1f) }
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Clean.tealDark)
                    .background(Brush.verticalGradient(listOf(Clean.tealLight, Clean.teal, Clean.tealDark)), RoundedCornerShape(18.dp))
                    .border(2.5.dp, Color.White, RoundedCornerShape(18.dp))
                    .padding(horizontal = 26.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Icon.PAUSE, Color.White, size = 24.dp)
                HSpace(10.dp)
                GameText("PAUSED", 32.sp, color = Color.White, outline = Clean.tealDark, outlineWidth = 0.07f)
            }
        }
    }
}

/** Cream chip with a small label and a value (pause card). */
@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier, warn: Boolean = false) {
    Row(
        modifier.background(if (warn) Color(0xFFFFE1DC) else Color(0xFFF7F2EC), RoundedCornerShape(14.dp))
            .border(1.5.dp, if (warn) Candy.pink.copy(alpha = 0.6f) else Color(0xFFEDE1D3), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
            letterSpacing = 1.sp, color = Color(0xFF5E7280)))
        HSpace(8.dp)
        BodyText(value, size = 18.sp, color = if (warn) Candy.pinkDark else Pro.text, shadow = false)
    }
}

/** Wood tile toggle like the Home quick tiles: coloured icon disc when on, grey slashed disc when off. */
@Composable
private fun WoodToggle(icon: Icon, label: String, color: Color, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val k by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "tog")
    Column(
        modifier.shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Clean.cardTop, Clean.cardBottom)))
            .border(1.5.dp, if (on) color.copy(alpha = 0.5f) else Clean.cardBorder, RoundedCornerShape(18.dp))
            .clickable { tapFeedback(); onClick() }
            .semantics { contentDescription = "$label ${if (on) "on" else "off"}" }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val c = lerp(Color(0xFFC2CCD2), color, k)
        Box(Modifier.size(40.dp).shadow(if (on) 3.dp else 0.dp, CircleShape, spotColor = color).background(Brush.verticalGradient(listOf(lerp(c, Color.White, 0.25f), c)), CircleShape), contentAlignment = Alignment.Center) {
            GameIcon(icon, Color.White.copy(alpha = 0.7f + 0.3f * k), size = 22.dp)
            if (k < 1f) Canvas(Modifier.size(28.dp).graphicsLayer { alpha = 1f - k }) {
                drawLine(Color.White, Offset(size.width * 0.12f, size.height * 0.12f), Offset(size.width * 0.88f, size.height * 0.88f), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        VSpace(4.dp)
        BodyText(label, size = 13.sp, color = Clean.ink, align = TextAlign.Center)
        Text(if (on) "ON" else "OFF", style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.Black, fontSize = 10.sp,
            letterSpacing = 1.5.sp, color = if (on) Clean.tealDark else Clean.inkSoft.copy(alpha = 0.55f)))
    }
}

/** Secondary wood pill button with an icon and a label (pause card). */
@Composable
private fun WoodButton(icon: Icon, label: String, color: Color, dark: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(25.dp)
    Box(modifier.height(54.dp).clickable(remember { MutableInteractionSource() }, null) { tapFeedback(); onClick() }) {
        Box(Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).shadow(3.dp, shape, spotColor = dark).background(dark, shape))
        Row(
            Modifier.fillMaxWidth().height(50.dp)
                .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.22f), color)), shape)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            GameIcon(icon, Color.White, size = 22.dp)
            HSpace(8.dp)
            GameText(label, 20.sp, color = Color.White, outline = dark, outlineWidth = 0.06f)
        }
    }
}

/** "LEVEL 23" between two fading gold rules (header of the dark result/pause cards). */
@Composable
private fun CardHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFE8B45A)))))
        HSpace(10.dp)
        Text(text, style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
            letterSpacing = 2.5.sp, color = Color(0xFFFFE6B0)))
        HSpace(10.dp)
        Box(Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color(0xFFE8B45A), Color.Transparent))))
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

    // staged entrance: backdrop -> title -> stars one by one -> info -> bottom panel
    val backdrop = remember { Animatable(0f) }
    val starAnims = remember { List(3) { Animatable(0f) } }
    val bursts = remember { List(3) { Animatable(0f) } }
    val details = remember { Animatable(0f) }
    val panel = remember { Animatable(0f) }
    val coinPop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { backdrop.animateTo(1f, tween(350)) }
        delay(120)
        delay(280)
        for (i in 0 until 3) {
            if (i < info.stars) {
                launch { starAnims[i].animateTo(1f, spring(dampingRatio = 0.40f, stiffness = 480f)) }
                delay(150)
                sound.star(i + 1)
                launch { bursts[i].animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
                delay(170)
            } else {
                launch { starAnims[i].animateTo(1f, tween(260)) }
                delay(120)
            }
        }
        launch { details.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
        launch { panel.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 260f)) }
        delay(380)
        if (coinsTotal > 0) {
            sound.coin(); coinAnim.animateTo(coinsTotal.toFloat(), tween(650, easing = FastOutSlowInEasing))
            onCoinsFrom(coinPos); coinsLanded = true
            coinPop.snapTo(1f); coinPop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
        if ((info.reward?.chest ?: 0) > 0) { delay(150); sound.chest() }
    }

    val title = when (info.stars) { 3 -> "PERFECT!"; 2 -> "GREAT!"; else -> "NICE!" }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = backdrop.value }
            .background(Brush.radialGradient(listOf(Color(0xCC17232C), Color(0xF20B1117)), radius = 1500f))
            // fade the game HUD (top bar, bottom tiles) out of view behind the title and buttons
            .background(Brush.verticalGradient(0f to Color(0xF20D151B), 0.12f to Color(0xC80D151B), 0.24f to Color.Transparent, 0.62f to Color.Transparent, 0.80f to Color(0xF50D151B), 1f to Color(0xFF0D151B)))
            .clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            // ---- celebration (centre of the screen); the bottom padding keeps a gap above the floating buttons
            Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                BouncyTitle(title, TitleColors.forStars(info.stars))
                VSpace(2.dp)
                // the stars take whatever height is left after the title and the results card, shrinking on short
                // screens (or when bonus lines make the card taller) so the card is never cut off
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) {
                    val k = (maxHeight / 220.dp).coerceIn(0.4f, 1f)
                    Box(Modifier.fillMaxWidth().height(220.dp * k), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxWidth().requiredHeight(220.dp).graphicsLayer { scaleX = k; scaleY = k }) {
                            FloatingStars(info.stars, starAnims.map { it.value }, bursts.map { it.value })
                        }
                    }
                }
                VSpace(10.dp)
                val dv = details.value
                val atPar = info.moves <= info.par
                // ---- results card
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                        .graphicsLayer { alpha = dv; translationY = (1f - dv) * 24.dp.toPx(); val sc = 0.94f + 0.06f * dv; scaleX = sc; scaleY = sc }
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
                        .background(Brush.verticalGradient(listOf(Color(0xF22A3D4A), Color(0xF21C2A34))), RoundedCornerShape(24.dp))
                        .border(1.5.dp, Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x33FFFFFF), Color(0x18FFFFFF))), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CardHeader(if (isDaily) "DAILY COMPLETE" else "LEVEL $levelNumber COMPLETE")
                    VSpace(12.dp)
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                        StatCell("MOVES", Modifier.weight(1f), icon = { GameIcon(Icon.PLAY, Color(0xFFFFE6B0).copy(alpha = 0.8f), size = 16.dp) }) {
                            GameText("${info.moves}", 30.sp, color = if (atPar) Color(0xFFA5EB7E) else Color(0xFFFFF4E0), outline = Color(0xFF101A21), outlineWidth = 0.06f)
                        }
                        StatDivider()
                        StatCell("PAR", Modifier.weight(1f), icon = { GameIcon(Icon.TROPHY, Color(0xFFFFE6B0).copy(alpha = 0.8f), size = 16.dp) }) {
                            GameText("${info.par}", 30.sp, color = Color(0xFFFFF4E0), outline = Color(0xFF101A21), outlineWidth = 0.06f)
                        }
                        if (coinsTotal > 0) {
                            StatDivider()
                            val cp = coinPop.value
                            StatCell("COINS", Modifier.weight(1.25f).onGloballyPositioned { coinPos = it.boundsInRoot().center }, icon = null) {
                                Row(Modifier.graphicsLayer { val sc = 1f + 0.22f * cp; scaleX = sc; scaleY = sc }, verticalAlignment = Alignment.CenterVertically) {
                                    // the coin spins while the amount counts up
                                    val spinK = coinAnim.value / coinsTotal
                                    CoinIcon(size = 30.dp, modifier = Modifier.graphicsLayer { rotationY = if (coinsLanded) 0f else spinK * 720f; cameraDistance = 12f * density })
                                    HSpace(6.dp)
                                    GameText("+${coinAnim.value.toInt()}", 30.sp, color = Color(0xFFFFD54A), outline = Color(0xFF101A21), outlineWidth = 0.06f)
                                }
                            }
                        }
                    }
                    val streakBonus = info.reward?.streakBonus ?: 0
                    val chest = info.reward?.chest ?: 0
                    val newItem = info.reward?.newItem
                    if (streakBonus > 0 || chest > 0 || isDaily || newItem != null) {
                        VSpace(12.dp)
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFE6B0)))
                        VSpace(10.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (streakBonus > 0) BonusLine({ StarIcon(true, size = 18.dp) }, "Perfect streak x${info.reward?.streak}", "+$streakBonus", Color(0xFFA5EB7E))
                            if (newItem != null) BonusLine({ ItemSwatch(newItem, Modifier.size(20.dp)) }, "New item: ${newItem.name}${if (newItem.kind == GameStore.ShopItem.Kind.BOARD) " theme" else " ball"}", "FREE", Color(0xFF7EE0D4))
                            if (chest > 0) BonusLine({ GameIcon(Icon.CHEST, Candy.orange, size = 20.dp) }, "Chapter chest  (+1 hint)", "+$chest", Color(0xFFFFD54A))
                            if (isDaily) BonusLine({ GameIcon(Icon.FLAME, Candy.orange, size = 20.dp) }, "Daily streak", "${store.dailyStreak}", Color(0xFFFFB066))
                        }
                    }
                }
            }

            // ---- actions float over the dimmed board (no panel)
            val pv = panel.value
            Column(
                Modifier.fillMaxWidth()
                    .graphicsLayer { translationY = (1f - pv) * 120.dp.toPx(); alpha = pv.coerceIn(0f, 1f) }
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 32.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (Ads.rewarded.available && coinsTotal > 0 && !doubled) {
                    CandyButton("x2 COINS", Modifier.fillMaxWidth().graphicsLayer { alpha = if (coinsLanded) 1f else 0.5f },
                        height = 52.dp, textSize = 22.sp, color = Candy.orange, dark = Candy.orangeDark, icon = Icon.VIDEO, enabled = coinsLanded) { adPlaying = true }
                    VSpace(14.dp)
                }
                CandyButton(if (isDaily) "HOME" else "NEXT", Modifier.fillMaxWidth().scale(if (pv > 0.99f) rememberPulse(900, 0.03f) else 1f),
                    height = 66.dp, textSize = 32.sp, icon = if (isDaily) Icon.HOME else Icon.NEXT) { onNext() }
                VSpace(18.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    GlassRound(Icon.RESTART, "Replay") { onReplay() }
                    GlassRound(Icon.HOME, "Home") { onHome() }
                }
            }
        }
    }
    if (adPlaying) FakeAdOverlay {
        store.addCoins(coinsTotal)
        MazeApp.instance.telemetry.rewardedAd("x2_coins")
        MazeApp.instance.telemetry.earn("coins", coinsTotal.toLong(), "ad_x2")
        doubled = true
        adPlaying = false
        scope.launch { coinAnim.animateTo(coinsTotal * 2f, tween(600)) }
        onCoinsFrom(coinPos)
    }
}

/** Fill gradient (top, middle, bottom) and outline for a [BouncyTitle]. */
private class TitleColors(val top: Color, val mid: Color, val bottom: Color, val outline: Color) {
    companion object {
        val GOLD = TitleColors(Color(0xFFFFF6C2), Color(0xFFFFD54A), Color(0xFFFF9F1C), Color(0xFF1E2B35))
        /** 3 stars: white to warm cream with a dark slate outline. */
        val CREAM = TitleColors(Color(0xFFFFFFFF), Color(0xFFFFF1DC), Color(0xFFF3CFA2), Color(0xFF1E2B35))
        /** 2 stars: lime green, like the NEXT button. */
        val GREEN = TitleColors(Color(0xFFE9FFC4), Color(0xFF8FE04F), Color(0xFF3FAE2A), Color(0xFF1F5410))
        /** 1 star: ice blue. */
        val BLUE = TitleColors(Color(0xFFFFFFFF), Color(0xFFD9F1FF), Color(0xFF7CC6F0), Color(0xFF173F66))
        fun forStars(stars: Int) = when (stars) { 3 -> CREAM; 2 -> GREEN; else -> BLUE }
    }
}

/** Title whose letters drop in one by one and bounce, filled with a gold gradient; a glint runs across it now and then. */
@Composable
private fun BouncyTitle(text: String, colors: TitleColors = TitleColors.GOLD) {
    val letters = remember(text) { text.toList() }
    val anims = remember(text) { letters.map { Animatable(0f) } }
    LaunchedEffect(text) {
        delay(120)
        anims.forEachIndexed { i, a ->
            launch { delay(i * 55L); a.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
        }
    }
    val glint by rememberInfiniteTransition(label = "glint").animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "g")
    val n = letters.size
    Row(verticalAlignment = Alignment.Bottom) {
        letters.forEachIndexed { i, ch ->
            val p = anims[i].value
            // glint: a bright band sweeps across the letters in the first 40% of every cycle
            val x = glint / 0.4f * (n + 4) - i - 2f
            val g = if (glint < 0.4f) kotlin.math.exp(-x * x * 0.6f) else 0f
            val top = lerp(colors.top, Color.White, g)
            val mid = lerp(colors.mid, Color.White, g * 0.75f)
            val bot = lerp(colors.bottom, Color.White, g * 0.5f)
            val base = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = 64.sp, letterSpacing = 0.5.sp)
            Box(
                Modifier.graphicsLayer {
                    alpha = (p * 4f).coerceIn(0f, 1f)
                    translationY = (1f - p) * -90.dp.toPx()
                    val sc = 1f + (1f - p) * 0.6f
                    scaleX = sc; scaleY = sc
                    rotationZ = (1f - p) * (if (i % 2 == 0) -18f else 18f)
                },
            ) {
                Text(ch.toString(), style = base.copy(color = colors.outline, drawStyle = Stroke(width = 15f, join = StrokeJoin.Round),
                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 9f), 6f)))
                Text(ch.toString(), style = base.copy(brush = Brush.verticalGradient(listOf(top, mid, bot))))
            }
        }
    }
}

/** A big glossy star: gold gradient, thick warm outline, inner bevel, gloss and a passing glint. */
@Composable
private fun GloryStar(size: Dp, glint: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val sc = this.size.minDimension / 24f
        scale(sc, sc, pivot = Offset.Zero) {
            val outer = starPath(12f, 12.9f, 11.2f, 5.3f)
            val inner = starPath(12f, 13.1f, 8.6f, 4.0f)
            translate(0f, 1.0f) { drawPath(outer, Color(0xFF3E1A08).copy(alpha = 0.45f)) }
            drawPath(outer, Brush.verticalGradient(listOf(Color(0xFFFFB627), Color(0xFFE07B00)), 1.5f, 23f))
            drawPath(outer, Color(0xFF8A3E00), style = Stroke(1.1f, join = StrokeJoin.Round))
            drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFF7C4), Color(0xFFFFDD55), Color(0xFFFFB31A)), 3f, 21f))
            clipPath(inner) {
                // gloss on the upper-left
                drawOval(Color.White.copy(alpha = 0.55f), Offset(5.5f, 4.5f), Size(8f, 4.2f))
                // diagonal glint band
                if (glint in 0f..1f) {
                    val gx = -10f + glint * 40f
                    val path = Path().apply { moveTo(gx, 0f); lineTo(gx + 4f, 0f); lineTo(gx - 6f, 24f); lineTo(gx - 10f, 24f); close() }
                    drawPath(path, Color.White.copy(alpha = 0.65f))
                }
            }
            drawCircle(Color.White.copy(alpha = 0.9f), 0.9f, Offset(8.6f, 7.4f))
        }
    }
}

/** Big stars floating over the dimmed board in a gentle arc, with soft turning rays, halos and twinkles. */
@Composable
private fun FloatingStars(stars: Int, pops: List<Float>, bursts: List<Float>) {
    val inf = rememberInfiniteTransition(label = "stars")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "spin")
    val bob by inf.animateFloat(-1f, 1f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val clock by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "clock")
    val lit0 = pops.take(stars).maxOrNull() ?: 0f
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        if (stars > 0) Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = lit0.coerceIn(0f, 1f) }) {
            val c = center + Offset(0f, -8.dp.toPx())
            val rr = size.width * 0.70f
            fun rays(n: Int, width: Float, a: Float, angle: Float) = rotate(angle, c) {
                for (i in 0 until n) {
                    val a0 = i * 360f / n
                    val t0 = Math.toRadians(a0.toDouble()); val t1 = Math.toRadians((a0 + width).toDouble())
                    val path = Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + rr * kotlin.math.cos(t0).toFloat(), c.y + rr * kotlin.math.sin(t0).toFloat())
                        lineTo(c.x + rr * kotlin.math.cos(t1).toFloat(), c.y + rr * kotlin.math.sin(t1).toFloat())
                        close()
                    }
                    drawPath(path, Brush.radialGradient(listOf(Color(0xFFFFF4DC).copy(alpha = a), Color.Transparent), c, rr * 0.85f))
                }
            }
            rays(12, 11f, 0.10f, spin)
            rays(12, 5f, 0.06f, -spin * 0.6f + 15f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4DC).copy(alpha = 0.20f), Color(0xFFFFE2A8).copy(alpha = 0.05f), Color.Transparent), c, size.height * 0.50f), size.height * 0.50f, c)
            // twinkles: small four-point sparkles that blink at fixed spots around the stars
            for (k in 0 until 14) {
                val h1 = ((k * 73 + 11) % 97) / 97f; val h2 = ((k * 41 + 29) % 89) / 89f
                val pos = Offset(size.width * (0.06f + 0.88f * h1), size.height * (0.06f + 0.88f * h2))
                val ph = (clock + k * 0.137f) % 1f
                val a = kotlin.math.sin(ph * Math.PI.toFloat()).let { it * it * it }
                val l = (5f + 6f * h2) * density * (0.6f + 0.4f * a)
                drawLine(Color.White.copy(alpha = a * 0.9f), pos - Offset(l, 0f), pos + Offset(l, 0f), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
                drawLine(Color.White.copy(alpha = a * 0.9f), pos - Offset(0f, l), pos + Offset(0f, l), strokeWidth = 2.2f * density, cap = StrokeCap.Round)
                drawCircle(Color.White.copy(alpha = a * 0.45f), l * 0.30f, pos)
            }
        }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center) {
            for (i in 0 until 3) {
                val big = i == 1
                val sz = if (big) 132.dp else 98.dp
                val tilt = when (i) { 0 -> -16f; 2 -> 16f; else -> 0f }
                val breathe = if (big && i < stars) 1f + 0.035f * bob else 1f
                Box(
                    Modifier.padding(horizontal = if (big) 0.dp else 0.dp).padding(top = if (big) 0.dp else 48.dp).size(sz)
                        .graphicsLayer {
                            rotationZ = tilt
                            translationY = bob * (if (big) 3.dp else 2.dp).toPx() * (if (big) 1f else -1f)
                            scaleX = breathe; scaleY = breathe
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val lit = i < stars
                    val p = pops[i]
                    // empty slot: a dark star with a faint gold edge
                    if (!lit) Canvas(Modifier.matchParentSize().graphicsLayer { alpha = p.coerceIn(0f, 1f) }) {
                        val sc = size.minDimension / 24f
                        scale(sc, sc, pivot = Offset.Zero) {
                            val path = starPath(12f, 12.9f, 11.2f, 5.3f)
                            drawPath(path, Color(0xFF101A21).copy(alpha = 0.72f))
                            drawPath(path, Color(0xFFFFD54A).copy(alpha = 0.30f), style = Stroke(0.6f, join = StrokeJoin.Round))
                        }
                    }
                    if (lit) {
                        // halo under the star
                        Canvas(Modifier.size(sz * 1.5f).graphicsLayer { alpha = (p * 1.5f).coerceIn(0f, 1f) }) {
                            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4DC).copy(alpha = 0.22f), Color.Transparent), center, size.minDimension / 2.4f), size.minDimension / 2.4f)
                        }
                        val b = bursts[i]
                        if (b > 0f && b < 1f) Canvas(Modifier.size(sz * 2.4f)) {
                            val c = center
                            for (k in 0 until 14) {
                                val t = Math.toRadians((k * (360f / 14) + i * 13f).toDouble())
                                val d = size.minDimension * (0.14f + 0.38f * b) * (if (k % 3 == 0) 1.15f else 1f)
                                val pos = c + Offset(kotlin.math.cos(t).toFloat() * d, kotlin.math.sin(t).toFloat() * d)
                                val a = 1f - b
                                if (k % 2 == 0) drawCircle(Color(0xFFFFF1A8).copy(alpha = a), size.minDimension * 0.024f * (1f - b * 0.5f), pos)
                                else {
                                    val l = size.minDimension * 0.06f * (1f - b * 0.4f)
                                    drawLine(Color.White.copy(alpha = a), pos - Offset(l, 0f), pos + Offset(l, 0f), strokeWidth = 4f, cap = StrokeCap.Round)
                                    drawLine(Color.White.copy(alpha = a), pos - Offset(0f, l), pos + Offset(0f, l), strokeWidth = 4f, cap = StrokeCap.Round)
                                }
                            }
                            drawCircle(Color.White.copy(alpha = 0.6f * (1f - b)), size.minDimension * 0.46f * b, c, style = Stroke(6f * (1f - b) + 1f))
                        }
                        // each star glints in turn
                        val g = ((clock - i * 0.12f + 1f) % 1f) / 0.30f
                        GloryStar(sz, if (p > 0.98f) g else -1f, Modifier.graphicsLayer {
                            val s0 = 2.6f - 1.6f * p
                            scaleX = s0; scaleY = s0
                            alpha = (p * 3f).coerceIn(0f, 1f)
                            rotationZ = (1f - p) * -70f
                        })
                    }
                }
            }
        }
    }
}

/** One column of the results card: small label with an icon above a big value. */
@Composable
private fun StatCell(label: String, modifier: Modifier, icon: (@Composable () -> Unit)?, value: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { icon(); HSpace(4.dp) }
            Text(label, style = TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp,
                letterSpacing = 1.5.sp, color = Color(0xFFFFE6B0).copy(alpha = 0.65f)))
        }
        VSpace(2.dp)
        value()
    }
}

@Composable
private fun StatDivider() = Box(Modifier.fillMaxHeight().padding(vertical = 4.dp).width(1.dp)
    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66FFE6B0), Color.Transparent))))

/** A bonus line in the card footer: icon, text, and the amount on the right. */
@Composable
private fun BonusLine(icon: @Composable () -> Unit, text: String, amount: String, color: Color) {
    Row(Modifier.fillMaxWidth().background(Color(0x22FFE6B0), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        icon(); HSpace(8.dp)
        BodyText(text, Modifier.weight(1f), size = 14.sp, color = Color(0xFFFFF4E0), shadow = false)
        BodyText(amount, size = 15.sp, color = color, shadow = false)
    }
}

/** Small round glass button with a label, for secondary actions over the dimmed board. */
@Composable
private fun GlassRound(icon: Icon, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(54.dp).clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x1AFFFFFF))))
                .border(1.5.dp, Pro.cream.copy(alpha = 0.45f), CircleShape)
                .clickable { tapFeedback(); onClick() }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { GameIcon(icon, Pro.cream, size = 26.dp) }
        VSpace(4.dp)
        BodyText(label, size = 12.sp, color = Pro.creamDim, shadow = false)
    }
}

/**
 * The skin sheet (paint-bucket button): the same Ball / Trail / Paint / Maze panel as the Skins tab, over the level.
 * The live board above it is the preview; closing the sheet puts back whatever is equipped.
 */
@Composable
private fun SkinSheet(store: GameStore, pv: ShopPreview, onHeight: (Int) -> Unit, onClose: () -> Unit) {
    BackHandler { onClose() }
    Box(
        Modifier.fillMaxSize().background(Color(0x22263A46))
            .clickable(remember { MutableInteractionSource() }, null) { onClose() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().onGloballyPositioned { onHeight(it.size.height) }
                .clickable(remember { MutableInteractionSource() }, null) { }
                .navigationBarsPadding().padding(horizontal = 14.dp).padding(bottom = 10.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PreviewChip(pv, store)
                Spacer(Modifier.weight(1f))
                WhitePill(store.coins.toString()) { CoinIcon(size = 22.dp) }
                HSpace(10.dp)
                TealButton(Icon.CLOSE, "Close", size = 40.dp) { onClose() }
            }
            ShopPanel(pv, Modifier.fillMaxWidth())
        }
    }
}

