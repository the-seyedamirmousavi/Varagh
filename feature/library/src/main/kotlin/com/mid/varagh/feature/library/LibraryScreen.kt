package com.mid.varagh.feature.library

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
internal fun LibraryScreen(
    onOpenBook: (bookId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
    ) { padding ->
        VaraghEmptyState(
            icon = VaraghIcons.LibraryOutlined,
            title = stringResource(R.string.library_empty_title),
            message = stringResource(R.string.library_empty_message),
            actionLabel = stringResource(R.string.library_add_first_book),
            onAction = { /* Wired to the SAF importer in phase 3. */ },
            modifier = Modifier.padding(padding),
        )
    }
}
