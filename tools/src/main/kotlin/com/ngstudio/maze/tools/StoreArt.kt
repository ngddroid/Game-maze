package com.ngstudio.maze.tools

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.sin

/**
 * Play Store graphics drawn with Java2D, matching the adaptive launcher icon
 * (res/drawable/ic_launcher_background.xml + ic_launcher_foreground.xml).
 */
object StoreArt {
    private fun c(hex: Int, a: Int = 255) = Color((hex shr 16) and 255, (hex shr 8) and 255, hex and 255, a)

    fun run(out: File) {
        out.mkdirs()
        icon(512).let { ImageIO.write(it, "png", File(out, "icon_512.png")) }
        feature().let { ImageIO.write(it, "png", File(out, "feature_graphic.png")) }
        // legacy launcher icons (API 24-25; API 26+ uses the adaptive vector icon)
        val res = File("app/src/main/res")
        for ((dir, px) in listOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)) {
            val full = icon(px * 4)
            for (round in listOf(false, true)) {
                val img = BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB)
                val g = g2(img)
                val m = px * 0.04
                val shape = if (round) Ellipse2D.Double(m, m, px - 2 * m, px - 2 * m) else RoundRectangle2D.Double(m, m, px - 2 * m, px - 2 * m, px * 0.36, px * 0.36)
                g.clip(shape)
                g.drawImage(full.getScaledInstance(px, px, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
                g.dispose()
                val f = File(res, "mipmap-$dir/${if (round) "ic_launcher_round" else "ic_launcher"}.png")
                f.parentFile.mkdirs()
                ImageIO.write(img, "png", f)
            }
        }
        println("  ${File(out, "icon_512.png")}\n  ${File(out, "feature_graphic.png")}")
    }

    private fun g2(img: BufferedImage): Graphics2D = img.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    /** Icon on the 108-unit adaptive grid, scaled to [size]. */
    fun icon(size: Int): BufferedImage {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = g2(img)
        g.scale(size / 108.0, size / 108.0)
        drawBackground(g, 108.0, 108.0)
        drawSwooshAndBall(g, 0.0, 0.0, 1.0)
        g.dispose()
        return img
    }

