package com.ngstudio.maze.tools

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/** Tiny offline synthesizer: everything renders into FloatArray buffers, then 16-bit mono WAV. */
class Buf(val rate: Int, seconds: Double) {
    val data = FloatArray((seconds * rate).toInt())
    val size get() = data.size

    fun add(at: Double, src: FloatArray, gain: Double = 1.0) {
        val off = (at * rate).toInt()
        for (i in src.indices) {
            val j = off + i
            if (j in data.indices) data[j] += (src[i] * gain).toFloat()
        }
    }

    /** Adds [src] wrapping around the end (for seamless loops). */
    fun addWrapped(at: Double, src: FloatArray, gain: Double = 1.0) {
        val off = (at * rate).toInt()
        for (i in src.indices) {
            val j = Math.floorMod(off + i, data.size)
            data[j] += (src[i] * gain).toFloat()
        }
    }

    fun normalize(peak: Double) {
        var m = 1e-9f
        for (v in data) m = max(m, abs(v))
        val g = (peak / m).toFloat()
        for (i in data.indices) data[i] *= g
    }

    fun softClip(drive: Double = 1.2) {
        for (i in data.indices) data[i] = (tanh(data[i] * drive) / tanh(drive)).toFloat()
    }

    fun fadeEdges(inMs: Double, outMs: Double) {
        val a = (inMs / 1000 * rate).toInt(); val b = (outMs / 1000 * rate).toInt()
        for (i in 0 until min(a, size)) data[i] *= i.toFloat() / a
        for (i in 0 until min(b, size)) data[size - 1 - i] *= i.toFloat() / b
    }

    fun writeWav(file: File) {
        file.parentFile.mkdirs()
        val n = data.size
        RandomAccessFile(file, "rw").use { f ->
            f.setLength(0)
            fun le32(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
            fun le16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
            f.write("RIFF".toByteArray()); f.write(le32(36 + n * 2)); f.write("WAVE".toByteArray())
            f.write("fmt ".toByteArray()); f.write(le32(16)); f.write(le16(1)); f.write(le16(1))
            f.write(le32(rate)); f.write(le32(rate * 2)); f.write(le16(2)); f.write(le16(16))
            f.write("data".toByteArray()); f.write(le32(n * 2))
            val out = ByteArray(n * 2)
            for (i in 0 until n) {
                val s = (data[i].coerceIn(-1f, 1f) * 32767).toInt()
                out[i * 2] = s.toByte(); out[i * 2 + 1] = (s shr 8).toByte()
            }
            f.write(out)
        }
    }
}

object Dsp {
    val rng = Random(2026)
    fun noteHz(midi: Double) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Marimba-like bar: tuned modal partials with a soft mallet click. */
    fun marimba(rate: Int, hz: Double, dur: Double, bright: Double = 1.0): FloatArray {
        val n = (dur * rate).toInt()
        val out = FloatArray(n)
        val partials = listOf(1.0 to 1.0, 3.93 to 0.30 * bright, 9.2 to 0.10 * bright, 2.0 to 0.06)
        val decays = listOf(6.5, 14.0, 28.0, 10.0)
        for ((k, p) in partials.withIndex()) {
            val f = hz * p.first
            if (f > rate * 0.45) continue
            val d = decays[k] * (hz / 520.0).pow(0.35)
            for (i in 0 until n) {
                val t = i.toDouble() / rate
                out[i] += (sin(2 * PI * f * t) * p.second * exp(-d * t)).toFloat()
            }
        }
        // mallet: short low-passed noise
        var lp = 0.0
        val clickN = (0.012 * rate).toInt()
        for (i in 0 until min(clickN, n)) {
            lp += 0.25 * ((rng.nextDouble() * 2 - 1) - lp)
            out[i] += (lp * 0.35 * (1 - i.toDouble() / clickN)).toFloat()
        }
        attack(out, rate, 0.002)
        return out
    }

    /** FM bell. */
    fun bell(rate: Int, hz: Double, dur: Double, ratio: Double = 3.5, index: Double = 2.2, decay: Double = 3.2): FloatArray {
        val n = (dur * rate).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val env = exp(-decay * t)
            val mod = sin(2 * PI * hz * ratio * t) * index * exp(-decay * 1.6 * t)
            out[i] = (sin(2 * PI * hz * t + mod) * env * 0.8 + sin(2 * PI * hz * 2.0 * t) * env * env * 0.15).toFloat()
        }
        attack(out, rate, 0.003)
        return out
    }

    /** Karplus-Strong plucked string. */
    fun pluck(rate: Int, hz: Double, dur: Double, damp: Double = 0.996, tone: Double = 0.5): FloatArray {
        val n = (dur * rate).toInt()
        val period = max(2, (rate / hz).toInt())
        val buf = DoubleArray(period) { rng.nextDouble() * 2 - 1 }
        // soften the excitation
        for (k in 0 until 3) for (i in 1 until period) buf[i] = buf[i] * tone + buf[i - 1] * (1 - tone)
        val out = FloatArray(n)
        var idx = 0
        for (i in 0 until n) {
            val a = buf[idx]; val b = buf[(idx + 1) % period]
            val v = (a + b) * 0.5 * damp
            buf[idx] = v
            out[i] = a.toFloat()
            idx = (idx + 1) % period
        }
        attack(out, rate, 0.002)
        return out
    }

