package com.mid.varagh.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shared brand palette (FitSho, 504 Daily, Timeboxing, Varagh): light-grey canvas, flat white
// cards, charcoal buttons, near-black text, grey captions, grey filled inputs and a single green
// accent for goals/success. Keep these hex values in sync with the other apps' colors.xml.
// Used when dynamic colour is unavailable (API < 31) or turned off (the default).

// Light
internal val Ink = Color(0xFF111111)
internal val Charcoal = Color(0xFF2B2B2B)
internal val Slate = Color(0xFF8A8A8A)
internal val ContainerHigh = Color(0xFFEDEDED)
internal val Outline = Color(0xFFE6E6E6)
internal val Hairline = Color(0xFFEEEEEE)
internal val Field = Color(0xFFF4F4F4)
internal val Canvas = Color(0xFFF7F7F7)
internal val Snow = Color(0xFFFFFFFF)

// Dark: the same monochrome, inverted.
internal val NightCanvas = Color(0xFF0C0C0C)
internal val NightCard = Color(0xFF181818)
internal val NightField = Color(0xFF262626)
internal val NightOutline = Color(0xFF2E2E2E)
internal val NightButton = Color(0xFFF2F2F2)
internal val NightText = Color(0xFFF4F4F4)
internal val NightSlate = Color(0xFF9A9A9A)

// Accent (goals, streaks, success) and error.
internal val Green = Color(0xFF1FB51F)
internal val GreenContainer = Color(0xFFE9FBE9)
internal val OnGreenContainer = Color(0xFF118A11)
internal val GreenDark = Color(0xFF3EE03A)
internal val GreenContainerDark = Color(0xFF10301A)
internal val OnGreenContainerDark = Color(0xFF7EE88A)
internal val Red = Color(0xFFC62828)
internal val RedContainer = Color(0xFFFDECEC)
internal val RedDark = Color(0xFFFF8A80)
internal val RedContainerDark = Color(0xFF3A1414)

val VaraghLightColors = lightColorScheme(
    primary = Charcoal,
    onPrimary = Snow,
    primaryContainer = Field,
    onPrimaryContainer = Ink,
    inversePrimary = NightButton,
    secondary = Ink,
    onSecondary = Snow,
    secondaryContainer = Field,
    onSecondaryContainer = Ink,
    tertiary = Green,
    onTertiary = Snow,
    tertiaryContainer = GreenContainer,
    onTertiaryContainer = OnGreenContainer,
    error = Red,
    onError = Snow,
    errorContainer = RedContainer,
    onErrorContainer = Red,
    background = Canvas,
    onBackground = Ink,
    surface = Snow,
    onSurface = Ink,
    surfaceVariant = Field,
    onSurfaceVariant = Slate,
    surfaceTint = Color.Transparent,
    outline = Outline,
    outlineVariant = Hairline,
    surfaceContainerLowest = Snow,
    surfaceContainerLow = Snow,
    surfaceContainer = Snow,
    surfaceContainerHigh = Field,
    surfaceContainerHighest = ContainerHigh,
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
    secondary = NightText,
    onSecondary = Ink,
    secondaryContainer = NightField,
    onSecondaryContainer = NightText,
    tertiary = GreenDark,
    onTertiary = Ink,
    tertiaryContainer = GreenContainerDark,
    onTertiaryContainer = OnGreenContainerDark,
    error = RedDark,
    onError = Ink,
    errorContainer = RedContainerDark,
    onErrorContainer = RedDark,
    background = NightCanvas,
    onBackground = NightText,
    surface = NightCard,
    onSurface = NightText,
    surfaceVariant = NightField,
    onSurfaceVariant = NightSlate,
    surfaceTint = Color.Transparent,
    outline = NightOutline,
    outlineVariant = NightOutline,
    surfaceContainerLowest = NightCanvas,
    surfaceContainerLow = NightCard,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightField,
    surfaceContainerHighest = NightOutline,
    surfaceDim = NightCanvas,
    surfaceBright = NightField,
    inverseSurface = NightField,
    inverseOnSurface = NightText,
    scrim = Color.Black,
)
