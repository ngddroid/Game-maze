package com.ngstudio.maze.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.CandyButton
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.Panel
import com.ngstudio.maze.ui.components.Ribbon
import com.ngstudio.maze.ui.components.Scrim
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.rememberPopIn
import com.ngstudio.maze.ui.components.rememberPulse
import com.ngstudio.maze.ui.theme.Candy

/**
 * Rewarded-ad hook. Today only the debug build has it (a fake 3-second "ad"). When real ads are added,
 * implement [RewardedAds.show] with the ad SDK and set [Ads.rewarded] for release too; the UI does not change.
 */
interface RewardedAds {
    val available: Boolean
    /** True when the UI must show [FakeAdOverlay] itself (debug stand-in). */
    val fake: Boolean
}

object Ads {
    var rewarded: RewardedAds = if (com.ngstudio.maze.BuildConfig.DEBUG) object : RewardedAds {
        override val available = true
        override val fake = true
    } else object : RewardedAds {
        override val available = false
        override val fake = false
    }
}

/**
 * Tapping a locked level in the debug build: "Watch an ad to unlock". The button plays the fake ad, then
 * unlocks exactly that level.
 */
@Composable
fun WatchAdDialog(level: Int, onClose: () -> Unit, onPlay: (Int) -> Unit) {
    val store = MazeApp.instance.store
    var playingAd by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(store.isUnlocked(level)) }
    Scrim(onDismiss = { if (!playingAd) onClose() }) {
        val pop = rememberPopIn()
        Panel(Modifier.width(310.dp).scale(pop)) {
            if (!unlocked) {
                Ribbon("LEVEL $level", color = Candy.sky, dark = Candy.skyDark)
                VSpace(14.dp)
                Box(contentAlignment = Alignment.Center) {
                    GameIcon(Icon.LOCK, Candy.plum, size = 70.dp)
                }
                VSpace(10.dp)
                BodyText("This level is locked.\nWatch a short ad to unlock it now!", align = TextAlign.Center, size = 16.sp)
                VSpace(16.dp)
                CandyButton("WATCH AD", Modifier.fillMaxWidth().scale(rememberPulse(900, 0.04f)), icon = Icon.VIDEO, color = Candy.pink, dark = Candy.pinkDark) {
                    playingAd = true
                }
                VSpace(8.dp)
                CandyButton("NOT NOW", Modifier.fillMaxWidth(), height = 48.dp, textSize = 20.sp, color = Candy.grey, dark = Candy.greyDark) { onClose() }
            } else {
                Ribbon("UNLOCKED!", color = Candy.mint, dark = Candy.mintDark)
                VSpace(14.dp)
                GameText("LEVEL $level", 40.sp, color = Candy.pink, outline = Color.White, outlineWidth = 0.05f)
                VSpace(6.dp)
                BodyText("Ready to play!", align = TextAlign.Center)
                VSpace(16.dp)
                CandyButton("PLAY", Modifier.fillMaxWidth(), icon = Icon.PLAY) { onPlay(level) }
                VSpace(8.dp)
                CandyButton("LATER", Modifier.fillMaxWidth(), height = 48.dp, textSize = 20.sp, color = Candy.grey, dark = Candy.greyDark) { onClose() }
            }
        }
    }
    if (playingAd) FakeAdOverlay {
        playingAd = false
        store.unlockSingle(level)
        MazeApp.instance.telemetry.rewardedAd("unlock_level")
        MazeApp.instance.sound.unlock()
        unlocked = true
    }
}

/** Debug stand-in for a rewarded ad: a 3-second countdown, then the reward. */
@Composable
fun FakeAdOverlay(onRewarded: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(3000, easing = LinearEasing))
        onRewarded()
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
    Box(
        Modifier.fillMaxSize().background(Color(0xF2140B24)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BodyText("TEST AD · debug build", color = Color.White.copy(alpha = 0.7f), size = 13.sp)
            VSpace(18.dp)
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(Color.White.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(14f))
                    drawArc(Candy.pink, -90f, 360f * p.value, false, style = Stroke(14f, cap = StrokeCap.Round), topLeft = Offset(0f, 0f), size = Size(size.width, size.height))
                }
                GameText("${(3 - (p.value * 3).toInt()).coerceAtLeast(1)}", 64.sp, color = Color.White, outline = Candy.pinkDark)
            }
            VSpace(18.dp)
            GameText("Ad playing…", 28.sp, color = Color.White, outline = Candy.plum)
            VSpace(6.dp)
            BodyText("The real rewarded ad goes here later.", color = Color.White.copy(alpha = 0.7f), size = 13.sp)
        }
    }
}
}
