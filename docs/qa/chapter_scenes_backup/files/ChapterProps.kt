package com.ngstudio.mazebrush.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Corner props and menu motion for chapters 11..100 (chapters 1..10 are hand-made in ThemeDecor.kt).
 * One entry per chapter, in chapter order; keep in sync with Backdrop.ALL (render/Visuals.kt) and the
 * scene paintings (tools SceneArt.SCENES).
 */
class Prop(val floating: Boolean, val draw: DrawScope.(t: Float) -> Unit)

enum class Amb { NONE, SNOW, BUBBLES, PETALS, TWINKLE, BUTTERFLIES, LEAVES, RAIN, FIREFLIES, EMBERS, CONFETTI, NOTES, LANTERNS, METEORS, FIREWORKS, SPARKLES, HEARTS, BEES }

class ChapterDecor(val tl: Prop?, val tr: Prop?, val bl: Prop?, val br: Prop?, val ambient: Amb, val tint: Color = Color.White)

private fun e(d: DrawScope.(Float) -> Unit) = Prop(false, d)
private fun f(d: DrawScope.(Float) -> Unit) = Prop(true, d)
private fun C(h: Long) = Color(h)

private val RED = C(0xFFE53950); private val YEL = C(0xFFFFD23F); private val BLU = C(0xFF3A8AE8); private val PNK = C(0xFFFF8AB8)
private val PURP = C(0xFFB88AFF); private val ORG = C(0xFFFF9A2E); private val LEMON = C(0xFFF6D21E); private val MINT = C(0xFF8AE6C8)
private val PASTELS = listOf(C(0xFFFF8AB8), C(0xFF8AD8FF), C(0xFFFFE45C), C(0xFFB8F08A), C(0xFFC8A8FF))
private val CIRCUS = listOf(RED, YEL, BLU, C(0xFF6CD06A))

