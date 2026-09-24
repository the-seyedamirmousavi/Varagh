package com.mid.varagh.core.designsystem.illustration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.motion.rememberReducedMotion

/**
 * A small stack of books whose top book gently bobs, with twinkling dots. Decorative only
 * (no semantics). Colours follow [ink] and the green [accent], so it works on light and dark cards.
 */
@Composable
fun BookStackIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    ink: Color = LocalContentColor.current,
    accent: Color = MaterialTheme.colorScheme.tertiary,
) {
    val reducedMotion = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "bookStack")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    Canvas(modifier.size(size, size * 0.8f)) {
        val w = size.toPx()
        val h = w * 0.8f
        val bookH = h * 0.17f
        val corner = CornerRadius(bookH * 0.22f)

        // Shadow
        drawOval(ink.copy(alpha = 0.08f), topLeft = Offset(w * 0.12f, h * 0.9f), size = Size(w * 0.76f, h * 0.07f))

        // Bottom book
        book(Offset(w * 0.12f, h * 0.72f), Size(w * 0.76f, bookH), ink.copy(alpha = 0.9f), ink, corner)
        // Middle book (accent)
        book(Offset(w * 0.18f, h * 0.72f - bookH), Size(w * 0.66f, bookH), accent, ink, corner)
        // Top book, tilted and bobbing
        val lift = bob * h * 0.05f
        translate(top = -lift) {
            rotate(degrees = -7f + bob * 3f, pivot = Offset(w * 0.5f, h * 0.72f - bookH * 1.5f)) {
                book(Offset(w * 0.22f, h * 0.72f - bookH * 2.05f), Size(w * 0.58f, bookH * 0.95f), ink.copy(alpha = 0.55f), ink, corner)
            }
        }

        // Twinkles
        val twinkle = 0.35f + 0.65f * bob
        drawCircle(accent.copy(alpha = twinkle), radius = w * 0.018f, center = Offset(w * 0.86f, h * 0.22f))
        drawCircle(ink.copy(alpha = 0.25f + 0.4f * (1 - bob)), radius = w * 0.012f, center = Offset(w * 0.14f, h * 0.3f))
        drawCircle(ink.copy(alpha = 0.2f + 0.3f * bob), radius = w * 0.009f, center = Offset(w * 0.74f, h * 0.08f))
    }
}

private fun DrawScope.book(topLeft: Offset, size: Size, cover: Color, ink: Color, corner: CornerRadius) {
    drawRoundRect(cover, topLeft, size, corner)
    // Page edge
    val pageInset = size.height * 0.2f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(topLeft.x + size.width - size.width * 0.08f, topLeft.y + pageInset),
        size = Size(size.width * 0.05f, size.height - pageInset * 2),
        cornerRadius = CornerRadius(size.height * 0.08f),
    )
    // Spine band
    drawRect(
        color = ink.copy(alpha = 0.18f),
        topLeft = Offset(topLeft.x + size.width * 0.14f, topLeft.y),
        size = Size(size.width * 0.05f, size.height),
    )
}
