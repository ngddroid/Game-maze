package com.ngstudio.mazebrush.render

/** RGB colour from 0xRRGGBB, as floats 0..1. */
class Rgb(hex: Int) {
    val r = ((hex shr 16) and 0xFF) / 255f
    val g = ((hex shr 8) and 0xFF) / 255f
    val b = (hex and 0xFF) / 255f
    val argb: Int = (0xFF shl 24) or hex
    val hex: Int = hex
}

/** Surface material ids, shared by Shaders.MATERIAL and tools/MaterialArt. */
object Material {
    const val WOOD = 0
    const val MARBLE = 1
    const val CLAY = 2
    const val ICE = 3
    const val SAND = 4
    const val STONE = 5
    /** Varnished planks laid horizontally (world 1). */
    const val PLANKS = 6
    /** Teak/walnut veneer, grain top to bottom (old world 1, unused). */
    const val VENEER = 7
    /** Honey oak, straight grain top to bottom with cathedral flames (unused). */
    const val OAK = 8
    /** Pale golden maple with wide wavy grain (world 1). */
    const val MAPLE = 9
}

/**
 * One visual world: the whole screen is a surface material and the maze is carved into it.
 * Worlds change every 5 chapters (250 levels) and cycle after level 2500.
 */
class WorldTheme(
    val index: Int,
    val name: String,
    val material: Int,
    /** Surface colours: light, dark, accent (grain streak / vein / crack). */
    val matA: Rgb, val matB: Rgb, val matC: Rgb,
    /** Carved channel floor: centre and edge. */
    val floorA: Rgb, val floorB: Rgb,
    val paints: List<Rgb>,
    /** UI accent colours for buttons in this world. */
    val accent: Rgb, val accentDark: Rgb,
    /** Text colour engraved on the surface. */
    val ink: Rgb,
    /** Neon-style paint that glows. */
    val glow: Boolean = false,
) {
    fun paintFor(level: Int): Rgb = paints[(level - 1).mod(paints.size)]

    companion object {
        private fun c(h: Int) = Rgb(h)

        val ALL: List<WorldTheme> = listOf(
            WorldTheme(0, "Golden Teak", Material.MAPLE, c(0xF4D3A3), c(0xE5B67C), c(0xB97B42), c(0x2B160A), c(0x1B0D05),
                listOf(c(0xFFE14D), c(0x46E0A3), c(0x5CC8FF), c(0xFF7FA8), c(0xB48CFF)), c(0x43B25D), c(0x2C7F40), c(0x5A2D12)),
            WorldTheme(1, "Marble Hall", Material.MARBLE, c(0xF5F3EF), c(0xE2DDD5), c(0xA39C93), c(0x3E4652), c(0x2B323B),
                listOf(c(0xFF5C8A), c(0xFFC93C), c(0x2EC4B6), c(0x4D7CFF), c(0xFF8A3D)), c(0x4D7CFF), c(0x2F55C4), c(0x3E4652)),
            WorldTheme(2, "Pastel Clay", Material.CLAY, c(0xF9CBDB), c(0xEDAFC6), c(0xE296B4), c(0x5B3C8C), c(0x43286D),
                listOf(c(0xFFE14D), c(0x5CF2C5), c(0x4FD8FF), c(0xFF8A5C), c(0xFFFFFF)), c(0xFF4F9A), c(0xC2186F), c(0x6A2E5A)),
            WorldTheme(3, "Walnut Study", Material.WOOD, c(0x8E5E3D), c(0x6B4128), c(0x4A2A18), c(0x1E2A38), c(0x131B25),
                listOf(c(0xFFD23F), c(0xFF6B6B), c(0x4ECDC4), c(0xA78BFA), c(0xF9F7F3)), c(0xF59E0B), c(0xB45309), c(0xF7E7D4)),
            WorldTheme(4, "Frost Ice", Material.ICE, c(0xE8F6FF), c(0xB9DCF5), c(0x8FBFE3), c(0x23486F), c(0x163253),
                listOf(c(0xFF5C8A), c(0xFFC93C), c(0x6CF59B), c(0xB98CFF), c(0xFF8A3D)), c(0x3B82F6), c(0x1E4FB8), c(0x1E3A5F)),
            WorldTheme(5, "Zen Sand", Material.SAND, c(0xF0DCB6), c(0xDCC091), c(0xC7A36B), c(0x3B3F3A), c(0x292C28),
                listOf(c(0xFF6B6B), c(0x4ECDC4), c(0xFFD166), c(0x8E7DFF), c(0xF4A261)), c(0x2A9D8F), c(0x1B6E64), c(0x5A4630)),
            WorldTheme(6, "Jade Stone", Material.STONE, c(0x86CBAE), c(0x5FAE8D), c(0x3E8C6C), c(0x1F3D33), c(0x142A23),
                listOf(c(0xFFE14D), c(0xFF7FA8), c(0xFFFFFF), c(0xFF9F43), c(0x9EC5FF)), c(0x10B981), c(0x047857), c(0x0F3B2D)),
            WorldTheme(7, "Slate Night", Material.STONE, c(0x80889A), c(0x636B7D), c(0x474E5E), c(0x151A2B), c(0x0C0F1A),
                listOf(c(0xFF2BD6), c(0x00F0FF), c(0xB6FF00), c(0xFFE600), c(0xFF7A00)), c(0x8B5CF6), c(0x5B21B6), c(0xE5E7EB), glow = true),
            WorldTheme(8, "Cherry Wood", Material.WOOD, c(0xB8644A), c(0x934A33), c(0x6F3222), c(0x2A1A16), c(0x1B100D),
                listOf(c(0xFFE14D), c(0x5CF2C5), c(0x7CC8FF), c(0xFFFFFF), c(0xFFB86B)), c(0xE11D48), c(0x9F1239), c(0xFBE7DC)),
            WorldTheme(9, "Sandstone", Material.STONE, c(0xE9BC8E), c(0xD49F6D), c(0xB8814F), c(0x4A2F55), c(0x34203D),
                listOf(c(0xFFE14D), c(0x5CF2C5), c(0x4FD8FF), c(0xFF8FB1), c(0xFFFFFF)), c(0xD97706), c(0x92400E), c(0x5A3A1E)),
        )

        const val LEVELS_PER_CHAPTER = 50
        const val CHAPTERS_PER_WORLD = 5

        fun chapterOf(level: Int) = (level - 1) / LEVELS_PER_CHAPTER
        fun worldIndexOf(level: Int) = (chapterOf(level) / CHAPTERS_PER_WORLD) % ALL.size
        fun forLevel(level: Int): WorldTheme = ALL[worldIndexOf(level)]
    }
}

