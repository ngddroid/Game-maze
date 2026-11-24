package com.ngstudio.mazebrush.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Debug-build-only switches (Settings > Debug tools). Not saved. */
object DebugFlags {
    /** Plays each level's stored solution automatically. */
    var autoplay by mutableStateOf(false)
    /** Forces the chapter background (0-based [com.ngstudio.mazebrush.render.Backdrop] index); -1 = follow progress. */
    var backdrop by mutableStateOf(-1)
}
