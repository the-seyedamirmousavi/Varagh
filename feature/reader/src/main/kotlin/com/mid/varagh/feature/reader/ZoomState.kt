package com.mid.varagh.feature.reader

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Zoom/pan state for a page (paged mode) or the whole page column (vertical mode, where vertical
 * movement is left to scrolling and only horizontal panning is clamped here).
 */
@Stable
class ZoomState(private val allowVerticalPan: Boolean) {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    internal var size: IntSize = IntSize.Zero

    val isZoomed: Boolean get() = scale > ZOOM_EPSILON

    fun transform(zoom: Float, pan: Offset) {
        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = if (scale <= ZOOM_EPSILON) Offset.Zero else clamp(offset + pan)
    }

    fun reset() {
        scale = MIN_SCALE
        offset = Offset.Zero
    }

    /** Double tap: zoom in around [tap], or back out. */
    suspend fun toggle(tap: Offset) {
        val from = scale
        val target = if (isZoomed) MIN_SCALE else DOUBLE_TAP_SCALE
        val center = Offset(size.width / 2f, size.height / 2f)
        val targetOffset = if (target == MIN_SCALE) Offset.Zero else (center - tap) * (target - 1f)
        val fromOffset = offset
        animate(0f, 1f) { t, _ ->
            scale = from + (target - from) * t
            offset = clamp(fromOffset + (targetOffset - fromOffset) * t)
        }
    }

    private fun clamp(value: Offset): Offset {
        val maxX = (scale - 1f) * size.width / 2f
        val maxY = if (allowVerticalPan) (scale - 1f) * size.height / 2f else 0f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f
        const val DOUBLE_TAP_SCALE = 2.5f
        private const val ZOOM_EPSILON = 1.01f
    }
}

@Composable
fun rememberZoomState(allowVerticalPan: Boolean, key: Any? = null): ZoomState =
    remember(key) { ZoomState(allowVerticalPan) }

/**
 * Gesture container: pinch to zoom (two fingers), pan while zoomed, double-tap to toggle zoom and
 * single taps forwarded to [onTap]. Single-finger drags at 1x are left to the scroll container /
 * pager underneath. Apply [zoomContent] to the child that should be transformed.
 */
fun Modifier.zoomGestures(
    state: ZoomState,
    scope: CoroutineScope,
    onTap: (Offset, IntSize) -> Unit,
): Modifier = this
    .onSizeChanged { state.size = it }
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2) {
                    state.transform(event.calculateZoom(), event.calculatePan())
                    // Stop the list/pager from scrolling while pinching.
                    event.changes.forEach { it.consume() }
                } else if (pressed == 1 && state.isZoomed) {
                    state.transform(1f, event.calculatePan())
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .then(
        Modifier.pointerInput(state) {
            detectTapGestures(
                onDoubleTap = { tap -> scope.launch { state.toggle(tap) } },
                onTap = { onTap(it, size) },
            )
        },
    )

/** Applies the zoom transform to the content. */
fun Modifier.zoomContent(state: ZoomState): Modifier = graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offset.x
    translationY = state.offset.y
}

