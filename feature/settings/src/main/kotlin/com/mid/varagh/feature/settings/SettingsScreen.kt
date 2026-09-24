package com.mid.varagh.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mid.varagh.core.model.FeatureFlags

@Composable
internal fun SettingsScreenRoute(
    onOpenDeveloperInfo: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    SettingsScreen(featureFlags = viewModel.featureFlags, onOpenDeveloperInfo = onOpenDeveloperInfo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    featureFlags: FeatureFlags,
    onOpenDeveloperInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (featureFlags.showDeveloperInfo) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_developer_info)) },
                    supportingContent = { Text(stringResource(R.string.settings_developer_info_summary)) },
                    leadingContent = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onOpenDeveloperInfo),
                )
            }
        }
    }
}
