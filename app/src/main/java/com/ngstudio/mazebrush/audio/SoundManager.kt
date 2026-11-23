package com.ngstudio.mazebrush.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.ngstudio.mazebrush.data.GameStore

/**
 * Sound effects (SoundPool) and looping music (MediaPlayer) with a short crossfade.
 * All audio files are original and synthesized by `./gradlew :tools:run --args="audio"` into res/raw.
 * Files are looked up by name so a missing file never crashes the game.
 */
class SoundManager(private val context: Context, private val store: GameStore) {

    enum class Music(val res: String) { HOME("music_home"), GAME("music_game") }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = HashMap<String, Int>()
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var current: Music? = null
    private var wanted: Music? = null
    private var foreground = true
    private var lastPaint = 0L

    init {
        val names = listOf("sfx_tap", "sfx_thud", "sfx_win", "sfx_star1", "sfx_star2", "sfx_star3", "sfx_coin", "sfx_whoosh",
            "sfx_unlock", "sfx_bonk", "sfx_pop", "sfx_teleport", "sfx_chest", "sfx_undo", "sfx_hint") + (0 until 12).map { "sfx_paint_$it" }
        for (n in names) {
            val res = rawId(n)
            if (res != 0) ids[n] = pool.load(context, res, 1)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun rawId(name: String) = context.resources.getIdentifier(name, "raw", context.packageName)

    private fun play(name: String, volume: Float = 1f, rate: Float = 1f) {
        if (!store.sound) return
        val id = ids[name] ?: return
        pool.play(id, volume, volume, 1, 0, rate)
    }

    fun tap() = play("sfx_tap", 0.7f)
    fun whoosh() = play("sfx_whoosh", 0.6f)
    fun bonk() = play("sfx_bonk", 0.7f)
    fun thud(strength: Float) = play("sfx_thud", 0.35f + 0.5f * strength, 0.92f + 0.12f * strength)
    fun win() = play("sfx_win", 0.9f)
    fun star(i: Int) = play("sfx_star${i.coerceIn(1, 3)}", 0.9f)
    fun coin() = play("sfx_coin", 0.7f)
    fun unlock() = play("sfx_unlock", 0.9f)
    fun pop() = play("sfx_pop", 0.7f)
    fun teleport() = play("sfx_teleport", 0.8f)
    fun chest() = play("sfx_chest", 0.9f)
    fun undo() = play("sfx_undo", 0.6f)
    fun hint() = play("sfx_hint", 0.7f)

    /** Rising pentatonic marimba note for the n-th cell painted in one swipe. */
    fun paint(indexInMove: Int) {
        val t = SystemClock.uptimeMillis()
        if (t - lastPaint < 28) return
        lastPaint = t
        play("sfx_paint_${indexInMove.coerceIn(0, 11)}", 0.55f)
    }

    // ---------------------------------------------------------------- music
    fun music(m: Music) {
        wanted = m
        refreshMusic()
    }

    fun onForeground(fg: Boolean) {
        foreground = fg
        refreshMusic()
    }

    fun refreshMusic() {
        val target = if (store.music && foreground) wanted else null
        if (target == current) {
            if (target != null && player?.isPlaying == false) player?.start()
            return
        }
        val old = player
        player = null
        current = null
        if (old != null) fadeOut(old)
        if (target != null) {
            val res = rawId(target.res)
            if (res == 0) return
            val p = runCatching { MediaPlayer.create(context, res) }.getOrNull() ?: return
            p.isLooping = true
            p.setVolume(0f, 0f)
            p.start()
            fadeIn(p)
            player = p
            current = target
        }
    }

    private fun fadeIn(p: MediaPlayer) {
        var v = 0f
        val step = object : Runnable {
            override fun run() {
                if (player !== p) return
                v = (v + 0.05f).coerceAtMost(MUSIC_VOLUME)
                runCatching { p.setVolume(v, v) }
                if (v < MUSIC_VOLUME) main.postDelayed(this, 40)
            }
        }
        main.post(step)
    }

    private fun fadeOut(p: MediaPlayer) {
        var v = MUSIC_VOLUME
        val step = object : Runnable {
            override fun run() {
                v -= 0.06f
                if (v <= 0f) { runCatching { p.stop(); p.release() }; return }
                runCatching { p.setVolume(v, v) }
                main.postDelayed(this, 35)
            }
        }
        main.post(step)
    }

    companion object {
        const val MUSIC_VOLUME = 0.42f
    }
}
