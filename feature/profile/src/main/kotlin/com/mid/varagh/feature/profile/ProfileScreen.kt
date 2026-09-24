package com.mid.varagh.feature.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.icon.VaraghIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
    ) { padding ->
        VaraghEmptyState(
            icon = VaraghIcons.ProfileOutlined,
            title = stringResource(R.string.profile_placeholder_title),
            message = stringResource(R.string.profile_placeholder_message),
            modifier = Modifier.padding(padding),
        )
    }
}
