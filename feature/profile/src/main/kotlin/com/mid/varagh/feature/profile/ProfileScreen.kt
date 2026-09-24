package com.mid.varagh.feature.profile

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.icon.VaraghIcons

@Composable
internal fun ProfileScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.profile_title)) },
    ) { padding ->
        VaraghEmptyState(
            icon = VaraghIcons.ProfileOutlined,
            title = stringResource(R.string.profile_placeholder_title),
            message = stringResource(R.string.profile_placeholder_message),
            modifier = Modifier.padding(padding),
        )
    }
}
