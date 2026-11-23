package com.ngstudio.mazebrush

import android.app.Application
import com.ngstudio.mazebrush.audio.Haptics
import com.ngstudio.mazebrush.audio.SoundManager
import com.ngstudio.mazebrush.data.GameStore
import com.ngstudio.mazebrush.data.LevelRepository
import com.ngstudio.mazebrush.notifications.DailyReminders
import com.ngstudio.mazebrush.notifications.Notifications
import com.ngstudio.mazebrush.push.Push
import com.ngstudio.mazebrush.telemetry.Telemetry
import com.ngstudio.mazebrush.update.AppUpdater

/** App-wide singletons (no DI framework: the game is small and single-activity). */
class MazeApp : Application() {
    lateinit var store: GameStore
    lateinit var levels: LevelRepository
    lateinit var sound: SoundManager
    lateinit var haptics: Haptics
    lateinit var telemetry: Telemetry
    lateinit var updater: AppUpdater

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Firebase (Analytics + Crashlytics) starts itself from google-services.json; this only adds context.
        telemetry = Telemetry(this)
        telemetry.setCollectionEnabled(true)
        telemetry.boot(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.DEBUG)
        // Channels must exist before any reminder or push arrives, including when the app is closed.
        Notifications.createChannels(this)
        Push.init(debug = BuildConfig.DEBUG)
        updater = AppUpdater(this, telemetry)
        store = GameStore(this)
        levels = LevelRepository(this)
        sound = SoundManager(this, store)
        haptics = Haptics(this, store)
        // Re-arm the 9 AM / 7 PM reminders on every launch (they also recover from a force-stop this way).
        // Players who don't want them turn them off in Android's notification settings.
        DailyReminders.scheduleAll(this)
    }

    companion object {
        lateinit var instance: MazeApp
            private set
    }
}
