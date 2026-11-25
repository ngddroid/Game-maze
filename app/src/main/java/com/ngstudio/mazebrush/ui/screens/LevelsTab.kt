package com.ngstudio.mazebrush.ui.screens

import com.ngstudio.mazebrush.ui.theme.Clean
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.mazebrush.MazeApp
import com.ngstudio.mazebrush.data.LevelRepository
import com.ngstudio.mazebrush.render.WorldTheme
import com.ngstudio.mazebrush.ui.components.BodyText
import com.ngstudio.mazebrush.ui.components.GameIcon
import com.ngstudio.mazebrush.ui.components.EngravedText
import com.ngstudio.mazebrush.ui.components.GameText
import com.ngstudio.mazebrush.ui.components.Icon
import com.ngstudio.mazebrush.ui.components.StarIcon
import com.ngstudio.mazebrush.ui.components.VSpace
import com.ngstudio.mazebrush.ui.components.color
import com.ngstudio.mazebrush.ui.components.rememberPulse
import com.ngstudio.mazebrush.ui.components.tapFeedback
import com.ngstudio.mazebrush.ui.theme.Candy
import com.ngstudio.mazebrush.ui.theme.Pro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PER_CHAPTER = WorldTheme.LEVELS_PER_CHAPTER

@Composable
fun LevelsTab(onPlay: (Int) -> Unit) {
    val store = MazeApp.instance.store
    val chapters = LevelRepository.TOTAL / PER_CHAPTER
    var chapter by remember { mutableIntStateOf(WorldTheme.chapterOf(store.currentLevel)) }
    var adFor by remember { mutableStateOf<Int?>(null) }
    var lockedMsg by remember { mutableStateOf<Int?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rowState = rememberLazyListState()
    LaunchedEffect(Unit) { rowState.scrollToItem((chapter - 1).coerceAtLeast(0)) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            androidx.compose.material3.Text("LEVELS", Modifier.align(Alignment.CenterHorizontally), style = androidx.compose.ui.text.TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = 34.sp, color = Clean.ink, letterSpacing = 1.sp))
            VSpace(6.dp)
            LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chapters) { c ->
                    ChapterChip(c, c == chapter) { chapter = c }
                }
            }
            VSpace(8.dp)
            val theme = WorldTheme.forLevel(chapter * PER_CHAPTER + 1)
            Box(Modifier.align(Alignment.CenterHorizontally).shadow(2.dp, RoundedCornerShape(14.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow).background(Color.White, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp)) {
                BodyText("Chapter ${chapter + 1}  ·  Levels ${chapter * PER_CHAPTER + 1}-${(chapter + 1) * PER_CHAPTER}", size = 14.sp, color = Clean.inkSoft, shadow = false)
            }
            val gridState = rememberLazyGridState()
            LaunchedEffect(chapter) {
                val cur = store.currentLevel
                if (WorldTheme.chapterOf(cur) == chapter) gridState.scrollToItem(((cur - 1) % PER_CHAPTER - 5).coerceAtLeast(0)) else gridState.scrollToItem(0)
            }
            LazyVerticalGrid(
                GridCells.Fixed(5), Modifier.fillMaxSize(), state = gridState,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(PER_CHAPTER) { i ->
                    val n = chapter * PER_CHAPTER + i + 1
                    LevelBubble(n, theme, Modifier.graphicsLayer { if (lockedMsg == n) translationX = shake.value }) {
                        if (store.isUnlocked(n)) onPlay(n)
                        else if (Ads.rewarded.available) adFor = n // debug fake ad only; release: no ads, so finish the previous level
                        else {
                            MazeApp.instance.sound.bonk()
                            lockedMsg = n
                            scope.launch {
                                shake.snapTo(0f)
                                for (k in 0 until 6) shake.animateTo(if (k % 2 == 0) 14f else -14f, tween(45))
                                shake.animateTo(0f, tween(45))
                                delay(1600)
                                if (lockedMsg == n) lockedMsg = null
                            }
                        }
                    }
                }
            }
        }
        val msg = lockedMsg
        if (msg != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp).shadow(8.dp, RoundedCornerShape(18.dp))
                    .background(Clean.ink, RoundedCornerShape(18.dp)).border(2.dp, Color.White, RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(Icon.LOCK, Color.White, size = 20.dp)
                    Box(Modifier.width(8.dp))
                    BodyText("Finish level ${store.currentLevel} first", color = Color.White, size = 15.sp)
                }
            }
        }
        adFor?.let { n -> WatchAdDialog(n, onClose = { adFor = null }, onPlay = { adFor = null; onPlay(it) }) }
    }
}