/** Index 0 = chapter 11. */
val CHAPTER_DECOR: List<ChapterDecor> = listOf(
    // 11-20
    ChapterDecor(e { leafSpray(it, C(0xFFB8401A), C(0xFFF5B042)) }, e { leafSpray(it, C(0xFFA83A1A), C(0xFFF2C84A)) }, e { mushrooms(it, C(0xFFD8402A)) }, e { grassTuft(it, C(0xFFC88A2A)) }, Amb.LEAVES, C(0xFFF08A2A)),
    ChapterDecor(f { bats(it) }, f { moon(it) }, e { pumpkins(it, face = true) }, e { pumpkins(it) }, Amb.FIREFLIES, C(0xFFFFC84A)),
    ChapterDecor(f { rainCloud(it, false) }, f { rainCloud(it, false) }, e { umbrella(it, YEL) }, e { frog(it) }, Amb.RAIN),
    ChapterDecor(f { cloudPuff(it, top = true) }, f { rainbowSmall(it) }, e { tulips(it, listOf(RED, PNK, YEL)) }, e { grassFlowers(it, false) }, Amb.BUTTERFLIES),
    ChapterDecor(e { leafSpray(it, C(0xFF1E5A3A), C(0xFF5EAE6A), 0.12f) }, e { leafSpray(it, C(0xFF1E5A3A), C(0xFF5EAE6A), 0.12f) }, e { mushrooms(it, C(0xFF5AE6C8), glow = true) }, e { mushrooms(it, C(0xFFE84A6A)) }, Amb.FIREFLIES, C(0xFFB8FF8A)),
    ChapterDecor(e { leafSpray(it, C(0xFF4E8E2A), C(0xFFA8E07A), 0.12f) }, e { leafSpray(it, C(0xFF4E8E2A), C(0xFFA8E07A), 0.12f) }, e { bamboo(it) }, e { bamboo(it) }, Amb.LEAVES, C(0xFF8ACD5A)),
    ChapterDecor(f { bees(it) }, f { butterflyFloat(it, PURP) }, e { lavender(it) }, e { lavender(it) }, Amb.BUTTERFLIES, PURP),
    ChapterDecor(f { sun(it) }, f { bees(it) }, e { sunflowers(it) }, e { sunflowers(it) }, Amb.BEES),
    ChapterDecor(e { leafSpray(it, C(0xFF1E5A3A), C(0xFF4E9E5A), 0.08f, 9) }, e { leafSpray(it, C(0xFF1E5A3A), C(0xFF4E9E5A), 0.08f, 9) }, e { pineTrees(it) }, e { mushrooms(it, C(0xFFB86A3A)) }, Amb.NONE),
    ChapterDecor(f { dragonfly(it) }, f { dragonfly(it) }, e { frog(it) }, e { lotus(it) }, Amb.SPARKLES),
    // 21-30
    ChapterDecor(f { bigStars(it) }, f { moon(it) }, e { pineTrees(it, snow = true) }, e { igloo(it) }, Amb.TWINKLE),
    ChapterDecor(e { crystalCeiling(it, C(0xFFBFE6FF)) }, e { crystalCeiling(it, C(0xFFA8DCFF)) }, e { iceCrystals(it) }, e { iceCrystals(it) }, Amb.SPARKLES),
    ChapterDecor(f { snowflakes(it) }, f { snowflakes(it) }, e { penguin(it) }, e { igloo(it) }, Amb.SNOW),
    ChapterDecor(e { ornaments(it) }, e { ornaments(it) }, e { xmasTree(it) }, e { gifts(it) }, Amb.SNOW),
    ChapterDecor(e { lightString(it) }, e { lightString(it) }, e { gifts(it) }, e { xmasTree(it) }, Amb.TWINKLE),
    ChapterDecor(f { butterflyFloat(it, PNK) }, f { butterflyFloat(it, YEL) }, e { tulips(it, listOf(PNK, PURP, RED)) }, e { grassFlowers(it, true) }, Amb.BUTTERFLIES),
    ChapterDecor(e { cherries(it) }, e { blossomBranch(it, true) }, e { petalPile(it, false) }, e { grassTuft(it) }, Amb.PETALS),
    ChapterDecor(e { citrusBranch(it, ORG) }, e { citrusBranch(it, LEMON) }, e { citrusPile(it, ORG, C(0xFFFFB84A)) }, e { citrusPile(it, LEMON, C(0xFFFFF08A)) }, Amb.SPARKLES),
    ChapterDecor(e { leafSpray(it, C(0xFF2E7A2A), C(0xFF7ED957)) }, e { leafSpray(it, C(0xFF2E7A2A), C(0xFF7ED957)) }, e { strawberries(it) }, e { strawberries(it) }, Amb.HEARTS, C(0xFFFF6A8A)),
    ChapterDecor(e { beehive(it) }, f { bees(it) }, e { honeyPot(it) }, e { sunflowers(it) }, Amb.BEES),
    // 31-45
    ChapterDecor(f { seagulls(it) }, f { cloudPuff(it, top = true) }, e { grassTuft(it, C(0xFF5A3A6A)) }, e { palmTree(it) }, Amb.NONE),
    ChapterDecor(f { sun(it) }, f { desertBirds(it) }, e { grassTuft(it, C(0xFFC8A040)) }, e { windmill(it, false) }, Amb.LEAVES, C(0xFFE8B850)),
    ChapterDecor(f { rocket(it) }, f { bigStars(it) }, e { moonFlag(it) }, e { rocks(it, C(0xFFA8ACB8)) }, Amb.TWINKLE),
    ChapterDecor(f { bigStars(it) }, f { comet(it) }, e { ringedPlanet(it) }, e { smallPlanet(it) }, Amb.TWINKLE),
    ChapterDecor(f { rocket(it) }, f { moon(it) }, e { rocks(it, C(0xFF5A4A7A)) }, e { telescope(it) }, Amb.TWINKLE),
    ChapterDecor(f { ufo(it) }, f { bigStars(it) }, e { alienPlants(it) }, e { alienPlants(it) }, Amb.TWINKLE),
    ChapterDecor(f { comet(it) }, f { bigStars(it) }, e { telescope(it) }, e { rocks(it, C(0xFF4A4A6A)) }, Amb.METEORS),
    ChapterDecor(f { kite(it, RED, YEL) }, f { kite(it, BLU, C(0xFFFFFFFF)) }, e { grassFlowers(it, false) }, e { grassTuft(it) }, Amb.NONE),
    ChapterDecor(f { balloon(it) }, f { balloon(it) }, e { balloonBunch(it) }, e { grassTuft(it, C(0xFFA8B85A)) }, Amb.NONE),
    ChapterDecor(f { paperPlane(it) }, f { paperPlane(it, C(0xFFFFF4C8)) }, e { cloudPuff(it, top = false) }, e { cloudPuff(it, top = false) }, Amb.NONE),
    ChapterDecor(f { cloudPuff(it, top = true) }, f { starProp(it, C(0xFFFFE89A)) }, e { cloudPuff(it, top = false) }, e { cloudPuff(it, top = false) }, Amb.SPARKLES),
    ChapterDecor(f { rainCloud(it, true) }, f { rainCloud(it, true) }, e { umbrella(it, RED) }, e { grassTuft(it, C(0xFF2E5A3A)) }, Amb.RAIN),
    ChapterDecor(f { desertBirds(it) }, f { cloudPuff(it, top = true) }, e { windmill(it, true) }, e { windmill(it, false) }, Amb.NONE),
    ChapterDecor(f { moon(it) }, f { bigStars(it) }, e { reeds(it) }, e { reeds(it) }, Amb.FIREFLIES, C(0xFFD8FF8A)),
    ChapterDecor(f { comet(it) }, f { bigStars(it) }, e { ringedPlanet(it) }, e { smallPlanet(it) }, Amb.METEORS),
    // 46-56
    ChapterDecor(f { fish(it, false) }, f { fish(it, true) }, e { reef(it, false) }, e { reef(it, true) }, Amb.BUBBLES),
    ChapterDecor(f { jellyfish(it, C(0xFF5AE6FF)) }, f { jellyfish(it, C(0xFFFF8AD8)) }, e { reef(it, false) }, e { treasureChest(it) }, Amb.BUBBLES),
    ChapterDecor(f { seagulls(it) }, f { cloudPuff(it, top = true) }, e { treasureChest(it) }, e { crab(it) }, Amb.NONE),
    ChapterDecor(f { seagulls(it) }, f { seagulls(it) }, e { lighthouse(it) }, e { crab(it) }, Amb.NONE),
    ChapterDecor(f { seagulls(it) }, f { cloudPuff(it, top = true) }, e { sailboat(it, C(0xFFFFFFFF), C(0xFFB8402A)) }, e { sailboat(it, C(0xFFFFE45C), C(0xFF2A5A9A)) }, Amb.NONE),
    ChapterDecor(f { fish(it, false) }, f { fish(it, true) }, e { reef(it, false) }, e { turtle(it) }, Amb.BUBBLES),
    ChapterDecor(f { jellyfish(it, PNK) }, f { jellyfish(it, PURP) }, e { reef(it, true) }, e { oyster(it) }, Amb.BUBBLES),
    ChapterDecor(f { snowflakes(it) }, f { snowflakes(it) }, e { snowman(it) }, e { pineTrees(it, snow = true) }, Amb.SNOW),
    ChapterDecor(e { palmFronds(it, false) }, e { palmFronds(it, true) }, e { oyster(it) }, e { beachBottom(it, true) }, Amb.NONE),
    ChapterDecor(e { palmFronds(it, false) }, e { palmFronds(it, true) }, e { turtle(it) }, e { crab(it) }, Amb.NONE),
    ChapterDecor(f { desertBirds(it) }, f { cloudPuff(it, top = true) }, e { reeds(it) }, e { frog(it) }, Amb.NONE),
    // 57-67
    ChapterDecor(f { sun(it) }, f { desertBirds(it) }, e { saguaro(it) }, e { pricklyPear(it) }, Amb.NONE),
    ChapterDecor(f { desertBirds(it) }, f { desertBirds(it) }, e { acacia(it) }, e { grassTuft(it, C(0xFFC8943A)) }, Amb.NONE),
    ChapterDecor(f { sun(it) }, f { desertBirds(it) }, e { palmTree(it) }, e { rocks(it, C(0xFFD8B080)) }, Amb.NONE),
    ChapterDecor(e { palmFronds(it, false) }, e { palmFronds(it, true) }, e { palmTree(it) }, e { pricklyPear(it) }, Amb.NONE),
    ChapterDecor(f { moon(it) }, f { bigStars(it) }, e { volcano(it, true) }, e { rocks(it, C(0xFF5A3A30)) }, Amb.EMBERS, C(0xFFFF8A3A)),
    ChapterDecor(e { crystalCeiling(it, C(0xFFFF6A1E)) }, e { crystalCeiling(it, C(0xFFFF8A3A)) }, e { volcano(it, false) }, e { rocks(it, C(0xFF4A2A22)) }, Amb.EMBERS, C(0xFFFF8A3A)),
    ChapterDecor(e { crystalCeiling(it, C(0xFFB88AFF)) }, e { crystalCeiling(it, C(0xFFD8A8FF)) }, e { iceCrystals(it, C(0xFFB88AFF)) }, e { gems(it) }, Amb.SPARKLES),
    ChapterDecor(e { lantern(it, YEL) }, e { lantern(it, ORG) }, e { gems(it) }, e { rocks(it, C(0xFF7A5A3A)) }, Amb.SPARKLES, C(0xFFFFD23F)),
    ChapterDecor(e { leafSpray(it, C(0xFF2E7A2E), C(0xFF8AD05A), 0.12f) }, e { leafSpray(it, C(0xFF2E7A2E), C(0xFF8AD05A), 0.12f) }, e { dinoEgg(it) }, e { fern(it) }, Amb.LEAVES, C(0xFF6CB84A)),
    ChapterDecor(f { desertBirds(it) }, f { cloudPuff(it, top = true) }, e { pineTrees(it) }, e { rocks(it) }, Amb.NONE),
    ChapterDecor(e { leafSpray(it, C(0xFF2E5A2A), C(0xFF7AAE5A), 0.25f) }, e { leafSpray(it, C(0xFF2E5A2A), C(0xFF7AAE5A), 0.25f) }, e { mushrooms(it, C(0xFFB86A3A)) }, e { grassFlowers(it, true) }, Amb.NONE),
    // 68-78
    ChapterDecor(e { bunting(it, PASTELS) }, e { bunting(it, PASTELS) }, e { iceCream(it, listOf(C(0xFFFF9EC2), C(0xFFBFF0DC), C(0xFFFFF0C8))) }, e { iceCream(it, listOf(C(0xFF8A5A3A), C(0xFFFFE45C))) }, Amb.SPARKLES),
    ChapterDecor(e { bunting(it, PASTELS) }, e { bunting(it, PASTELS) }, e { cupcakes(it, listOf(C(0xFFFF9EC2), C(0xFFFFFFFF))) }, e { cupcakes(it, listOf(C(0xFF8A5A3A), MINT)) }, Amb.HEARTS, C(0xFFFF8AB8)),
    ChapterDecor(f { donutFloat(it, PNK) }, f { donutFloat(it, C(0xFF7A4A2A)) }, e { donutStack(it) }, e { donutStack(it) }, Amb.NONE),
    ChapterDecor(f { chocolateBar(it) }, f { chocolateBar(it) }, e { truffles(it) }, e { truffles(it) }, Amb.NONE),
    ChapterDecor(f { heartFloat(it, PNK) }, f { heartFloat(it, PURP) }, e { lollipops(it, false) }, e { lollipops(it, true) }, Amb.BUBBLES),
    ChapterDecor(e { citrusBranch(it, LEMON) }, e { citrusBranch(it, LEMON) }, e { lemonadeGlass(it) }, e { citrusPile(it, LEMON, C(0xFFFFF08A)) }, Amb.NONE),
    ChapterDecor(f { cookieFloat(it) }, f { cookieFloat(it) }, e { cookieJar(it) }, e { cookieJar(it) }, Amb.NONE),
    ChapterDecor(f { watermelonFloat(it) }, f { sun(it) }, e { beachBall(it) }, e { watermelonSlices(it) }, Amb.NONE),
    ChapterDecor(e { candyTop(it, true) }, e { candyTop(it, false) }, e { lollipops(it, false) }, e { lollipops(it, true) }, Amb.CONFETTI),
    ChapterDecor(e { candyTop(it, false) }, e { candyTop(it, true) }, e { lollipops(it, true) }, e { cupcakes(it, listOf(C(0xFFFF6A8A), C(0xFFFFFFFF))) }, Amb.NONE),
    ChapterDecor(f { cloudPuff(it, top = true) }, f { heartFloat(it, PNK) }, e { cloudPuff(it, top = false) }, e { cloudPuff(it, top = false) }, Amb.HEARTS, C(0xFFFF9EC2)),
    // 79-90
    ChapterDecor(f { starProp(it) }, f { balloon(it) }, e { toyBlocks(it) }, e { beachBall(it) }, Amb.CONFETTI),
    ChapterDecor(e { bunting(it, CIRCUS) }, e { bunting(it, CIRCUS) }, e { balloonBunch(it) }, e { drum(it) }, Amb.CONFETTI),
    ChapterDecor(e { lightString(it) }, e { lightString(it) }, e { balloonBunch(it) }, e { lanternsGround(it) }, Amb.TWINKLE),
    ChapterDecor(f { musicNotes(it) }, f { musicNotes(it) }, e { drum(it) }, e { drum(it) }, Amb.NOTES),
    ChapterDecor(f { paletteFloat(it) }, f { starProp(it, C(0xFF8AD8FF)) }, e { paintPalette(it) }, e { bookStack(it) }, Amb.NONE),
    ChapterDecor(e { lantern(it, C(0xFFFFC86A)) }, e { lantern(it, C(0xFFFFC86A)) }, e { bookStack(it) }, e { bookStack(it) }, Amb.NONE),
    ChapterDecor(f { crownFloat(it) }, f { starProp(it) }, e { chessPieces(it) }, e { chessPieces(it) }, Amb.NONE),
    ChapterDecor(f { desertBirds(it) }, f { cloudPuff(it, top = true) }, e { castleTower(it) }, e { grassFlowers(it, false) }, Amb.NONE),
    ChapterDecor(f { butterflyFloat(it, PURP) }, f { butterflyFloat(it, PNK) }, e { mushrooms(it, C(0xFFFF8AD8), glow = true) }, e { grassFlowers(it, true) }, Amb.SPARKLES),
    ChapterDecor(e { lantern(it, RED) }, e { lantern(it, ORG) }, e { lanternsGround(it) }, e { lanternsGround(it) }, Amb.LANTERNS, C(0xFFFFB04A)),
    ChapterDecor(f { bats(it) }, f { ghost(it) }, e { pumpkins(it, face = true) }, e { pumpkins(it, face = true) }, Amb.FIREFLIES, C(0xFFFFB04A)),
    ChapterDecor(f { ghost(it) }, f { bats(it) }, e { reeds(it) }, e { mushrooms(it, C(0xFF8AE67A), glow = true) }, Amb.FIREFLIES, C(0xFFD8FF7A)),
    // 91-100
    ChapterDecor(f { gearsFloat(it) }, f { gearsFloat(it) }, e { gearsGround(it) }, e { gearsGround(it) }, Amb.SPARKLES, C(0xFF5CE6FF)),
    ChapterDecor(f { ufo(it) }, f { moon(it) }, null, null, Amb.TWINKLE),
    ChapterDecor(f { heartFloat(it, C(0xFFFF4FD8)) }, f { starProp(it, C(0xFF5CE6FF), face = false) }, null, null, Amb.TWINKLE),
    ChapterDecor(e { bunting(it, listOf(RED, C(0xFFFFFFFF), BLU)) }, e { bunting(it, listOf(RED, C(0xFFFFFFFF), BLU)) }, e { soccerBall(it) }, e { trophy(it) }, Amb.CONFETTI),
    ChapterDecor(f { butterflyFloat(it, YEL) }, f { sun(it) }, e { strawberries(it) }, e { watermelonSlices(it) }, Amb.BUTTERFLIES),
    ChapterDecor(f { desertBirds(it) }, f { cloudPuff(it, top = true) }, e { windmill(it, true) }, e { sunflowers(it) }, Amb.NONE),
    ChapterDecor(f { firework(it, listOf(C(0xFFFF5C8A), C(0xFF5CE6FF))) }, f { firework(it, listOf(C(0xFFFFD23F), C(0xFF9AFF6A))) }, null, null, Amb.FIREWORKS),
    ChapterDecor(f { crownFloat(it) }, f { starProp(it, YEL, face = false) }, e { treasureChest(it) }, e { gems(it) }, Amb.SPARKLES, C(0xFFFFD23F)),
    ChapterDecor(f { paperPlane(it, C(0xFFFFD6D6)) }, f { paperPlane(it, C(0xFFD6F0FF)) }, e { paperBoat(it) }, e { paperBoat(it) }, Amb.NONE),
    ChapterDecor(f { crownFloat(it) }, f { starProp(it) }, e { trophy(it) }, e { gifts(it) }, Amb.CONFETTI),
)

