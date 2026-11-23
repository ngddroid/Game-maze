package com.ngstudio.maze.ui.screens

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
import com.ngstudio.maze.BuildConfig
import com.ngstudio.maze.MazeApp
import com.ngstudio.maze.data.LevelRepository
import com.ngstudio.maze.render.WorldTheme
import com.ngstudio.maze.ui.components.BodyText
import com.ngstudio.maze.ui.components.GameIcon
import com.ngstudio.maze.ui.components.EngravedText
import com.ngstudio.maze.ui.components.GameText
import com.ngstudio.maze.ui.components.Icon
import com.ngstudio.maze.ui.components.StarIcon
import com.ngstudio.maze.ui.components.VSpace
import com.ngstudio.maze.ui.components.color
import com.ngstudio.maze.ui.components.rememberPulse
import com.ngstudio.maze.ui.components.tapFeedback
import com.ngstudio.maze.ui.theme.Candy
import com.ngstudio.maze.ui.theme.Pro
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
            EngravedText("LEVELS", 34.sp, WorldTheme.forLevel(store.currentLevel).ink.color(), Modifier.align(Alignment.CenterHorizontally))
            VSpace(6.dp)
            LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chapters) { c ->
                    ChapterChip(c, c == chapter) { chapter = c }
                }
            }
            VSpace(8.dp)
            val theme = WorldTheme.forLevel(chapter * PER_CHAPTER + 1)
            Box(Modifier.align(Alignment.CenterHorizontally).background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 4.dp)) {
                BodyText("Chapter ${chapter + 1}  ·  ${theme.name}", size = 14.sp, color = theme.accentDark.color())
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
                        else if (BuildConfig.DEBUG && Ads.rewarded.available) adFor = n
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
                    .background(Candy.plum, RoundedCornerShape(18.dp)).border(2.dp, Color.White, RoundedCornerShape(18.dp))
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
    val theme = WorldTheme.forLevel(first)
    val unlocked = store.isUnlocked(first) || (first..first + PER_CHAPTER - 1).any { it in store.extraUnlocked }
    store.starsVersion
    val stars = remember(store.starsVersion, c) { (first until first + PER_CHAPTER).sumOf { store.starsOf(it) } }
    Column(
        Modifier.width(92.dp).scale(if (selected) 1.06f else 1f)
            .shadow(if (selected) 10.dp else 4.dp, RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Pro.chrome, Pro.chromeDark)), RoundedCornerShape(18.dp))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) Pro.gold else Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .clickable { tapFeedback(); onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EngravedText("CH ${c + 1}", 20.sp, if (selected) Pro.gold else Pro.cream)
        BodyText("$first-${first + PER_CHAPTER - 1}", size = 11.sp, color = Pro.creamDim, shadow = false)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (unlocked) { StarIcon(true, size = 14.dp); BodyText(" $stars", size = 12.sp, color = Pro.cream, shadow = false) }
            else GameIcon(Icon.LOCK, Pro.creamDim, size = 16.dp)
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
    val color = when {
        !unlocked -> Pro.chromeDark
        current -> Pro.gold
        stars > 0 -> Pro.cream
        else -> Candy.sky
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).scale(if (current) rememberPulse(900, 0.06f) else 1f)
                .shadow(5.dp, CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.3f), color)), CircleShape)
                .border(if (unlocked) 2.dp else 1.dp, if (unlocked) Pro.border else Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable { tapFeedback(); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked && (stars > 0 || current)) EngravedText("$n", if (n >= 1000) 16.sp else 21.sp, Pro.text)
            else if (unlocked) GameText("$n", if (n >= 1000) 15.sp else 20.sp, outline = lerp(color, Color.Black, 0.35f), outlineWidth = 0.08f)
            else GameIcon(Icon.LOCK, Pro.creamDim, size = 22.dp)
        }
        Row(Modifier.height(14.dp).padding(top = 2.dp)) {
            if (unlocked && stars > 0) for (k in 1..3) StarIcon(k <= stars, size = 12.dp)
            else if (!unlocked) BodyText("$n", size = 10.sp, color = Pro.cream)
        }
    }
}
