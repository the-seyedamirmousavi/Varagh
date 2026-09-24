package com.mid.varagh.feature.history

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
internal fun HistoryScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
    ) { padding ->
        VaraghEmptyState(
            icon = VaraghIcons.HistoryOutlined,
            title = stringResource(R.string.history_empty_title),
            message = stringResource(R.string.history_empty_message),
            modifier = Modifier.padding(padding),
        )
    }
}
