package com.ngstudio.mazebrush.ui.theme

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
import com.ngstudio.mazebrush.R

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
    val plum = Color(0xFF263A46)        // deep slate: icons, dark text (was espresso brown before the clean redesign)
    val plumDark = Color(0xFF17232C)
    val lilac = Color(0xFFEDE1D3)       // warm light tint for panels
    val sky = Color(0xFF3BA7F0)
    val skyDark = Color(0xFF1F74B8)
    val mint = Color(0xFF1FA39B)        // teal: main action (clean redesign; was lime green)
    val mintDark = Color(0xFF12746E)
    val lemon = Color(0xFFFFC93C)
    val lemonDark = Color(0xFFD18E00)
    val orange = Color(0xFFFF8A3D)
    val orangeDark = Color(0xFFC85A12)
    val cream = Color(0xFFFFF8EF)
    val ink = Color(0xFF263A46)
    val grey = Color(0xFFD3D9DE)
    val greyDark = Color(0xFF8E9BA5)
    val gold = Color(0xFFFFC53D)
    val goldDark = Color(0xFFE08A00)
}

/** Chrome on the wood: warm polished-wood buttons and bars, cream text, gold-rimmed cream cards. */
object Pro {
    // 2026-11-25: the old "wood" chrome is mapped onto the clean palette (teal chrome, white text, soft rims), so every
    // screen and dialog that still uses Pro matches the new look. The names are kept to avoid touching every caller.
    val chrome = Color(0xFF1FA39B)
    val chromeDark = Color(0xFF168880)
    val chromeLip = Color(0xFF0E5E58)
    val glass = Color(0xEE263A46)
    val cream = Color(0xFFFFFFFF)
    val creamDim = Color(0xD9FFFFFF)
    val gold = Color(0xFFFFC94D)
    val goldDark = Color(0xFFC98A12)
    val border = Color(0xFFEDE1D3)
    /** Card rim: barely-there warm greys (it was gold). */
    val rimLight = Color(0xFFF6EFE6)
    val rimDark = Color(0xFFE6D8C8)
    val text = Color(0xFF263A46)
}

/**
 * Clean style (2026-11-25 redesign): a soft warm-cream background, a raised slate board, teal round buttons
 * with white icons, white pills, deep-slate text, coral highlights. Our own palette; the layout ideas come
 * from the hyper-casual genre (see CLAUDE.md §6).
 */
object Clean {
    val bgTop = Color(0xFFFFF6EC)
    val bgBottom = Color(0xFFF5E5D5)
    val ink = Color(0xFF263A46)
    val inkSoft = Color(0xFF5E7280)
    val teal = Color(0xFF1FA39B)
    val tealLight = Color(0xFF3FC4BA)
    val tealDark = Color(0xFF12746E)
    val coral = Color(0xFFFF6B5B)
    val coralDark = Color(0xFFD94A3C)
    val pill = Color(0xFFFFFFFF)
    val pillShadow = Color(0xFFD9C4AE)
    val track = Color(0xFFEDE1D3)
    val gold = Color(0xFFFFC53D)
    val cardTop = Color(0xFFFFFFFF)
    val cardBottom = Color(0xFFFFF3E6)
    val cardBorder = Color(0xFFF1E3D2)
    val amber = Color(0xFFFFA62E)
    val amberDark = Color(0xFFE07C12)
    val sky = Color(0xFF4FA8FF)
    val skyDark = Color(0xFF2E7FD1)
    val slate = Color(0xFF6E8392)
    val slateDark = Color(0xFF4A5E6C)
    val navTop = Color(0xFFFFFFFF)
    val navBottom = Color(0xFFFCEFE0)
    val selTint = Color(0x1F1FA39B)
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
