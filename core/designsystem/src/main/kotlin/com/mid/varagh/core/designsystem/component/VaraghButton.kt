package com.mid.varagh.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.mid.varagh.core.designsystem.motion.pressScale
import com.mid.varagh.core.designsystem.theme.VaraghDimens
import com.mid.varagh.core.designsystem.theme.VaraghPillShape

/**
 * Charcoal pill, 56dp tall: the brand's primary button (same as FitSho, 504 Daily and Timeboxing),
 * with a springy press and a light haptic tick.
 *
 * @param inverted use on dark hero surfaces: light button with dark text.
 */
@Composable
fun VaraghPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    inverted: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        enabled = enabled,
        shape = VaraghPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (inverted) scheme.inverseOnSurface else scheme.primary,
            contentColor = if (inverted) scheme.inverseSurface else scheme.onPrimary,
        ),
        elevation = null,
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        modifier = modifier
            .pressScale(interaction)
            .defaultMinSize(minHeight = VaraghDimens.ButtonHeight),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = null))
    }
}

/** Low-emphasis text action, e.g. "update" in a top bar. */
@Composable
fun VaraghTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = null))
    }
}
