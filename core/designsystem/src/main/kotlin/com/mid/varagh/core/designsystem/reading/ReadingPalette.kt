package com.mid.varagh.core.designsystem.reading

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import com.mid.varagh.core.designsystem.R
import com.mid.varagh.core.model.CustomReadingColors
import com.mid.varagh.core.model.ReadingTheme

/**
 * The colours of one reading theme.
 *
 * @property background the colour paper maps to; also used behind/between pages.
 * @property text the colour ink maps to.
 * @property isIdentity true for [ReadingTheme.DAY]: pages are drawn untouched.
 */
@Immutable
data class ReadingPalette(
    val background: Color,
    val text: Color,
    val isIdentity: Boolean = false,
) {
    /** Dark-background themes interpolate on luminance to avoid hue-inverted images. */
    val isDark: Boolean
        get() = ReadingColorMatrix.luminance(background.toArgb()) < ReadingColorMatrix.luminance(text.toArgb())

    /** Row-major 4x5 matrix, or null when pages should be drawn as-is. */
    val matrix: FloatArray? by lazy(LazyThreadSafetyMode.NONE) {
        if (isIdentity) null else ReadingColorMatrix.duotone(background.toArgb(), text.toArgb(), useLuminance = isDark)
    }

    /** Compose colour filter for `Image(colorFilter = …)`. */
    val colorFilter: ColorFilter? by lazy(LazyThreadSafetyMode.NONE) {
        matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }

    /** Framework colour filter for drawing bitmaps with a `Paint` (e.g. share cards, thumbnails). */
    fun toAndroidColorFilter(): android.graphics.ColorMatrixColorFilter? =
        matrix?.let { android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(it)) }

    companion object {
        val Day = ReadingPalette(background = Color(0xFFFFFFFF), text = Color(0xFF000000), isIdentity = true)
        val Sepia = ReadingPalette(background = Color(0xFFF4ECD8), text = Color(0xFF3B2F20))
        val SoftGrey = ReadingPalette(background = Color(0xFFDCDCD7), text = Color(0xFF2A2A2A))
        val Dark = ReadingPalette(background = Color(0xFF1F2023), text = Color(0xFFD6D6D0))
        val Night = ReadingPalette(background = Color(0xFF000000), text = Color(0xFFA88C66))

        fun forTheme(theme: ReadingTheme, custom: CustomReadingColors = CustomReadingColors.Default): ReadingPalette =
            when (theme) {
                ReadingTheme.DAY -> Day
                ReadingTheme.SEPIA -> Sepia
                ReadingTheme.SOFT_GREY -> SoftGrey
                ReadingTheme.DARK -> Dark
                ReadingTheme.NIGHT -> Night
                ReadingTheme.CUSTOM -> ReadingPalette(
                    background = Color(custom.backgroundArgb),
                    text = Color(custom.textArgb),
                )
            }
    }
}

@StringRes
fun ReadingTheme.labelRes(): Int = when (this) {
    ReadingTheme.DAY -> R.string.reading_theme_day
    ReadingTheme.SEPIA -> R.string.reading_theme_sepia
    ReadingTheme.SOFT_GREY -> R.string.reading_theme_soft_grey
    ReadingTheme.DARK -> R.string.reading_theme_dark
    ReadingTheme.NIGHT -> R.string.reading_theme_night
    ReadingTheme.CUSTOM -> R.string.reading_theme_custom
}
