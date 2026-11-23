package com.ngstudio.maze.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.ngstudio.maze.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the update UI should show right now. */
sealed interface UpdateUi {
    data object Idle : UpdateUi
    /** A newer version is on Play; ask the player (our dialog, then Play's own confirm sheet). */
    data object Available : UpdateUi
    data class Downloading(val progress: Float) : UpdateUi
    /** Downloaded in the background; a restart finishes the install. */
    data object ReadyToInstall : UpdateUi
    data object Installing : UpdateUi
    data object Failed : UpdateUi
}

/**
 * Google Play in-app updates.
 *
 * Policy:
 * - Normal releases use a **flexible** update: our themed dialog asks first, the APK downloads in the
 *   background while the player keeps playing, then a banner offers RESTART to install.
 * - Releases sent with update priority >= [FORCED_PRIORITY] use an **immediate** update: Play shows
 *   its full-screen, blocking update page. (Priority is set per release through the Play Developer
 *   API's `inAppUpdatePriority`; the Play Console UI has no field for it.)
 * - "Later" hides the prompt for that version for [SNOOZE_DAYS] days.
 *
 * Only builds installed from Google Play (including internal testing / internal app sharing) get
 * real update info; sideloaded and debug installs just see "no update".
 */
class AppUpdater(context: Context, private val telemetry: Telemetry) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("app_update", Context.MODE_PRIVATE)
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(app)

    private val _ui = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val ui: StateFlow<UpdateUi> = _ui.asStateFlow()

    private var offered: AppUpdateInfo? = null
    /** A forced (immediate) update was started this launch; it is resumed until it completes. */
    private var forced: AppUpdateInfo? = null
    private var checkedThisLaunch = false

    private val listener = InstallStateUpdatedListener { onInstallState(it) }

    init {
        manager.registerListener(listener)
    }

    /**
     * Ask Play about updates. [firstOfLaunch] = true may prompt for a new version (done once, on Home);
     * otherwise this only resumes work already in progress (call it from onResume).
     */
    suspend fun check(launcher: ActivityResultLauncher<IntentSenderRequest>, firstOfLaunch: Boolean) {
        if (firstOfLaunch) {
            if (checkedThisLaunch) return
            checkedThisLaunch = true
        }
        val info = try {
            manager.requestAppUpdateInfo()
        } catch (_: Exception) {
            return // Not installed from Play, no Play Store, offline… nothing to do.
        }
        when {
            info.installStatus() == InstallStatus.DOWNLOADED -> _ui.value = UpdateUi.ReadyToInstall
            info.installStatus() == InstallStatus.DOWNLOADING -> _ui.value = UpdateUi.Downloading(progressOf(info.bytesDownloaded(), info.totalBytesToDownload()))
            // A forced update was interrupted (app killed or backgrounded): Play requires resuming it.
            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> startImmediate(info, launcher)
            // A forced update the player backed out of: show Play's update screen again until it is installed.
            forced != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> startImmediate(info, launcher)
            !firstOfLaunch -> Unit
            info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> Unit
            info.updatePriority() >= FORCED_PRIORITY && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                telemetry.event("update_forced_started", "version" to info.availableVersionCode())
                startImmediate(info, launcher)
            }
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && !isSnoozed(info.availableVersionCode()) -> offer(info)
        }
    }

    /** Player tapped UPDATE in our dialog. */
    fun acceptFlexible(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = offered ?: return
        telemetry.event("update_accepted", "version" to info.availableVersionCode())
        _ui.value = UpdateUi.Idle // Play's confirm sheet takes over; progress comes back through the listener.
        val started = runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
        }.getOrDefault(false)
        if (!started) _ui.value = UpdateUi.Failed
    }

    /** Player tapped LATER in our dialog (or pressed Back). */
    fun later() {
        offered?.let { info ->
            prefs.edit {
                putInt(KEY_SNOOZED_VERSION, info.availableVersionCode())
                putLong(KEY_SNOOZED_UNTIL, System.currentTimeMillis() + SNOOZE_DAYS * DAY_MS)
            }
        }
        telemetry.event("update_later")
        offered = null
        _ui.value = UpdateUi.Idle
    }

    /** Result of Play's own confirm sheet / immediate flow. */
    fun onFlowResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> Unit // Flexible: download running, listener reports progress.
            Activity.RESULT_CANCELED -> {
                telemetry.event("update_declined_in_play")
                _ui.value = UpdateUi.Idle
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> _ui.value = UpdateUi.Failed
        }
    }

    /** RESTART on the "update ready" banner: Play installs and relaunches the game. */
    fun completeUpdate() {
        telemetry.event("update_restart")
        _ui.value = UpdateUi.Installing
        manager.completeUpdate()
    }

    /** Hide the ready banner for now; the next resume shows it again, as Play recommends. */
    fun dismissReady() {
        _ui.value = UpdateUi.Idle
    }

    fun dismissFailed() {
        _ui.value = UpdateUi.Idle
    }

    private fun offer(info: AppUpdateInfo) {
        offered = info
        _ui.value = UpdateUi.Available
        telemetry.event("update_prompt_shown", "version" to info.availableVersionCode())
    }

    private fun startImmediate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        forced = info
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
        }
    }

    private fun onInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.PENDING -> _ui.value = UpdateUi.Downloading(0f)
            InstallStatus.DOWNLOADING -> _ui.value = UpdateUi.Downloading(progressOf(state.bytesDownloaded(), state.totalBytesToDownload()))
            InstallStatus.DOWNLOADED -> {
                _ui.value = UpdateUi.ReadyToInstall
                telemetry.event("update_downloaded")
            }
            InstallStatus.INSTALLING -> _ui.value = UpdateUi.Installing
            InstallStatus.INSTALLED, InstallStatus.CANCELED -> _ui.value = UpdateUi.Idle
            InstallStatus.FAILED -> {
                _ui.value = UpdateUi.Failed
                telemetry.event("update_failed", "code" to state.installErrorCode())
            }
        }
    }

    private fun isSnoozed(versionCode: Int): Boolean =
        prefs.getInt(KEY_SNOOZED_VERSION, -1) == versionCode && System.currentTimeMillis() < prefs.getLong(KEY_SNOOZED_UNTIL, 0L)

    private fun progressOf(done: Long, total: Long) = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f

    companion object {
        /** Play update priority (0–5) at or above which the update is forced. */
        const val FORCED_PRIORITY = 4
        const val SNOOZE_DAYS = 3
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val KEY_SNOOZED_VERSION = "snoozed_version"
        private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    }
}
