package com.mid.varagh.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mid.varagh.core.designsystem.R

/** Vazirmatn (SIL OFL 1.1) covers both Persian and Latin glyphs, so one family serves both locales. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val Default = Typography()

private fun TextStyle.vazirmatn() = copy(fontFamily = Vazirmatn)

/** Material 3 type scale with every style switched to Vazirmatn. Sizes are in sp, so they scale. */
val VaraghTypography = Typography(
    displayLarge = Default.displayLarge.vazirmatn(),
    displayMedium = Default.displayMedium.vazirmatn(),
    displaySmall = Default.displaySmall.vazirmatn(),
    headlineLarge = Default.headlineLarge.vazirmatn().copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Default.headlineMedium.vazirmatn().copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Default.headlineSmall.vazirmatn().copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Default.titleLarge.vazirmatn().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.vazirmatn().copy(fontWeight = FontWeight.Medium),
    titleSmall = Default.titleSmall.vazirmatn().copy(fontWeight = FontWeight.Medium),
    bodyLarge = Default.bodyLarge.vazirmatn(),
    bodyMedium = Default.bodyMedium.vazirmatn(),
    bodySmall = Default.bodySmall.vazirmatn(),
    labelLarge = Default.labelLarge.vazirmatn().copy(fontWeight = FontWeight.Medium),
    labelMedium = Default.labelMedium.vazirmatn().copy(fontWeight = FontWeight.Medium),
    labelSmall = Default.labelSmall.vazirmatn().copy(fontWeight = FontWeight.Medium),
)
