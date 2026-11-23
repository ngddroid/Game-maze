package com.ngstudio.mazebrush.telemetry

import android.content.Context
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper over Firebase Analytics and Crashlytics, so the rest of the game never touches the
 * Firebase SDK directly (same pattern as the studio's Ridge Riders).
 *
 * Firebase starts itself from `google-services.json` through its startup ContentProvider, so there
 * is nothing to wire up beyond grabbing the singletons. Every method is fire-and-forget and never
 * throws: if Firebase failed to start, the game keeps running without telemetry.
 *
 * Where GA4 has a standard event (level_start, level_end, spend_virtual_currency, ...) we use it, so
 * the Firebase console's built-in game reports work; game-specific events use snake_case names.
 */
class Telemetry(context: Context) {

    private val analytics: FirebaseAnalytics? = runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
    private val crashlytics: FirebaseCrashlytics? = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()

    /** Turn collection on or off. Called once at start; call again if a consent screen is added. */
    fun setCollectionEnabled(enabled: Boolean) = safe {
        analytics?.setAnalyticsCollectionEnabled(enabled)
        crashlytics?.isCrashlyticsCollectionEnabled = enabled
    }

    /** Version context on every crash report, and as a user property in Analytics. */
    fun boot(versionName: String, versionCode: Int, debug: Boolean) = safe {
        crashlytics?.setCustomKey("version_name", versionName)
        crashlytics?.setCustomKey("version_code", versionCode)
        crashlytics?.setCustomKey("build_type", if (debug) "debug" else "release")
        analytics?.setUserProperty("app_version", versionName)
    }

    // ---- screens ------------------------------------------------------------------------------

    /** A navigation to one of the app's screens (GA4 screen_view), plus a Crashlytics breadcrumb. */
    fun screenView(screen: String) = safe {
        analytics?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to screen, FirebaseAnalytics.Param.SCREEN_CLASS to screen),
        )
        crashlytics?.log("screen: $screen")
        crashlytics?.setCustomKey("last_screen", screen)
    }

    // ---- levels -------------------------------------------------------------------------------

    /** A level (or the daily) begins; [attempt] is 0 for the first try, then counts restarts. */
    fun levelStart(level: Int, daily: Boolean, world: Int, par: Int, moveLimit: Int?, attempt: Int) = safe {
        crashlytics?.setCustomKey("level", level)
        crashlytics?.setCustomKey("mode", mode(daily))
        crashlytics?.log("level_start ${mode(daily)} $level attempt $attempt")
        log(
            FirebaseAnalytics.Event.LEVEL_START,
            FirebaseAnalytics.Param.LEVEL_NAME to levelName(level, daily),
            FirebaseAnalytics.Param.LEVEL to level.toLong(),
            "mode" to mode(daily), "world" to world.toLong(), "par" to par.toLong(),
            "move_limit" to (moveLimit ?: 0).toLong(), "attempt" to attempt.toLong(),
        )
    }

    /** The level was painted in full. */
    fun levelComplete(level: Int, daily: Boolean, moves: Int, par: Int, stars: Int, coins: Int, firstClear: Boolean, hintsUsed: Int) = safe {
        log(
            FirebaseAnalytics.Event.LEVEL_END,
            FirebaseAnalytics.Param.LEVEL_NAME to levelName(level, daily),
            FirebaseAnalytics.Param.SUCCESS to "true",
            FirebaseAnalytics.Param.LEVEL to level.toLong(),
            "mode" to mode(daily), "moves" to moves.toLong(), "par" to par.toLong(), "stars" to stars.toLong(),
            "coins" to coins.toLong(), "first_clear" to (if (firstClear) 1L else 0L), "hints_used" to hintsUsed.toLong(),
        )
        if (coins > 0) earn("coins", coins.toLong(), if (daily) "daily" else "level")
    }

    /** The move limit ran out. */
    fun outOfMoves(level: Int, daily: Boolean, tilesLeft: Int) = safe {
        log("out_of_moves", FirebaseAnalytics.Param.LEVEL to level.toLong(), "mode" to mode(daily), "tiles_left" to tilesLeft.toLong())
    }

    /** The player restarted, or left the level unfinished. [reason] = "restart" | "retry" | "home". */
    fun levelQuit(level: Int, daily: Boolean, reason: String, moves: Int, progress: Float) = safe {
        log(
            FirebaseAnalytics.Event.LEVEL_END,
            FirebaseAnalytics.Param.LEVEL_NAME to levelName(level, daily),
            FirebaseAnalytics.Param.SUCCESS to "false",
            FirebaseAnalytics.Param.LEVEL to level.toLong(),
            "mode" to mode(daily), "reason" to reason, "moves" to moves.toLong(), "painted_pct" to (progress * 100).toLong(),
        )
    }

    fun hintUsed(level: Int, daily: Boolean) = safe {
        log("hint_used", FirebaseAnalytics.Param.LEVEL to level.toLong(), "mode" to mode(daily))
    }

    // ---- economy ------------------------------------------------------------------------------

    /** Coins spent; [item] says on what ("hint", "moves_5", "ball_3", ...). */
    fun spend(item: String, coins: Int) = safe {
        log(
            FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY,
            FirebaseAnalytics.Param.ITEM_NAME to item,
            FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME to "coins",
            FirebaseAnalytics.Param.VALUE to coins.toLong(),
        )
    }

    /** Coins or hints earned outside a level win ("calendar", "achievement", "ad_x2", ...). */
    fun earn(currency: String, amount: Long, source: String) = safe {
        log(
            FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY,
            FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME to currency,
            FirebaseAnalytics.Param.VALUE to amount,
            "source" to source,
        )
    }

    /** A rewarded ad finished ([placement]: "unlock_level", "x2_coins", "hint", "moves_5", "skin"). */
    fun rewardedAd(placement: String) = safe { log("rewarded_ad", "placement" to placement) }

    fun achievementUnlocked(id: String) = safe {
        log(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, FirebaseAnalytics.Param.ACHIEVEMENT_ID to id)
    }

    fun tutorialBegin() = safe { log(FirebaseAnalytics.Event.TUTORIAL_BEGIN) }
    fun tutorialComplete() = safe { log(FirebaseAnalytics.Event.TUTORIAL_COMPLETE) }

    // ---- notifications and updates -------------------------------------------------------------

    /** Any other event, e.g. "reminder_shown", "notification_tap", "update_prompt_shown". */
    fun event(name: String, vararg params: Pair<String, Any?>) = safe { log(name, *params) }

    // ---- crashlytics --------------------------------------------------------------------------

    /** Report a handled (non-fatal) exception. */
    fun recordException(t: Throwable) = safe { crashlytics?.recordException(t) }

    /** Add a breadcrumb to the next crash report. */
    fun breadcrumb(message: String) = safe { crashlytics?.log(message) }

    // -------------------------------------------------------------------------------------------

    private fun log(event: String, vararg params: Pair<String, Any?>) {
        analytics?.logEvent(event, if (params.isEmpty()) null else bundleOf(*params))
    }

    private fun mode(daily: Boolean) = if (daily) "daily" else "campaign"
    private fun levelName(level: Int, daily: Boolean) = if (daily) "daily_$level" else "level_$level"

    private inline fun safe(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Telemetry must never take the game down.
        }
    }
}
