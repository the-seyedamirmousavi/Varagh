package com.mid.varagh.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.theme.VaraghDimens

/**
 * White top bar with a bold title and a soft shadow separating it from the grey canvas.
 *
 * @param centered centred title (main tabs) or start-aligned next to the back arrow (details).
 * @param onBack shows an auto-mirrored back arrow when not null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaraghTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
    onBack: (() -> Unit)? = null,
    backContentDescription: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )
    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val navContent: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(VaraghIcons.Back, contentDescription = backContentDescription)
            }
        } else {
            navigationIcon()
        }
    }
    Surface(modifier = modifier, shadowElevation = VaraghDimens.BarElevation, color = MaterialTheme.colorScheme.surface) {
        if (centered) {
            CenterAlignedTopAppBar(title = titleContent, navigationIcon = navContent, actions = actions, colors = colors)
        } else {
            TopAppBar(title = titleContent, navigationIcon = navContent, actions = actions, colors = colors)
        }
    }
}
