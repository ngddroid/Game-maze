package com.ngstudio.maze.tools

import com.ngstudio.maze.tools.Dsp.bell
import com.ngstudio.maze.tools.Dsp.marimba
import com.ngstudio.maze.tools.Dsp.noteHz
import com.ngstudio.maze.tools.Dsp.noiseSweep
import com.ngstudio.maze.tools.Dsp.pad
import com.ngstudio.maze.tools.Dsp.pluck
import com.ngstudio.maze.tools.Dsp.reverb
import com.ngstudio.maze.tools.Dsp.sineSweep
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * All game audio, synthesized from scratch (original, no samples):
 *   sfx_*  : 44.1 kHz mono, short effects
 *   music_*: 22.05 kHz mono seamless loops
 */
object AudioGen {
    private const val SR = 44100
    private const val MR = 22050

    fun run(outDir: File) {
        fun save(name: String, b: Buf) { b.writeWav(File(outDir, "$name.wav")); println("  $name.wav  ${"%.2f".format(b.size.toDouble() / b.rate)}s") }

        // ---- UI tap: soft candy pop
        save("sfx_tap", Buf(SR, 0.09).apply {
            add(0.0, sineSweep(SR, 950.0, 520.0, 0.08, 45.0), 0.7)
            add(0.0, noiseSweep(SR, 3000.0, 1500.0, 0.02, 2.0) { 1 - it }, 0.15)
            normalize(0.7); fadeEdges(1.0, 8.0)
        })

        // ---- paint notes: rising pentatonic marimba + a wet "plip"
        val penta = listOf(67, 69, 72, 74, 76, 79, 81, 84, 86, 88, 91, 93)
        for ((i, m) in penta.withIndex()) {
            val b = Buf(SR, 0.42)
            b.add(0.0, marimba(SR, noteHz(m.toDouble()), 0.42, bright = 0.9), 0.85)
            b.add(0.0, sineSweep(SR, 1400.0 + i * 60, 2600.0 + i * 80, 0.035, 60.0), 0.18)
            val wet = reverb(b.data, SR, 0.18)
            System.arraycopy(wet, 0, b.data, 0, b.size)
            b.normalize(0.62); b.fadeEdges(0.5, 30.0)
            save("sfx_paint_$i", b)
        }

        // ---- wall thud: rubbery bump
        save("sfx_thud", Buf(SR, 0.22).apply {
            add(0.0, sineSweep(SR, 165.0, 62.0, 0.2, 18.0, 0.6), 0.9)
            add(0.0, noiseSweep(SR, 900.0, 250.0, 0.08, 1.2) { exp(-it * 5) }, 0.35)
            add(0.004, sineSweep(SR, 420.0, 300.0, 0.05, 50.0), 0.15)
            normalize(0.8); fadeEdges(0.5, 20.0)
        })

        // ---- win jingle: marimba arpeggio + bell chord + sparkles
        save("sfx_win", Buf(SR, 2.2).apply {
            val arp = listOf(72, 76, 79, 84, 88)
            arp.forEachIndexed { k, m -> add(k * 0.085, marimba(SR, noteHz(m.toDouble()), 0.8), 0.6) }
            for (m in listOf(72, 76, 79, 84)) add(0.48, bell(SR, noteHz(m.toDouble()), 1.6, ratio = 2.0, index = 1.2, decay = 2.2), 0.28)
            for (k in 0 until 14) {
                val m = 88 + Dsp.rng.nextInt(12)
                add(0.5 + k * 0.06, bell(SR, noteHz(m.toDouble()), 0.4, ratio = 3.0, index = 0.8, decay = 9.0), 0.08)
            }
            val wet = reverb(data, SR, 0.28)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.85); fadeEdges(1.0, 200.0)
        })