/**
 * The clean light background behind every screen (2026-11-25 redesign, replacing the chapter scenes).
 * Compose ([com.ngstudio.mazebrush.ui.components.SurfaceBackground]) and GL (Shaders.BG_FS) draw the same
 * vertical gradient. [ink] colours titles drawn straight on it.
 */
class Backdrop(val ink: Rgb, val top: Rgb, val bottom: Rgb) {
    companion object {
        val CLEAN = Backdrop(Rgb(0x263A46), Rgb(0xFFF6EC), Rgb(0xF5E5D5))
        val ALL = listOf(CLEAN)
    }
}

/**
 * Board colour theme (shop Maze tab): the whole board changes, not just its floor. [sideTop] is the slab and its
 * walls, [sideBottom] the slab's front lip, [floor] the carved channels, and [pattern] an optional surface
 * (0 plain, 1 wood grain, 2 terrazzo speckles, 3 marble veins; drawn by Shaders.BAKE_FS). [paints] are chosen to
 * stand out on both the floor and the walls. Bought with coins or unlocked by the New Item bar.
 */
class BoardTheme(
    val id: Int, val name: String, val price: Int,
    /** Unpainted floor of the carved channels. */
    val floor: Rgb,
    /** The board slab's top (walls) and its front lip. */
    val sideTop: Rgb, val sideBottom: Rgb,
    val paints: List<Rgb>,
    val pattern: Int = 0,
) {
    fun paintFor(level: Int): Rgb = paints[(level - 1).mod(paints.size)]

    companion object {
        private fun c(h: Int) = Rgb(h)
        const val PLAIN = 0
        const val WOOD = 1
        const val TERRAZZO = 2
        const val MARBLE = 3
        val ALL = listOf(
            BoardTheme(0, "Classic", 0, c(0x34495A), c(0xFFFBF5), c(0xE6D4C0),
                listOf(c(0xFF6B5B), c(0x2EC4B6), c(0xFFC93C), c(0x4FA8FF), c(0xFF7FB2))),
            // the Classic look in other colours: light walls, a dark coloured channel
            BoardTheme(12, "Navy", 200, c(0x1F2F5C), c(0xF8F9FF), c(0xD3D9EE),
                listOf(c(0xFFC93C), c(0xFF6B5B), c(0x5CE0B8), c(0xFF8FC8))),
            BoardTheme(13, "Pine", 200, c(0x1F4A3A), c(0xF7FBF4), c(0xCFE0C6),
                listOf(c(0xFFB347), c(0xFF6B8A), c(0xFFE45C), c(0x8AD8FF))),
            BoardTheme(14, "Plum", 250, c(0x4A2450), c(0xFFF8FC), c(0xE6CCE0),
                listOf(c(0x7CF2D0), c(0xFFE14D), c(0x8AD8FF), c(0xFFA06A))),
            BoardTheme(15, "Cocoa", 250, c(0x4A2E22), c(0xFFF8EF), c(0xE8D3B8),
                listOf(c(0x7EE0C8), c(0xFF8AB8), c(0xFFD23F), c(0x8AC8FF))),
            BoardTheme(16, "Charcoal", 300, c(0x2C2C31), c(0xFAFAFA), c(0xD8D8DD),
                listOf(c(0xFFD23F), c(0xFF5C5C), c(0x5CE0B8), c(0x5CA8FF))),
            BoardTheme(17, "Deep Teal", 300, c(0x0F4B55), c(0xF3FBFB), c(0xC6E2E2),
                listOf(c(0xFFC93C), c(0xFF7A6B), c(0xFF8FC8), c(0xFFFFFF))),
            BoardTheme(18, "Wine", 350, c(0x5A1A2A), c(0xFFF6F4), c(0xECCFCB),
                listOf(c(0xFFE45C), c(0x6CF2C8), c(0x8AD8FF), c(0xFFFFFF))),
            BoardTheme(19, "Indigo", 350, c(0x2E2A6B), c(0xF7F6FF), c(0xD6D3F0),
                listOf(c(0xFFC93C), c(0x6CF2B0), c(0xFF8A8A), c(0x8AE0FF))),
            BoardTheme(1, "Ocean", 250, c(0x123F66), c(0x4AB3E8), c(0x2A86BE),
                listOf(c(0xFFD23F), c(0xFF7A6B), c(0xFFFFFF), c(0xFF8FC8))),
            BoardTheme(2, "Mint", 300, c(0x0E5E50), c(0x63D9B8), c(0x36AB8C),
                listOf(c(0xFFD23F), c(0xFF6B8A), c(0xFFFFFF), c(0xFF9442))),
            BoardTheme(3, "Walnut", 350, c(0x3E220F), c(0xB8723C), c(0x7A4420),
                listOf(c(0x7EF0D0), c(0xFFE14D), c(0x8AD8FF), c(0xFF9AC8)), WOOD),
            BoardTheme(4, "Bubblegum", 400, c(0x8E2A63), c(0xFF9BCB), c(0xDB6AA3),
                listOf(c(0xFFE45C), c(0x6CF2C8), c(0xFFFFFF), c(0x8AD8FF))),
            BoardTheme(5, "Galaxy", 500, c(0x170E45), c(0x6A4BE0), c(0x4128A8),
                listOf(c(0xFF2BD6), c(0x00F0FF), c(0xB6FF00), c(0xFFE600))),
            BoardTheme(6, "Terrazzo", 600, c(0x47423D), c(0xF3EEE6), c(0xCDC3B5),
                listOf(c(0xFF6B5B), c(0x2EC4B6), c(0xFFC93C), c(0x4FA8FF)), TERRAZZO),
            BoardTheme(7, "Sunset", 700, c(0x7A2E12), c(0xFFAA4A), c(0xD97B22),
                listOf(c(0xFFFFFF), c(0x6CF2C8), c(0x8AD8FF), c(0xFFE45C))),
            BoardTheme(8, "Lime", 800, c(0x264D16), c(0xA8DE5C), c(0x76AE36),
                listOf(c(0xFF6BA8), c(0xFFFFFF), c(0xFFC93C), c(0x5CC8FF))),
            BoardTheme(9, "Marble", 900, c(0x2B3440), c(0xF4F4F6), c(0xC9CDD4),
                listOf(c(0xFFC93C), c(0xFF6B8A), c(0x5CE0B8), c(0x5CA8FF)), MARBLE),
            BoardTheme(10, "Cherry", 1000, c(0x5E1119), c(0xF0545F), c(0xB5303B),
                listOf(c(0xFFE45C), c(0xFFFFFF), c(0x6CF2C8), c(0x8AD8FF))),
            BoardTheme(11, "Frost", 1200, c(0x1F5A86), c(0xD4F0FF), c(0x92C8E8),
                listOf(c(0xFF6BA8), c(0xFFC93C), c(0xFF7A3D), c(0xB98CFF))),
        )

        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[0]

        /** Levels the board stays Classic before the colours start changing. */
        const val PLAIN_LEVELS = 5

        /**
         * The board is not chosen by the player: levels 1-5 are Classic, then every level takes the next theme in
         * [ALL] order, and after the last one the list starts again from the first.
         */
        fun forLevel(level: Int): BoardTheme = if (level <= PLAIN_LEVELS) ALL[0] else ALL[(level - PLAIN_LEVELS) % ALL.size]

        /** The Daily Challenge takes a theme by date. */
        fun forDay(day: Long): BoardTheme = ALL[(day % ALL.size).toInt().let { if (it < 0) it + ALL.size else it }]
    }
}

