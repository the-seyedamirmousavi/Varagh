package com.mid.varagh.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * High-contrast feature card: charcoal in light mode, near-white in dark mode. Use at most one per
 * screen for the main moment (empty library, "continue reading", finished-book celebration).
 */
@Composable
fun VaraghHeroCard(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    eyebrow: (@Composable () -> Unit)? = null,
    illustration: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (eyebrow != null) {
                eyebrow()
                Spacer(Modifier.height(16.dp))
            }
            if (illustration != null) {
                illustration()
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.72f),
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(24.dp))
                VaraghPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    leadingIcon = actionIcon,
                    inverted = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
