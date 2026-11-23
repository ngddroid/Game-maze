package com.ngstudio.maze.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ngstudio.maze.R

/** Display font: Lilita One (OFL). Body font: Nunito variable (OFL). Licenses in assets/licenses. */
val Lilita = FontFamily(Font(R.font.lilita_one))

@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

/**
 * Menu palette: warm and natural, to sit on the carved surfaces (honey oak first).
 * (The object keeps its historic name "Candy"; colour names describe roles, not exact hues.)
 */
object Candy {
    val pink = Color(0xFFF2617A)        // rose: highlights, badges
    val pinkDark = Color(0xFFB83A55)
    val plum = Color(0xFF4A3426)        // espresso: icons, dark text
    val plumDark = Color(0xFF2B1D14)
    val lilac = Color(0xFFE9D9C4)       // beige tint for panels
    val sky = Color(0xFF3BA7F0)
    val skyDark = Color(0xFF1F74B8)
    val mint = Color(0xFF3FBF6E)        // green: main action
    val mintDark = Color(0xFF26864A)
    val lemon = Color(0xFFFFC93C)
    val lemonDark = Color(0xFFD18E00)
    val orange = Color(0xFFFF8A3D)
    val orangeDark = Color(0xFFC85A12)
    val cream = Color(0xFFFFF8EF)
    val ink = Color(0xFF4A3426)
    val grey = Color(0xFFCDBEAB)
    val greyDark = Color(0xFF9C8A76)
    val gold = Color(0xFFFFC53D)
    val goldDark = Color(0xFFE08A00)
}

/** Professional chrome on the wood: dark espresso glass, cream text, gold highlights. */
object Pro {
    val chrome = Color(0xFF4A2E1A)
    val chromeDark = Color(0xFF2E1B0E)
    val chromeLip = Color(0xFF1A0E06)
    val glass = Color(0xE62B1A0E)
    val cream = Color(0xFFFFF4E0)
    val creamDim = Color(0xB3FFF4E0)
    val gold = Color(0xFFF6B93B)
    val goldDark = Color(0xFFC98A12)
    val border = Color(0xFFE2C08F)
    val text = Color(0xFF3B2412)
}

@Composable
fun MazeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Candy.pink, secondary = Candy.sky, tertiary = Candy.mint,
            background = Candy.cream, surface = Color.White, onPrimary = Color.White, onSurface = Candy.ink,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Candy.ink),
            bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Candy.ink),
            titleLarge = TextStyle(fontFamily = Lilita, fontSize = 26.sp, color = Candy.ink),
        ),
        content = content,
    )
}
