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

private fun TextStyle.vazirmatn(weight: FontWeight? = null) =
    copy(fontFamily = Vazirmatn, fontWeight = weight ?: fontWeight)

/**
 * Brand type scale: stock Material 3 sizes, as in FitSho, 504 Daily and Timeboxing. Headings, titles
 * and big numbers are bold; body text is regular; labels are medium. Varagh sets it in Vazirmatn
 * because it is a Persian-first reading app (the other apps use the system font). Sizes are in sp,
 * so they follow the user's font scale.
 */
val VaraghTypography = Typography(
    displayLarge = Default.displayLarge.vazirmatn(FontWeight.Bold),
    displayMedium = Default.displayMedium.vazirmatn(FontWeight.Bold),
    displaySmall = Default.displaySmall.vazirmatn(FontWeight.Bold),
    headlineLarge = Default.headlineLarge.vazirmatn(FontWeight.Bold),
    headlineMedium = Default.headlineMedium.vazirmatn(FontWeight.Bold),
    headlineSmall = Default.headlineSmall.vazirmatn(FontWeight.Bold),
    titleLarge = Default.titleLarge.vazirmatn(FontWeight.Bold),
    titleMedium = Default.titleMedium.vazirmatn(FontWeight.Bold),
    titleSmall = Default.titleSmall.vazirmatn(FontWeight.SemiBold),
    bodyLarge = Default.bodyLarge.vazirmatn(),
    bodyMedium = Default.bodyMedium.vazirmatn(),
    bodySmall = Default.bodySmall.vazirmatn(),
    labelLarge = Default.labelLarge.vazirmatn(FontWeight.Medium),
    labelMedium = Default.labelMedium.vazirmatn(FontWeight.Medium),
    labelSmall = Default.labelSmall.vazirmatn(FontWeight.Medium),
)
