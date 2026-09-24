package com.mid.varagh.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/** Shared motion tokens so the whole app moves with one personality. */
object VaraghMotion {
    const val STAGGER_MS = 70
    const val ENTER_MS = 420
    val Bouncy = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * True when the user turned animations off (Developer options / Accessibility "Remove
 * animations"). Decorative motion must be skipped then.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Springy "squish" while pressed. Pair it with the same [interactionSource] as the clickable. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = VaraghMotion.Bouncy,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fades and lifts [content] in the first time it is shown. Use increasing [index] values for a
 * staggered cascade down a screen.
 */
@Composable
fun AppearAnimated(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val state = remember { MutableTransitionState(reducedMotion).apply { targetState = true } }
    val delay = index * VaraghMotion.STAGGER_MS
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(VaraghMotion.ENTER_MS, delayMillis = delay)) +
            slideInVertically(tween(VaraghMotion.ENTER_MS, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 5 },
        exit = ExitTransition.None,
    ) {
        content()
    }
}
