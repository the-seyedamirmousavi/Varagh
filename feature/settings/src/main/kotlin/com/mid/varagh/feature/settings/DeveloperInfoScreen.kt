package com.mid.varagh.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghPrimaryButton
import com.mid.varagh.core.designsystem.component.VaraghProgressRing
import com.mid.varagh.core.designsystem.component.VaraghStatRow
import com.mid.varagh.core.designsystem.component.VaraghTextField
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.FeatureFlags

@Composable
internal fun DeveloperInfoScreenRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    DeveloperInfoScreen(featureFlags = viewModel.featureFlags, onBack = onBack)
}

/**
 * Debug-only: shows the backend switch and API base URL baked into this build, plus a small
 * gallery of design-system components for visual checks on a device.
 */
@Composable
internal fun DeveloperInfoScreen(
    featureFlags: FeatureFlags,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.settings_developer_info),
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(R.string.settings_back),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            VaraghCard(title = stringResource(R.string.settings_section_build)) {
                InfoRow(label = "USE_REMOTE_BACKEND", value = featureFlags.useRemoteBackend.toString(), valueTag = "flag_value")
                InfoRow(label = "API_BASE_URL", value = featureFlags.apiBaseUrl)
            }
            DesignGallery()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueTag: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
        )
    }
}

/** Sample data only; lets you eyeball the design system on a real device. */
@Composable
private fun DesignGallery() {
    var note by rememberSaveable { mutableStateOf("") }
    VaraghCard(title = "Daily reading", horizontalAlignment = Alignment.CenterHorizontally) {
        VaraghProgressRing(
            progress = 0.35f,
            value = "12",
            caption = "30",
            modifier = Modifier.fillMaxWidth(0.72f),
        )
        VaraghStatRow(
            stats = listOf("48 min" to "Minutes", "3 days" to "Streak"),
            modifier = Modifier.padding(top = VaraghSpacing.XLarge),
        )
    }
    VaraghCard(title = "Stats") {
        VaraghStatRow(stats = listOf("12" to "Finished", "3,480" to "Pages", "41 h" to "Time", "7" to "Streak"))
    }
    VaraghCard(title = "Bookmark") {
        VaraghTextField(value = note, onValueChange = { note = it }, label = "Note")
        Row(Modifier.fillMaxWidth().padding(top = VaraghSpacing.XLarge)) {
            VaraghPrimaryButton(
                text = "Add bookmark",
                onClick = {},
                leadingIcon = VaraghIcons.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
