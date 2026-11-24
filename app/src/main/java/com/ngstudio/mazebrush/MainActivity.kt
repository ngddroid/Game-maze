package com.ngstudio.mazebrush

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
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ngstudio.mazebrush.notifications.Notifications
import com.ngstudio.mazebrush.ui.screens.UpdateOverlay
import kotlinx.coroutines.delay
import com.ngstudio.mazebrush.audio.SoundManager
import com.ngstudio.mazebrush.ui.screens.GameScreen
import com.ngstudio.mazebrush.ui.screens.HomeScreen
import com.ngstudio.mazebrush.ui.screens.HomeTab
import com.ngstudio.mazebrush.ui.screens.OnboardingScreen
import com.ngstudio.mazebrush.ui.screens.PlayMode
import com.ngstudio.mazebrush.ui.screens.SettingsScreen
import com.ngstudio.mazebrush.ui.screens.SplashScreen
import com.ngstudio.mazebrush.ui.screens.StudioIntroScreen
import com.ngstudio.mazebrush.ui.theme.MazeTheme

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
        if (BuildConfig.DEBUG && intent.getBooleanExtra("autoplay", false)) com.ngstudio.mazebrush.game.DebugFlags.autoplay = true
        if (savedInstanceState == null) logNotificationOpen(intent)
        setContent { MazeTheme { AppNav() } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logNotificationOpen(intent)
    }

    /** Taps on our own reminders and pushes carry a source tag; FCM console campaigns are tracked by Firebase itself. */
    private fun logNotificationOpen(intent: android.content.Intent?) {
        val source = intent?.getStringExtra(Notifications.EXTRA_SOURCE) ?: return
        intent.removeExtra(Notifications.EXTRA_SOURCE)
        MazeApp.instance.telemetry.event("notification_tap", "source" to source)
    }

    override fun onResume() {
        super.onResume()
        Notifications.appInForeground = true
    }

    override fun onPause() {
        Notifications.appInForeground = false
        super.onPause()
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
    /** NG Studio ident, shown on every launch before the game's own splash. */
    object StudioIntro : Route()
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
    // Debug only: `adb shell am start -n com.ngstudio.mazebrush/.MainActivity --ei debugLevel 251` opens any level for QA
    // (it does not unlock anything).
    val activity = LocalContext.current as? android.app.Activity
    val debugLevel = if (BuildConfig.DEBUG) activity?.intent?.getIntExtra("debugLevel", 0) ?: 0 else 0
    var stack by remember {
        mutableStateOf(
            if (BuildConfig.DEBUG && debugLevel > 0) listOf(Route.Home(HomeTab.HOME), Route.Game(PlayMode.Campaign(debugLevel)))
            else listOf<Route>(Route.StudioIntro)
        )
    }
    val top = stack.last()

    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = top is Route.Home && top.tab != HomeTab.HOME) { stack = stack.dropLast(1) + Route.Home(HomeTab.HOME) }

    LaunchedEffect(top) {
        app.telemetry.screenView(
            when (top) {
                Route.StudioIntro -> "studio_intro"
                Route.Splash -> "splash"
                Route.Onboarding -> "onboarding"
                is Route.Home -> "home_${top.tab.name.lowercase()}"
                is Route.Game -> if (top.mode is PlayMode.Daily) "game_daily" else "game"
                Route.Settings -> "settings"
            },
        )
        if (top is Route.Onboarding) app.telemetry.tutorialBegin()
        when (top) {
            is Route.Game -> app.sound.music(SoundManager.Music.GAME)
            is Route.Splash, is Route.StudioIntro -> Unit
            else -> app.sound.music(SoundManager.Music.HOME)
        }
    }

    // Android 13+: ask for the notification permission once per launch, on Home (after onboarding, never mid-level).
    var askedNotifications by remember { mutableStateOf(false) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        app.telemetry.event(if (granted) "notif_permission_granted" else "notif_permission_denied")
    }
    val context = LocalContext.current
    LaunchedEffect(top) {
        if (top is Route.Home && !askedNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Notifications.hasPermission(context)
        ) {
            askedNotifications = true
            delay(900) // let the Home transition finish first
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            top,
            transitionSpec = {
                if (initialState is Route.StudioIntro) fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                else (fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f)) togetherWith fadeOut(tween(160))
            },
            contentKey = { r -> if (r is Route.Home) "home" else r },
            label = "nav",
        ) { r ->
            when (r) {
                Route.StudioIntro -> StudioIntroScreen { stack = listOf(Route.Splash) }
                Route.Splash -> SplashScreen {
                    stack = if (store.onboardingDone) listOf(Route.Home(HomeTab.HOME)) else listOf(Route.Onboarding)
                }
                Route.Onboarding -> OnboardingScreen {
                    store.setOnboardingDone()
                    app.telemetry.tutorialComplete()
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
        // Google Play in-app updates: checked a moment after reaching Home; shown only on Home and Settings.
        UpdateOverlay(onHome = top is Route.Home, allowed = top is Route.Home || top is Route.Settings)
    }
}
