package com.timewgui.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** CompositionLocal for [TimewColors]. Provided by [TimewGuiTheme]. */
val LocalTimewColors = compositionLocalOf<TimewColors> {
    error("No TimewColors provided. Wrap content in TimewGuiTheme.")
}

// ---- Design tokens: Light Mode (Warm Savanna Light) ----
private val LightBgPrimary = Color(0xFFFFF8ED)
private val LightBgSecondary = Color(0xFFF0E0CC)
private val LightBgTertiary = Color(0xFFE8D2B8)
private val LightBorder = Color(0xFFD4B896)
private val LightTextPrimary = Color(0xFF2D1B0E)
private val LightTextSecondary = Color(0xFF6B4A30)
private val LightTextTertiary = Color(0xFF9B7A5A)
private val LightAccent = Color(0xFFD4760A)
private val LightSuccess = Color(0xFF4A8C2F)
private val LightDestructive = Color(0xFFB83A2A)
private val LightWarning = Color(0xFFD4A62B)
private val LightCardSurface = Color(0xFFFFFFFF)

// ---- Design tokens: Dark Mode (African Savanna) ----
private val DarkBgPrimary = Color(0xFF2D1B0E)
private val DarkBgSecondary = Color(0xFF1A0F08)
private val DarkBgTertiary = Color(0xFF3D2A1A)
private val DarkBorder = Color(0xFF5A3D28)
private val DarkTextPrimary = Color(0xFFF5E6D0)
private val DarkTextSecondary = Color(0xFFC4956A)
private val DarkTextTertiary = Color(0xFF8B6840)
private val DarkAccent = Color(0xFFD4760A)
private val DarkSuccess = Color(0xFF5A9C3F)
private val DarkDestructive = Color(0xFFD45A4A)
private val DarkWarning = Color(0xFFD4A62B)
private val DarkCardSurface = Color(0xFFFFF5E8)

// ---- Tag color slots: warm earth-tone palette ----
private val Slot1Light = Color(0xFFB83A2A)   // Rust red
private val Slot1Dark = Color(0xFFD45A4A)
private val Slot2Light = Color(0xFF4A7C2F)   // Forest green
private val Slot2Dark = Color(0xFF6A9C4F)
private val Slot3Light = Color(0xFFB8860B)   // Deep amber
private val Slot3Dark = Color(0xFFD4A62B)
private val Slot4Light = Color(0xFF7A4A6A)   // Mauve
private val Slot4Dark = Color(0xFF9A6A8A)
private val Slot5Light = Color(0xFF2A7A6A)   // Teal
private val Slot5Dark = Color(0xFF4A9A8A)
private val Slot6Light = Color(0xFF8A2A5A)   // Berry
private val Slot6Dark = Color(0xFFAA4A7A)
private val Slot7Light = Color(0xFF6B6B20)   // Olive
private val Slot7Dark = Color(0xFF8B8B40)
private val Slot8Light = Color(0xFFC4652A)   // Burnt sienna
private val Slot8Dark = Color(0xFFE4854A)

/** Material3 light color scheme mapped to warm savanna tokens. */
val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightAccent.copy(alpha = 0.15f),
    onPrimaryContainer = LightAccent,
    secondary = LightTextSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightBgTertiary,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightSuccess,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightSuccess.copy(alpha = 0.15f),
    onTertiaryContainer = LightSuccess,
    error = LightDestructive,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightDestructive.copy(alpha = 0.15f),
    onErrorContainer = LightDestructive,
    background = LightBgPrimary,
    onBackground = LightTextPrimary,
    surface = LightBgSecondary,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBgTertiary,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.6f),
    surfaceTint = LightAccent,
)

/** Material3 dark color scheme mapped to African savanna tokens. */
val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = DarkAccent.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFFF5D0A0),
    secondary = DarkTextSecondary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = DarkBgTertiary,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkSuccess,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = DarkSuccess.copy(alpha = 0.25f),
    onTertiaryContainer = Color(0xFFA0E0A0),
    error = DarkDestructive,
    onError = Color(0xFF000000),
    errorContainer = DarkDestructive.copy(alpha = 0.25f),
    onErrorContainer = Color(0xFFFECACA),
    background = DarkBgPrimary,
    onBackground = DarkTextPrimary,
    surface = DarkBgSecondary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgTertiary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.6f),
    surfaceTint = DarkAccent,
)

/**
 * Custom color tokens beyond Material3, themed for the African savanna palette.
 * Access via [LocalTimewColors] or [TimewTheme.colors].
 *
 * [textOnCardPrimary], [textOnCardSecondary], [textOnCardTertiary] and [borderOnCard]
 * must always contrast legibly against [cardSurface].
 */
data class TimewColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgTertiary: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val success: Color,
    val destructive: Color,
    val warning: Color,
    val cardSurface: Color,
    val textOnCardPrimary: Color,
    val textOnCardSecondary: Color,
    val textOnCardTertiary: Color,
    val borderOnCard: Color,
    val tagColors: List<TagColorPair>,
)

/** Pair of light and dark colors for a single tag slot. */
data class TagColorPair(
    val light: Color,
    val dark: Color,
)

private val TagColorPairsList: List<TagColorPair> = listOf(
    TagColorPair(Slot1Light, Slot1Dark),
    TagColorPair(Slot2Light, Slot2Dark),
    TagColorPair(Slot3Light, Slot3Dark),
    TagColorPair(Slot4Light, Slot4Dark),
    TagColorPair(Slot5Light, Slot5Dark),
    TagColorPair(Slot6Light, Slot6Dark),
    TagColorPair(Slot7Light, Slot7Dark),
    TagColorPair(Slot8Light, Slot8Dark),
)

/** Light mode TimewColors (Warm Savanna Light). */
val LightTimewColors = TimewColors(
    bgPrimary = LightBgPrimary,
    bgSecondary = LightBgSecondary,
    bgTertiary = LightBgTertiary,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accent = LightAccent,
    success = LightSuccess,
    destructive = LightDestructive,
    warning = LightWarning,
    cardSurface = LightCardSurface,
    textOnCardPrimary = LightTextPrimary,
    textOnCardSecondary = LightTextSecondary,
    textOnCardTertiary = LightTextTertiary,
    borderOnCard = LightBorder,
    tagColors = TagColorPairsList,
)

/** Dark mode TimewColors (African Savanna). */
val DarkTimewColors = TimewColors(
    bgPrimary = DarkBgPrimary,
    bgSecondary = DarkBgSecondary,
    bgTertiary = DarkBgTertiary,
    border = DarkBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accent = DarkAccent,
    success = DarkSuccess,
    destructive = DarkDestructive,
    warning = DarkWarning,
    cardSurface = DarkCardSurface,
    textOnCardPrimary = LightTextPrimary,
    textOnCardSecondary = LightTextSecondary,
    textOnCardTertiary = LightTextTertiary,
    borderOnCard = LightBorder,
    tagColors = TagColorPairsList,
)

/** Tag color pairs for auto-assignment. */
val TagColorPairs: List<TagColorPair> get() = TagColorPairsList