@Composable
private fun ChapterChip(c: Int, selected: Boolean, onClick: () -> Unit) {
    val store = MazeApp.instance.store
    val first = c * PER_CHAPTER + 1
    val unlocked = store.isUnlocked(first) || (first..first + PER_CHAPTER - 1).any { store.isUnlocked(it) }
    store.starsVersion
    val stars = remember(store.starsVersion, c) { (first until first + PER_CHAPTER).sumOf { store.starsOf(it) } }
    val ink = if (selected) Color.White else Clean.ink
    val soft = if (selected) Color.White.copy(alpha = 0.85f) else Clean.inkSoft
    Column(
        Modifier.width(92.dp).scale(if (selected) 1.05f else 1f)
            .shadow(if (selected) 8.dp else 3.dp, RoundedCornerShape(18.dp), ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
            .background(if (selected) Brush.verticalGradient(listOf(Clean.tealLight, Clean.teal)) else Brush.verticalGradient(listOf(Color.White, Color.White)), RoundedCornerShape(18.dp))
            .clickable { tapFeedback(); onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Text("CH ${c + 1}", style = androidx.compose.ui.text.TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = 20.sp, color = ink))
        BodyText("$first-${first + PER_CHAPTER - 1}", size = 11.sp, color = soft, shadow = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unlocked) { StarIcon(true, size = 14.dp); BodyText(" $stars", size = 12.sp, color = ink, shadow = false) }
            else GameIcon(Icon.LOCK, soft, size = 16.dp)
        }
    }
}

@Composable
private fun LevelBubble(n: Int, theme: WorldTheme, modifier: Modifier, onClick: () -> Unit) {
    val store = MazeApp.instance.store
    store.starsVersion
    val unlocked = store.isUnlocked(n)
    val stars = store.starsOf(n)
    val current = n == store.currentLevel
    // clean style: done = teal, current = coral (pulsing), open but not done = white with a teal number, locked = white with a grey lock
    val fill = when {
        current -> Clean.coral
        unlocked && stars > 0 -> Clean.teal
        else -> Color.White
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).scale(if (current) rememberPulse(900, 0.06f) else 1f)
                .shadow(if (unlocked) 5.dp else 2.dp, CircleShape, ambientColor = Clean.pillShadow, spotColor = Clean.pillShadow)
                .background(if (fill == Color.White) Brush.verticalGradient(listOf(Color.White, Color.White)) else Brush.verticalGradient(listOf(lerp(fill, Color.White, 0.22f), fill)), CircleShape)
                .border(if (unlocked && fill == Color.White) 2.dp else 0.dp, if (unlocked && fill == Color.White) Clean.teal else Color.Transparent, CircleShape)
                .clickable { tapFeedback(); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            val numStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.ngstudio.mazebrush.ui.theme.Lilita, fontSize = if (n >= 1000) 16.sp else 21.sp)
            when {
                !unlocked -> GameIcon(Icon.LOCK, Clean.inkSoft.copy(alpha = 0.45f), size = 20.dp)
                fill == Color.White -> androidx.compose.material3.Text("$n", style = numStyle.copy(color = Clean.teal))
                else -> androidx.compose.material3.Text("$n", style = numStyle.copy(color = Color.White))
            }
        }
        Row(Modifier.height(14.dp).padding(top = 2.dp)) {
            if (unlocked && stars > 0) for (k in 1..3) StarIcon(k <= stars, size = 12.dp)
            else if (!unlocked) BodyText("$n", size = 10.sp, color = Clean.inkSoft.copy(alpha = 0.7f), shadow = false)
        }
    }
}
