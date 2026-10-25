package com.bytebench.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bytebench.app.R

/**
 * The website's three skins (public/css/site.css). Every colour on screen comes from
 * these tokens, just like the site's CSS variables.
 */
@Immutable
data class Skin(
    val key: String,
    val label: String,
    val dark: Boolean,
    val paper: Color,
    val surface: Color,
    val board: Color,
    val ink: Color,
    val steel: Color,
    val rule: Color,
    val accent: Color,
    val accentInk: Color,
    val gold: Color,
    val goldInk: Color,
    val green: Color,
    val danger: Color,
    val iconBg: Color,
    val iconInk: Color,
    val heroBg: Color,
    val heroInk: Color,
    val heroMuted: Color,
    val heroBtn: Color,
    val heroBtnInk: Color,
    val bandBg: Color,
    val bandInk: Color,
    val bandMuted: Color,
    val tagBg: Color,
    val tagInk: Color,
    val tagMuted: Color,
    val sans: FontFamily,
    val display: FontFamily,
    val displayWeight: FontWeight,
    val displayTracking: Float,
)

private fun variable(id: Int, weight: Int) =
    Font(id, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

private fun family(id: Int) = FontFamily(listOf(300, 400, 500, 600, 700, 800).map { variable(id, it) })

val Hanken = family(R.font.hanken_grotesk)
val Manrope = family(R.font.manrope)
val Newsreader = FontFamily(listOf(400, 500, 600, 700).map { variable(R.font.newsreader, it) })
val SourceSerif = FontFamily(listOf(400, 600, 700).map { variable(R.font.source_serif, it) })

val Porcelain = Skin(
    key = "porcelain", label = "Porcelain", dark = false,
    paper = Color(0xFFF6F7F9), surface = Color(0xFFFFFFFF), board = Color(0xFFEEF1F5),
    ink = Color(0xFF0F1B2D), steel = Color(0xFF5A6577), rule = Color(0xFFE3E7EE),
    accent = Color(0xFF2448C4), accentInk = Color.White,
    gold = Color(0xFFE7B53C), goldInk = Color(0xFF241A00),
    green = Color(0xFF1D7A4E), danger = Color(0xFFB42318),
    iconBg = Color(0xFF0F1B2D), iconInk = Color(0xFFE7B53C),
    heroBg = Color(0xFFFFFFFF), heroInk = Color(0xFF0F1B2D), heroMuted = Color(0xFF586377),
    heroBtn = Color(0xFF0F1B2D), heroBtnInk = Color.White,
    bandBg = Color(0xFF0F1B2D), bandInk = Color(0xFFF2F4F8), bandMuted = Color(0xFFA6B0C0),
    tagBg = Color.White, tagInk = Color(0xFF0F1B2D), tagMuted = Color(0xFF5A6577),
    sans = Hanken, display = Hanken, displayWeight = FontWeight(750), displayTracking = -0.03f,
)

val Ink = Skin(
    key = "ink", label = "Ink & Brass", dark = true,
    paper = Color(0xFF0B0F16), surface = Color(0xFF121823), board = Color(0xFF1A2130),
    ink = Color(0xFFECEEF2), steel = Color(0xFF9BA4B4), rule = Color(0xFF232C3B),
    accent = Color(0xFFD4B46E), accentInk = Color(0xFF17120A),
    gold = Color(0xFFC9A55C), goldInk = Color(0xFF17120A),
    green = Color(0xFF5CC49A), danger = Color(0xFFFF8F80),
    iconBg = Color(0xFF1B2331), iconInk = Color(0xFFD4B46E),
    heroBg = Color(0xFF0B0F16), heroInk = Color(0xFFF1F2F5), heroMuted = Color(0xFFA0A9B8),
    heroBtn = Color(0xFFC9A55C), heroBtnInk = Color(0xFF17120A),
    bandBg = Color(0xFF10161F), bandInk = Color(0xFFF1F2F5), bandMuted = Color(0xFFA0A9B8),
    tagBg = Color(0xFFF4F0E6), tagInk = Color(0xFF141A24), tagMuted = Color(0xFF5D6573),
    sans = Hanken, display = Newsreader, displayWeight = FontWeight.Medium, displayTracking = -0.02f,
)

val Emerald = Skin(
    key = "emerald", label = "Emerald", dark = false,
    paper = Color(0xFFF3F5F3), surface = Color(0xFFFFFFFF), board = Color(0xFFE8EEEA),
    ink = Color(0xFF102620), steel = Color(0xFF586A63), rule = Color(0xFFDCE4DF),
    accent = Color(0xFF0D6B51), accentInk = Color.White,
    gold = Color(0xFFC99A3D), goldInk = Color(0xFF1F1600),
    green = Color(0xFF17805A), danger = Color(0xFFB42318),
    iconBg = Color(0xFF0B2A22), iconInk = Color(0xFFD4A64A),
    heroBg = Color(0xFF0B2A22), heroInk = Color(0xFFF3F7F4), heroMuted = Color(0xFFA7C0B6),
    heroBtn = Color(0xFFD4A64A), heroBtnInk = Color(0xFF1B1300),
    bandBg = Color(0xFF0B2A22), bandInk = Color(0xFFF3F7F4), bandMuted = Color(0xFFA7C0B6),
    tagBg = Color.White, tagInk = Color(0xFF102620), tagMuted = Color(0xFF586A63),
    sans = Manrope, display = Manrope, displayWeight = FontWeight.ExtraBold, displayTracking = -0.032f,
)

val Skins = listOf(Porcelain, Ink, Emerald)

val LocalSkin = staticCompositionLocalOf { Ink }

/** Resolves the saved choice; "auto" follows the phone: Porcelain by day, Ink & Brass at night. */
@Composable
fun resolveSkin(choice: String): Skin = when (choice) {
    "porcelain" -> Porcelain
    "ink" -> Ink
    "emerald" -> Emerald
    else -> if (isSystemInDarkTheme()) Ink else Porcelain
}

private fun Skin.colorScheme(): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent, onPrimary = accentInk,
        secondary = gold, onSecondary = goldInk,
        tertiary = green,
        background = paper, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = board, onSurfaceVariant = steel,
        surfaceContainer = surface, surfaceContainerLow = surface, surfaceContainerHigh = board,
        surfaceContainerHighest = board, surfaceContainerLowest = paper,
        outline = rule, outlineVariant = rule,
        error = danger,
        secondaryContainer = board, onSecondaryContainer = ink,
        primaryContainer = board, onPrimaryContainer = ink,
    )
}

private fun Skin.typography(): Typography {
    val body = TextStyle(fontFamily = sans, color = Color.Unspecified)
    val head = TextStyle(fontFamily = display, fontWeight = displayWeight, letterSpacing = displayTracking.em)
    return Typography(
        displayLarge = head.copy(fontSize = 40.sp, lineHeight = 44.sp),
        displayMedium = head.copy(fontSize = 34.sp, lineHeight = 38.sp),
        displaySmall = head.copy(fontSize = 30.sp, lineHeight = 34.sp),
        headlineLarge = head.copy(fontSize = 28.sp, lineHeight = 33.sp),
        headlineMedium = head.copy(fontSize = 24.sp, lineHeight = 29.sp),
        headlineSmall = head.copy(fontSize = 21.sp, lineHeight = 26.sp),
        titleLarge = body.copy(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold),
        titleMedium = body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = body.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = body.copy(fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium = body.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
        bodySmall = body.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = body.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = body.copy(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = body.copy(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em),
    )
}

@Composable
fun BytebenchTheme(skin: Skin, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(colorScheme = skin.colorScheme(), typography = skin.typography(), content = content)
    }
}
