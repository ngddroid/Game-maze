package com.ngstudio.maze.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.data.GameStore
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.CandyButton
import com.ngstudio.maze.ui.components.CoinIcon
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.EngravedText
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.HSpace
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.Panel
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.theme.Candy

class Achievement(val id: String, val title: String, val desc: String, val icon: Icon, val target: Long, val reward: Int, val value: (GameStore) -> Long)

val ACHIEVEMENTS = listOf(
    Achievement("first_win", "First Splash", "Finish your first level", Icon.PLAY, 1, 20) { it.completedCount.toLong() },
    Achievement("lv10", "Getting Rolling", "Finish 10 levels", Icon.LEVELS, 10, 50) { it.completedCount.toLong() },
    Achievement("lv50", "Paint Explorer", "Finish 50 levels", Icon.LEVELS, 50, 100) { it.completedCount.toLong() },
    Achievement("lv100", "Century Painter", "Finish 100 levels", Icon.LEVELS, 100, 200) { it.completedCount.toLong() },
    Achievement("lv250", "World Traveler", "Finish 250 levels", Icon.LEVELS, 250, 300) { it.completedCount.toLong() },
    Achievement("lv500", "Maze Master", "Finish 500 levels", Icon.TROPHY, 500, 500) { it.completedCount.toLong() },
    Achievement("lv1000", "Legend of the Glide", "Finish 1,000 levels", Icon.TROPHY, 1000, 800) { it.completedCount.toLong() },
    Achievement("lv2500", "Unstoppable", "Finish 2,500 levels", Icon.TROPHY, 2500, 1200) { it.completedCount.toLong() },
    Achievement("lv5000", "Every Last Tile", "Finish all 5,000 levels", Icon.TROPHY, 5000, 3000) { it.completedCount.toLong() },
    Achievement("stars30", "Star Collector", "Collect 30 stars", Icon.STAR, 30, 50) { it.totalStars.toLong() },
    Achievement("stars300", "Star Hoarder", "Collect 300 stars", Icon.STAR, 300, 200) { it.totalStars.toLong() },
    Achievement("stars1500", "Galaxy of Stars", "Collect 1,500 stars", Icon.STAR, 1500, 600) { it.totalStars.toLong() },
    Achievement("perfect10", "Flawless", "Get 3 stars on 10 levels", Icon.RATE, 10, 80) { it.perfects.toLong() },
    Achievement("perfect100", "Perfectionist", "Get 3 stars on 100 levels", Icon.RATE, 100, 300) { it.perfects.toLong() },
    Achievement("tiles1k", "Busy Brush", "Paint 1,000 tiles", Icon.PAINT, 1000, 60) { it.tilesPainted },
    Achievement("tiles25k", "Paint Factory", "Paint 25,000 tiles", Icon.PAINT, 25000, 300) { it.tilesPainted },
    Achievement("tiles100k", "Ocean of Paint", "Paint 100,000 tiles", Icon.PAINT, 100000, 800) { it.tilesPainted },
    Achievement("daily1", "Daily Dabbler", "Finish a Daily Challenge", Icon.CALENDAR, 1, 30) { it.dailiesDone.toLong() },
    Achievement("daily7", "Week Streak", "Reach a 7-day daily streak", Icon.FLAME, 7, 150) { it.bestDailyStreak.toLong() },
    Achievement("daily30", "Monthly Master", "Reach a 30-day daily streak", Icon.FLAME, 30, 300) { it.bestDailyStreak.toLong() },
    Achievement("chests10", "Treasure Hunter", "Open 10 chapter chests", Icon.CHEST, 10, 100) { it.chestsOpened.toLong() },
    Achievement("skins3", "Fashionista", "Own 3 balls", Icon.BALL, 3, 80) { it.ownedBalls.size.toLong() },
    Achievement("hint1", "Bright Idea", "Use a hint", Icon.HINT, 1, 10) { it.hintsUsed.toLong() },
)

/** Number of finished, unclaimed achievements (for badges). */
fun claimableAchievements(store: GameStore) = ACHIEVEMENTS.count { it.id !in store.claimedAchievements && it.value(store) >= it.target }

@Composable
fun TrophiesTab() {
    val store = MazeApp.instance.store
    store.starsVersion
    val sorted = ACHIEVEMENTS.sortedBy {
        val done = it.value(store) >= it.target
        val claimed = it.id in store.claimedAchievements
        when { done && !claimed -> 0; !done -> 1; else -> 2 }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                EngravedText("TROPHIES", 34.sp, com.ngstudio.maze.render.WorldTheme.forLevel(store.currentLevel).ink.color())
                VSpace(8.dp)
                Panel(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("${store.completedCount}", "Levels")
                        Stat("${store.totalStars}", "Stars")
                        Stat("${store.perfects}", "Perfect")
                    }
                    VSpace(10.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat(compact(store.tilesPainted), "Tiles")
                        Stat(compact(store.swipes), "Swipes")
                        Stat("${store.bestDailyStreak}", "Best streak")
                    }
                }
                VSpace(4.dp)
            }
        }
        items(sorted, key = { it.id }) { a -> AchievementRow(a, store) }
    }
}

private fun compact(v: Long): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000.0)
    v >= 10_000 -> "%.1fk".format(v / 1000.0)
    else -> v.toString()
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EngravedText(value, 26.sp, Candy.mintDark)
        BodyText(label, size = 12.sp, color = Candy.greyDark)
    }
}

@Composable
private fun AchievementRow(a: Achievement, store: GameStore) {
    val v = a.value(store)
    val done = v >= a.target
    val claimed = a.id in store.claimedAchievements
    val accent = when { claimed -> Candy.mint; done -> Candy.orange; else -> Candy.lilac }
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Candy.cream)), RoundedCornerShape(20.dp))
            .border(2.dp, if (done && !claimed) Candy.orange else Color.White, RoundedCornerShape(20.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).background(Brush.verticalGradient(listOf(lerp(accent, Color.White, 0.3f), accent)), CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            GameIcon(a.icon, Color.White, size = 26.dp)
        }
        HSpace(12.dp)
        Column(Modifier.weight(1f)) {
            GameText(a.title, 18.sp, color = Candy.ink, outline = Color.Transparent, outlineWidth = 0f, align = androidx.compose.ui.text.style.TextAlign.Start)
            BodyText(a.desc, size = 12.sp, color = Candy.greyDark)
            VSpace(4.dp)
            val f = (v.toFloat() / a.target).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFF1E6D6), RoundedCornerShape(5.dp))) {
                Box(Modifier.fillMaxWidth(f).height(10.dp).background(accent, RoundedCornerShape(5.dp)))
            }
            BodyText("${v.coerceAtMost(a.target)} / ${a.target}", size = 11.sp, color = Candy.greyDark)
        }
        HSpace(10.dp)
        when {
            claimed -> GameIcon(Icon.CHECK, Candy.mint, size = 30.dp)
            done -> CandyButton("+${a.reward}", Modifier.width(86.dp), height = 40.dp, textSize = 17.sp, color = Candy.orange, dark = Candy.orangeDark) {
                store.claimAchievement(a.id, a.reward); MazeApp.instance.sound.coin()
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) { CoinIcon(size = 18.dp); BodyText(" ${a.reward}", size = 13.sp, color = Candy.greyDark) }
        }
    }
}
