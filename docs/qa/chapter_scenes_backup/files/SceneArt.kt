package com.ngstudio.maze.tools

import com.ngstudio.maze.tools.SceneKit.aurora
import com.ngstudio.maze.tools.SceneKit.bamboo
import com.ngstudio.maze.tools.SceneKit.bokeh
import com.ngstudio.maze.tools.SceneKit.books
import com.ngstudio.maze.tools.SceneKit.bricks
import com.ngstudio.maze.tools.SceneKit.bubbles
import com.ngstudio.maze.tools.SceneKit.castle
import com.ngstudio.maze.tools.SceneKit.caustics
import com.ngstudio.maze.tools.SceneKit.checker
import com.ngstudio.maze.tools.SceneKit.chocoWaves
import com.ngstudio.maze.tools.SceneKit.clouds
import com.ngstudio.maze.tools.SceneKit.coins
import com.ngstudio.maze.tools.SceneKit.confetti
import com.ngstudio.maze.tools.SceneKit.craters
import com.ngstudio.maze.tools.SceneKit.crystals
import com.ngstudio.maze.tools.SceneKit.donuts
import com.ngstudio.maze.tools.SceneKit.dots
import com.ngstudio.maze.tools.SceneKit.dunes
import com.ngstudio.maze.tools.SceneKit.fields
import com.ngstudio.maze.tools.SceneKit.flat
import com.ngstudio.maze.tools.SceneKit.floes
import com.ngstudio.maze.tools.SceneKit.gingham
import com.ngstudio.maze.tools.SceneKit.glow
import com.ngstudio.maze.tools.SceneKit.grad
import com.ngstudio.maze.tools.SceneKit.grain
import com.ngstudio.maze.tools.SceneKit.grass
import com.ngstudio.maze.tools.SceneKit.ground
import com.ngstudio.maze.tools.SceneKit.hexes
import com.ngstudio.maze.tools.SceneKit.hills
import com.ngstudio.maze.tools.SceneKit.kelp
import com.ngstudio.maze.tools.SceneKit.lavaCracks
import com.ngstudio.maze.tools.SceneKit.leaves
import com.ngstudio.maze.tools.SceneKit.lightning
import com.ngstudio.maze.tools.SceneKit.lilyPads
import com.ngstudio.maze.tools.SceneKit.mesas
import com.ngstudio.maze.tools.SceneKit.meteors
import com.ngstudio.maze.tools.SceneKit.mottle
import com.ngstudio.maze.tools.SceneKit.mountains
import com.ngstudio.maze.tools.SceneKit.mowed
import com.ngstudio.maze.tools.SceneKit.nebula
import com.ngstudio.maze.tools.SceneKit.paperFolds
import com.ngstudio.maze.tools.SceneKit.petals
import com.ngstudio.maze.tools.SceneKit.pool
import com.ngstudio.maze.tools.SceneKit.pyramids
import com.ngstudio.maze.tools.SceneKit.radialStripes
import com.ngstudio.maze.tools.SceneKit.rain
import com.ngstudio.maze.tools.SceneKit.rainbow
import com.ngstudio.maze.tools.SceneKit.rays
import com.ngstudio.maze.tools.SceneKit.ripples
import com.ngstudio.maze.tools.SceneKit.rows
import com.ngstudio.maze.tools.SceneKit.seaBand
import com.ngstudio.maze.tools.SceneKit.skyline
import com.ngstudio.maze.tools.SceneKit.snowDots
import com.ngstudio.maze.tools.SceneKit.splats
import com.ngstudio.maze.tools.SceneKit.sprinkles
import com.ngstudio.maze.tools.SceneKit.stars
import com.ngstudio.maze.tools.SceneKit.strata
import com.ngstudio.maze.tools.SceneKit.stripes
import com.ngstudio.maze.tools.SceneKit.sunDisc
import com.ngstudio.maze.tools.SceneKit.sunburst
import com.ngstudio.maze.tools.SceneKit.swirl
import com.ngstudio.maze.tools.SceneKit.synthGrid
import com.ngstudio.maze.tools.SceneKit.terraces
import com.ngstudio.maze.tools.SceneKit.treeline
import com.ngstudio.maze.tools.SceneKit.vig
import com.ngstudio.maze.tools.SceneKit.waterTop
import com.ngstudio.maze.tools.SceneKit.zenRakes
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Chapters 11..100: one scene each, painted from SceneKit layers into backdrop_10..99.jpg (720 x 1560,
 * smaller than chapters 1..10 to keep the APK down; they are soft pictures, so they scale up cleanly).
 * The order and names must match render/Visuals.kt Backdrop.ALL (and the props in ui/components/ChapterProps.kt).
 *
 *   ./gradlew :tools:run --args="scenes"            all 90
 *   ./gradlew :tools:run --args="scenes 31 40"      only chapters 31..40
 */
