package com.ngstudio.mazebrush.audio

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ngstudio.mazebrush.data.GameStore

/** Light haptic feedback; respects the Vibration setting. */
class Haptics(context: Context, private val store: GameStore) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    private var last = 0L

    private fun ok(minGapMs: Long): Boolean {
        if (!store.vibration || vibrator?.hasVibrator() != true) return false
        val t = SystemClock.uptimeMillis()
        if (t - last < minGapMs) return false
        last = t
        return true
    }

    /** Tiny tick for each painted cell. */
    fun tick() {
        if (!ok(45)) return
        if (Build.VERSION.SDK_INT >= 29) vibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        else oneShot(6, 40)
    }

    /** Ball hits a wall. */
    fun impact(strength: Float) {
        if (!ok(30)) return
        if (Build.VERSION.SDK_INT >= 29 && strength > 0.6f) vibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        else oneShot(12, (60 + 120 * strength).toInt().coerceIn(1, 255))
    }

    fun win() {
        if (!ok(0)) return
        if (Build.VERSION.SDK_INT >= 26) vibrator!!.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 18, 60, 18, 60, 40), intArrayOf(0, 140, 0, 180, 0, 255), -1))
    }

    fun light() {
        if (!ok(20)) return
        if (Build.VERSION.SDK_INT >= 29) vibrator!!.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)) else oneShot(8, 60)
    }

    private fun oneShot(ms: Long, amp: Int) {
        if (Build.VERSION.SDK_INT >= 26) vibrator!!.vibrate(VibrationEffect.createOneShot(ms, amp))
        else @Suppress("DEPRECATION") vibrator!!.vibrate(ms)
    }
}
