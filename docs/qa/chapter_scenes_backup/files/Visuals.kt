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

/** Corner decorations of a chapter theme (drawn by ui/components/ThemeDecor.kt). */
enum class Decor { LEAVES, BEACH, SNOW, MEADOW, NIGHT, CANDY, DESERT, SEA, SAKURA, CLOUDS, PROPS }

/**
 * The scene behind every screen (menus and the game): a full-screen picture plus its own corner decorations.
 * It changes every chapter (50 levels), 100 scenes for the 100 chapters (and repeats after level 5000):
 * chapter 1 is the wood with leaves, 2..10 are hand-made (ThemeArt + ThemeDecor), 11..100 are built from
 * the scene kit (SceneArt + ChapterProps). The world ([WorldTheme]) still decides the board, channel
 * and paint colours. Pictures: surface_0.jpg (chapter 1) and backdrop_N.jpg from `:tools:run --args="backdrops"`
 * (tools ThemeArt.THEMES; keep the order in sync).
 */
class Backdrop(
    val index: Int,
    val name: String,
    /** drawable-nodpi image (900 x 1950). */
    val res: Int,
    /** Titles and text drawn straight on the background. */
    val ink: Rgb,
    val decor: Decor,
) {
    companion object {
        private fun c(h: Int) = Rgb(h)

        val ALL: List<Backdrop> = listOf(
            Backdrop(0, "Golden Teak", com.ngstudio.mazebrush.R.drawable.surface_0, c(0x5A2D12), Decor.LEAVES),
            Backdrop(1, "Sunny Beach", com.ngstudio.mazebrush.R.drawable.backdrop_1, c(0x6B3A12), Decor.BEACH),
            Backdrop(2, "Snowy Peaks", com.ngstudio.mazebrush.R.drawable.backdrop_2, c(0x1E3A5F), Decor.SNOW),
            Backdrop(3, "Flower Meadow", com.ngstudio.mazebrush.R.drawable.backdrop_3, c(0x2F5212), Decor.MEADOW),
            Backdrop(4, "Starry Night", com.ngstudio.mazebrush.R.drawable.backdrop_4, c(0xFFF1D6), Decor.NIGHT),
            Backdrop(5, "Candy Land", com.ngstudio.mazebrush.R.drawable.backdrop_5, c(0x8A1F55), Decor.CANDY),
            Backdrop(6, "Desert Dunes", com.ngstudio.mazebrush.R.drawable.backdrop_6, c(0x6A2E0C), Decor.DESERT),
            Backdrop(7, "Under the Sea", com.ngstudio.mazebrush.R.drawable.backdrop_7, c(0x0B3556), Decor.SEA),
            Backdrop(8, "Sakura Garden", com.ngstudio.mazebrush.R.drawable.backdrop_8, c(0x7A2348), Decor.SAKURA),
            Backdrop(9, "Cloud Kingdom", com.ngstudio.mazebrush.R.drawable.backdrop_9, c(0x1F4A78), Decor.CLOUDS),
            // chapters 11..100: SceneArt paintings + ChapterProps.kt props
            Backdrop(10, "Autumn Forest", com.ngstudio.mazebrush.R.drawable.backdrop_10, c(0x5A2208), Decor.PROPS),
            Backdrop(11, "Pumpkin Patch", com.ngstudio.mazebrush.R.drawable.backdrop_11, c(0xFFF1D6), Decor.PROPS),
            Backdrop(12, "Rainy Day", com.ngstudio.mazebrush.R.drawable.backdrop_12, c(0x2A3A4E), Decor.PROPS),
            Backdrop(13, "Rainbow Valley", com.ngstudio.mazebrush.R.drawable.backdrop_13, c(0x1F4A78), Decor.PROPS),
            Backdrop(14, "Mushroom Forest", com.ngstudio.mazebrush.R.drawable.backdrop_14, c(0xE8FFF0), Decor.PROPS),
            Backdrop(15, "Bamboo Grove", com.ngstudio.mazebrush.R.drawable.backdrop_15, c(0x2E5A1E), Decor.PROPS),
            Backdrop(16, "Lavender Fields", com.ngstudio.mazebrush.R.drawable.backdrop_16, c(0x5A2E7A), Decor.PROPS),
            Backdrop(17, "Sunflower Farm", com.ngstudio.mazebrush.R.drawable.backdrop_17, c(0x1F4A78), Decor.PROPS),
            Backdrop(18, "Pine Woods", com.ngstudio.mazebrush.R.drawable.backdrop_18, c(0x1E4A38), Decor.PROPS),
            Backdrop(19, "Lily Pond", com.ngstudio.mazebrush.R.drawable.backdrop_19, c(0x0E3328), Decor.PROPS),
            Backdrop(20, "Northern Lights", com.ngstudio.mazebrush.R.drawable.backdrop_20, c(0xF0F8FF), Decor.PROPS),
            Backdrop(21, "Ice Cave", com.ngstudio.mazebrush.R.drawable.backdrop_21, c(0x0E3A6A), Decor.PROPS),
            Backdrop(22, "Polar Bay", com.ngstudio.mazebrush.R.drawable.backdrop_22, c(0x1E3A5F), Decor.PROPS),
            Backdrop(23, "Christmas Eve", com.ngstudio.mazebrush.R.drawable.backdrop_23, c(0xFFF1D6), Decor.PROPS),
            Backdrop(24, "Holiday Lights", com.ngstudio.mazebrush.R.drawable.backdrop_24, c(0xFFF1D6), Decor.PROPS),
            Backdrop(25, "Spring Garden", com.ngstudio.mazebrush.R.drawable.backdrop_25, c(0x2F5212), Decor.PROPS),
            Backdrop(26, "Cherry Orchard", com.ngstudio.mazebrush.R.drawable.backdrop_26, c(0x7A2348), Decor.PROPS),
            Backdrop(27, "Citrus Grove", com.ngstudio.mazebrush.R.drawable.backdrop_27, c(0x8A4A00), Decor.PROPS),
            Backdrop(28, "Berry Patch", com.ngstudio.mazebrush.R.drawable.backdrop_28, c(0x8A1F3A), Decor.PROPS),
            Backdrop(29, "Honey Hive", com.ngstudio.mazebrush.R.drawable.backdrop_29, c(0x6A3A00), Decor.PROPS),
            Backdrop(30, "Sunset Sky", com.ngstudio.mazebrush.R.drawable.backdrop_30, c(0xFFF1D6), Decor.PROPS),
            Backdrop(31, "Golden Hills", com.ngstudio.mazebrush.R.drawable.backdrop_31, c(0x6A3A08), Decor.PROPS),
            Backdrop(32, "Moon Base", com.ngstudio.mazebrush.R.drawable.backdrop_32, c(0xF0F4FF), Decor.PROPS),
            Backdrop(33, "Galaxy Swirl", com.ngstudio.mazebrush.R.drawable.backdrop_33, c(0xF6E8FF), Decor.PROPS),
            Backdrop(34, "Rocket Launch", com.ngstudio.mazebrush.R.drawable.backdrop_34, c(0xF0F4FF), Decor.PROPS),
            Backdrop(35, "Alien Planet", com.ngstudio.mazebrush.R.drawable.backdrop_35, c(0xF0FFF8), Decor.PROPS),
            Backdrop(36, "Meteor Shower", com.ngstudio.mazebrush.R.drawable.backdrop_36, c(0xF0F4FF), Decor.PROPS),
            Backdrop(37, "Kite Festival", com.ngstudio.mazebrush.R.drawable.backdrop_37, c(0x1F4A78), Decor.PROPS),
            Backdrop(38, "Balloon Fiesta", com.ngstudio.mazebrush.R.drawable.backdrop_38, c(0x6A3A1E), Decor.PROPS),
            Backdrop(39, "Paper Planes", com.ngstudio.mazebrush.R.drawable.backdrop_39, c(0x2A4A7A), Decor.PROPS),
            Backdrop(40, "Pastel Clouds", com.ngstudio.mazebrush.R.drawable.backdrop_40, c(0x7A3A7A), Decor.PROPS),
            Backdrop(41, "Thunder Storm", com.ngstudio.mazebrush.R.drawable.backdrop_41, c(0xF0F4FF), Decor.PROPS),
            Backdrop(42, "Windmill Hills", com.ngstudio.mazebrush.R.drawable.backdrop_42, c(0x1F4A78), Decor.PROPS),
            Backdrop(43, "Starlight Lake", com.ngstudio.mazebrush.R.drawable.backdrop_43, c(0xF0F4FF), Decor.PROPS),
            Backdrop(44, "Comet Trail", com.ngstudio.mazebrush.R.drawable.backdrop_44, c(0xF0F4FF), Decor.PROPS),
            Backdrop(45, "Coral Reef", com.ngstudio.mazebrush.R.drawable.backdrop_45, c(0x0B3556), Decor.PROPS),
            Backdrop(46, "Deep Sea", com.ngstudio.mazebrush.R.drawable.backdrop_46, c(0xE6F8FF), Decor.PROPS),
            Backdrop(47, "Pirate Cove", com.ngstudio.mazebrush.R.drawable.backdrop_47, c(0x0B3556), Decor.PROPS),
            Backdrop(48, "Lighthouse Coast", com.ngstudio.mazebrush.R.drawable.backdrop_48, c(0x6A2E1A), Decor.PROPS),
            Backdrop(49, "Sailing Bay", com.ngstudio.mazebrush.R.drawable.backdrop_49, c(0xFFF1D6), Decor.PROPS),
            Backdrop(50, "Kelp Forest", com.ngstudio.mazebrush.R.drawable.backdrop_50, c(0x0E3A2E), Decor.PROPS),
            Backdrop(51, "Jellyfish Lagoon", com.ngstudio.mazebrush.R.drawable.backdrop_51, c(0xF6E8FF), Decor.PROPS),
            Backdrop(52, "Ice Rink", com.ngstudio.mazebrush.R.drawable.backdrop_52, c(0x1E3A5F), Decor.PROPS),
            Backdrop(53, "Pearl Shore", com.ngstudio.mazebrush.R.drawable.backdrop_53, c(0x6B3A12), Decor.PROPS),
            Backdrop(54, "Tropical Lagoon", com.ngstudio.mazebrush.R.drawable.backdrop_54, c(0x6B3A12), Decor.PROPS),
            Backdrop(55, "River Rapids", com.ngstudio.mazebrush.R.drawable.backdrop_55, c(0x1F4A78), Decor.PROPS),
            Backdrop(56, "Red Canyon", com.ngstudio.mazebrush.R.drawable.backdrop_56, c(0x6A1E0A), Decor.PROPS),
            Backdrop(57, "Savanna", com.ngstudio.mazebrush.R.drawable.backdrop_57, c(0x5A2208), Decor.PROPS),
            Backdrop(58, "Pyramid Sands", com.ngstudio.mazebrush.R.drawable.backdrop_58, c(0x6A2E0C), Decor.PROPS),
            Backdrop(59, "Desert Oasis", com.ngstudio.mazebrush.R.drawable.backdrop_59, c(0x6A2E0C), Decor.PROPS),
            Backdrop(60, "Volcano Island", com.ngstudio.mazebrush.R.drawable.backdrop_60, c(0xFFE6CC), Decor.PROPS),
            Backdrop(61, "Lava Lake", com.ngstudio.mazebrush.R.drawable.backdrop_61, c(0xFFE6CC), Decor.PROPS),
            Backdrop(62, "Crystal Cavern", com.ngstudio.mazebrush.R.drawable.backdrop_62, c(0xF6E8FF), Decor.PROPS),
            Backdrop(63, "Gold Mine", com.ngstudio.mazebrush.R.drawable.backdrop_63, c(0xFFE8B0), Decor.PROPS),
            Backdrop(64, "Dino Valley", com.ngstudio.mazebrush.R.drawable.backdrop_64, c(0x1E4A28), Decor.PROPS),
            Backdrop(65, "Rocky Mountains", com.ngstudio.mazebrush.R.drawable.backdrop_65, c(0x1E3A5F), Decor.PROPS),
            Backdrop(66, "Stone Garden", com.ngstudio.mazebrush.R.drawable.backdrop_66, c(0x3A3A2A), Decor.PROPS),
            Backdrop(67, "Ice Cream Parlor", com.ngstudio.mazebrush.R.drawable.backdrop_67, c(0x8A1F55), Decor.PROPS),
            Backdrop(68, "Cupcake Bakery", com.ngstudio.mazebrush.R.drawable.backdrop_68, c(0x8A1F55), Decor.PROPS),
            Backdrop(69, "Donut Shop", com.ngstudio.mazebrush.R.drawable.backdrop_69, c(0x6A2E0C), Decor.PROPS),
            Backdrop(70, "Chocolate Factory", com.ngstudio.mazebrush.R.drawable.backdrop_70, c(0xFFE8CC), Decor.PROPS),
            Backdrop(71, "Bubblegum Pop", com.ngstudio.mazebrush.R.drawable.backdrop_71, c(0x8A1F55), Decor.PROPS),
            Backdrop(72, "Lemonade Stand", com.ngstudio.mazebrush.R.drawable.backdrop_72, c(0x7A4A00), Decor.PROPS),
            Backdrop(73, "Cookie Jar", com.ngstudio.mazebrush.R.drawable.backdrop_73, c(0x5A2E14), Decor.PROPS),
            Backdrop(74, "Watermelon Summer", com.ngstudio.mazebrush.R.drawable.backdrop_74, c(0x4A0E16), Decor.PROPS),
            Backdrop(75, "Jelly Beans", com.ngstudio.mazebrush.R.drawable.backdrop_75, c(0x8A1F55), Decor.PROPS),
            Backdrop(76, "Candy Stripes", com.ngstudio.mazebrush.R.drawable.backdrop_76, c(0x8A1F55), Decor.PROPS),
            Backdrop(77, "Marshmallow Clouds", com.ngstudio.mazebrush.R.drawable.backdrop_77, c(0x7A3A7A), Decor.PROPS),
            Backdrop(78, "Toy Box", com.ngstudio.mazebrush.R.drawable.backdrop_78, c(0x1A3A6A), Decor.PROPS),
            Backdrop(79, "Circus Tent", com.ngstudio.mazebrush.R.drawable.backdrop_79, c(0x7A1010), Decor.PROPS),
            Backdrop(80, "Carnival Night", com.ngstudio.mazebrush.R.drawable.backdrop_80, c(0xFFF1D6), Decor.PROPS),
            Backdrop(81, "Music Stage", com.ngstudio.mazebrush.R.drawable.backdrop_81, c(0xFFF1D6), Decor.PROPS),
            Backdrop(82, "Art Studio", com.ngstudio.mazebrush.R.drawable.backdrop_82, c(0x3A2A4A), Decor.PROPS),
            Backdrop(83, "Library", com.ngstudio.mazebrush.R.drawable.backdrop_83, c(0xFFE8C8), Decor.PROPS),
            Backdrop(84, "Chess Hall", com.ngstudio.mazebrush.R.drawable.backdrop_84, c(0x4A2A10), Decor.PROPS),
            Backdrop(85, "Castle Kingdom", com.ngstudio.mazebrush.R.drawable.backdrop_85, c(0x1F4A78), Decor.PROPS),
            Backdrop(86, "Fairy Garden", com.ngstudio.mazebrush.R.drawable.backdrop_86, c(0x6A3A8A), Decor.PROPS),
            Backdrop(87, "Lantern Festival", com.ngstudio.mazebrush.R.drawable.backdrop_87, c(0xFFF1D6), Decor.PROPS),
            Backdrop(88, "Halloween Night", com.ngstudio.mazebrush.R.drawable.backdrop_88, c(0xFFE6C8), Decor.PROPS),
            Backdrop(89, "Spooky Swamp", com.ngstudio.mazebrush.R.drawable.backdrop_89, c(0xE8FFE8), Decor.PROPS),
            Backdrop(90, "Robot Lab", com.ngstudio.mazebrush.R.drawable.backdrop_90, c(0xE0FAFF), Decor.PROPS),
            Backdrop(91, "Neon City", com.ngstudio.mazebrush.R.drawable.backdrop_91, c(0xFFE6FF), Decor.PROPS),
            Backdrop(92, "Retro Arcade", com.ngstudio.mazebrush.R.drawable.backdrop_92, c(0xFFF1D6), Decor.PROPS),
            Backdrop(93, "Sports Field", com.ngstudio.mazebrush.R.drawable.backdrop_93, c(0xFFFFFF), Decor.PROPS),
            Backdrop(94, "Picnic Park", com.ngstudio.mazebrush.R.drawable.backdrop_94, c(0x7A1010), Decor.PROPS),
            Backdrop(95, "Farm Fields", com.ngstudio.mazebrush.R.drawable.backdrop_95, c(0x1F4A78), Decor.PROPS),
            Backdrop(96, "Fireworks Night", com.ngstudio.mazebrush.R.drawable.backdrop_96, c(0xFFF1D6), Decor.PROPS),
            Backdrop(97, "Treasure Vault", com.ngstudio.mazebrush.R.drawable.backdrop_97, c(0xFFE8B0), Decor.PROPS),
            Backdrop(98, "Origami Paper", com.ngstudio.mazebrush.R.drawable.backdrop_98, c(0x4A3A6A), Decor.PROPS),
            Backdrop(99, "Grand Finale", com.ngstudio.mazebrush.R.drawable.backdrop_99, c(0x7A3A00), Decor.PROPS),
        )

        fun forChapter(chapter: Int): Backdrop = ALL[chapter.mod(ALL.size)]
        fun forLevel(level: Int): Backdrop = forChapter(WorldTheme.chapterOf(level.coerceAtLeast(1)))
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
