package com.mid.varagh.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A calm, book-themed palette: leather-brown primary, olive secondary, ink-teal tertiary on paper.
// Used when dynamic colour is unavailable (API < 31) or turned off.

internal val Leather10 = Color(0xFF2E1500)
internal val Leather20 = Color(0xFF4A2800)
internal val Leather30 = Color(0xFF653D16)
internal val Leather40 = Color(0xFF7F542B)
internal val Leather80 = Color(0xFFF2BC8C)
internal val Leather90 = Color(0xFFFFDCC2)

internal val Olive10 = Color(0xFF1A1E0E)
internal val Olive20 = Color(0xFF2F3321)
internal val Olive30 = Color(0xFF454A36)
internal val Olive40 = Color(0xFF5D624C)
internal val Olive80 = Color(0xFFC6CAAF)
internal val Olive90 = Color(0xFFE2E6CA)

internal val Ink10 = Color(0xFF001F24)
internal val Ink20 = Color(0xFF00363D)
internal val Ink30 = Color(0xFF1F4D54)
internal val Ink40 = Color(0xFF38656C)
internal val Ink80 = Color(0xFFA0CFD6)
internal val Ink90 = Color(0xFFBCEBF2)

internal val Red10 = Color(0xFF410002)
internal val Red20 = Color(0xFF690005)
internal val Red30 = Color(0xFF93000A)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

internal val Paper = Color(0xFFFFF8F3)
internal val PaperOn = Color(0xFF221A14)
internal val PaperVariant = Color(0xFFF3DFD2)
internal val PaperVariantOn = Color(0xFF52443A)
internal val PaperOutline = Color(0xFF857468)
internal val PaperOutlineVariant = Color(0xFFD7C3B5)
internal val PaperContainerLowest = Color(0xFFFFFFFF)
internal val PaperContainerLow = Color(0xFFFFF1E8)
internal val PaperContainer = Color(0xFFF9EBE2)
internal val PaperContainerHigh = Color(0xFFF4E5DC)
internal val PaperContainerHighest = Color(0xFFEEE0D7)

internal val Night = Color(0xFF19120C)
internal val NightOn = Color(0xFFEFE0D6)
internal val NightVariant = Color(0xFF52443A)
internal val NightVariantOn = Color(0xFFD7C3B5)
internal val NightOutline = Color(0xFFA08D80)
internal val NightOutlineVariant = Color(0xFF52443A)
internal val NightContainerLowest = Color(0xFF130D07)
internal val NightContainerLow = Color(0xFF221A14)
internal val NightContainer = Color(0xFF261E18)
internal val NightContainerHigh = Color(0xFF312822)
internal val NightContainerHighest = Color(0xFF3C332C)

val VaraghLightColors = lightColorScheme(
    primary = Leather40,
    onPrimary = Color.White,
    primaryContainer = Leather90,
    onPrimaryContainer = Leather30,
    secondary = Olive40,
    onSecondary = Color.White,
    secondaryContainer = Olive90,
    onSecondaryContainer = Olive30,
    tertiary = Ink40,
    onTertiary = Color.White,
    tertiaryContainer = Ink90,
    onTertiaryContainer = Ink30,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red30,
    background = Paper,
    onBackground = PaperOn,
    surface = Paper,
    onSurface = PaperOn,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = PaperVariantOn,
    outline = PaperOutline,
    outlineVariant = PaperOutlineVariant,
    surfaceContainerLowest = PaperContainerLowest,
    surfaceContainerLow = PaperContainerLow,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperContainerHigh,
    surfaceContainerHighest = PaperContainerHighest,
    inverseSurface = Color(0xFF382E28),
    inverseOnSurface = Color(0xFFFFEDE3),
    inversePrimary = Leather80,
)

val VaraghDarkColors = darkColorScheme(
    primary = Leather80,
    onPrimary = Leather20,
    primaryContainer = Leather30,
    onPrimaryContainer = Leather90,
    secondary = Olive80,
    onSecondary = Olive20,
    secondaryContainer = Olive30,
    onSecondaryContainer = Olive90,
    tertiary = Ink80,
    onTertiary = Ink20,
    tertiaryContainer = Ink30,
    onTertiaryContainer = Ink90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Night,
    onBackground = NightOn,
    surface = Night,
    onSurface = NightOn,
    surfaceVariant = NightVariant,
    onSurfaceVariant = NightVariantOn,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    surfaceContainerLowest = NightContainerLowest,
    surfaceContainerLow = NightContainerLow,
    surfaceContainer = NightContainer,
    surfaceContainerHigh = NightContainerHigh,
    surfaceContainerHighest = NightContainerHighest,
    inverseSurface = NightOn,
    inverseOnSurface = Color(0xFF382E28),
    inversePrimary = Leather40,
)
