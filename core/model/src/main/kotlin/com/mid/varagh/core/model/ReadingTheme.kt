package com.mid.varagh.core.model

/**
 * Colour treatment applied to rendered PDF pages. The white paper of the page is mapped to a
 * comfortable background and black text to a softer foreground (see `ReadingColorMatrix` in
 * :core:designsystem).
 *
 * Known limitation: the transform is applied to the whole page bitmap, so images/photos in the
 * PDF are tinted too ("preserve images" is out of scope for v1).
 */
enum class ReadingTheme {
    /** Original page colours. */
    DAY,

    /** Warm paper. */
    SEPIA,

    /** Low-contrast grey paper. */
    SOFT_GREY,

    /** Dark grey background with light text. */
    DARK,

    /** True black background with dimmed warm text (AMOLED friendly). */
    NIGHT,

    /** User-picked background and text colours. */
    CUSTOM,
}

/** Colours for [ReadingTheme.CUSTOM], stored as ARGB ints so the model stays platform-free. */
data class CustomReadingColors(
    val backgroundArgb: Int,
    val textArgb: Int,
) {
    companion object {
        val Default = CustomReadingColors(backgroundArgb = 0xFFE8F0E3.toInt(), textArgb = 0xFF1F2A1C.toInt())
    }
}
