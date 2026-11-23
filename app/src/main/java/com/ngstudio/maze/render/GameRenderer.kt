package com.ngstudio.maze.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import com.ngstudio.maze.core.Dir
import com.ngstudio.maze.core.Level
import com.ngstudio.maze.core.Slide
import com.ngstudio.maze.core.Tile
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

private val SURFACE_RES = intArrayOf(
    com.ngstudio.maze.R.drawable.surface_0, com.ngstudio.maze.R.drawable.surface_1, com.ngstudio.maze.R.drawable.surface_2,
    com.ngstudio.maze.R.drawable.surface_3, com.ngstudio.maze.R.drawable.surface_4, com.ngstudio.maze.R.drawable.surface_5,
    com.ngstudio.maze.R.drawable.surface_6, com.ngstudio.maze.R.drawable.surface_7, com.ngstudio.maze.R.drawable.surface_8,
    com.ngstudio.maze.R.drawable.surface_9,
)

class SceneConfig(
    val level: Level,
    val levelNumber: Int,
    val theme: WorldTheme,
    val skin: BallSkin,
    val trail: TrailStyle,
    val finish: PaintFinish,
    val paint: Rgb,
    val paint2: Rgb,
    /** Skip the drop-in animation (used for previews). */
    val instant: Boolean = false,
)

/** Called on the main thread. */
interface RenderListener {
    fun onCellPainted(cell: Int, indexInMove: Int) {}
    fun onMoveFinished(endCell: Int, cells: Int) {}
    fun onTeleport() {}
    fun onImpact(strength: Float) {}
    fun onIntroDone() {}
    fun onWinShown() {}
}

/**
 * Renders the whole playfield. Public methods may be called from any thread; they are queued and
 * executed on the GL thread at the start of the next frame.
 */
class GameRenderer(private val listener: RenderListener) : GLSurfaceView.Renderer {

    private val main = Handler(Looper.getMainLooper())
    private val commands = ConcurrentLinkedQueue<Runnable>()

    // ------------------------------------------------------------------ public API (any thread)
    fun setScene(cfg: SceneConfig) = commands.add(Runnable { requestScene(cfg) })
    fun playSlide(slide: Slide) = commands.add(Runnable { startMove(slide.from, slide.cells, undo = false, unpaint = null) })
    /** Animate back along [slide] (reverse) and un-paint [unpaint] cells. */
    fun playUndo(slide: Slide, unpaint: IntArray) = commands.add(Runnable {
        val pts = ArrayList<Int>(slide.cells.size + 1)
        for (i in slide.cells.indices.reversed()) pts.add(slide.cells[i])
        // reversed path: start at end, visit cells back to `from`
        val path = pts.drop(1) + slide.from
        startMove(slide.end, path.toIntArray(), undo = true, unpaint = unpaint.toHashSet())
    })
    fun showHint(cells: IntArray, dir: Dir) = commands.add(Runnable { hintCells = cells; hintDir = dir.ordinal; hintT = 0f })
    fun clearHint() = commands.add(Runnable { hintCells = null })
    fun playWin() = commands.add(Runnable { winT = 0f; winShownSent = false })
    fun setSafeArea(topPx: Float, bottomPx: Float) = commands.add(Runnable {
        safeTop = topPx; safeBottom = bottomPx
        cfg?.let { if (layoutFor(it.level)) needsBake = true }
    })
    /** Re-paint the given cells instantly (restore after restart/resume). */
    fun setPainted(cells: IntArray) = commands.add(Runnable {
        val l = level ?: return@Runnable
        for (i in 0 until l.size) if (l.isFloor(i)) { paint[i * 4] = 0f }
        for (c in cells) { paint[c * 4] = 1f; paint[c * 4 + 1] = 4f; paint[c * 4 + 2] = now - 5f }
        fills.clear(); paintDirty = true
    })
    fun setBallCell(cell: Int) = commands.add(Runnable {
        val l = level ?: return@Runnable
        ballX = l.x(cell) + 0.5f; ballY = l.y(cell) + 0.5f; moving = false
    })

    // ------------------------------------------------------------------ GL state
    private class Prog(val id: Int) {
        private val cache = HashMap<String, Int>()
        fun u(name: String): Int = cache.getOrPut(name) { GLES30.glGetUniformLocation(id, name) }
    }

    private lateinit var pBg: Prog
    private lateinit var pBake: Prog
    private lateinit var pTex: Prog
    private lateinit var pPaint: Prog
    private lateinit var pSpecial: Prog
    private lateinit var pBall: Prog
    private lateinit var pShadow: Prog
    private lateinit var pHint: Prog
    private lateinit var pParticle: Prog
    private var quadVbo = 0
    private var quadVao = 0

    private val ambient = Particles(90)
    private val boardFx = Particles(900)
    private val screenFx = Particles(500)

    private var w = 1f
    private var h = 1f
    private var safeTop = 0f
    private var safeBottom = 0f
    private var now = 0f
    private var lastNanos = 0L
    private var fpsFrames = 0
    private var fpsStart = 0f

