package com.mid.varagh.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.FeatureFlags

@Composable
internal fun SettingsScreenRoute(
    onOpenDeveloperInfo: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsScreen(featureFlags = viewModel.featureFlags, onOpenDeveloperInfo = onOpenDeveloperInfo)
}

@Composable
internal fun SettingsScreen(
    featureFlags: FeatureFlags,
    onOpenDeveloperInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.settings_title)) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            // Reading, appearance and backup sections arrive in phase 7.
            if (featureFlags.showDeveloperInfo) {
                VaraghCard(title = stringResource(R.string.settings_section_developer), contentPadding = PaddingValues(vertical = VaraghSpacing.XSmall)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_developer_info)) },
                        supportingContent = { Text(stringResource(R.string.settings_developer_info_summary)) },
                        leadingContent = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.clickable(onClick = onOpenDeveloperInfo),
                    )
                }
            }
        }
    }
}