object SceneArt {
    private val CANDY = intArrayOf(0xFF5C8A, 0x4FC3F7, 0xFFD54F, 0x81C784, 0xB39DDB, 0xFFFFFF, 0xFF8A50)
    private val PARTY = intArrayOf(0xFF5C8A, 0x4FC3F7, 0xFFD54F, 0x81C784, 0xB39DDB, 0xFF8A50)

    val SCENES: List<Pair<String, List<Layer>>> = listOf(
        // ---------------------------------------------------------------- 11-20 nature
        "Autumn Forest" to listOf(grad(0xFFE3AE, 0xF7B46C, 0xE0803C), treeline(0.50, 0.20, 0xD06A34, 9.0, 1.0), treeline(0.64, 0.22, 0xA04A24, 7.0, 2.0),
            ground(0.80, 0xC8702E, 0x9A4A1E), leaves(intArrayOf(0xF2A23A, 0xE0602A, 0xC8402A, 0xF5C842), 1 / 7.0, 0.35), vig()),
        "Pumpkin Patch" to listOf(grad(0x5A3478, 0xC05A78, 0xFFAE66, 0xFFD49A), stars(40.0, 0.9, 1.2, vMax = 0.25), hills(0.56, 0.06, 1.2, 1.0, 0x8A7A30, 0x5E5222),
            hills(0.70, 0.05, 1.7, 3.0, 0x7A8A2E, 0x4E5A1E), dots(0xFF8A1E, 70.0, 11.0, 0.95, 0.6, vMin = 0.74), grain(), vig()),
        "Rainy Day" to listOf(grad(0x7D90A8, 0xA9B9CB, 0xC9D6E2), clouds(0xB4C2D2, 0x7A8A9E, listOf(0.08, 0.22)), hills(0.70, 0.05, 1.3, 0.5, 0x7FA07A, 0x5E7E5E),
            rain(0xE6EEF6, 0.55), vig(0.16)),
        "Rainbow Valley" to listOf(grad(0x7CCBFF, 0xC6ECFF, 0xEAF8FF), rainbow(0.5, 0.66, 0.62, 0.11, 0.6), clouds(0xFFFFFF, 0xD2E6F6, listOf(0.12)),
            hills(0.60, 0.07, 1.4, 0.3, 0x9ADB6A, 0x6CBF4A, 0xC8F29A), hills(0.74, 0.06, 1.9, 2.5, 0x6CC64E, 0x3E9A34, 0xA8E67A), vig(0.08)),
        "Mushroom Forest" to listOf(grad(0x0E3A3E, 0x1A5650, 0x23704E), bokeh(intArrayOf(0x7DF2C8, 0xB8FF8A, 0x7AD8FF), 0.22), treeline(0.62, 0.30, 0x0B2E2E, 6.0, 4.0),
            ground(0.80, 0x2E6A3A, 0x1A4426), dots(0xD8FF7A, 26.0, 2.0, 0.8, 1.0), vig(0.2)),
        "Bamboo Grove" to listOf(grad(0xE6F6D2, 0xC2E6A2, 0x9ED07E), bamboo(0x8CC85A, 4.5, 0.45), bamboo(0x5EA83E, 2.6, 0.9), glow(0.3, 0.1, 0xFFFFFF, 0.4, 0.35), vig(0.1)),
        "Lavender Fields" to listOf(grad(0xFFD7B0, 0xF4C2D8, 0xC7B6F2), sunDisc(0.68, 0.36, 0.10, 0xFFF0C8, 0xFFD0A0), hills(0.42, 0.03, 0.8, 1.0, 0x9A8AC8, 0x8A78BE),
            rows(0.47, 0xA77FE0, 0x6E8E4A), vig(0.12)),
        "Sunflower Farm" to listOf(grad(0x6CC1F2, 0xBFE8FF), clouds(0xFFFFFF, 0xD0E6F6, listOf(0.1)), hills(0.48, 0.04, 1.0, 0.2, 0x7EBF4E, 0x5E9E3A),
            hills(0.58, 0.04, 1.3, 2.0, 0xF6C832, 0xE8A81E), dots(0x7A4A1A, 44.0, 6.0, 0.9, 0.8, vMin = 0.6), dots(0xFFD43A, 44.0, 12.0, 0.35, 0.8, vMin = 0.6), vig(0.1)),
        "Pine Woods" to listOf(grad(0xD6EEE6, 0xAED8C6), mountains(0.40, 0.12, 1.4, 0.3, 0x9CC2C0, null, 0xC4DED8), treeline(0.52, 0.22, 0x5E9A7E, 10.0, 5.0),
            treeline(0.66, 0.26, 0x3E7A5E, 7.0, 6.0), treeline(0.82, 0.30, 0x24583E, 5.0, 7.0), vig(0.1)),
        "Lily Pond" to listOf(grad(0x5EB89A, 0x3A8A78, 0x2A6E62), caustics(0.6, 1.0), lilyPads(0x6CC24A, 0xFF9EC2), vig(0.15)),
        // ---------------------------------------------------------------- 21-30 seasons
        "Northern Lights" to listOf(grad(0x061733, 0x0E2C4C, 0x1C4A62), stars(18.0, 0.88, 1.0), aurora(0x3CF2A0, 0x8A5CFF, 0.75),
            mountains(0.66, 0.10, 1.8, 0.2, 0x1E3A55, 0xDDEEFF), ground(0.82, 0xE6F2FF, 0xBCD6EE), vig(0.15)),
        "Ice Cave" to listOf(crystals(0xBDE6FF, 0x6FB2E6, 1.4), glow(0.5, 0.45, 0xFFFFFF, 0.35, 0.45), vig(0.2)),
        "Polar Bay" to listOf(grad(0xBFE2F8, 0xE6F6FF), mountains(0.34, 0.10, 1.2, 0.6, 0xE8F4FF, null, 0xCFE4F5), floes(0.36, 0.78, 0xF4FAFF, 0x3C8EC6),
            ground(0.80, 0xFFFFFF, 0xDCEBF8), snowDots(0.6), vig(0.08)),
        "Christmas Eve" to listOf(grad(0x0C1C44, 0x1E3A74, 0x3A5A9A), stars(30.0, 0.85, 1.2, 0xFFF4C8), treeline(0.70, 0.22, 0x123A3A, 8.0, 8.0, snow = true),
            ground(0.80, 0xF4FAFF, 0xC9DDF0), snowDots(), vig(0.15)),
        "Holiday Lights" to listOf(grad(0x0E3B2A, 0x16503A), bokeh(intArrayOf(0xFF4D4D, 0xFFD23F, 0x5CC8FF, 0x7CFF8A, 0xFF8AD8), 0.55, 1 / 5.5, 0.55), stripes(0xFFFFFF, 6.0, 35.0, 0.04), vig(0.2)),
        "Spring Garden" to listOf(grad(0xEAF8D8, 0xC4E8A2, 0xA8DA82), mottle(0xD8F2B0, 0.7, 0.4), grass(0.12),
            dots(0xFFFFFF, 50.0, 4.5, 0.9, 1.0, palette = intArrayOf(0xFFFFFF, 0xFFE45C, 0xFF9EC2, 0xC9B6FF)), glow(0.3, 0.0, 0xFFFFFF, 0.35, 0.3), vig(0.1)),
        "Cherry Orchard" to listOf(grad(0xFFF2E8, 0xFFDCD8), hills(0.66, 0.05, 1.2, 1.0, 0xB8DA8A, 0x8EC266), petals(0xF7A1BE, 0xFFD6E4, 1 / 8.0, 0.4), vig(0.08)),
        "Citrus Grove" to listOf(flat(0xFFF6D2), dots(0xFFC93C, 90.0, 26.0, 0.35, 0.0), dots(0xFF9A3C, 90.0, 9.0, 0.25, 0.0), stripes(0xFFFFFF, 7.0, 0.0, 0.2), vig(0.08)),
        "Berry Patch" to listOf(grad(0xFFE6EE, 0xFFD0DE), dots(0xFF5C7A, 64.0, 13.0, 0.55, 0.7), dots(0x5CB85C, 64.0, 5.0, 0.6, 0.7), dots(0xFFFFFF, 22.0, 1.4, 0.6, 1.0), vig(0.08)),
        "Honey Hive" to listOf(grad(0xFFDA5C, 0xF6AE2E), hexes(0xB86A06, 60.0, 0.95, 0xFFF0B0), glow(0.5, 0.4, 0xFFF4C0, 0.4, 0.35), vig(0.14)),
        // ---------------------------------------------------------------- 31-45 sky & space
        "Sunset Sky" to listOf(grad(0x3A2A6E, 0xC4507E, 0xFF9E6A, 0xFFD89A), sunDisc(0.5, 0.74, 0.12, 0xFFF0B0, 0xFFB070), clouds(0xFFB8C8, 0x8A4A8A, listOf(0.22, 0.48), 0.9),
            hills(0.88, 0.04, 1.0, 1.0, 0x4A2A5E, 0x2A1A3E), vig(0.12)),
        "Golden Hills" to listOf(grad(0xFFEFB8, 0xFFD078), sunDisc(0.3, 0.2, 0.08, 0xFFFFFF, 0xFFE08A), hills(0.46, 0.06, 1.0, 0.5, 0xE8B850, 0xC89838),
            hills(0.62, 0.06, 1.5, 2.2, 0xC8A040, 0xA07A2E), hills(0.78, 0.06, 1.1, 4.0, 0x9A8A3A, 0x6E6226), vig(0.1)),
        "Moon Base" to listOf(grad(0x04050E, 0x141832), stars(20.0, 0.86, 1.0), sunDisc(0.78, 0.18, 0.11, 0x5CA8FF, 0x2A5AA8), craters(0.62, 0xB8BCC8, 0x7A7E8A), vig(0.18)),
        "Galaxy Swirl" to listOf(flat(0x0A0820), stars(14.0, 0.9, 0.9), swirl(0.5, 0.45, 0x9A5CFF, 0xFF5CC8, 1.1), stars(60.0, 0.8, 2.0), vig(0.2)),
        "Rocket Launch" to listOf(grad(0x0A0F30, 0x2A2470, 0x5A3A9A), stars(22.0, 0.86, 1.1), nebula(0x5A3AB0, 0x2A6AB0, 0.35), hills(0.82, 0.04, 1.0, 0.3, 0x2A1E4A, 0x14102A), vig(0.15)),
        "Alien Planet" to listOf(grad(0x260C46, 0x6A2484, 0xB0469A), stars(26.0, 0.88, 1.1), sunDisc(0.72, 0.24, 0.17, 0x7AF0C8, 0x3AA88A), sunDisc(0.25, 0.14, 0.05, 0xFFD27A, 0xFF9A5A),
            hills(0.66, 0.06, 1.4, 1.0, 0x2EC4B6, 0x1A7A8A, 0x8AF8E8), hills(0.80, 0.05, 2.0, 3.0, 0x1A8A8A, 0x0E4A5A), vig(0.15)),
        "Meteor Shower" to listOf(grad(0x080B22, 0x1E1650, 0x3A2266), stars(16.0, 0.86, 1.0), meteors(0xFFF4D0, 7), hills(0.86, 0.03, 1.0, 1.0, 0x16122E, 0x0A0818), vig(0.15)),
        "Kite Festival" to listOf(grad(0x5EC0FF, 0xBFEAFF), clouds(0xFFFFFF, 0xCFE4F6, listOf(0.2, 0.45)), hills(0.76, 0.05, 1.3, 0.8, 0x8AD46A, 0x58A848, 0xBFF29A), vig(0.08)),
        "Balloon Fiesta" to listOf(grad(0xFFD7A6, 0xFFE8C8, 0xBFE4FF), mountains(0.62, 0.10, 1.6, 0.4, 0xA88AA8, null, 0xD6C0D0), hills(0.80, 0.03, 1.0, 1.0, 0xC8A878, 0x9A7A52), vig(0.08)),
        "Paper Planes" to listOf(grad(0xE6F4FF, 0xCFE8FF), stripes(0xFFFFFF, 9.0, -25.0, 0.18), clouds(0xFFFFFF, 0xDCEBF8, listOf(0.3, 0.7), 0.85), vig(0.06)),
        "Pastel Clouds" to listOf(grad(0xFFD6EE, 0xE6DEFF, 0xD2EEFF), clouds(0xFFFFFF, 0xF0C8E6, listOf(0.15, 0.4, 0.68), 0.9), vig(0.06)),
        "Thunder Storm" to listOf(grad(0x252E40, 0x3E4A60, 0x56627A), clouds(0x6A7488, 0x2E3648, listOf(0.08, 0.26)), lightning(0.62, 0.18, 0.62, 0xFFF6B0), rain(0xC8D2E6, 0.45),
            hills(0.82, 0.04, 1.2, 0.5, 0x2E4A3A, 0x1A2E24), vig(0.2)),
        "Windmill Hills" to listOf(grad(0x9EDBFF, 0xE4F6FF), clouds(0xFFFFFF, 0xD6E8F6, listOf(0.1)), hills(0.52, 0.05, 1.1, 0.6, 0xA6DC7A, 0x86C25E), fields(0.62, intArrayOf(0xE8D25A, 0x9AD06A, 0x6EB84A, 0xC8B04A)), vig(0.08)),
        "Starlight Lake" to listOf(grad(0x0E1640, 0x2A2A6E), stars(18.0, 0.85, 1.0), mountains(0.50, 0.10, 1.5, 0.2, 0x16204A, null), seaBand(0.52, 1.0, 0x1E2A66, 0x0E1640),
            stars(18.0, 0.92, 0.8, bright = 0.5), vig(0.15)),
        "Comet Trail" to listOf(grad(0x07122E, 0x153060, 0x24508A), stars(24.0, 0.87, 1.1), meteors(0xBFF0FF, 2), nebula(0x2A8AC8, 0x5A3AB0, 0.25), vig(0.15)),
        // ---------------------------------------------------------------- 46-56 water
        "Coral Reef" to listOf(grad(0x8BE4F0, 0x2C9CC8, 0x1E78B0), rays(0xFFFFFF, 0.10), caustics(1.0), ground(0.84, 0xE8D29A, 0xC9AE72), dots(0xFF7A8A, 54.0, 10.0, 0.6, 0.9, vMin = 0.84), vig(0.15)),
        "Deep Sea" to listOf(grad(0x0A3458, 0x061E3A, 0x020C1C), rays(0x5AC8FF, 0.06), bokeh(intArrayOf(0x5AF0FF, 0x7A8AFF), 0.18, 1 / 7.0, 0.4), dots(0x9AF6FF, 30.0, 1.3, 0.7, 1.0), vig(0.25)),
        "Pirate Cove" to listOf(grad(0x7FD8F0, 0x3AA8D8), clouds(0xFFFFFF, 0xD0E6F6, listOf(0.08)), seaBand(0.30, 0.74, 0x2A9AD0, 0x1A6AA8), ground(0.74, 0xF2D49A, 0xD8B070), ripples(0xC89A5A, 7.0, 1.0, 0.76), vig(0.1)),
        "Lighthouse Coast" to listOf(grad(0xFFD2AE, 0xFFE6CC, 0xB6E2F6), seaBand(0.48, 1.0, 0x5AB8E0, 0x2A7AB0), sunDisc(0.3, 0.44, 0.07, 0xFFF4D0, 0xFFD0A0), vig(0.1)),
        "Sailing Bay" to listOf(grad(0x6A4AA0, 0xE07A9A, 0xFFC890), sunDisc(0.62, 0.40, 0.09, 0xFFF2C8, 0xFFB080), clouds(0xFFC0D0, 0x9A5A9A, listOf(0.15), 0.85),
            seaBand(0.44, 1.0, 0x8A5AA8, 0x2A2A6A), vig(0.12)),
        "Kelp Forest" to listOf(grad(0x5AC0A0, 0x23807A, 0x0E4A52), rays(0xE6FFF4, 0.10), kelp(0x3E9A4A, 5.0), kelp(0x2E7A3A, 3.0), ground(0.9, 0xC8B880, 0x9A8A5A), vig(0.18)),
        "Jellyfish Lagoon" to listOf(grad(0x2E2066, 0x1A4A8A, 0x0E6A9A), caustics(0.7), bokeh(intArrayOf(0xFF8AD8, 0xC88AFF, 0x8AE6FF), 0.25, 1 / 5.0), vig(0.2)),
        "Ice Rink" to listOf(grad(0xE2F4FF, 0xC4E4FA), stripes(0xFFFFFF, 3.0, 20.0, 0.2, 0.02), stripes(0xA8D0EE, 2.2, -35.0, 0.12, 0.02), crystals(0xE8F6FF, 0xD0EAFA, 0.6, 0xFFFFFF).let { l -> { k: Ctx -> val keep = k.c; l(k); k.c = MaterialArt.mix(keep, k.c, 0.3) } }, vig(0.08)),
        "Pearl Shore" to listOf(grad(0xFFF4E8, 0xF8DCC8), waterTop(0.14, 0x3AB6C8, 0x8AE0E0), ripples(0xE8C2A0, 8.0, 0.8), dots(0xFFFFFF, 60.0, 5.0, 0.8, 1.0, vMin = 0.2), vig(0.08)),
        "Tropical Lagoon" to listOf(grad(0xFBE9C0, 0xF2D49A), ripples(0xE0BC82, 7.0, 0.8), waterTop(0.52, 0x0E9AB8, 0x5AE6DC), caustics(0.8, 0.5), vig(0.1)),
        "River Rapids" to listOf(grad(0xA8DCFF, 0xD8F0FF), treeline(0.25, 0.14, 0x3E8A52, 9.0, 13.0), seaBand(0.25, 0.75, 0x4AB8D8, 0x2A8AB8), dots(0xFFFFFF, 40.0, 3.5, 0.5, 1.0, vMin = 0.26), ground(0.75, 0x7AB86A, 0x4E8A4A), vig(0.12)),
        // ---------------------------------------------------------------- 57-67 earth & fire
        "Red Canyon" to listOf(grad(0xFFD0A0, 0xFFB880), mesas(0.44, 0xC0583A), strata(0.50, intArrayOf(0xD86A3E, 0xE88A52, 0xC4583A, 0xF2A066, 0xB24A30)), vig(0.12)),
        "Savanna" to listOf(grad(0xFFE08A, 0xFFAE5A, 0xF0803E), sunDisc(0.5, 0.42, 0.16, 0xFFF0B0, 0xFFC070), hills(0.60, 0.02, 0.8, 0.0, 0xC8883A, 0xA0662A), grass(0.18, 0.6), vig(0.12)),
        "Pyramid Sands" to listOf(grad(0xFFE2A8, 0xFFD08A), sunDisc(0.78, 0.14, 0.07, 0xFFFFFF, 0xFFE0A0), pyramids(0.46, 0xE0A860), dunes(0.46, intArrayOf(0xF2C882, 0xEDB670, 0xE6A460, 0xDE9452)), vig(0.1)),
        "Desert Oasis" to listOf(grad(0xFFDCA0, 0xFFE8C4), dunes(0.30, intArrayOf(0xF4D094, 0xEEBE7E, 0xE8AE6A)), pool(0.5, 0.62, 0.34, 0.10, 0x3ABCC8, 0x8AC85A), vig(0.1)),
        "Volcano Island" to listOf(grad(0x2E0C0C, 0x6A1E10, 0xB0401A), stars(40.0, 0.9, 1.0, 0xFFC890, vMax = 0.3), mountains(0.60, 0.26, 0.8, 0.25, 0x2A1612, 0xFF6A1E, 0x4A2016),
            lavaCracks(0x2A1410, 0xFF6A1E, 1.4, 0.78), vig(0.18)),
        "Lava Lake" to listOf(lavaCracks(0x26120E, 0xFF7A1E, 1.2), glow(0.5, 0.8, 0xFF8A2E, 0.5, 0.35), vig(0.2)),
        "Crystal Cavern" to listOf(crystals(0x6A3AB0, 0x2A1A5A, 1.1, 0xE0C8FF), glow(0.5, 0.5, 0xC890FF, 0.4, 0.35), vig(0.22)),
        "Gold Mine" to listOf(grad(0x3A2612, 0x1E140A), crystals(0x4A3218, 0x2A1C0E, 1.6, 0x6A4A26), dots(0xFFD23F, 34.0, 2.2, 0.9, 1.0), glow(0.5, 0.45, 0xFFB84A, 0.4, 0.3), vig(0.2)),
        "Dino Valley" to listOf(grad(0xD2F2C2, 0xA8E0A0), mountains(0.46, 0.16, 1.0, 0.3, 0x6E9A6A, null, 0x9AC290), hills(0.66, 0.05, 1.3, 1.0, 0x5EAA4A, 0x3E8236), leaves(intArrayOf(0x4E9E3A, 0x6CB84A, 0x3A7E2E), 1 / 6.0, 0.3), vig(0.1)),
        "Rocky Mountains" to listOf(grad(0x8CCBF2, 0xD4EEFF), mountains(0.42, 0.20, 1.2, 0.15, 0x7A8CA6, 0xFFFFFF, 0xA8B8CC), mountains(0.56, 0.16, 1.7, 0.6, 0x5E7088, 0xF4F8FF, 0x8A9AB0),
            treeline(0.74, 0.16, 0x2E5E4A, 12.0, 9.0), vig(0.1)),
        "Stone Garden" to listOf(grad(0xE6E2D6, 0xCFC8B8), bricks(0xB8AE98, 120.0, 60.0, 0.1), mottle(0x9EB87A, 1.6, 0.18), vig(0.12)),
        // ---------------------------------------------------------------- 68-78 sweets
        "Ice Cream Parlor" to listOf(flat(0xFFF0F4), stripes(0xBFF0DC, 4.0, 0.0, 0.7, 0.02), dots(0xFFFFFF, 40.0, 3.0, 0.7, 0.0), vig(0.06)),
        "Cupcake Bakery" to listOf(flat(0xFFE4EE), checker(0xFFF2F7, 56.0, 0.8), sprinkles(CANDY, 1 / 14.0, 0.4, 0xE0A0B8, 0.8), vig(0.06)),
        "Donut Shop" to listOf(grad(0xFFE6C8, 0xFFD6B0), donuts(0xE0A060, intArrayOf(0xFF8AB8, 0x7A4A2A, 0xFFF0F4, 0x8AD8FF)), vig(0.08)),
        "Chocolate Factory" to listOf(chocoWaves(0x8A4A26, 0x5A2C14), dots(0xF2D8B0, 120.0, 4.0, 0.5, 1.0), vig(0.18)),
        "Bubblegum Pop" to listOf(grad(0xFFA6D2, 0xFF86C0), bubbles(0xFFFFFF, 0.6), vig(0.08)),
        "Lemonade Stand" to listOf(flat(0xFFF6C8), stripes(0xFFE45C, 5.0, 0.0, 0.8, 0.02), dots(0xFFFFFF, 46.0, 4.0, 0.5, 1.0), vig(0.06)),
        "Cookie Jar" to listOf(grad(0xF6D8A8, 0xEBC48C), dots(0x5A2E14, 34.0, 4.2, 0.85, 1.0), mottle(0xD8A86A, 1.0, 0.3), grain(0.08), vig(0.1)),
        "Watermelon Summer" to listOf(grad(0xFF94A2, 0xFF6A84), dots(0x3A1E1E, 52.0, 3.4, 0.85, 1.0), ground(0.9, 0xF4FFE8, 0x5ABF5A, 0.004), vig(0.08)),
        "Jelly Beans" to listOf(flat(0xFFF6E6), sprinkles(CANDY, 1 / 9.0, 0.7, 0xE6CCAA, 1.8), vig(0.06)),
        "Candy Stripes" to listOf(flat(0xFFFFFF), stripes(0xFF6A8A, 6.0, 40.0, 0.85, 0.05), stripes(0xFFFFFF, 18.0, 40.0, 0.25, 0.05), vig(0.06)),
        "Marshmallow Clouds" to listOf(grad(0xFFE0F0, 0xE6E6FF), clouds(0xFFFFFF, 0xF6D0E6, listOf(0.25, 0.55, 0.85), 0.95, 0.7), sprinkles(CANDY, 1 / 12.0, 0.25, 0xE6C0D8, 0.7), vig(0.06)),
        // ---------------------------------------------------------------- 79-90 places & fun
        "Toy Box" to listOf(flat(0x8ED8FF), dots(0, 80.0, 18.0, 0.55, 0.0, palette = intArrayOf(0xFFE45C, 0xFF6A6A, 0x7ED957, 0xFFFFFF)), vig(0.08)),
        "Circus Tent" to listOf(flat(0xFFF4E6), radialStripes(0.5, -0.1, 0xE84A4A, 22, 0.85), glow(0.5, 0.0, 0xFFFFFF, 0.3, 0.3), vig(0.15)),
        "Carnival Night" to listOf(grad(0x180C38, 0x3A1A6A, 0x5A2A7A), stars(28.0, 0.9, 1.0), bokeh(intArrayOf(0xFFD23F, 0xFF5C8A, 0x5CE0FF, 0x9AFF6A), 0.4, 1 / 6.0, 0.5), vig(0.2)),
        "Music Stage" to listOf(grad(0x22083E, 0x0E0A24), rays(0xFF5CC8, 0.10, 7.0), rays(0x5CC8FF, 0.08, 11.0), bokeh(intArrayOf(0xFFD23F, 0xFF5CC8, 0x5CC8FF), 0.2, 1 / 5.0), ground(0.86, 0x3A1A1A, 0x1A0A0A, 0.0), vig(0.2)),
        "Art Studio" to listOf(flat(0xFFF8EE), splats(intArrayOf(0xFF5C5C, 0x5CB8FF, 0xFFD23F, 0x6CD06A, 0xB88AFF, 0xFF9A3C), 1 / 4.2, 0.4), grain(0.05), vig(0.08)),
        "Library" to listOf(books(intArrayOf(0x9A3A2E, 0x2E5A8A, 0x3E7A4A, 0xC89A3A, 0x6A3A7A, 0x2E2E4A), 0x7A4A24), vig(0.25)),
        "Chess Hall" to listOf(grad(0xF6E6CC, 0xE6CCA4), checker(0x8A5A3A, 90.0, 0.35), vig(0.18)),
        "Castle Kingdom" to listOf(grad(0x8ED2FF, 0xD8F0FF), clouds(0xFFFFFF, 0xD6E8F6, listOf(0.1)), castle(0.50, 0xA8A0C8), hills(0.62, 0.05, 1.0, 0.5, 0x8ACD6A, 0x5EA84A), vig(0.1)),
        "Fairy Garden" to listOf(grad(0xEADAFF, 0xFFE2F2, 0xD8F6E4), bokeh(intArrayOf(0xFFFFFF, 0xFFE8A0, 0xE0C8FF), 0.35, 1 / 6.0), dots(0xFFFFFF, 28.0, 1.6, 0.8, 1.0), grass(0.1, 0.8), vig(0.08)),
        "Lantern Festival" to listOf(grad(0x1A0E3E, 0x4A1A4A, 0x7A2A3A), bokeh(intArrayOf(0xFFB04A, 0xFF8A3A, 0xFFD27A), 0.45, 1 / 6.5, 0.55), stars(30.0, 0.92, 1.0, 0xFFE0A0), vig(0.2)),
        "Halloween Night" to listOf(grad(0x1C0C32, 0x4A1A5A, 0xD06A2A), sunDisc(0.5, 0.42, 0.18, 0xFFE6A0, 0xFFB060), treeline(0.74, 0.18, 0x1A0E1E, 6.0, 11.0), ground(0.84, 0x1A0E1E, 0x0E060E), vig(0.2)),
        "Spooky Swamp" to listOf(grad(0x1A2E26, 0x2E4A3A, 0x3E5E46), glow(0.5, 0.6, 0xA8E6A0, 0.6, 0.25), treeline(0.66, 0.24, 0x10201A, 5.0, 12.0), dots(0xD8FF7A, 30.0, 1.8, 0.85, 1.0), vig(0.22)),
        // ---------------------------------------------------------------- 91-100 bright finale
        "Robot Lab" to listOf(grad(0x1C2A3C, 0x101A28), stripes(0x2EE6FF, 14.0, 0.0, 0.12, 0.01), stripes(0x2EE6FF, 14.0, 90.0, 0.12, 0.01), bokeh(intArrayOf(0x2EE6FF, 0x7A8AFF), 0.15), vig(0.2)),
        "Neon City" to listOf(grad(0x14082E, 0x3A1258, 0x6A1E6E), stars(26.0, 0.9, 1.0), skyline(0.82, 0x1E0E3A, 0xFF5CD8, 7.0, 0.30, 1.0), skyline(0.90, 0x0E0820, 0x5CE6FF, 11.0, 0.22, 2.0), vig(0.18)),
        "Retro Arcade" to listOf(grad(0x1A0838, 0x5A1A7A, 0xFF5C9A), sunDisc(0.5, 0.42, 0.20, 0xFFD23F, 0xFF7A5A, stripes = true), ground(0.52, 0x1A0838, 0x0A0418, 0.0), synthGrid(0.52, 0xFF4FD8), vig(0.15)),
        "Sports Field" to listOf(mowed(0x6CC24A, 0x5AB03C, 12), stripes(0xFFFFFF, 0.5, 90.0, 0.0), glow(0.5, 0.0, 0xFFFFFF, 0.5, 0.2), vig(0.15)),
        "Picnic Park" to listOf(flat(0xFFFFFF), gingham(0xE84A4A, 48.0, 0.6), vig(0.1)),
        "Farm Fields" to listOf(grad(0x9EDCFF, 0xDCF4FF), clouds(0xFFFFFF, 0xD6E8F6, listOf(0.08)), fields(0.36, intArrayOf(0xE8D25A, 0x9AD06A, 0x6EB84A, 0xC8A04A, 0xB8DC7A)), vig(0.1)),
        "Fireworks Night" to listOf(grad(0x080A26, 0x18184A, 0x2A2260), stars(22.0, 0.88, 1.0), skyline(0.86, 0x0A0A1E, 0xFFD27A, 8.0, 0.18, 3.0, 0.35), vig(0.18)),
        "Treasure Vault" to listOf(grad(0x4A3410, 0x241808), coins(0xF6C23A, 1 / 6.0), glow(0.5, 0.5, 0xFFD86A, 0.5, 0.25), vig(0.25)),
        "Origami Paper" to listOf(paperFolds(intArrayOf(0xFFD6D6, 0xFFE8C2, 0xD6F0FF, 0xE2D6FF, 0xD6FFE6, 0xFFF4C2)), vig(0.06)),
        "Grand Finale" to listOf(grad(0xFFE48A, 0xFFB43A), sunburst(0.5, 0.4, 0xFFF0B8, 18, 0.45), confetti(PARTY, 1 / 10.0, 0.5), glow(0.5, 0.4, 0xFFFFFF, 0.3, 0.35), vig(0.12)),
    )

    fun run(outDir: File, from: Int = 11, to: Int = 100, width: Int = 720, height: Int = 1560) {
        require(SCENES.size == 90) { "need 90 scenes for chapters 11..100, have ${SCENES.size}" }
        outDir.mkdirs()
        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        for ((i, s) in SCENES.withIndex()) {
            val chapter = i + 11
            if (chapter !in from..to) continue
            pool.submit {
                try {
                    MaterialArt.renderJpg(File(outDir, "backdrop_${chapter - 1}.jpg"), width, height, 0.84f) { x, y ->
                        val k = Ctx(x, y, width.toDouble(), height.toDouble())
                        for (l in s.second) l(k)
                        k.c
                    }
                } catch (e: Throwable) { System.err.println("chapter $chapter ${s.first}: $e"); e.printStackTrace() }
            }
        }
        pool.shutdown(); pool.awaitTermination(1, TimeUnit.HOURS)
        println("  chapters $from..$to done")
    }
}