/**
 * Ball skins (shop Ball tab). Each ball is a painted texture, assets/balls/tex_<id>.png, wrapped round the 3D ball by
 * Shaders.BALL_FS, with a lit picture assets/balls/icon_<id>.png for the menus. Both are made by
 * `:tools:run --args="balls"` (tools/BallArt.kt, same ids, names and materials). [a] and [b] colour the comet streak
 * and particles. [gloss] scales reflections, [metal] makes it a mirror, [glass] makes it see-through at the centre.
 */
class BallSkin(
    val id: Int, val name: String, val price: Int,
    val a: Rgb, val b: Rgb, val c: Rgb = Rgb(0xFFFFFF),
    val gloss: Float = 1f, val metal: Float = 0f, val glass: Float = 0f,
) {
    companion object {
        private fun k(h: Int) = Rgb(h)
        val ALL = listOf(
            BallSkin(0, "Beach Ball", 0, k(0xE53935), k(0x1E88E5), gloss = 1.1f),
            BallSkin(1, "Basketball", 150, k(0xE8742A), k(0x1A1210), gloss = 0.35f),
            BallSkin(2, "Soccer", 150, k(0xF6F6F2), k(0x1C1C22), gloss = 0.7f),
            BallSkin(3, "Tennis", 200, k(0xD4E63A), k(0xF4F6EC), gloss = 0.15f),
            BallSkin(4, "Baseball", 200, k(0xF6F1E6), k(0xD3262E), gloss = 0.45f),
            BallSkin(5, "Volleyball", 250, k(0x2F6BD8), k(0xFFD23F), gloss = 0.5f),
            BallSkin(6, "Cricket", 250, k(0xB3141E), k(0xF4EFE4), gloss = 0.9f),
            BallSkin(7, "Golf Ball", 300, k(0xF4F4F0), k(0xBDBDBD), gloss = 0.6f),
            BallSkin(8, "Rainbow Swirl", 300, k(0xF44336), k(0x2196F3)),
            BallSkin(9, "Rainbow Stripes", 350, k(0xFDD835), k(0x1E88E5)),
            BallSkin(10, "Watermelon", 350, k(0x1F6B2A), k(0x76C450), gloss = 0.8f),
            BallSkin(11, "8-Ball", 400, k(0x2A2A33), k(0xF7F7F2), gloss = 1.6f),
            BallSkin(12, "Bowling Ball", 400, k(0xFF7A1F), k(0xC4380E), gloss = 1.5f),
            BallSkin(13, "Earth", 500, k(0x2A86E8), k(0x4FAE3C), gloss = 0.9f),
            BallSkin(14, "Winky", 500, k(0xFFD43A), k(0xE8484F)),
            BallSkin(15, "Candy Soccer", 550, k(0xFF6FB5), k(0x29B6F6), gloss = 0.9f),
            BallSkin(16, "Glass Marble", 600, k(0x9CD8F2), k(0xFF8A2A), gloss = 1.6f, glass = 0.35f),
            BallSkin(17, "Bubble", 650, k(0xD9CCFF), k(0x8AE0FF), gloss = 1.8f, glass = 0.75f),
            BallSkin(18, "Glitter", 700, k(0xE0399B), k(0xFFFFFF), gloss = 1.3f),
            BallSkin(19, "Ice", 800, k(0x7CCDF0), k(0xFFFFFF), gloss = 1.6f, glass = 0.35f),
            BallSkin(20, "Disco", 900, k(0xC9D6FF), k(0xF4C8FF), gloss = 1.3f, metal = 0.85f),
            BallSkin(21, "Gold", 1000, k(0xF0BE45), k(0xFFF0B0), gloss = 1.2f, metal = 1f),
            BallSkin(22, "Lava", 1200, k(0xFF6A10), k(0xFFE070), gloss = 0.6f),
            BallSkin(23, "Galaxy", 1500, k(0x6A3BD0), k(0xE05AB8), gloss = 1.3f),
        )

        const val DEFAULT = 1
        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}

/** Trail effects (shop). */
class TrailStyle(val id: Int, val name: String, val price: Int) {
    companion object {
        val ALL = listOf(
            TrailStyle(0, "None", 0),
            TrailStyle(1, "Sparkle", 0),
            TrailStyle(2, "Bubbles", 300),
            TrailStyle(3, "Comet", 400),
            TrailStyle(4, "Confetti", 500),
            TrailStyle(5, "Rainbow", 700),
            TrailStyle(6, "Paint Drops", 800),
            TrailStyle(7, "Stardust", 1000),
            TrailStyle(8, "Dots", 600),
            TrailStyle(9, "Hearts", 900),
            TrailStyle(10, "Stars", 900),
            TrailStyle(11, "Smoke", 700),
            TrailStyle(12, "Fire", 1100),
            TrailStyle(13, "Ice", 1100),
            TrailStyle(14, "Electric", 1200),
            TrailStyle(15, "Leaves", 800),
            TrailStyle(16, "Flowers", 900),
            TrailStyle(17, "Music", 1000),
            TrailStyle(18, "Crystal", 1200),
            TrailStyle(19, "Neon", 1300),
            TrailStyle(20, "Galaxy", 1400),
            TrailStyle(21, "Snow", 1000),
            TrailStyle(22, "Petals", 1100),
            TrailStyle(23, "Aurora", 1500),
        )

        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[1]
    }
}

/** Paint finishes (shop). Ids match Shaders.PAINT_FS uFinish. */
class PaintFinish(val id: Int, val name: String, val price: Int) {
    companion object {
        val ALL = listOf(
            PaintFinish(0, "Classic", 0),
            PaintFinish(1, "Pastel", 250),
            PaintFinish(2, "Neon Glow", 500),
            PaintFinish(3, "Metallic", 650),
            PaintFinish(4, "Rainbow", 900),
            PaintFinish(5, "Gradient", 1000),
        )

        const val DEFAULT = 1
        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}

/**
 * Paint colours (shop Paint tab): the player's paint is always the one colour they picked, on every level and maze
 * theme (there is no automatic colour; "Auto", id 0, was removed on 2026-11-25 at the user's request). Coral (id 1)
 * is the default and free; the others unlock for free by finishing [levels] levels.
 */
class PaintColor(val id: Int, val name: String, val rgb: Rgb, val levels: Int) {
    /** The paint colour (the same on every board and level). */
    @Suppress("UNUSED_PARAMETER")
    fun first(board: BoardTheme, level: Int): Rgb = rgb
    /** The second colour (the Gradient finish blends into it). */
    fun second(board: BoardTheme, level: Int): Rgb = board.paintFor(level + 2)

    companion object {
        private fun c(h: Int) = Rgb(h)
        val ALL = listOf(
            PaintColor(1, "Coral", c(0xFF6B5B), 0),
            PaintColor(2, "Teal", c(0x2EC4B6), 5),
            PaintColor(3, "Sunny", c(0xFFC93C), 10),
            PaintColor(4, "Sky", c(0x4FA8FF), 20),
            PaintColor(5, "Bubblegum", c(0xFF7FB2), 30),
            PaintColor(6, "Lime", c(0x8BD43A), 45),
            PaintColor(7, "Lavender", c(0xA98BFF), 60),
            PaintColor(8, "Tangerine", c(0xFF9442), 80),
            PaintColor(9, "Mint", c(0x5CE0B8), 100),
            PaintColor(10, "Cherry", c(0xE8364F), 125),
            PaintColor(11, "Royal Blue", c(0x2F6BFF), 150),
        )

        const val DEFAULT = 1
        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}
