package com.ngstudio.mazebrush.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.data.GameStore
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.CandyButton
import com.ngstudio.mazebrush.ui.components.CoinIcon
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.HSpace
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.MiniBoard
import com.ngstudio.mazebrush.ui.components.Panel
import com.ngstudio.mazebrush.ui.components.Ribbon
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.rememberPulse
import com.ngstudio.mazebrush.ui.theme.Candy
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DailyTab(onPlayDaily: (Long) -> Unit) {
    val app = MazeApp.instance
    val store = app.store
    val today = store.today()
    val level = remember(today) { app.levels.dailyLevel(today) }
    val theme = WorldTheme.ALL[(today % WorldTheme.ALL.size).toInt()]
    var claimedNow by remember { mutableStateOf<Int?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val untilMidnight = run {
        val local = now + TimeZone.getDefault().getOffset(now)
        86_400_000L - (local % 86_400_000L)
    }
    val hms = "%dh %02dm".format(untilMidnight / 3_600_000, (untilMidnight / 60_000) % 60)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.Text("DAILY", style = androidx.compose.ui.text.TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = 34.sp, color = com.ngstudio.mazebrush.ui.theme.Clean.ink, letterSpacing = 1.sp))
        VSpace(8.dp)
        Panel(Modifier.fillMaxWidth(), padding = 16.dp) {
            Ribbon("DAILY CHALLENGE", color = Candy.sky, dark = Candy.skyDark)
            VSpace(6.dp)
            BodyText(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)), size = 14.sp, color = Candy.greyDark)
            VSpace(8.dp)
            val dailyBoard = com.ngstudio.mazebrush.render.BoardTheme.forDay(store.today())
            com.ngstudio.mazebrush.ui.components.CleanMiniBoard(level, dailyBoard, store.ballSkin, Modifier.fillMaxWidth().height(170.dp),
                paint = com.ngstudio.mazebrush.render.PaintColor.byId(store.paintColor).rgb.color())
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameIcon(Icon.FLAME, Candy.orange, size = 26.dp)
                HSpace(4.dp)
                BodyText("Streak ${store.liveDailyStreak}", size = 15.sp, color = Candy.orangeDark)
                HSpace(16.dp)
                CoinIcon(size = 22.dp)
                HSpace(4.dp)
                BodyText("+${50 + (10 * store.liveDailyStreak).coerceAtMost(100)}", size = 15.sp, color = Candy.goldDark)
            }
            VSpace(12.dp)
            if (store.dailyDoneToday) {
                CandyButton("DONE!", Modifier.fillMaxWidth(), enabled = false, icon = Icon.CHECK) {}
                VSpace(6.dp)
                BodyText("New challenge in $hms", size = 14.sp, color = Candy.greyDark)
            } else {
                CandyButton("PLAY", Modifier.fillMaxWidth().scale(rememberPulse()), color = Candy.sky, dark = Candy.skyDark, icon = Icon.PLAY) { onPlayDaily(today) }
            }
        }
        VSpace(16.dp)
        Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
            Ribbon("DAILY REWARD", color = Candy.orange, dark = Candy.orangeDark)
            VSpace(10.dp)
            val next = store.calendarNextIndex
            val ready = store.calendarReady
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in 0 until 4) DayTile(d, next, ready, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in 4 until 7) DayTile(d, next, ready, Modifier.weight(if (d == 6) 2f else 1f))
                }
            }
            VSpace(12.dp)
            if (ready) {
                CandyButton("CLAIM ${GameStore.CALENDAR[next]}", Modifier.fillMaxWidth().scale(rememberPulse()), color = Candy.orange, dark = Candy.orangeDark, icon = Icon.GIFT) {
                    val got = store.claimCalendar()
                    if (got > 0) { app.sound.coin(); claimedNow = got; app.telemetry.earn("coins", got.toLong(), "calendar") }
                }
            } else {
                BodyText(claimedNow?.let { "+$it coins claimed! Come back tomorrow." } ?: "Come back in $hms for your next reward.", size = 14.sp, color = Candy.greyDark, align = TextAlign.Center)
            }
        }
        VSpace(60.dp)
    }
}

@Composable
private fun DayTile(d: Int, next: Int, ready: Boolean, modifier: Modifier) {
    val claimed = if (ready) d < next else (next == 0 || d < next)
    val isToday = ready && d == next
    val color = when { claimed -> Candy.mint; isToday -> Candy.orange; else -> Color(0xFFF1E6D6) }
    Column(
        modifier.scale(if (isToday) rememberPulse(800, 0.05f) else 1f).shadow(if (isToday) 8.dp else 2.dp, RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color)), RoundedCornerShape(16.dp))
            .border(2.dp, Color.White, RoundedCornerShape(16.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BodyText("Day ${d + 1}", size = 12.sp, color = if (claimed || isToday) Color.White else Candy.greyDark)
        Box(contentAlignment = Alignment.Center) {
            if (d == 6) GameIcon(Icon.CHEST, if (claimed) Color.White else Candy.orange, size = 34.dp) else CoinIcon(size = 30.dp)
            if (claimed) Box(Modifier.background(Candy.mint, RoundedCornerShape(10.dp))) { GameIcon(Icon.CHECK, Color.White, size = 22.dp) }
        }
        BodyText(if (d == 6) "${GameStore.CALENDAR[d]} + hint" else "${GameStore.CALENDAR[d]}", size = 12.sp, color = if (claimed || isToday) Color.White else Candy.ink)
    }
}
