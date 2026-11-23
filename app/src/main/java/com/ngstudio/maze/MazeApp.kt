package com.ngstudio.maze

import android.app.Application
import com.ngstudio.maze.audio.Haptics
import com.ngstudio.maze.audio.SoundManager
import com.ngstudio.maze.data.GameStore
import com.ngstudio.maze.data.LevelRepository

/** App-wide singletons (no DI framework: the game is small and single-activity). */
class MazeApp : Application() {
    lateinit var store: GameStore
    lateinit var levels: LevelRepository
    lateinit var sound: SoundManager
    lateinit var haptics: Haptics

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = GameStore(this)
        levels = LevelRepository(this)
        sound = SoundManager(this, store)
        haptics = Haptics(this, store)
    }

    companion object {
        lateinit var instance: MazeApp
            private set
    }
}
