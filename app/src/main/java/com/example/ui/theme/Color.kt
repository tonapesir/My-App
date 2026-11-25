package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Constant brand accents & dark ink (used on Gold/WhatsApp buttons, Peach inputs, and photo overlays)
val DeepNavy = Color(0xFF0D1B35)
val Gold = Color(0xFFD4902A)
val GoldDark = Color(0xFF9C6615)
val Peach = Color(0xFFFDF0E6)
val PeachBorder = Color(0xFFD4A87A)
val WarmOrange = Color(0xFFC0521A)
val Emerald = Color(0xFF1A7A3A)
val Crimson = Color(0xFF9A2424)
val RoyalBlue = Color(0xFF1A4A7A)
val WhatsAppGreen = Color(0xFF25D366)

@Immutable
data class SgsSemanticColors(
    val isDark: Boolean,
    val screenBg: Color,
    val navy: Color,
    val navy2: Color,
    val navy3: Color,
    val navySurface: Color,
    val goldLight: Color,
    val goldContainer: Color,
    val textCream: Color,
    val textMuted: Color,
    val mintGreen: Color,
    val softRed: Color,
    val skyBlue: Color,
    val errorBg: Color,
    val successBg: Color,
    val warningBg: Color,
    val warningText: Color
)

val DarkSgsColors = SgsSemanticColors(
    isDark = true,
    screenBg = Color(0xFF0D1B35),
    navy = Color(0xFF1A2A4A),
    navy2 = Color(0xFF223262),
    navy3 = Color(0xFF2C3E6B),
    navySurface = Color(0xFF152340),
    goldLight = Color(0xFFE8B05A),
    goldContainer = Color(0xFF3E2D12),
    textCream = Color(0xFFF0E8D8),
    textMuted = Color(0xFFC2B9A7),
    mintGreen = Color(0xFF7EF7A0),
    softRed = Color(0xFFFFB0B0),
    skyBlue = Color(0xFF8FD0FF),
    errorBg = Color(0xFF4A1A1A),
    successBg = Color(0xFF1A4A2A),
    warningBg = Color(0xFF4A3A14),
    warningText = Color(0xFFFFD98A)
)

val LightSgsColors = SgsSemanticColors(
    isDark = false,
    screenBg = Color(0xFFF2F5FA),
    navy = Color(0xFFFFFFFF),
    navy2 = Color(0xFFECF0F8),
    navy3 = Color(0xFFE0E7F3),
    navySurface = Color(0xFFFAFBFD),
    goldLight = Color(0xFF8C5300),
    goldContainer = Color(0xFFFFEFC7),
    textCream = Color(0xFF14213D),
    textMuted = Color(0xFF51607A),
    mintGreen = Color(0xFF116D33),
    softRed = Color(0xFFB31E29),
    skyBlue = Color(0xFF10559A),
    errorBg = Color(0xFFFCE8EA),
    successBg = Color(0xFFE4F4EB),
    warningBg = Color(0xFFFFF3D6),
    warningText = Color(0xFF7D4E00)
)

val LocalSgsColors = staticCompositionLocalOf { DarkSgsColors }

val ScreenBg: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.screenBg

val Navy: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.navy

val Navy2: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.navy2

val Navy3: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.navy3

val NavySurface: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.navySurface

val GoldLight: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.goldLight

val GoldContainer: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.goldContainer

val TextCream: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.textCream

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.textMuted

val MintGreen: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.mintGreen

val SoftRed: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.softRed

val SkyBlue: Color
    @Composable @ReadOnlyComposable get() = LocalSgsColors.current.skyBlue
