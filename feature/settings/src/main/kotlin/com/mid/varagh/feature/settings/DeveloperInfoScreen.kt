package com.mid.varagh.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.model.FeatureFlags

@Composable
internal fun DeveloperInfoScreenRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    DeveloperInfoScreen(featureFlags = viewModel.featureFlags, onBack = onBack)
}

/** Debug-only: shows the backend switch and API base URL baked into this build. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeveloperInfoScreen(
    featureFlags: FeatureFlags,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_developer_info)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(VaraghIcons.Back, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("USE_REMOTE_BACKEND") },
                supportingContent = {
                    Text(featureFlags.useRemoteBackend.toString(), modifier = Modifier.testTag("flag_value"))
                },
            )
            ListItem(
                headlineContent = { Text("API_BASE_URL") },
                supportingContent = {
                    Text(
                        featureFlags.apiBaseUrl,
                        style = androidx.compose.material3.LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                    )
                },
            )
        }
    }
}
