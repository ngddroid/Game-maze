package com.ngstudio.mazebrush.ui.screens

import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.BuildConfig
import com.ngstudio.mazebrush.LegalActivity
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.game.DebugFlags
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.SurfaceBackground
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.Ribbon
import com.ngstudio.mazebrush.ui.components.RoundButton
import com.ngstudio.mazebrush.ui.components.Scrim
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.rememberPopIn
import com.ngstudio.mazebrush.ui.components.tapFeedback
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.theme.Candy
import androidx.compose.ui.draw.scale

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = MazeApp.instance
    val store = app.store
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    BackHandler { if (confirmReset) confirmReset = false else onBack() }
    SurfaceBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(Icon.BACK, size = 46.dp) { onBack() }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EngravedText("SETTINGS", 32.sp, com.ngstudio.mazebrush.ui.components.LocalBackdrop.current.ink.color()) }
                Box(Modifier.width(52.dp))
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                VSpace(8.dp)
                Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
                    ToggleRow(Icon.SOUND, "Sound effects", Candy.pink, store.sound) { store.toggleSound() }
                    Divider()
                    ToggleRow(Icon.MUSIC, "Music", Candy.sky, store.music) { store.toggleMusic(); app.sound.refreshMusic() }
                    Divider()
                    ToggleRow(Icon.VIBRATE, "Vibration", Candy.mint, store.vibration) { store.toggleVibration(); app.haptics.light() }
                }
                VSpace(14.dp)
                Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
                    LinkRow(Icon.RATE, "Rate the game", Candy.lemon) { openStore(ctx) }
                    Divider()
                    LinkRow(Icon.SHARE, "Share with friends", Candy.sky) { share(ctx) }
                    Divider()
                    LinkRow(Icon.DOC, "Terms of Use", Candy.orange) { LegalActivity.open(ctx, LegalActivity.PAGE_TERMS) }
                    Divider()
                    LinkRow(Icon.SHIELD, "Privacy Policy", Candy.mint) { LegalActivity.open(ctx, LegalActivity.PAGE_PRIVACY) }
                }
                if (BuildConfig.DEBUG) {
                    VSpace(14.dp)
                    Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Ribbon("DEBUG TOOLS", color = com.ngstudio.mazebrush.ui.theme.Clean.inkSoft, dark = com.ngstudio.mazebrush.ui.theme.Clean.ink)
                        VSpace(8.dp)
                        ToggleRow(Icon.PLAY, "Autoplay solutions", com.ngstudio.mazebrush.ui.theme.Clean.inkSoft, DebugFlags.autoplay) { DebugFlags.autoplay = !DebugFlags.autoplay }
                        Divider()
                        LinkRow(Icon.LEVELS, "Unlock all 5000 levels", com.ngstudio.mazebrush.ui.theme.Clean.inkSoft) { store.debugUnlockAll(); app.sound.unlock() }
                        Divider()
                        LinkRow(Icon.COIN, "+1000 coins", com.ngstudio.mazebrush.ui.theme.Clean.inkSoft) { store.addCoins(1000); app.sound.coin() }
                        Divider()
                        LinkRow(Icon.RESTART, "Reset all progress", Candy.pink) { confirmReset = true }
                    }
                }
                VSpace(18.dp)
                BodyText("Maze Brush  ·  v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", size = 13.sp, color = com.ngstudio.mazebrush.ui.components.LocalBackdrop.current.ink.color(), align = TextAlign.Center)
                VSpace(40.dp)
            }
        }
        if (confirmReset) Scrim(onDismiss = { confirmReset = false }) {
            Panel(Modifier.width(300.dp).scale(rememberPopIn())) {
                Ribbon("RESET?")
                VSpace(12.dp)
                BodyText("All levels, stars, coins and skins go back to the start.", align = TextAlign.Center)
                VSpace(14.dp)
                CandyButton("RESET", Modifier.fillMaxWidth(), color = Candy.pink, dark = Candy.pinkDark) { store.debugReset(); confirmReset = false; onBack() }
                VSpace(8.dp)
                CandyButton("CANCEL", Modifier.fillMaxWidth(), height = 48.dp, textSize = 20.sp, color = Candy.grey, dark = Candy.greyDark) { confirmReset = false }
            }
        }
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.5.dp).background(Color(0xFFEFE3D2)))

@Composable
private fun RowIcon(icon: Icon, color: Color) {
    Box(Modifier.size(40.dp).background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color)), CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
        GameIcon(icon, Color.White, size = 22.dp)
    }
}

@Composable
private fun ToggleRow(icon: Icon, label: String, color: Color, on: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { tapFeedback(); onToggle() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon, color)
        HSpace(12.dp)
        BodyText(label, Modifier.weight(1f), size = 17.sp)
        CandySwitch(on)
    }
}

@Composable
private fun LinkRow(icon: Icon, label: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { tapFeedback(); onClick() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon, color)
        HSpace(12.dp)
        BodyText(label, Modifier.weight(1f), size = 17.sp)
        Box(Modifier.size(24.dp).scale(-1f, 1f)) { GameIcon(Icon.BACK, Candy.grey, size = 20.dp) }
    }
}

@Composable
private fun CandySwitch(on: Boolean) {
    val x by animateFloatAsState(if (on) 1f else 0f, label = "sw")
    Box(
        Modifier.width(58.dp).height(32.dp).shadow(2.dp, RoundedCornerShape(16.dp))
            .background(lerp(Candy.grey, Candy.mint, x), RoundedCornerShape(16.dp)).border(2.dp, Color.White, RoundedCornerShape(16.dp)),
    ) {
        Box(Modifier.offset(x = (3 + 26 * x).dp, y = 3.dp).size(26.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
    }
}

private fun openStore(ctx: Context) {
    val pkg = ctx.packageName
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (e: ActivityNotFoundException) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun share(ctx: Context) {
    val text = "Slide the ball and paint every tile! Play Maze Brush: https://play.google.com/store/apps/details?id=${ctx.packageName}"
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
