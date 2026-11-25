package com.ngstudio.mazebrush.render

import android.opengl.GLES30
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * CPU-simulated particles drawn with one instanced call. Structure-of-arrays for speed.
 * Shapes: 0 soft dot, 1 confetti (3D tumble), 2 sparkle, 3 paint droplet, 4 ring, 5 bubble.
 */
class Particles(private val max: Int) {
    private val x = FloatArray(max); private val y = FloatArray(max)
    private val vx = FloatArray(max); private val vy = FloatArray(max)
    private val size = FloatArray(max); private val grow = FloatArray(max)
    private val rot = FloatArray(max); private val vrot = FloatArray(max)
    private val r = FloatArray(max); private val g = FloatArray(max); private val b = FloatArray(max); private val a = FloatArray(max)
    private val life = FloatArray(max); private val maxLife = FloatArray(max)
    private val shape = FloatArray(max); private val flip = FloatArray(max); private val flipSpeed = FloatArray(max)
    private val gravity = FloatArray(max); private val drag = FloatArray(max)
    private val wrap = BooleanArray(max)
    var count = 0
        private set
    private val rng = Random(42)

    private val stride = 10
    private val data: FloatBuffer = GlUtil.floatBuffer(max * stride)
    private val array = FloatArray(max * stride)
    private var vao = 0
    private var instanceVbo = 0

    fun clear() { count = 0 }

    fun spawn(
        px: Float, py: Float, pvx: Float, pvy: Float, psize: Float, color: Rgb, alpha: Float, lifeSec: Float,
        pshape: Int, pgravity: Float = 0f, pdrag: Float = 0f, pgrow: Float = 0f, spin: Float = 0f, wrapAround: Boolean = false,
        /** Starting angle in radians; NaN = random. */
        angle: Float = Float.NaN,
    ) {
        if (count >= max) return
        val i = count++
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy; size[i] = psize; grow[i] = pgrow
        rot[i] = if (angle.isNaN()) rng.nextFloat() * 6.283f else angle; vrot[i] = spin
        r[i] = color.r; g[i] = color.g; b[i] = color.b; a[i] = alpha
        life[i] = lifeSec; maxLife[i] = lifeSec
        shape[i] = pshape.toFloat(); flip[i] = rng.nextFloat() * 6.283f; flipSpeed[i] = 4f + rng.nextFloat() * 8f
        gravity[i] = pgravity; drag[i] = pdrag; wrap[i] = wrapAround
    }

    fun random() = rng.nextFloat()

    /** Ambient wrap-around particles live forever and wrap inside (0,0)-(w,h). */
    fun update(dt: Float, w: Float, h: Float) {
        var i = 0
        while (i < count) {
            if (!wrap[i]) life[i] -= dt
            if (life[i] <= 0f) {
                val last = --count
                if (i != last) copy(last, i)
                continue
            }
            vy[i] += gravity[i] * dt
            val d = 1f - drag[i] * dt
            vx[i] *= d; vy[i] *= d
            x[i] += vx[i] * dt; y[i] += vy[i] * dt
            rot[i] += vrot[i] * dt
            flip[i] += flipSpeed[i] * dt
            size[i] = (size[i] + grow[i] * dt).coerceAtLeast(0f)
            if (wrap[i]) {
                if (y[i] < -40f) y[i] = h + 40f
                if (y[i] > h + 40f) y[i] = -40f
                if (x[i] < -40f) x[i] = w + 40f
                if (x[i] > w + 40f) x[i] = -40f
            }
            i++
        }
    }

    private fun copy(from: Int, to: Int) {
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from]
        size[to] = size[from]; grow[to] = grow[from]; rot[to] = rot[from]; vrot[to] = vrot[from]
        r[to] = r[from]; g[to] = g[from]; b[to] = b[from]; a[to] = a[from]
        life[to] = life[from]; maxLife[to] = maxLife[from]; shape[to] = shape[from]
        flip[to] = flip[from]; flipSpeed[to] = flipSpeed[from]; gravity[to] = gravity[from]; drag[to] = drag[from]
        wrap[to] = wrap[from]
    }

    fun initGl(quadVbo: Int) {
        val v = IntArray(1)
        GLES30.glGenVertexArrays(1, v, 0); vao = v[0]
        val b = IntArray(1)
        GLES30.glGenBuffers(1, b, 0); instanceVbo = b[0]
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, max * stride * 4, null, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, stride * 4, 0)
        GLES30.glVertexAttribDivisor(1, 1)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride * 4, 16)
        GLES30.glVertexAttribDivisor(2, 1)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 2, GLES30.GL_FLOAT, false, stride * 4, 32)
        GLES30.glVertexAttribDivisor(3, 1)
        GLES30.glBindVertexArray(0)
    }

    /** Caller has bound the particle program and set uniforms. */
    fun draw() {
        if (count == 0) return
        var k = 0
        for (i in 0 until count) {
            val t = life[i] / maxLife[i]
            // fade in quickly, fade out over the last 35% of life
            val fade = if (wrap[i]) 1f else (minOf(1f, (1f - t) * 12f) * minOf(1f, t / 0.35f))
            val al = a[i] * fade
            array[k++] = x[i]; array[k++] = y[i]; array[k++] = size[i]; array[k++] = rot[i]
            array[k++] = r[i] * al; array[k++] = g[i] * al; array[k++] = b[i] * al; array[k++] = al
            array[k++] = shape[i]; array[k++] = cos(flip[i])
        }
        data.position(0)
        data.put(array, 0, k)
        data.position(0)
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, k * 4, data)
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, count)
        GLES30.glBindVertexArray(0)
    }

    companion object {
        fun dirX(angle: Float) = cos(angle)
        fun dirY(angle: Float) = sin(angle)
    }
}
