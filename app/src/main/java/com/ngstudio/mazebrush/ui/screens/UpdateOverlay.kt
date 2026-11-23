package com.ngstudio.mazebrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.PaintProgressBar
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.Scrim
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.rememberPulse
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Pro
import com.ngstudio.mazebrush.update.UpdateUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything the player sees of a Google Play in-app update: the "update available" dialog, the
 * download progress banner, the "update ready -> RESTART" banner and failures. It sits on top of all
 * screens, but only shows on Home and Settings (never mid-level, over the intro or onboarding).
 */
@Composable
fun UpdateOverlay(onHome: Boolean, allowed: Boolean) {
    val updater = MazeApp.instance.updater
    val ui by updater.ui.collectAsState()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        updater.onFlowResult(r.resultCode)
    }

    // First check of the launch: a moment after landing on Home, so it never fights the intro.
    LaunchedEffect(onHome) {
        if (onHome) {
            delay(2500)
            updater.check(launcher, firstOfLaunch = true)
        }
    }
    // On every resume: pick up a finished background download, or a forced update in progress.
    LifecycleResumeEffect(Unit) {
        val job = scope.launch { updater.check(launcher, firstOfLaunch = false) }
        onPauseOrDispose { job.cancel() }
    }
    LaunchedEffect(ui) {
        if (ui == UpdateUi.Failed) {
            delay(3500)
            updater.dismissFailed()
        }
    }
    BackHandler(enabled = allowed && ui == UpdateUi.Available) { updater.later() }

    // Keep the last banner state so its exit animation doesn't flash empty content.
    var lastBanner by remember { mutableStateOf<UpdateUi>(UpdateUi.Downloading(0f)) }
    if (ui != UpdateUi.Available && ui != UpdateUi.Idle) lastBanner = ui

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = allowed && ui != UpdateUi.Idle && ui != UpdateUi.Available,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp, start = 14.dp, end = 14.dp),
            enter = slideInVertically(tween(320)) { -it * 2 } + fadeIn(tween(320)),
            exit = slideOutVertically(tween(260)) { -it * 2 } + fadeOut(tween(260)),
        ) {
            UpdateBanner(lastBanner, onRestart = updater::completeUpdate, onLater = updater::dismissReady)
        }
        AnimatedVisibility(
            visible = allowed && ui == UpdateUi.Available,
            enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.9f),
            exit = fadeOut(tween(180)),
        ) {
            Scrim(alpha = 0.55f, onDismiss = { updater.later() }) {
                UpdateDialog(onLater = updater::later, onUpdate = { updater.acceptFlexible(launcher) })
            }
        }
    }
}

@Composable
private fun UpdateDialog(onLater: () -> Unit, onUpdate: () -> Unit) {
    Panel(Modifier.padding(horizontal = 28.dp).fillMaxWidth()) {
        Box(
            Modifier.size(76.dp).scale(rememberPulse(700, 0.08f)).shadow(8.dp, CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF8FE36A), Candy.mint, Candy.mintDark)), CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { GameIcon(Icon.NEXT, Color.White, size = 38.dp) }
        VSpace(12.dp)
        EngravedText("UPDATE AVAILABLE", 28.sp, Color(0xFF5E2F14))
        VSpace(6.dp)
        BodyText(
            "A new version is ready. Keep playing while it downloads.",
            size = 15.sp, color = Color(0xFF7A4A2A), align = TextAlign.Center,
        )
        VSpace(18.dp)
        CandyButton("UPDATE", Modifier.fillMaxWidth(), height = 58.dp, textSize = 26.sp, icon = Icon.NEXT) { onUpdate() }
        VSpace(10.dp)
        CandyButton("LATER", Modifier.fillMaxWidth(), height = 46.dp, textSize = 20.sp, color = Pro.chrome, dark = Pro.chromeLip) { onLater() }
    }
}

@Composable
private fun UpdateBanner(state: UpdateUi, onRestart: () -> Unit, onLater: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth()
            .shadow(10.dp, shape, spotColor = Pro.chromeLip)
            .background(Brush.verticalGradient(listOf(Color.White, Candy.cream, lerp(Candy.cream, Pro.border, 0.25f))), shape)
            .border(2.5.dp, Brush.verticalGradient(listOf(Pro.rimLight, Pro.rimDark)), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            is UpdateUi.Downloading -> {
                Badge(Icon.NEXT, Candy.sky)
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BodyText("Downloading update", Modifier.weight(1f), size = 15.sp, color = Color(0xFF5E2F14), shadow = false)
                        BodyText("${(state.progress * 100).toInt()}%", size = 15.sp, color = Candy.mintDark, shadow = false)
                    }
                    VSpace(6.dp)
                    PaintProgressBar(state.progress, Candy.mint, Modifier.fillMaxWidth(), height = 12.dp)
                }
            }
            UpdateUi.ReadyToInstall -> {
                Badge(Icon.CHECK, Candy.mint)
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    BodyText("Update ready!", size = 16.sp, color = Color(0xFF5E2F14), shadow = false)
                    BodyText("Restart to finish installing.", size = 12.sp, color = Color(0xFF8A5530), shadow = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CandyButton("LATER", height = 38.dp, textSize = 14.sp, color = Pro.chrome, dark = Pro.chromeLip) { onLater() }
                    CandyButton("RESTART", height = 38.dp, textSize = 15.sp) { onRestart() }
                }
            }
            UpdateUi.Installing -> {
                val angle by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "a")
                Badge(Icon.RESTART, Candy.orange, Modifier.rotate(angle))
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    BodyText("Installing update...", size = 16.sp, color = Color(0xFF5E2F14), shadow = false)
                    BodyText("The game will restart in a moment.", size = 12.sp, color = Color(0xFF8A5530), shadow = false)
                }
            }
            UpdateUi.Failed -> {
                Badge(Icon.CLOSE, Candy.pink)
                HSpace(12.dp)
                Column(Modifier.weight(1f)) {
                    BodyText("Update failed", size = 16.sp, color = Color(0xFF5E2F14), shadow = false)
                    BodyText("No worries, we'll try again next time.", size = 12.sp, color = Color(0xFF8A5530), shadow = false)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun Badge(icon: Icon, color: Color, iconModifier: Modifier = Modifier) {
    Box(
        Modifier.size(40.dp).background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.2f), color)), CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { GameIcon(icon, Color.White, iconModifier, size = 22.dp) }
}
