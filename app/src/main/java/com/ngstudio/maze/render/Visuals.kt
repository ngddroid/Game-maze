package com.ngstudio.maze.render

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

/** Ball skins (shop). Pattern ids match Shaders.BALL_FS. [metal] 0 = glossy plastic, 1 = mirror metal. */
class BallSkin(
    val id: Int, val name: String, val price: Int,
    val a: Rgb, val b: Rgb, val c: Rgb, val pattern: Int, val gloss: Float = 1f, val metal: Float = 0f,
) {
    companion object {
        private fun k(h: Int) = Rgb(h)
        val ALL = listOf(
            BallSkin(0, "Pearl Pop", 0, k(0xFFF7FB), k(0xFF4F9A), k(0xFFFFFF), 1),
            BallSkin(1, "Lemon Drop", 200, k(0xFFE04D), k(0xFFF5B0), k(0xFFFFFF), 0),
            BallSkin(2, "Mint Swirl", 300, k(0x6EEBC8), k(0xFFFFFF), k(0xFFFFFF), 4),
            BallSkin(3, "Polka Pop", 400, k(0xFF6F91), k(0xFFFFFF), k(0xFFFFFF), 2),
            BallSkin(4, "Beach Day", 500, k(0xFF4D4D), k(0xFFD23F), k(0x2F80ED), 5),
            BallSkin(5, "Ocean Marble", 600, k(0x2F80ED), k(0xC6EBFF), k(0xFFFFFF), 6),
            BallSkin(6, "Twin Tone", 600, k(0x8B5CF6), k(0xFF9F43), k(0xFFFFFF), 3),
            BallSkin(7, "Melon Slice", 700, k(0xFF5C8A), k(0x3DDC84), k(0xFFFFFF), 1),
            BallSkin(8, "Star Bright", 700, k(0x3D5AFE), k(0xFFE45C), k(0xFFFFFF), 9),
            BallSkin(9, "Ringed Planet", 800, k(0xFFB36B), k(0xB86B32), k(0xFFFFFF), 8),
            BallSkin(10, "Chrome", 800, k(0xD9DEE5), k(0xFFFFFF), k(0xFFFFFF), 0, 1.4f, 1f),
            BallSkin(11, "Bubble Glass", 900, k(0xCFEFFF), k(0xFFFFFF), k(0xFFFFFF), 0, 1.6f, 0.3f),
            BallSkin(12, "Galaxy", 1000, k(0x1A1440), k(0x7A63D6), k(0xFFFFFF), 7),
            BallSkin(13, "Neon Rush", 1200, k(0x00E5FF), k(0xFF2BD6), k(0xFFFFFF), 4, 1.3f),
            BallSkin(14, "Lava Core", 1200, k(0xFF4D2E), k(0xFFD23F), k(0xFFFFFF), 6, 1.2f),
            BallSkin(15, "Rainbow Pearl", 1500, k(0xFF9AA2), k(0xA8E6CF), k(0xB5B8FF), 5, 1.3f),
        )

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

        fun byId(id: Int) = ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}
