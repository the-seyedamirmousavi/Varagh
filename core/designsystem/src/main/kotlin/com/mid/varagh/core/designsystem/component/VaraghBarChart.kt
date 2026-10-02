package com.mid.varagh.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.motion.rememberReducedMotion

/**
 * Lightweight bar chart drawn with Canvas (no chart library). Bars grow in on first show; the
 * last bar (the current period) uses the green accent. Values are drawn above non-zero bars and
 * [labels] under each bar. Ordering follows the layout direction (newest on the left in RTL).
 *
 * @param description spoken summary for TalkBack, e.g. "Books per month: Mehr 2, Aban 0, …".
 */
@Composable
fun VaraghBarChart(
    values: List<Int>,
    labels: List<String>,
    description: String,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    formatValue: (Int) -> String = { it.toString() },
) {
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(values) {
        if (!reducedMotion) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }
    val barColor = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer: TextMeasurer = rememberTextMeasurer()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(modifier.semantics { contentDescription = description }) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (values.isEmpty()) return@Canvas
            val labelHeight = 18.dp.toPx()
            val valueHeight = 18.dp.toPx()
            val chartTop = valueHeight
            val chartBottom = size.height - labelHeight - 4.dp.toPx()
            val slot = size.width / values.size
            val barWidth = (slot * 0.55f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            values.forEachIndexed { i, value ->
                val position = if (rtl) values.size - 1 - i else i
                val centerX = slot * position + slot / 2
                val left = centerX - barWidth / 2
                // Track
                drawRoundRect(track, Offset(left, chartTop), Size(barWidth, chartBottom - chartTop), radius)
                val fraction = value.toFloat() / max * progress.value
                if (value > 0) {
                    val top = chartBottom - (chartBottom - chartTop) * fraction
                    drawRoundRect(
                        color = if (i == values.lastIndex) accent else barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, chartBottom - top),
                        cornerRadius = radius,
                    )
                    drawCentered(measurer, formatValue(value), valueStyle, centerX, top - valueHeight)
                }
                labels.getOrNull(i)?.let { drawCentered(measurer, it, labelStyle, centerX, chartBottom + 4.dp.toPx()) }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    centerX: Float,
    top: Float,
) {
    val layout = measurer.measure(text, style, maxLines = 1)
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top.coerceAtLeast(0f)))
}