    // ------------------------------------------------------------------ scene state
    private var cfg: SceneConfig? = null
    private var pendingCfg: SceneConfig? = null
    private var level: Level? = null
    private var cellPx = 1f
    private var gridX = 0f
    private var gridY = 0f
    private var bakeX = 0f
    private var bakeY = 0f
    private var bakeW = 1
    private var bakeH = 1
    private var surfaceTex = 0
    private var surfaceTheme: WorldTheme? = null
    private var surfW = 1f
    private var surfH = 1f
    private var bakeColor: RenderTarget? = null
    private var bakeMask: RenderTarget? = null
    private var needsBake = false
    private var gridTex = 0
    private var paintTex = 0
    private var paint = FloatArray(0)
    private var paintBuf: FloatBuffer = GlUtil.floatBuffer(4)
    private var paintDirty = false
    private var paintOrder = 0f
    private val specials = ArrayList<IntArray>() // (cell, type, color)

    private class Fill(val cell: Int, val start: Float, val dur: Float, val from: Float, val to: Float)
    private val fills = ArrayList<Fill>()

    // ball
    private var ballX = 0f
    private var ballY = 0f
    private val rot = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    private var ballScale = 1f
    private var impactT = 9f
    private var impactH = true
    private var impactAmp = 0f

    // movement
    private class Step(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val jump: Boolean, val cell: Int, val dir: Int, val fromCell: Int)
    private val steps = ArrayList<Step>()
    private var stepIdx = 0
    private var stepProg = 0f
    private var stepTriggered = false
    private var speed = 0f
    private var moving = false
    private var undoMode = false
    private var unpaintSet: Set<Int>? = null
    private var moveCellCount = 0
    private var paintedInMove = 0
    private var trailAcc = 0f

    // transitions
    private var introT = -1f
    private var ballLanded = true
    private var introDoneSent = true
    private var outroT = -1f
    private var winT = -1f
    private var winShownSent = true
    private var shake = 0f
    private var shakeX = 0f
    private var shakeY = 0f

    // hint
    private var hintCells: IntArray? = null
    private var hintDir = 0
    private var hintT = 0f

