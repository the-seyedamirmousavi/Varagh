package com.mid.varagh.core.designsystem.reading

/**
 * Pure maths for reading themes, kept free of Android types so it is unit-testable.
 *
 * A rendered PDF page is (mostly) dark text on white paper. A reading theme remaps it as a
 * "duotone": paper white -> [ReadingPalette.background], ink black -> [ReadingPalette.text], with
 * everything in between interpolated:
 *
 *     out = text + (background - text) * x          (per RGB channel, x in 0..1)
 *
 * where `x` is either the input channel itself (light themes; keeps the hue of images, just
 * tinted) or the pixel's luminance (dark themes; avoids photographic "negative" hue inversion).
 *
 * The result is a 4x5 row-major matrix in the format used by both `android.graphics.ColorMatrix`
 * and Compose's `ColorMatrix` (colour components in 0..255, offsets in 0..255).
 *
 * Limitation (documented, out of scope for v1): the whole bitmap is transformed, so photos and
 * figures in the PDF are tinted/inverted as well.
 */
object ReadingColorMatrix {

    // Rec. 709 luma coefficients.
    private const val LUMA_R = 0.2126f
    private const val LUMA_G = 0.7152f
    private const val LUMA_B = 0.0722f

    val IDENTITY: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /**
     * @param backgroundArgb colour that white paper maps to.
     * @param textArgb colour that black ink maps to.
     * @param useLuminance interpolate on luminance (true) or per channel (false).
     */
    fun duotone(backgroundArgb: Int, textArgb: Int, useLuminance: Boolean): FloatArray {
        val bg = channels(backgroundArgb)
        val fg = channels(textArgb)
        val m = FloatArray(20)
        for (row in 0..2) {
            val k = (bg[row] - fg[row]) / 255f
            val base = row * 5
            if (useLuminance) {
                m[base + 0] = k * LUMA_R
                m[base + 1] = k * LUMA_G
                m[base + 2] = k * LUMA_B
            } else {
                m[base + row] = k
            }
            m[base + 4] = fg[row]
        }
        m[18] = 1f // keep alpha
        return m
    }

    /** Applies [matrix] to a single opaque ARGB colour. Used by tests and colour previews. */
    fun apply(matrix: FloatArray, argb: Int): Int {
        val (r, g, b) = channels(argb)
        fun row(i: Int): Int {
            val o = i * 5
            val v = matrix[o] * r + matrix[o + 1] * g + matrix[o + 2] * b + matrix[o + 3] * 255f + matrix[o + 4]
            return v.coerceIn(0f, 255f).let { kotlin.math.round(it).toInt() }
        }
        return (0xFF shl 24) or (row(0) shl 16) or (row(1) shl 8) or row(2)
    }

    /** Relative luminance in 0..1 (sRGB, not linearised; good enough to compare two colours). */
    fun luminance(argb: Int): Float {
        val (r, g, b) = channels(argb)
        return (LUMA_R * r + LUMA_G * g + LUMA_B * b) / 255f
    }

    private fun channels(argb: Int): FloatArray = floatArrayOf(
        ((argb shr 16) and 0xFF).toFloat(),
        ((argb shr 8) and 0xFF).toFloat(),
        (argb and 0xFF).toFloat(),
    )
}
