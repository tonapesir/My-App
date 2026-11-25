package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SgsDarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DeepNavy,
    primaryContainer = DarkSgsColors.goldContainer,
    onPrimaryContainer = DarkSgsColors.goldLight,
    secondary = DarkSgsColors.goldLight,
    onSecondary = DeepNavy,
    secondaryContainer = DarkSgsColors.navy3,
    onSecondaryContainer = DarkSgsColors.textCream,
    tertiary = Emerald,
    onTertiary = Color.White,
    background = DarkSgsColors.screenBg,
    onBackground = DarkSgsColors.textCream,
    surface = DarkSgsColors.navy,
    onSurface = DarkSgsColors.textCream,
    surfaceVariant = DarkSgsColors.navy3,
    onSurfaceVariant = DarkSgsColors.textMuted,
    surfaceContainerLowest = DarkSgsColors.screenBg,
    surfaceContainerLow = DarkSgsColors.navySurface,
    surfaceContainer = DarkSgsColors.navy,
    surfaceContainerHigh = DarkSgsColors.navy2,
    surfaceContainerHighest = DarkSgsColors.navy3,
    outline = GoldDark,
    outlineVariant = DarkSgsColors.navy2,
    error = Crimson,
    onError = DarkSgsColors.softRed,
    errorContainer = DarkSgsColors.errorBg,
    onErrorContainer = DarkSgsColors.softRed
)

private val SgsLightColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = DeepNavy,
    primaryContainer = LightSgsColors.goldContainer,
    onPrimaryContainer = LightSgsColors.goldLight,
    secondary = RoyalBlue,
    onSecondary = Color.White,
    secondaryContainer = LightSgsColors.navy3,
    onSecondaryContainer = LightSgsColors.textCream,
    tertiary = Emerald,
    onTertiary = Color.White,
    background = LightSgsColors.screenBg,
    onBackground = LightSgsColors.textCream,
    surface = LightSgsColors.navy,
    onSurface = LightSgsColors.textCream,
    surfaceVariant = LightSgsColors.navy3,
    onSurfaceVariant = LightSgsColors.textMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSgsColors.navySurface,
    surfaceContainer = LightSgsColors.navy,
    surfaceContainerHigh = LightSgsColors.navy2,
    surfaceContainerHighest = LightSgsColors.navy3,
    outline = GoldDark,
    outlineVariant = LightSgsColors.navy2,
    error = Crimson,
    onError = Color.White,
    errorContainer = LightSgsColors.errorBg,
    onErrorContainer = LightSgsColors.softRed
)

object ColorTokens {
    val ErrorBg: Color
        @Composable @ReadOnlyComposable get() = LocalSgsColors.current.errorBg
    val SuccessBg: Color
        @Composable @ReadOnlyComposable get() = LocalSgsColors.current.successBg
    val WarningBg: Color
        @Composable @ReadOnlyComposable get() = LocalSgsColors.current.warningBg
    val WarningText: Color
        @Composable @ReadOnlyComposable get() = LocalSgsColors.current.warningText
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SGSSchoolTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val semantic = if (darkTheme) DarkSgsColors else LightSgsColors
    val scheme = if (darkTheme) SgsDarkColorScheme else SgsLightColorScheme
    CompositionLocalProvider(LocalSgsColors provides semantic) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