    // ------------------------------------------------------------------ GLSurfaceView.Renderer
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        pBg = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.BG_FS))
        pBake = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.BAKE_FS))
        pTex = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.TEX_FS))
        pPaint = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.PAINT_FS))
        pSpecial = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.SPECIAL_FS))
        pBall = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.BALL_FS))
        pShadow = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.SHADOW_FS))
        pHint = Prog(GlUtil.program(Shaders.QUAD_VS, Shaders.HINT_FS))
        pParticle = Prog(GlUtil.program(Shaders.PARTICLE_VS, Shaders.PARTICLE_FS))

        val b = IntArray(1)
        GLES30.glGenBuffers(1, b, 0); quadVbo = b[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
        val quad = GlUtil.floatBuffer(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 8 * 4, quad, GLES30.GL_STATIC_DRAW)
        val v = IntArray(1)
        GLES30.glGenVertexArrays(1, v, 0); quadVao = v[0]
        GLES30.glBindVertexArray(quadVao)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
        ambient.initGl(quadVbo); boardFx.initGl(quadVbo); screenFx.initGl(quadVbo)

        // Context was (re)created: GPU resources must be rebuilt.
        bakeColor = null; bakeMask = null; gridTex = 0; paintTex = 0; surfaceTex = 0; surfaceTheme = null
        if (cfg != null) needsBake = true
        lastNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        w = width.toFloat(); h = height.toFloat()
        GLES30.glViewport(0, 0, width, height)
        cfg?.let { if (layoutFor(it.level)) needsBake = true }
        // (the surface texture mapping depends on the screen size; uniforms are recomputed every draw)
    }

    override fun onDrawFrame(gl: GL10?) {
        val t = System.nanoTime()
        val dt = ((t - lastNanos) / 1e9f).coerceIn(0f, 1f / 20f)
        lastNanos = t
        now += dt
        while (true) commands.poll()?.run() ?: break

        if (com.ngstudio.maze.BuildConfig.DEBUG) {
            fpsFrames++
            if (now - fpsStart >= 3f) {
                android.util.Log.i("MazeGL", "fps=%.1f".format(fpsFrames / (now - fpsStart)))
                fpsFrames = 0; fpsStart = now
            }
        }
        update(dt)
        if (needsBake) bake()
        if (paintDirty && paintTex != 0) uploadPaint()
        draw()
    }

    // ------------------------------------------------------------------ scene setup
    private fun requestScene(c: SceneConfig) {
        if (cfg != null && !c.instant && level != null) {
            pendingCfg = c
            if (outroT < 0f) outroT = 0f
        } else applyScene(c)
    }

    private fun applyScene(c: SceneConfig) {
        val first = cfg == null || cfg?.theme !== c.theme
        cfg = c
        val l = c.level
        level = l
        layoutFor(l)
        paint = FloatArray(l.size * 4)
        for (i in 0 until l.size) {
            paint[i * 4] = 0f
            paint[i * 4 + 1] = if (l.isFloor(i)) 4f else -1f
            paint[i * 4 + 2] = -10f
            paint[i * 4 + 3] = 0f
        }
        paintOrder = 0f
        paintBuf = GlUtil.floatBuffer(l.size * 4)
        fills.clear()
        specials.clear()
        for (i in 0 until l.size) {
            val tile = l.tile(i)
            val type = when {
                tile == Tile.STOPPER -> 1
                Tile.isOneWay(tile) -> 2 + Tile.oneWayDir(tile).ordinal
                tile == Tile.DEFLECT_SLASH -> 6
                tile == Tile.DEFLECT_BACKSLASH -> 7
                tile == Tile.PORTAL -> 8
                else -> 0
            }
            if (type == 0) continue
            val color = when (type) {
                1 -> 0xFFB547
                in 2..5 -> 0x38BDF8
                6, 7 -> 0x2DD4A7
                else -> if (portalPairIndex(l, i) == 0) 0xA855F7 else 0x22D3EE
            }
            specials.add(intArrayOf(i, type, color))
        }
        ballX = l.x(l.start) + 0.5f; ballY = l.y(l.start) + 0.5f
        rot.fill(0f); rot[0] = 1f; rot[4] = 1f; rot[8] = 1f
        moving = false; steps.clear(); hintCells = null; winT = -1f; winShownSent = true
        boardFx.clear()
        needsBake = true
        paintDirty = true
        if (c.instant) {
            introT = -1f; introDoneSent = true; ballScale = 1f; ballLanded = true
            startFill(l.start, 4f, 0.01f)
        } else {
            introT = 0f; introDoneSent = false; ballScale = 0f; ballLanded = false
        }
        if (first) ambient.clear()
    }

    private fun portalPairIndex(l: Level, cell: Int): Int {
        var k = 0
        while (k + 1 < l.portals.size) { if (l.portals[k] == cell || l.portals[k + 1] == cell) return k / 2; k += 2 }
        return 0
    }

    /** Fits the island between the HUD areas. Returns true when the layout changed. */
    private fun layoutFor(l: Level): Boolean {
        val margin = 0.32f
        val thick = 0f
        val padX = w * 0.03f
        val availW = w - padX * 2
        val top = safeTop + h * 0.015f
        val availH = (h - safeBottom - h * 0.02f) - top
        var cs = min(availW / (l.width + margin * 2), availH / (l.height + margin * 2 + thick))
        cs = min(cs, w / 6.6f)
        val gw = l.width * cs; val gh = l.height * cs
        val nx = (w - gw) / 2f
        val ny = top + (availH - gh - thick * cs) / 2f
        val changed = abs(cs - cellPx) > 0.5f || abs(nx - gridX) > 0.5f || abs(ny - gridY) > 0.5f
        cellPx = cs; gridX = nx; gridY = ny
        bakeX = (gridX - 1.0f * cs).toInt().toFloat()
        bakeY = (gridY - 1.0f * cs).toInt().toFloat()
        bakeW = (gw + 2.0f * cs).toInt().coerceAtLeast(8)
        bakeH = (gh + 2.0f * cs).toInt().coerceAtLeast(8)
        return changed
    }

    private fun bake() {
        val c = cfg ?: return
        val l = level ?: return
        needsBake = false
        // load first: binding the new texture would otherwise replace the grid texture on unit 0
        if (surfaceTex == 0 || surfaceTheme !== c.theme) loadSurface(c.theme)
        bakeColor?.release(); bakeMask?.release()
        GlUtil.deleteTexture(gridTex); GlUtil.deleteTexture(paintTex)

        gridTex = GlUtil.genTexture()
        GlUtil.nearest(gridTex)
        val bytes = ByteBuffer.allocateDirect(l.size)
        for (i in 0 until l.size) bytes.put(if (l.isFloor(i)) 255.toByte() else 0)
        bytes.position(0)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_R8, l.width, l.height, 0, GLES30.GL_RED, GLES30.GL_UNSIGNED_BYTE, bytes)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)

        paintTex = GlUtil.genTexture()
        GlUtil.nearest(paintTex)
        paintBuf.position(0); paintBuf.put(paint); paintBuf.position(0)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, l.width, l.height, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, paintBuf)
        paintDirty = false

        val color = RenderTarget(bakeW, bakeH)
        val mask = RenderTarget(bakeW, bakeH)
        GLES30.glDisable(GLES30.GL_BLEND)
        for ((mode, target) in listOf(0 to color, 1 to mask)) {
            target.bind()
            GLES30.glClearColor(0f, 0f, 0f, 0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glUseProgram(pBake.id)
            quadUniforms(pBake, 0f, 0f, bakeW.toFloat(), bakeH.toFloat(), bakeW.toFloat(), bakeH.toFloat(), identity = true)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, gridTex)
            GLES30.glUniform1i(pBake.u("uGrid"), 0)
            GLES30.glUniform2i(pBake.u("uGridSize"), l.width, l.height)
            GLES30.glUniform1f(pBake.u("uCell"), cellPx)
            GLES30.glUniform2f(pBake.u("uOrigin"), gridX - bakeX, gridY - bakeY)
            GLES30.glUniform1i(pBake.u("uMode"), mode)
            GLES30.glUniform2f(pBake.u("uBakeOrigin"), bakeX, bakeY)
            surfaceUniforms(pBake, 1)
            col3(pBake, "uFloorA", c.theme.floorA); col3(pBake, "uFloorB", c.theme.floorB)
            drawQuad()
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, w.toInt(), h.toInt())
        bakeColor = color; bakeMask = mask
        GlUtil.check("bake")
    }

    /** Loads the world's surface image (same picture as the menus) as a GL texture. */
    private fun loadSurface(th: WorldTheme) {
        GlUtil.deleteTexture(surfaceTex)
        val res = SURFACE_RES[th.index % SURFACE_RES.size]
        val opts = android.graphics.BitmapFactory.Options().apply { inScaled = false }
        val bmp = android.graphics.BitmapFactory.decodeResource(com.ngstudio.maze.MazeApp.instance.resources, res, opts)
        surfaceTex = GlUtil.genTexture()
        GlUtil.linear(surfaceTex)
        android.opengl.GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        surfW = bmp.width.toFloat(); surfH = bmp.height.toFloat()
        bmp.recycle()
        surfaceTheme = th
        GlUtil.check("surface")
    }

    /** Centre-crop mapping of the surface image onto the screen. */
    private fun surfaceUniforms(p: Prog, unit: Int) {
        val scale = max(w / surfW, h / surfH)
        val dw = surfW * scale; val dh = surfH * scale
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, surfaceTex)
        GLES30.glUniform1i(p.u("uSurface"), unit)
        GLES30.glUniform2f(p.u("uSurfOffset"), (dw - w) / 2f, (dh - h) / 2f)
        GLES30.glUniform2f(p.u("uSurfSize"), dw, dh)
        GLES30.glUniform2f(p.u("uRealScreen"), w, h)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
    }

    private fun uploadPaint() {
        val l = level ?: return
        paintBuf.position(0); paintBuf.put(paint); paintBuf.position(0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, paintTex)
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, l.width, l.height, GLES30.GL_RGBA, GLES30.GL_FLOAT, paintBuf)
        paintDirty = false
    }

    // ------------------------------------------------------------------ movement
    private fun startMove(from: Int, cells: IntArray, undo: Boolean, unpaint: Set<Int>?) {
        val l = level ?: return
        steps.clear()
        var prev = from
        var px = l.x(from) + 0.5f; var py = l.y(from) + 0.5f
        for (c in cells) {
            val cx = l.x(c) + 0.5f; val cy = l.y(c) + 0.5f
            val adjacent = abs(l.x(c) - l.x(prev)) + abs(l.y(c) - l.y(prev)) == 1
            val dir = when {
                !adjacent -> 4
                cx > px -> 1
                cx < px -> 3
                cy > py -> 2
                else -> 0
            }
            steps.add(Step(px, py, cx, cy, !adjacent, c, dir, prev))
            prev = c; px = cx; py = cy
        }
        if (steps.isEmpty()) return
        stepIdx = 0; stepProg = 0f; stepTriggered = false
        speed = if (undo) 9f else 4f
        moving = true
        undoMode = undo
        unpaintSet = unpaint
        moveCellCount = cells.size
        paintedInMove = 0
        hintCells = null
        ballX = l.x(from) + 0.5f; ballY = l.y(from) + 0.5f
    }

    private fun updateMove(dt: Float) {
        if (!moving) return
        var remaining = dt
        val vMax = if (undoMode) 22f else 15.5f
        val accel = if (undoMode) 160f else 95f
        while (remaining > 0f && moving) {
            val s = steps[stepIdx]
            if (s.jump) {
                val jumpDur = 0.28f
                stepProg += remaining / jumpDur
                remaining = 0f
                if (stepProg < 0.5f) {
                    ballX = s.x0; ballY = s.y0; ballScale = 1f - stepProg * 2f
                } else {
                    if (!stepTriggered) {
                        stepTriggered = true
                        cellReached(s)
                        main.post { listener.onTeleport() }
                        burst(s.x1, s.y1, 14, cfg!!.paint, 3, 3.5f)
                    }
                    ballX = s.x1; ballY = s.y1; ballScale = ((stepProg - 0.5f) * 2f).coerceAtMost(1f)
                }
                if (stepProg >= 1f) { ballScale = 1f; arrive(); }
            } else {
                speed = min(vMax, speed + accel * remaining)
                val adv = speed * remaining
                val left = 1f - stepProg
                val used = min(adv, left)
                stepProg += used
                remaining -= used / speed
                ballX = s.x0 + (s.x1 - s.x0) * stepProg
                ballY = s.y0 + (s.y1 - s.y0) * stepProg
                roll(s.x1 - s.x0, s.y1 - s.y0, used)
                if (!stepTriggered && stepProg >= (if (undoMode) 0.3f else 0.12f)) {
                    stepTriggered = true
                    cellReached(s)
                }
                if (stepProg >= 1f - 1e-4f) arrive()
            }
        }
    }

    private fun cellReached(s: Step) {
        val l = level ?: return
        if (undoMode) {
            val leaving = s.fromCell
            if (unpaintSet?.contains(leaving) == true) startFill(leaving, paint[leaving * 4 + 1], 0.14f, to = 0f)
            return
        }
        val c = s.cell
        if (paint[c * 4] < 1f && !isFilling(c)) {
            val dur = (1.05f / max(speed, 4f)).coerceIn(0.05f, 0.2f)
            paintOrder += 1f
            paint[c * 4 + 3] = paintOrder
            startFill(c, s.dir.toFloat(), dur)
            val idx = paintedInMove++
            main.post { listener.onCellPainted(c, idx) }
            if (l.isFloor(c)) sparkleAt(l.x(c) + 0.5f, l.y(c) + 0.5f)
        }
    }

    private fun isFilling(c: Int) = fills.any { it.cell == c && it.to > 0f }

    private fun arrive() {
        stepIdx++
        stepProg = 0f
        stepTriggered = false
        if (stepIdx >= steps.size) {
            moving = false
            val last = steps.last()
            if (undoMode) {
                val leaving = last.cell
                if (unpaintSet?.contains(leaving) == true) startFill(leaving, paint[leaving * 4 + 1], 0.14f, to = 0f)
            }
            val l = level!!
            val endCell = last.cell
            if (!undoMode && last.dir in 0..3) {
                // impact against the wall ahead
                impactT = 0f
                impactH = last.dir == 1 || last.dir == 3
                val strength = (speed / 15.5f).coerceIn(0.3f, 1f)
                impactAmp = 0.10f + 0.12f * strength
                val dx = Dir.of(last.dir).dx.toFloat(); val dy = Dir.of(last.dir).dy.toFloat()
                impactBurst(ballX + dx * 0.36f, ballY + dy * 0.36f, -dx, -dy, strength)
                // (no screen shake: the board is part of the surface)
                main.post { listener.onImpact(strength) }
            }
            val n = moveCellCount
            speed = 0f
            main.post { listener.onMoveFinished(endCell, n) }
            if (l.isFloor(endCell)) Unit
        }
    }

    /** Accumulates the rolling rotation for a move of [dist] cells along (dx, dy) (screen axes). */
    private fun roll(dx: Float, dy: Float, dist: Float) {
        if (dist <= 0f) return
        val mx = sign(dx); val myUp = -sign(dy)
        val ax = -myUp; val ay = mx
        val len = sqrt(ax * ax + ay * ay)
        if (len < 1e-4f) return
        val angle = dist / 0.36f
        val x = ax / len; val y = ay / len
        val c = cos(angle); val s = sin(angle); val t = 1 - c
        // rotation about (x, y, 0)
        val r00 = t * x * x + c; val r01 = t * x * y; val r02 = s * y
        val r10 = t * x * y; val r11 = t * y * y + c; val r12 = -s * x
        val r20 = -s * y; val r21 = s * x; val r22 = c
        val m = rot.copyOf()
        for (i in 0 until 3) {
            val a0 = m[i]; val a1 = m[3 + i]; val a2 = m[6 + i]
            rot[i] = r00 * a0 + r01 * a1 + r02 * a2
            rot[3 + i] = r10 * a0 + r11 * a1 + r12 * a2
            rot[6 + i] = r20 * a0 + r21 * a1 + r22 * a2
        }
    }

    private fun startFill(cell: Int, dir: Float, dur: Float, to: Float = 1f) {
        fills.removeAll { it.cell == cell }
        val from = paint[cell * 4]
        if (to > 0f) { paint[cell * 4 + 1] = dir; paint[cell * 4 + 2] = now }
        fills.add(Fill(cell, now, dur, from, to))
    }

    private fun updateFills() {
        if (fills.isEmpty()) return
        val it = fills.iterator()
        while (it.hasNext()) {
            val f = it.next()
            val k = ((now - f.start) / f.dur).coerceIn(0f, 1f)
            paint[f.cell * 4] = f.from + (f.to - f.from) * k
            if (k >= 1f) it.remove()
        }
        paintDirty = true
    }

    // ------------------------------------------------------------------ effects
    private fun boardPx(cx: Float, cy: Float) = floatArrayOf(gridX + cx * cellPx, gridY + cy * cellPx)

    private fun burst(cx: Float, cy: Float, n: Int, color: Rgb, shape: Int, speedCells: Float) {
        val p = boardPx(cx, cy)
        repeat(n) {
            val a = boardFx.random() * 6.283f
            val v = (0.4f + boardFx.random()) * speedCells * cellPx
            boardFx.spawn(p[0], p[1], cos(a) * v, sin(a) * v, cellPx * (0.10f + 0.10f * boardFx.random()), color, 1f,
                0.35f + 0.3f * boardFx.random(), shape, pdrag = 4f)
        }
    }

    private fun impactBurst(cx: Float, cy: Float, nx: Float, ny: Float, strength: Float) {
        val c = cfg ?: return
        val p = boardPx(cx, cy)
        val n = (8 + 10 * strength).toInt()
        repeat(n) {
            val spread = (boardFx.random() - 0.5f) * 2.4f
            val tx = nx + (if (nx == 0f) spread else 0f) * 0.9f
            val ty = ny + (if (ny == 0f) spread else 0f) * 0.9f
            val v = (1.5f + 3.5f * boardFx.random()) * cellPx * strength
            boardFx.spawn(p[0], p[1], tx * v, ty * v, cellPx * (0.07f + 0.09f * boardFx.random()), c.paint, 1f,
                0.35f + 0.35f * boardFx.random(), 3, pdrag = 5.5f, pgrow = -cellPx * 0.08f)
        }
        boardFx.spawn(p[0], p[1], 0f, 0f, cellPx * 0.3f, Rgb(0xFFFFFF), 0.55f, 0.3f, 4, pgrow = cellPx * 2.4f)
    }

    private fun sparkleAt(cx: Float, cy: Float) {
        val c = cfg ?: return
        val p = boardPx(cx, cy)
        if (boardFx.random() < 0.6f) {
            val a = boardFx.random() * 6.283f
            boardFx.spawn(p[0] + cos(a) * cellPx * 0.3f, p[1] + sin(a) * cellPx * 0.3f, 0f, -cellPx * 0.4f,
                cellPx * (0.18f + 0.12f * boardFx.random()), if (boardFx.random() < 0.5f) Rgb(0xFFFFFF) else c.paint, 0.9f,
                0.45f, 2, spin = 2f)
        }
    }

    private fun emitTrail(dt: Float) {
        val c = cfg ?: return
        if (!moving || undoMode || ballScale < 0.9f) return
        trailAcc += dt * 60f
        val p = boardPx(ballX, ballY)
        while (trailAcc >= 1f) {
            trailAcc -= 1f
            val jx = (boardFx.random() - 0.5f) * cellPx * 0.4f
            val jy = (boardFx.random() - 0.5f) * cellPx * 0.4f
            when (c.trail.id) {
                1 -> if (boardFx.random() < 0.55f) boardFx.spawn(p[0] + jx, p[1] + jy, 0f, 0f, cellPx * (0.14f + 0.12f * boardFx.random()), Rgb(0xFFFFFF), 0.9f, 0.4f, 2, spin = 3f)
                2 -> if (boardFx.random() < 0.5f) boardFx.spawn(p[0] + jx, p[1] + jy, 0f, -cellPx * 0.6f, cellPx * (0.12f + 0.14f * boardFx.random()), Rgb(0xFFFFFF), 0.7f, 0.7f, 5)
                3 -> boardFx.spawn(p[0], p[1], 0f, 0f, cellPx * 0.55f, c.skin.b, 0.5f, 0.28f, 0, pgrow = -cellPx * 1.6f)
                4 -> if (boardFx.random() < 0.5f) boardFx.spawn(p[0] + jx, p[1] + jy, jx * 2f, jy * 2f, cellPx * 0.16f, c.theme.paints[(boardFx.random() * 5).toInt().coerceAtMost(4)], 1f, 0.6f, 1, spin = 6f)
                5 -> {
                    val hue = (now * 0.8f) % 1f
                    boardFx.spawn(p[0] + jx * 0.3f, p[1] + jy * 0.3f, 0f, 0f, cellPx * 0.36f, hsv(hue, 0.6f, 1f), 0.6f, 0.35f, 0, pgrow = -cellPx * 0.9f)
                }
                6 -> if (boardFx.random() < 0.4f) boardFx.spawn(p[0] + jx, p[1] + jy, jx * 3f, jy * 3f, cellPx * (0.08f + 0.08f * boardFx.random()), c.paint, 1f, 0.5f, 3, pdrag = 3f)
                7 -> if (boardFx.random() < 0.7f) boardFx.spawn(p[0] + jx, p[1] + jy, 0f, 0f, cellPx * (0.08f + 0.10f * boardFx.random()), Rgb(0xFFE38A), 1f, 0.6f, 2, spin = 4f)
                else -> Unit
            }
        }
    }

    private fun hsv(hh: Float, s: Float, v: Float): Rgb {
        val i = (hh * 6).toInt() % 6
        val f = hh * 6 - (hh * 6).toInt()
        val p = v * (1 - s); val q = v * (1 - f * s); val t = v * (1 - (1 - f) * s)
        val (r, g, b) = when (i) { 0 -> Triple(v, t, p); 1 -> Triple(q, v, p); 2 -> Triple(p, v, t); 3 -> Triple(p, q, v); 4 -> Triple(t, p, v); else -> Triple(v, p, q) }
        return Rgb(((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt())
    }

    private fun confetti() {
        val c = cfg ?: return
        val colors = c.theme.paints + listOf(Rgb(0xFFFFFF), Rgb(0xFFE45C))
        for (side in 0..1) {
            val x0 = if (side == 0) -10f else w + 10f
            repeat(80) {
                val ang = (if (side == 0) -1.05f else -(PI.toFloat() - 1.05f)) + (screenFx.random() - 0.5f) * 0.7f
                val v = h * (0.75f + 0.55f * screenFx.random())
                screenFx.spawn(x0, h * 0.82f, cos(ang) * v, sin(ang) * v, w * (0.022f + 0.016f * screenFx.random()),
                    colors[(screenFx.random() * colors.size).toInt().coerceAtMost(colors.size - 1)], 1f,
                    2.2f + 1.2f * screenFx.random(), 1, pgravity = h * 0.55f, pdrag = 1.6f, spin = (screenFx.random() - 0.5f) * 10f)
            }
        }
    }

    // ------------------------------------------------------------------ per-frame update
    private fun update(dt: Float) {
        val c = cfg
        // outro (old island leaves) -> apply pending scene
        if (outroT >= 0f) {
            outroT += dt
            if (outroT >= 0.32f) {
                outroT = -1f
                pendingCfg?.let { pendingCfg = null; applyScene(it) }
            }
        }
        if (introT >= 0f) {
            introT += dt
            if (introT > 0.45f && !ballLanded) {
                val k = ((introT - 0.45f) / 0.3f).coerceIn(0f, 1f)
                ballScale = easeOutBack(k)
                if (k >= 1f) {
                    ballScale = 1f
                    ballLanded = true
                    level?.let { l ->
                        startFill(l.start, 4f, 0.22f)
                        val p = boardPx(l.x(l.start) + 0.5f, l.y(l.start) + 0.5f)
                        boardFx.spawn(p[0], p[1], 0f, 0f, cellPx * 0.4f, Rgb(0xFFFFFF), 0.6f, 0.35f, 4, pgrow = cellPx * 2f)
                    }
                }
            }
            if (introT > 0.8f && !introDoneSent) { introDoneSent = true; main.post { listener.onIntroDone() } }
            if (introT > 1.2f) introT = -1f
        }
        updateMove(dt)
        updateFills()
        emitTrail(dt)
        impactT += dt
        hintT += dt
        if (winT >= 0f) {
            val before = winT
            winT += dt
            if (before < 0.08f && winT >= 0.08f) confetti()
            if (winT > 1.0f && !winShownSent) { winShownSent = true; main.post { listener.onWinShown() } }
        }
        shake = max(0f, shake - dt * 14f)
        shakeX = (boardFx.random() - 0.5f) * 2f * shake
        shakeY = (boardFx.random() - 0.5f) * 2f * shake
        ambient.update(dt, w, h)
        boardFx.update(dt, w, h)
        screenFx.update(dt, w, h)
        if (c == null) return
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f; val c3 = c1 + 1f
        val x = t - 1f
        return 1f + c3 * x * x * x + c1 * x * x
    }

    // ------------------------------------------------------------------ drawing
    private var curCenterX = 0f
    private var curCenterY = 0f
    private var curScale = 1f
    private var curOffX = 0f
    private var curOffY = 0f

    private fun quadUniforms(p: Prog, x: Float, y: Float, qw: Float, qh: Float, sw: Float = w, sh: Float = h, identity: Boolean = false) {
        GLES30.glUniform4f(p.u("uRect"), x, y, qw, qh)
        GLES30.glUniform2f(p.u("uScreen"), sw, sh)
        if (identity) {
            GLES30.glUniform2f(p.u("uCenter"), 0f, 0f)
            GLES30.glUniform3f(p.u("uXform"), 1f, 0f, 0f)
        } else {
            GLES30.glUniform2f(p.u("uCenter"), curCenterX, curCenterY)
            GLES30.glUniform3f(p.u("uXform"), curScale, curOffX, curOffY)
        }
    }

    private fun col3(p: Prog, name: String, c: Rgb) = GLES30.glUniform3f(p.u(name), c.r, c.g, c.b)

    private fun drawQuad() {
        GLES30.glBindVertexArray(quadVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun draw() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, w.toInt(), h.toInt())
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        val c = cfg
        val theme = c?.theme ?: WorldTheme.ALL[0]

        // background: the world's surface image
        if (surfaceTex == 0 || surfaceTheme !== theme) loadSurface(theme)
        GLES30.glUseProgram(pBg.id)
        quadUniforms(pBg, 0f, 0f, w, h, identity = true)
        surfaceUniforms(pBg, 0)
        drawQuad()

        // ambient particles
        GLES30.glUseProgram(pParticle.id)
        GLES30.glUniform2f(pParticle.u("uScreen"), w, h)
        GLES30.glUniform2f(pParticle.u("uCenter"), 0f, 0f)
        GLES30.glUniform3f(pParticle.u("uXform"), 1f, 0f, 0f)
        ambient.draw()

        val l = level
        val bc = bakeColor
        if (c != null && l != null && bc != null) {
            // island transform: intro drop, outro fall, win bounce, shake
            curCenterX = gridX + l.width * cellPx / 2f
            curCenterY = gridY + l.height * cellPx / 2f
            // The board is carved into the screen surface, so it never moves: it fades in (carving) and out.
            var alpha = 1f
            if (introT >= 0f) alpha = (introT / 0.45f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
            if (outroT >= 0f) alpha = 1f - (outroT / 0.32f).coerceIn(0f, 1f)
            curScale = 1f
            curOffX = 0f
            curOffY = 0f

            GLES30.glUseProgram(pTex.id)
            quadUniforms(pTex, bakeX, bakeY, bakeW.toFloat(), bakeH.toFloat())
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, bc.texture)
            GLES30.glUniform1i(pTex.u("uTex"), 0)
            GLES30.glUniform1f(pTex.u("uAlpha"), alpha)
            drawQuad()

            // paint
            val mask = bakeMask
            if (mask != null && paintTex != 0) {
                GLES30.glUseProgram(pPaint.id)
                quadUniforms(pPaint, bakeX, bakeY, bakeW.toFloat(), bakeH.toFloat())
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mask.texture)
                GLES30.glUniform1i(pPaint.u("uMask"), 0)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, paintTex)
                GLES30.glUniform1i(pPaint.u("uPaint"), 1)
                GLES30.glUniform2i(pPaint.u("uGridSize"), l.width, l.height)
                GLES30.glUniform1f(pPaint.u("uCell"), cellPx)
                GLES30.glUniform2f(pPaint.u("uOrigin"), gridX, gridY)
                col3(pPaint, "uPaintA", c.paint); col3(pPaint, "uPaintB", c.paint2)
                GLES30.glUniform1f(pPaint.u("uTime"), now)
                val shine = if (winT in 0.05f..1.1f) (winT - 0.05f) / 0.85f * 1.3f - 0.15f else -1f
                GLES30.glUniform1f(pPaint.u("uShine"), shine)
                GLES30.glUniform1f(pPaint.u("uFinish"), c.finish.id.toFloat())
                GLES30.glUniform1f(pPaint.u("uGlow"), if (c.theme.glow) 1f else 0f)
                GLES30.glUniform1f(pPaint.u("uAlpha"), alpha)
                drawQuad()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            }

            // special tiles
            if (specials.isNotEmpty()) {
                GLES30.glUseProgram(pSpecial.id)
                GLES30.glUniform1f(pSpecial.u("uTime"), now)
                GLES30.glUniform1f(pSpecial.u("uCellPx"), cellPx)
                GLES30.glUniform1f(pSpecial.u("uAlpha"), alpha)
                for (s in specials) {
                    val cell = s[0]
                    quadUniforms(pSpecial, gridX + l.x(cell) * cellPx, gridY + l.y(cell) * cellPx, cellPx, cellPx)
                    GLES30.glUniform1i(pSpecial.u("uType"), s[1])
                    col3(pSpecial, "uColor", Rgb(s[2]))
                    drawQuad()
                }
            }

            // hint
            hintCells?.let { hc ->
                GLES30.glUseProgram(pHint.id)
                GLES30.glUniform1f(pHint.u("uTime"), now)
                val fade = (hintT / 0.25f).coerceIn(0f, 1f) * alpha
                GLES30.glUniform1f(pHint.u("uAlpha"), fade * 0.85f)
                GLES30.glUniform1i(pHint.u("uKind"), 0)
                GLES30.glUniform1i(pHint.u("uDir"), hintDir)
                val ds = cellPx * 0.26f
                for (cell in hc) {
                    quadUniforms(pHint, gridX + (l.x(cell) + 0.5f) * cellPx - ds / 2, gridY + (l.y(cell) + 0.5f) * cellPx - ds / 2, ds, ds)
                    drawQuad()
                }
                val d = Dir.of(hintDir)
                val ax = gridX + (ballX + d.dx * 0.78f) * cellPx
                val ay = gridY + (ballY + d.dy * 0.78f) * cellPx
                val asz = cellPx * 0.95f
                GLES30.glUniform1i(pHint.u("uKind"), 1)
                GLES30.glUniform1f(pHint.u("uAlpha"), fade)
                quadUniforms(pHint, ax - asz / 2, ay - asz / 2, asz, asz)
                drawQuad()
            }

            // ball shadow + ball
            if (ballScale > 0.01f) {
                val bs = cellPx * 0.80f * ballScale
                val bx = gridX + ballX * cellPx
                val by = gridY + ballY * cellPx
                // squash & stretch
                var sx = 1f; var sy = 1f
                if (moving && !steps.isEmpty() && stepIdx < steps.size && !steps[stepIdx].jump) {
                    val st = (speed / 15.5f).coerceIn(0f, 1f) * 0.10f
                    val horiz = steps[stepIdx].dir == 1 || steps[stepIdx].dir == 3
                    if (horiz) { sx = 1f + st; sy = 1f - st * 0.6f } else { sy = 1f + st; sx = 1f - st * 0.6f }
                }
                if (impactT < 0.5f) {
                    val sq = impactAmp * exp(-impactT * 9f) * cos(impactT * 32f)
                    if (impactH) { sx -= sq; sy += sq * 0.7f } else { sy -= sq; sx += sq * 0.7f }
                }
                GLES30.glUseProgram(pShadow.id)
                val shw = bs * 1.25f * sx; val shh = bs * 0.9f * sy
                quadUniforms(pShadow, bx - shw / 2 + cellPx * 0.07f, by - shh / 2 + cellPx * 0.16f, shw, shh)
                GLES30.glUniform1f(pShadow.u("uAlpha"), 0.38f * alpha)
                drawQuad()

                GLES30.glUseProgram(pBall.id)
                val qw = bs * sx; val qh = bs * sy
                quadUniforms(pBall, bx - qw / 2, by - qh / 2, qw, qh)
                GLES30.glUniformMatrix3fv(pBall.u("uRot"), 1, false, rot, 0)
                col3(pBall, "uColorA", c.skin.a); col3(pBall, "uColorB", c.skin.b); col3(pBall, "uColorC", c.skin.c)
                GLES30.glUniform1f(pBall.u("uPattern"), c.skin.pattern.toFloat())
                GLES30.glUniform1f(pBall.u("uGloss"), c.skin.gloss)
                GLES30.glUniform1f(pBall.u("uMetal"), c.skin.metal)
                col3(pBall, "uEnv", theme.matA)
                GLES30.glUniform1f(pBall.u("uPx"), bs)
                GLES30.glUniform1f(pBall.u("uAlpha"), alpha)
                GLES30.glUniform1f(pBall.u("uTime"), now)
                drawQuad()
            }

            GLES30.glUseProgram(pParticle.id)
            GLES30.glUniform2f(pParticle.u("uScreen"), w, h)
            GLES30.glUniform2f(pParticle.u("uCenter"), curCenterX, curCenterY)
            GLES30.glUniform3f(pParticle.u("uXform"), curScale, curOffX, curOffY)
            boardFx.draw()
        }

        GLES30.glUseProgram(pParticle.id)
        GLES30.glUniform2f(pParticle.u("uScreen"), w, h)
        GLES30.glUniform2f(pParticle.u("uCenter"), 0f, 0f)
        GLES30.glUniform3f(pParticle.u("uXform"), 1f, 0f, 0f)
        screenFx.draw()
    }
}