/** The props for a backdrop index (10..99), or null for the hand-made chapters 1..10. */
fun chapterDecor(index: Int): ChapterDecor? = CHAPTER_DECOR.getOrNull(index - 10)

// ============================================================================================ ambient motion
private const val T = TAU
internal fun DrawScope.ambient(kind: Amb, t: Float, tint: Color) {
    val w = size.width; val h = size.height
    fun seed(i: Int) = (i * 0.6180339f) % 1f
    when (kind) {
        Amb.NONE -> {}
        Amb.SNOW -> snowfall(t)
        Amb.BUBBLES -> bubbles(t)
        Amb.PETALS -> petals(t)
        Amb.TWINKLE -> twinkles(t)
        Amb.BUTTERFLIES -> butterflies(t)
        Amb.LEAVES -> for (i in 0 until 14) {
            val y = ((seed(i) + t * (0.5f + (i % 4) * 0.1f)) % 1f) * (h + 60f) - 30f
            val x = (seed(i) * 1.9f % 1f) * w + sin(t * T * 2 + i * 1.3f) * 36f
            rotate(t * 360f * (if (i % 2 == 0) 1.5f else -1.5f) + i * 40f, Offset(x, y)) { drawPath(leafPath(22f, 6f).apply { translate(Offset(x - 11f, y)) }, tint.copy(alpha = 0.85f)) }
        }
        Amb.RAIN -> for (i in 0 until 40) {
            val y = ((seed(i) + t * 4f) % 1f) * (h + 80f) - 40f; val x = (seed(i) * 2.3f % 1f) * w + y * 0.2f - h * 0.1f
            drawLine(Color.White.copy(alpha = 0.35f), Offset(x, y), Offset(x - 6f, y + 26f), 2.5f, StrokeCap.Round)
        }
        Amb.FIREFLIES -> for (i in 0 until 16) {
            val x = seed(i) * w + sin(t * T + i * 2f) * 40f; val y = (seed(i + 7) * 0.9f + 0.05f) * h + cos(t * T * 1.3f + i) * 30f
            val a = 0.5f + 0.5f * sin(t * T * 3 + i * 1.7f)
            drawCircle(tint.copy(alpha = 0.25f * a), 14f, Offset(x, y)); drawCircle(tint.copy(alpha = 0.9f * a), 4f, Offset(x, y))
        }
        Amb.EMBERS -> for (i in 0 until 24) {
            val p = (seed(i) + t * (0.6f + (i % 3) * 0.2f)) % 1f
            val x = seed(i + 3) * w + sin(t * T * 2 + i) * 20f; val y = h - p * (h + 40f)
            drawCircle(tint.copy(alpha = (1 - p) * 0.9f), 3f + (i % 3) * 1.5f, Offset(x, y))
        }
        Amb.CONFETTI -> { val cols = listOf(Color(0xFFFF5C8A), Color(0xFF4FC3F7), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFFB39DDB))
            for (i in 0 until 26) {
                val y = ((seed(i) + t * (0.5f + (i % 4) * 0.12f)) % 1f) * (h + 40f) - 20f; val x = (seed(i) * 1.7f % 1f) * w + sin(t * T * 2 + i) * 20f
                rotate(t * 720f + i * 30f, Offset(x, y)) { drawRect(cols[i % cols.size], Offset(x - 7f, y - 4f), Size(14f, 8f * kotlin.math.abs(cos(t * T * 3 + i)) + 2f)) }
            } }
        Amb.NOTES -> for (i in 0 until 8) {
            val p = (seed(i) + t * 0.5f) % 1f; val x = seed(i + 2) * w + sin(t * T * 2 + i) * 30f; val y = h - p * (h + 60f)
            val col = listOf(Color(0xFFFF5CC8), Color(0xFF5CC8FF), Color(0xFFFFD23F))[i % 3].copy(alpha = 1 - p)
            drawOval(col, Offset(x - 9f, y - 6f), Size(18f, 12f)); drawLine(col, Offset(x + 8f, y), Offset(x + 8f, y - 34f), 4f)
        }
        Amb.LANTERNS -> for (i in 0 until 10) {
            val p = (seed(i) + t * 0.35f) % 1f; val x = seed(i + 5) * w + sin(t * T + i) * 16f; val y = h - p * (h + 80f)
            drawCircle(Color(0xFFFFB04A).copy(alpha = 0.2f * (1 - p * 0.5f)), 26f, Offset(x, y))
            drawRoundRect(Color(0xFFFF8A3A).copy(alpha = 0.9f - p * 0.4f), Offset(x - 9f, y - 13f), Size(18f, 26f), androidx.compose.ui.geometry.CornerRadius(7f))
        }
        Amb.METEORS -> { twinkles(t); for (i in 0..1) { val k = (t * 2f + i * 0.5f) % 1f; if (k < 0.2f) { val p = k / 0.2f; val st = Offset(w * (0.9f - i * 0.4f), h * (0.08f + i * 0.2f)); val d = Offset(-w * 0.5f, h * 0.2f); val hd = st + d * p
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 1 - p)), hd - d * 0.3f, hd), hd - d * 0.3f, hd, 3.5f, StrokeCap.Round) } } }
        Amb.FIREWORKS -> { val cols = listOf(Color(0xFFFF5C8A), Color(0xFF5CE6FF), Color(0xFFFFD23F), Color(0xFF9AFF6A))
            for (i in 0..2) { val p = (t * 1.5f + i / 3f) % 1f; val c = Offset(w * (0.2f + i * 0.3f), h * (0.2f + (i % 2) * 0.25f)); val r = 140f * p
                for (k in 0 until 14) { val a = k * T / 14; drawCircle(cols[(i + k) % 4].copy(alpha = 1 - p), 4f, c + Offset(cos(a), sin(a) + p * 0.3f) * r) } } }
        Amb.SPARKLES -> for (i in 0 until 14) {
            val x = seed(i) * w; val y = seed(i + 11) * h; val a = (0.5f + 0.5f * sin(t * T * 2 + i * 2.1f))
            sparkle(Offset(x, y), 10f * a + 2f, tint.copy(alpha = 0.8f * a))
        }
        Amb.HEARTS -> for (i in 0 until 10) {
            val p = (seed(i) + t * 0.4f) % 1f; val x = seed(i + 4) * w + sin(t * T * 2 + i) * 20f; val y = h - p * (h + 40f)
            heart(Offset(x, y), 12f, tint.copy(alpha = 0.8f * (1 - p)))
        }
        Amb.BEES -> for (i in 0..2) beeAt(Offset(w * (0.5f + 0.4f * sin(t * T * (1f + i * 0.3f) + i * 2f)), h * (0.3f + 0.25f * i + 0.08f * sin(t * T * 3 + i))), 14f, t + i * 0.2f)
    }
}