    fun sineSweep(rate: Int, f0: Double, f1: Double, dur: Double, decay: Double, curve: Double = 1.0): FloatArray {
        val n = (dur * rate).toInt()
        val out = FloatArray(n)
        var ph = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val f = f0 + (f1 - f0) * t.pow(curve)
            ph += 2 * PI * f / rate
            out[i] = (sin(ph) * exp(-decay * i.toDouble() / rate)).toFloat()
        }
        attack(out, rate, 0.002)
        return out
    }

    /** Band-passed noise with a moving centre frequency (state-variable filter). */
    fun noiseSweep(rate: Int, f0: Double, f1: Double, dur: Double, q: Double, env: (Double) -> Double): FloatArray {
        val n = (dur * rate).toInt()
        val out = FloatArray(n)
        var low = 0.0; var band = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val fc = f0 * (f1 / f0).pow(t)
            val f = 2 * sin(PI * min(fc, rate * 0.2) / rate)
            val x = rng.nextDouble() * 2 - 1
            low += f * band
            val high = x - low - band / q
            band += f * high
            out[i] = (band * env(t)).toFloat()
        }
        return out
    }

    fun pad(rate: Int, midis: List<Double>, dur: Double, cutoff: Double, gain: Double = 0.18): FloatArray {
        val n = (dur * rate).toInt()
        val out = FloatArray(n)
        for (m in midis) {
            val hz = noteHz(m)
            for ((det, g) in listOf(-0.07 to 0.5, 0.0 to 0.6, 0.08 to 0.5)) {
                val f = hz * 2.0.pow(det / 12)
                var ph = rng.nextDouble()
                for (i in 0 until n) {
                    ph += f / rate; if (ph >= 1) ph -= 1
                    // soft saw (a few harmonics)
                    val s = sin(2 * PI * ph) + 0.35 * sin(4 * PI * ph) + 0.15 * sin(6 * PI * ph)
                    out[i] += (s * g * gain).toFloat()
                }
            }
        }
        lowpass(out, rate, cutoff)
        val a = (0.35 * rate).toInt(); val r = (0.45 * rate).toInt()
        for (i in 0 until n) {
            val e = min(1.0, i.toDouble() / a) * min(1.0, (n - i).toDouble() / r)
            out[i] *= e.toFloat()
        }
        return out
    }

    fun lowpass(x: FloatArray, rate: Int, hz: Double) {
        val a = 1 - exp(-2 * PI * hz / rate)
        var y = 0.0
        for (i in x.indices) { y += a * (x[i] - y); x[i] = y.toFloat() }
        y = 0.0
        for (i in x.indices) { y += a * (x[i] - y); x[i] = y.toFloat() }
    }

    fun highpass(x: FloatArray, rate: Int, hz: Double) {
        val a = 1 - exp(-2 * PI * hz / rate)
        var y = 0.0
        for (i in x.indices) { y += a * (x[i] - y); x[i] = (x[i] - y).toFloat() }
    }

    fun attack(x: FloatArray, rate: Int, sec: Double) {
        val n = (sec * rate).toInt().coerceAtLeast(1)
        for (i in 0 until min(n, x.size)) x[i] *= i.toFloat() / n
        // and a tiny release so nothing clicks at the end
        val r = (0.004 * rate).toInt()
        for (i in 0 until min(r, x.size)) x[x.size - 1 - i] *= i.toFloat() / r
    }

    /** Schroeder/Freeverb-style mono reverb, returns wet+dry. */
    fun reverb(x: FloatArray, rate: Int, mix: Double, room: Double = 0.84, damp: Double = 0.25, wrap: Boolean = false): FloatArray {
        val scale = rate / 44100.0
        val combs = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491).map { (it * scale).toInt() }
        val aps = intArrayOf(556, 441, 341).map { (it * scale).toInt() }
        val n = x.size
        val wet = DoubleArray(n)
        val passes = if (wrap) 2 else 1
        for (c in combs) {
            val buf = DoubleArray(c)
            var idx = 0; var filt = 0.0
            for (pass in 0 until passes) for (i in 0 until n) {
                val out = buf[idx]
                filt = out * (1 - damp) + filt * damp
                buf[idx] = x[i] * 0.015 + filt * room
                idx = (idx + 1) % c
                if (pass == passes - 1) wet[i] += out
            }
        }
        for (a in aps) {
            val buf = DoubleArray(a)
            var idx = 0
            for (pass in 0 until passes) {
                for (i in 0 until n) {
                    val b = buf[idx]
                    val inp = wet[i]
                    val out = -inp + b
                    buf[idx] = inp + b * 0.5
                    idx = (idx + 1) % a
                    if (pass == passes - 1) wet[i] = out
                }
            }
        }
        return FloatArray(n) { (x[it] * (1 - mix * 0.5) + wet[it] * mix * 3.0).toFloat() }
    }
}
