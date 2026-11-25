package com.ngstudio.mazebrush.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ngstudio.mazebrush.MazeApp

/** Lit pictures of the ball skins (assets/balls/icon_<id>.png, rendered by `:tools:run --args="balls"`), cached. */
object BallIcons {
    private val cache = HashMap<Int, ImageBitmap?>()
    fun get(id: Int): ImageBitmap? = synchronized(cache) {
        cache.getOrPut(id) {
            runCatching { MazeApp.instance.assets.open("balls/icon_$id.png").use { BitmapFactory.decodeStream(it).asImageBitmap() } }.getOrNull()
        }
    }
}

/** A ball skin as it looks in the game (shop cards, the New Item bar, onboarding). */
@Composable
fun BallIcon(id: Int, modifier: Modifier = Modifier) {
    val img = remember(id) { BallIcons.get(id) } ?: return
    Image(img, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
}
