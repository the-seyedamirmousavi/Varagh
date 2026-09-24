package com.mid.varagh.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.motion.rememberReducedMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Large ring with a pale track, a dark progress arc starting at 12 o'clock and a dot marking the
 * current position; [value] and [caption] sit in the centre (e.g. "12" / "30"). The arc sweeps in
 * on first show, turns green once the goal is reached, and runs counter-clockwise in RTL.
 *
 * @param progress 0f..1f
 */
@Composable
fun VaraghProgressRing(
    progress: Float,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
) {
    val target = progress.coerceIn(0f, 1f)
    val reducedMotion = rememberReducedMotion()
    // Sweeps in from 0 the first time, then follows progress changes.
    val sweepAnim = remember { Animatable(if (reducedMotion) target else 0f) }
    LaunchedEffect(target) {
        if (reducedMotion) sweepAnim.snapTo(target) else sweepAnim.animateTo(target, tween(1100, easing = FastOutSlowInEasing))
    }
    val animated = sweepAnim.value
    val track = MaterialTheme.colorScheme.surfaceVariant
    // Goal reached: the arc turns the green accent.
    val arc by animateColorAsState(
        if (target >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "ringColor",
    )
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx))
            val sweep = 360f * animated * direction
            if (animated > 0f) {
                drawArc(
                    arc, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians((-90f + sweep).toDouble())
            val radius = diameter / 2f
            drawCircle(
                color = arc,
                radius = strokePx * 0.55f,
                center = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, style = MaterialTheme.typography.displayLarge)
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = null),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
