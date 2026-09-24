package com.mid.varagh.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Monochrome palette: light-grey canvas, white rounded cards, near-black text and buttons,
// grey secondary labels, grey filled inputs and a single green accent for charts/goals.
// Used when dynamic colour is unavailable (API < 31) or turned off (the default).

internal val Ink = Color(0xFF111111)
internal val Charcoal = Color(0xFF2B2B2B)
internal val Graphite = Color(0xFF3A3A3A)
internal val Slate = Color(0xFF8A8A8A)
internal val Silver = Color(0xFFBDBDBD)
internal val Mist = Color(0xFFE6E6E6)
internal val Hairline = Color(0xFFEFEFEF)
internal val Field = Color(0xFFF4F4F4)
internal val Canvas = Color(0xFFF7F7F7)
internal val Snow = Color(0xFFFFFFFF)

internal val NightCanvas = Color(0xFF0E0E0E)
internal val NightCard = Color(0xFF1A1A1A)
internal val NightField = Color(0xFF262626)
internal val NightHairline = Color(0xFF2A2A2A)
internal val NightButton = Color(0xFFEDEDED)
internal val NightText = Color(0xFFF2F2F2)
internal val NightSlate = Color(0xFF9E9E9E)

internal val Green = Color(0xFF2ECC40)
internal val GreenDark = Color(0xFF4ADE5A)
internal val Red = Color(0xFFD32F2F)
internal val RedDark = Color(0xFFFF8A80)

val VaraghLightColors = lightColorScheme(
    primary = Charcoal,
    onPrimary = Snow,
    primaryContainer = Mist,
    onPrimaryContainer = Ink,
    inversePrimary = NightButton,
    secondary = Graphite,
    onSecondary = Snow,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Green,
    onTertiary = Snow,
    tertiaryContainer = Color(0xFFDDF7E0),
    onTertiaryContainer = Color(0xFF0B3D12),
    error = Red,
    onError = Snow,
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF5F1414),
    background = Canvas,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Field,
    onSurfaceVariant = Slate,
    surfaceTint = Color.Transparent,
    outline = Silver,
    outlineVariant = Hairline,
    surfaceContainerLowest = Snow,
    surfaceContainerLow = Snow,
    surfaceContainer = Snow,
    surfaceContainerHigh = Field,
    surfaceContainerHighest = Mist,
    surfaceDim = Canvas,
    surfaceBright = Snow,
    inverseSurface = Charcoal,
    inverseOnSurface = Snow,
    scrim = Color.Black,
)

val VaraghDarkColors = darkColorScheme(
    primary = NightButton,
    onPrimary = Ink,
    primaryContainer = NightField,
    onPrimaryContainer = NightText,
    inversePrimary = Charcoal,
    secondary = Silver,
    onSecondary = Ink,
    secondaryContainer = NightField,
    onSecondaryContainer = NightText,
    tertiary = GreenDark,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF14391A),
    onTertiaryContainer = Color(0xFFC8F5CE),
    error = RedDark,
    onError = Ink,
    errorContainer = Color(0xFF5F1414),
    onErrorContainer = Color(0xFFFDE2E2),
    background = NightCanvas,
    onBackground = NightText,
    surface = NightCard,
    onSurface = NightText,
    surfaceVariant = NightField,
    onSurfaceVariant = NightSlate,
    surfaceTint = Color.Transparent,
    outline = Color(0xFF5A5A5A),
    outlineVariant = NightHairline,
    surfaceContainerLowest = NightCanvas,
    surfaceContainerLow = NightCard,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightField,
    surfaceContainerHighest = Color(0xFF303030),
    surfaceDim = NightCanvas,
    surfaceBright = Color(0xFF2C2C2C),
    inverseSurface = NightText,
    inverseOnSurface = Ink,
    scrim = Color.Black,
)