        // ---- stars: rising dings
        for ((i, m) in listOf(84, 88, 91).withIndex()) save("sfx_star${i + 1}", Buf(SR, 0.9).apply {
            add(0.0, bell(SR, noteHz(m.toDouble()), 0.9, ratio = 2.0, index = 1.4, decay = 4.5), 0.7)
            add(0.0, marimba(SR, noteHz(m - 12.0), 0.5), 0.35)
            add(0.03, bell(SR, noteHz(m + 12.0), 0.4, ratio = 3.0, index = 0.6, decay = 10.0), 0.12)
            val wet = reverb(data, SR, 0.22)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.75); fadeEdges(0.5, 120.0)
        })

        // ---- coin: two quick bright notes
        save("sfx_coin", Buf(SR, 0.45).apply {
            add(0.0, bell(SR, noteHz(83.0), 0.2, ratio = 1.0, index = 1.5, decay = 18.0), 0.5)
            add(0.07, bell(SR, noteHz(88.0), 0.38, ratio = 1.0, index = 1.5, decay = 9.0), 0.6)
            normalize(0.65); fadeEdges(0.5, 40.0)
        })

        // ---- whoosh (screen / level change)
        save("sfx_whoosh", Buf(SR, 0.42).apply {
            add(0.0, noiseSweep(SR, 350.0, 2800.0, 0.42, 1.6) { sin(PI * it).let { s -> s * s } }, 1.0)
            normalize(0.45); fadeEdges(5.0, 30.0)
        })

        // ---- unlock: rising sparkle arpeggio
        save("sfx_unlock", Buf(SR, 1.1).apply {
            listOf(79, 84, 88, 91, 96).forEachIndexed { k, m -> add(k * 0.07, bell(SR, noteHz(m.toDouble()), 0.7, ratio = 2.0, index = 1.0, decay = 5.0), 0.4) }
            add(0.0, marimba(SR, noteHz(60.0), 0.6), 0.4)
            val wet = reverb(data, SR, 0.3)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.75); fadeEdges(0.5, 150.0)
        })

        // ---- bonk: blocked / locked
        save("sfx_bonk", Buf(SR, 0.25).apply {
            val n = size
            val out = FloatArray(n)
            var ph = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / SR
                val f = 230.0 * (1 - 0.35 * t / 0.25) * (1 + 0.06 * sin(2 * PI * 18 * t))
                ph += 2 * PI * f / SR
                out[i] = ((sin(ph) + 0.3 * sin(2 * ph)) * exp(-14 * t)).toFloat()
            }
            Dsp.attack(out, SR, 0.002)
            add(0.0, out, 0.8)
            normalize(0.6); fadeEdges(0.5, 20.0)
        })

        // ---- pop: bubble
        save("sfx_pop", Buf(SR, 0.12).apply {
            add(0.0, sineSweep(SR, 280.0, 1350.0, 0.06, 30.0, 0.5), 0.8)
            normalize(0.6); fadeEdges(0.5, 20.0)
        })

        // ---- teleport: magical zwoop
        save("sfx_teleport", Buf(SR, 0.5).apply {
            val n = size
            val out = FloatArray(n)
            var ph = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / n
                val f = if (t < 0.45) 300 + 1500 * (t / 0.45) * (t / 0.45) else 1800 - 1100 * ((t - 0.45) / 0.55)
                ph += 2 * PI * f / SR
                val trem = 0.6 + 0.4 * sin(2 * PI * 22 * t)
                out[i] = (sin(ph + 1.5 * sin(ph * 0.5)) * trem * sin(PI * t)).toFloat()
            }
            add(0.0, out, 0.5)
            val wet = reverb(data, SR, 0.3)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.6); fadeEdges(2.0, 60.0)
        })

        // ---- chest: pluck + chord + coin shower
        save("sfx_chest", Buf(SR, 1.6).apply {
            add(0.0, pluck(SR, noteHz(48.0), 0.8), 0.5)
            for (m in listOf(72, 76, 79, 84)) add(0.08, bell(SR, noteHz(m.toDouble()), 1.3, ratio = 2.0, index = 1.0, decay = 2.6), 0.22)
            for (k in 0 until 10) add(0.25 + k * 0.07, bell(SR, noteHz(86.0 + Dsp.rng.nextInt(10)), 0.25, ratio = 1.0, index = 1.4, decay = 14.0), 0.18)
            val wet = reverb(data, SR, 0.25)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.8); fadeEdges(0.5, 200.0)
        })

        // ---- undo: soft downward swoosh
        save("sfx_undo", Buf(SR, 0.2).apply {
            add(0.0, sineSweep(SR, 880.0, 380.0, 0.18, 12.0), 0.5)
            add(0.0, noiseSweep(SR, 2500.0, 600.0, 0.18, 1.5) { 1 - it }, 0.25)
            normalize(0.5); fadeEdges(1.0, 30.0)
        })

        // ---- hint: twinkle
        save("sfx_hint", Buf(SR, 0.9).apply {
            listOf(88, 91, 95).forEachIndexed { k, m -> add(k * 0.09, bell(SR, noteHz(m.toDouble()), 0.6, ratio = 3.0, index = 0.9, decay = 6.0), 0.45) }
            val wet = reverb(data, SR, 0.35)
            System.arraycopy(wet, 0, data, 0, size)
            normalize(0.65); fadeEdges(0.5, 120.0)
        })

        // ---- music
        save("music_home", music(bpm = 92.0, chords = listOf(listOf(60, 64, 67, 71), listOf(57, 60, 64, 67), listOf(53, 57, 60, 64), listOf(55, 59, 62, 65)), melody = true, seed = 7))
        save("music_game", music(bpm = 104.0, chords = listOf(listOf(53, 57, 60, 64), listOf(55, 59, 62, 67), listOf(52, 55, 59, 64), listOf(57, 60, 64, 67)), melody = false, seed = 11))
    }

    /**
     * A gentle looping track: soft pad, marimba arpeggios, round bass and a light shaker.
     * 4 chords x 2 bars, played twice (with variation) = 16 bars. Reverb tails wrap around for a seamless loop.
     */
    private fun music(bpm: Double, chords: List<List<Int>>, melody: Boolean, seed: Int): Buf {
        val beat = 60.0 / bpm
        val bars = 16
        val len = bars * 4 * beat
        val b = Buf(MR, len)
        val rnd = kotlin.random.Random(seed)
        val dry = Buf(MR, len)
        for (bar in 0 until bars) {
            val chord = chords[(bar / 2) % chords.size]
            val t0 = bar * 4 * beat
            // pad: one per 2 bars
            if (bar % 2 == 0) dry.addWrapped(t0, pad(MR, chord.map { it.toDouble() }, 8 * beat + 0.4, 1400.0, 0.10), 1.0)
            // bass: root on beats 1 and 3, fifth on the "and" of 4
            val root = chord[0] - 12
            for ((bt, m) in listOf(0.0 to root, 2.0 to root, 3.5 to root + 7)) {
                val bass = FloatArray((beat * 1.6 * MR).toInt())
                val hz = noteHz(m.toDouble())
                for (i in bass.indices) { val t = i.toDouble() / MR; bass[i] = (sin(2 * PI * hz * t) * exp(-3.5 * t) + 0.25 * sin(4 * PI * hz * t) * exp(-6 * t)).toFloat() }
                Dsp.attack(bass, MR, 0.008)
                dry.addWrapped(t0 + bt * beat, bass, 0.32)
            }
            // marimba arpeggio in 8ths
            val pattern = if (bar >= 8) listOf(0, 2, 1, 3, 2, 1, 3, 2) else listOf(0, 1, 2, 3, 2, 1, 2, 3)
            for (k in 0 until 8) {
                if (bar < 2 && k % 2 == 1) continue // sparse intro
                val m = chord[pattern[k]] + 12
                val vel = if (k % 2 == 0) 0.22 else 0.15
                dry.addWrapped(t0 + k * 0.5 * beat, marimba(MR, noteHz(m.toDouble()), 0.9, bright = 0.7), vel)
            }
            // shaker
            for (k in 0 until 8) {
                val sh = noiseSweep(MR, 6000.0, 7000.0, 0.05, 1.0) { (1 - it) * (1 - it) }
                dry.addWrapped(t0 + k * 0.5 * beat, sh, if (k % 2 == 1) 0.05 else 0.03)
            }
            // simple bell melody in the second half (home music only)
            if (melody && bar >= 8) {
                val notes = chord.map { it + 24 }
                for (k in 0 until 2) {
                    if (rnd.nextDouble() < 0.75) {
                        val m = notes[rnd.nextInt(notes.size)]
                        dry.addWrapped(t0 + (k * 2 + rnd.nextInt(2)) * beat, bell(MR, noteHz(m.toDouble()), 1.4, ratio = 2.0, index = 0.8, decay = 2.4), 0.10)
                    }
                }
            }
        }
        val wet = reverb(dry.data, MR, 0.22, room = 0.86, wrap = true)
        System.arraycopy(wet, 0, b.data, 0, b.size)
        Dsp.highpass(b.data, MR, 35.0)
        b.normalize(0.72)
        return b
    }
}
