package com.mid.varagh.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.mid.varagh.core.designsystem.component.ReadingThemeSwatch
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.reading.ReadingPalette
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.ReadingTheme

/** Phase-1 placeholder: navigation target + reading-theme preview. Real rendering lands in phase 4. */
@Composable
internal fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var theme by rememberSaveable { mutableStateOf(ReadingTheme.SEPIA) }
    val palette = ReadingPalette.forTheme(theme)
    Scaffold(
        modifier = modifier,
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.reader_title),
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(R.string.reader_back),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(palette.background),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.reader_placeholder, bookId),
                    color = palette.text,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            VaraghCard(title = stringResource(R.string.reader_theme), contentPadding = PaddingValues(vertical = VaraghSpacing.Large)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = VaraghSpacing.Large),
                    horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small),
                ) {
                    ReadingTheme.entries.forEach { t ->
                        ReadingThemeSwatch(
                            theme = t,
                            palette = ReadingPalette.forTheme(t),
                            selected = t == theme,
                            onClick = { theme = t },
                        )
                    }
                }
            }
        }
    }
}
