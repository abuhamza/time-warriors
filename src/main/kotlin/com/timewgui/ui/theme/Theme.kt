package com.timewgui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Base spacing unit (4px grid). */
object TimewSpacing {
    val unit: Dp = 4.dp
}

/** Layout and dimension constants. */
object TimewDimensions {
    val componentPaddingSmall: Dp = 8.dp
    val componentPaddingMedium: Dp = 12.dp
    val sectionGap: Dp = 16.dp
    val sidebarWidth: Dp = 200.dp
    val sidebarWidthCollapsed: Dp = 56.dp
    val borderRadiusInput: Dp = 6.dp
    val borderRadiusCard: Dp = 12.dp
    val borderRadiusTimeline: Dp = 0.dp
    val bannerHeight: Dp = 180.dp
}

/** Accessor for TimewColors and typography, analogous to MaterialTheme. */
object TimewTheme {
    val colors: TimewColors
        @Composable
        get() = LocalTimewColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography

    val colorScheme: androidx.compose.material3.ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme
}

/**
 * Main theme composable for TimewGUI.
 * Applies African savanna color palette and provides [TimewColors] via [LocalTimewColors].
 */
@Composable
fun TimewGuiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val timewColors = if (darkTheme) DarkTimewColors else LightTimewColors

    CompositionLocalProvider(LocalTimewColors provides timewColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DefaultTypography,
            content = content,
        )
    }
}
