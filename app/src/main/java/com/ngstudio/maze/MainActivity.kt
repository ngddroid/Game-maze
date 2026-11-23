package com.ngstudio.maze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ngstudio.maze.audio.SoundManager
import com.ngstudio.maze.ui.screens.GameScreen
import com.ngstudio.maze.ui.screens.HomeScreen
import com.ngstudio.maze.ui.screens.HomeTab
import com.ngstudio.maze.ui.screens.OnboardingScreen
import com.ngstudio.maze.ui.screens.PlayMode
import com.ngstudio.maze.ui.screens.SettingsScreen
import com.ngstudio.maze.ui.screens.SplashScreen
import com.ngstudio.maze.ui.theme.MazeTheme

/** Single activity; screens are Compose routes on a small back stack. Terms/Privacy open LegalActivity. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        // Debug QA: `--ez autoplay true` plays every level's stored solution (see scripts/device_qa.sh).
        if (BuildConfig.DEBUG && intent.getBooleanExtra("autoplay", false)) com.ngstudio.maze.game.DebugFlags.autoplay = true
        setContent { MazeTheme { AppNav() } }
    }

    override fun onStart() {
        super.onStart()
        MazeApp.instance.sound.onForeground(true)
    }

    override fun onStop() {
        super.onStop()
        MazeApp.instance.sound.onForeground(false)
    }
}

sealed class Route {
    object Splash : Route()
    object Onboarding : Route()
    data class Home(val tab: HomeTab) : Route()
    data class Game(val mode: PlayMode, val key: Long = System.nanoTime()) : Route()
    object Settings : Route()
}

@Composable
private fun AppNav() {
    val app = MazeApp.instance
    val store = app.store
    // Debug only: `adb shell am start -n com.ngstudio.maze/.MainActivity --ei debugLevel 251` opens any level for QA
    // (it does not unlock anything).
    val debugLevel = (LocalContext.current as? android.app.Activity)?.intent?.getIntExtra("debugLevel", 0) ?: 0
    var stack by remember {
        mutableStateOf(
            if (BuildConfig.DEBUG && debugLevel > 0) listOf(Route.Home(HomeTab.HOME), Route.Game(PlayMode.Campaign(debugLevel)))
            else listOf<Route>(Route.Splash)
        )
    }
    val top = stack.last()

    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = top is Route.Home && top.tab != HomeTab.HOME) { stack = stack.dropLast(1) + Route.Home(HomeTab.HOME) }

    LaunchedEffect(top) {
        when (top) {
            is Route.Game -> app.sound.music(SoundManager.Music.GAME)
            is Route.Splash -> Unit
            else -> app.sound.music(SoundManager.Music.HOME)
        }
    }

    AnimatedContent(
        top,
        transitionSpec = { (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f)) togetherWith fadeOut(tween(160)) },
        contentKey = { r -> if (r is Route.Home) "home" else r },
        label = "nav",
    ) { r ->
        when (r) {
            Route.Splash -> SplashScreen {
                stack = if (store.onboardingDone) listOf(Route.Home(HomeTab.HOME)) else listOf(Route.Onboarding)
            }
            Route.Onboarding -> OnboardingScreen {
                store.setOnboardingDone()
                stack = listOf(Route.Home(HomeTab.HOME), Route.Game(PlayMode.Campaign(1)))
            }
            is Route.Home -> HomeScreen(
                tab = r.tab,
                onTab = { stack = stack.dropLast(1) + Route.Home(it) },
                onPlay = { push(Route.Game(PlayMode.Campaign(it))) },
                onPlayDaily = { push(Route.Game(PlayMode.Daily(it))) },
                onSettings = { push(Route.Settings) },
            )
            is Route.Game -> GameScreen(r.mode, onHome = { pop() })
            Route.Settings -> SettingsScreen(onBack = { pop() })
        }
    }
}
