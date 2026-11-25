package com.ngstudio.mazebrush.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Debug-build-only switches (Settings > Debug tools). Not saved. */
object DebugFlags {
    /** Plays each level's stored solution automatically. */
    var autoplay by mutableStateOf(false)
    /** Previews a board theme in the game without saving it (`--ei debugBoard N`); -1 = the chosen one. */
    var board by mutableStateOf(-1)
    /** Slow motion for checking animations (`--ef debugSlowmo 0.1`); 1 = normal speed. Not a Compose state. */
    @Volatile var timeScale = 1f
}
