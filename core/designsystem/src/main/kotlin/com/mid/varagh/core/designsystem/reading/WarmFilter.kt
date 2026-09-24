package com.mid.varagh.core.designsystem.reading

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * Blue-light ("warm light") filter. A multiply-blended amber layer removes blue and some green,
 * the same idea as system night-light, but only inside the reader and independent of the OS.
 */
object WarmFilter {
    /** Maximum alpha of the amber layer at intensity 1. Chosen so text stays readable. */
    const val MAX_ALPHA = 0.55f

    private val Amber = Color(0xFFFF9A3C)

    /** Overlay colour for [intensity] in 0..1, or [Color.Transparent] when off. */
    fun overlayColor(intensity: Float): Color {
        val i = intensity.coerceIn(0f, 1f)
        return if (i == 0f) Color.Transparent else Amber.copy(alpha = i * MAX_ALPHA)
    }
}

/** Draws the warm filter on top of the content. */
fun Modifier.warmFilter(intensity: Float): Modifier = drawWithContent {
    drawContent()
    val color = WarmFilter.overlayColor(intensity)
    if (color.alpha > 0f) drawRect(color = color, blendMode = BlendMode.Multiply)
}