    private fun drawBackground(g: Graphics2D, w: Double, h: Double) {
        g.paint = LinearGradientPaint(Point2D.Double(0.0, 0.0), Point2D.Double(w, h), floatArrayOf(0f, 0.55f, 1f), arrayOf(c(0xD69552), c(0xA9622A), c(0x6B3211)))
        g.fill(java.awt.geom.Rectangle2D.Double(0.0, 0.0, w, h))
        // vertical teak grain
        g.stroke = BasicStroke((w / 50).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = c(0x3D1A06, 46)
        var x = w * 0.09
        var i = 0
        while (x < w) {
            val d = if (i % 2 == 0) 1.0 else -1.0
            g.draw(java.awt.geom.CubicCurve2D.Double(x, 0.0, x + d * w * 0.03, h * 0.3, x - d * w * 0.04, h * 0.62, x + d * w * 0.01, h))
            x += w * 0.135; i++
        }
        g.color = c(0xFFE7C2, 31)
        g.stroke = BasicStroke((w / 90).toFloat())
        x = w * 0.16
        while (x < w) { g.draw(java.awt.geom.CubicCurve2D.Double(x, 0.0, x + w * 0.02, h * 0.3, x - w * 0.03, h * 0.62, x + w * 0.01, h)); x += w * 0.27 }
        g.paint = GradientPaint(0f, (h * 0.75).toFloat(), c(0x000000, 0), 0f, h.toFloat(), c(0x000000, 70))
        g.fill(java.awt.geom.Rectangle2D.Double(0.0, h * 0.75, w, h * 0.25))
    }

    private fun drawSwooshAndBall(g: Graphics2D, ox: Double, oy: Double, s: Double) {
        val tx = g.transform
        g.translate(ox, oy); g.scale(s, s)
        // paint swoosh
        val swoosh = GeneralPath().apply { moveTo(24.0, 78.0); curveTo(30.0, 58.0, 44.0, 58.0, 58.0, 52.0) }
        g.paint = c(0x000000, 40)
        g.stroke = BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.translate(1.0, 3.0); g.draw(swoosh); g.translate(-1.0, -3.0)
        g.paint = LinearGradientPaint(Point2D.Double(24.0, 78.0), Point2D.Double(58.0, 52.0), floatArrayOf(0f, 1f), arrayOf(c(0x2EDB9B), c(0xFFD93D)))
        g.draw(swoosh)
        g.paint = c(0xFFFFFF, 95)
        g.stroke = BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(GeneralPath().apply { moveTo(27.0, 70.0); curveTo(32.0, 60.0, 42.0, 57.0, 52.0, 53.5) })
        // ball shadow
        g.paint = c(0x3A1F5C, 50)
        g.fill(Ellipse2D.Double(50.0, 61.0, 30.0, 9.0))
        // ball
        val ball = Ellipse2D.Double(45.0, 31.0, 34.0, 34.0)
        g.paint = RadialGradientPaint(Point2D.Double(56.0, 41.0), 30f, floatArrayOf(0f, 0.45f, 1f), arrayOf(c(0xFFFFFF), c(0xFFE6F2), c(0xE2A9CB)))
        g.fill(ball)
        val clip = g.clip
        g.clip(ball)
        val rot = g.transform
        g.rotate(Math.toRadians(-18.0), 62.0, 48.0)
        g.paint = GradientPaint(0f, 44f, c(0xFF6FB5), 0f, 54f, c(0xE0347F))
        g.fill(java.awt.geom.Rectangle2D.Double(40.0, 43.5, 44.0, 9.5))
        g.transform = rot
        g.paint = RadialGradientPaint(Point2D.Double(56.0, 40.0), 34f, floatArrayOf(0.55f, 1f), arrayOf(c(0x000000, 0), c(0x5B2A6E, 70)))
        g.fill(ball)
        g.clip = clip
        g.paint = c(0xFFFFFF, 235)
        val hl = g.transform
        g.rotate(Math.toRadians(-30.0), 55.0, 39.0)
        g.fill(Ellipse2D.Double(50.0, 36.0, 10.0, 6.0))
        g.transform = hl
        // sparkles
        for ((x, y, r) in listOf(Triple(84.0, 30.0, 5.0), Triple(30.0, 42.0, 3.5), Triple(86.0, 70.0, 3.0))) g.fill(sparkle(x, y, r))
        g.transform = tx
    }

    private fun sparkle(x: Double, y: Double, r: Double): Path2D = GeneralPath().apply {
        moveTo(x, y - r); quadTo(x, y, x + r, y); quadTo(x, y, x, y + r); quadTo(x, y, x - r, y); quadTo(x, y, x, y - r); closePath()
    }

    /** 1024x500 Play feature graphic. */
    fun feature(): BufferedImage {
        val w = 1024; val h = 500
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = g2(img)
        drawBackground(g, w.toDouble(), h.toDouble())
        // board on the right
        val bx = 610.0; val by = 70.0; val cs = 52.0
        val grid = listOf("S.....", ".####.", "...#..", "##.#.#", "...#..")
        val bw = grid[0].length * cs; val bh = grid.size * cs
        g.paint = c(0x4B2C7A, 70)
        g.fill(RoundRectangle2D.Double(bx - 30 + 8, by - 30 + 26, bw + 60, bh + 60, 60.0, 60.0))
        g.paint = GradientPaint(0f, (by + bh).toFloat(), c(0x8C4A1F), 0f, (by + bh + 50).toFloat(), c(0x4A2410))
        g.fill(RoundRectangle2D.Double(bx - 30, by - 30 + 22, bw + 60, bh + 60, 60.0, 60.0))
        g.paint = GradientPaint(0f, by.toFloat(), c(0xD3904F), 0f, (by + bh).toFloat(), c(0xA4602A))
        g.fill(RoundRectangle2D.Double(bx - 30, by - 30, bw + 60, bh + 60, 60.0, 60.0))
        val painted = setOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 5 to 1, 5 to 2, 4 to 2, 0 to 1, 0 to 2)
        for ((yy, row) in grid.withIndex()) for ((xx, ch) in row.withIndex()) {
            if (ch == '#') continue
            val x = bx + xx * cs; val y = by + yy * cs
            g.paint = c(0x1B0D05); g.fill(RoundRectangle2D.Double(x - 1, y - 1, cs + 2, cs + 2, 16.0, 16.0))
            g.paint = if ((xx to yy) in painted) c(0x46E0A3) else c(0x2B160A)
            g.fill(RoundRectangle2D.Double(x + 3, y + 3, cs - 6, cs - 6, 14.0, 14.0))
            if ((xx to yy) in painted) { g.paint = c(0xFFFFFF, 90); g.fill(RoundRectangle2D.Double(x + 8, y + 9, cs * 0.5, 7.0, 6.0, 6.0)) }
        }
        // ball at (2,2)
        val tx = g.transform
        g.translate(bx + 2.5 * cs - 62 * 1.6, by + 2.5 * cs - 48 * 1.6)
        g.scale(1.6, 1.6)
        val ball = Ellipse2D.Double(45.0, 31.0, 34.0, 34.0)
        g.paint = RadialGradientPaint(Point2D.Double(56.0, 41.0), 30f, floatArrayOf(0f, 0.45f, 1f), arrayOf(c(0xFFFFFF), c(0xFFE6F2), c(0xE2A9CB)))
        g.fill(ball)
        val clip = g.clip
        g.clip(ball)
        g.paint = c(0xFF4F9A)
        g.fill(java.awt.geom.Rectangle2D.Double(40.0, 43.5, 44.0, 9.5))
        g.clip = clip
        g.paint = c(0xFFFFFF, 230); g.fill(Ellipse2D.Double(51.0, 36.0, 9.0, 5.5))
        g.transform = tx
        // title
        val fontFile = File("app/src/main/res/font/lilita_one.ttf")
        val base = if (fontFile.exists()) Font.createFont(Font.TRUETYPE_FONT, fontFile) else Font("SansSerif", Font.BOLD, 10)
        fun title(text: String, x: Double, y: Double, size: Float, fill: Color) {
            val f = base.deriveFont(size)
            val gv = f.createGlyphVector(g.fontRenderContext, text)
            val shape = gv.getOutline(x.toFloat(), y.toFloat())
            g.composite = AlphaComposite.SrcOver
            g.paint = c(0x3A1F5C, 70)
            val st = g.transform; g.translate(0.0, 7.0); g.fill(shape); g.transform = st
            g.stroke = BasicStroke(size * 0.16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.paint = c(0x3B1A08)
            g.draw(shape)
            g.paint = fill
            g.fill(shape)
        }
        title("MAZE", 60.0, 210.0, 118f, c(0xFFF4E0))
        title("BRUSH", 92.0, 330.0, 132f, c(0xF6B93B))
        g.font = base.deriveFont(34f)
        g.paint = c(0x7A2E6E, 150)
        g.drawString("Swipe. Slide. Paint every tile!", 66, 424)
        g.paint = c(0xFFFFFF)
        g.drawString("Swipe. Slide. Paint every tile!", 64, 420)
        g.dispose()
        return img
    }
}
