package com.mid.varagh.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghFeatureRow
import com.mid.varagh.core.designsystem.component.VaraghHeroCard
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.illustration.BookStackIllustration
import com.mid.varagh.core.designsystem.motion.AppearAnimated
import com.mid.varagh.core.designsystem.theme.VaraghSpacing

@Composable
internal fun LibraryScreen(
    onOpenBook: (bookId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.library_title)) },
    ) { padding ->
        LibraryEmptyContent(
            onAddBook = { /* Wired to the SAF importer in phase 3. */ },
            modifier = Modifier.padding(padding),
        )
    }
}

/** First-run library: a hero with the main call to action, then what the app can do. */
@Composable
private fun LibraryEmptyContent(
    onAddBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VaraghSpacing.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
    ) {
        AppearAnimated(index = 0) {
            VaraghHeroCard(
                title = stringResource(R.string.library_empty_title),
                message = stringResource(R.string.library_empty_message),
                illustration = { BookStackIllustration() },
                actionLabel = stringResource(R.string.library_add_first_book),
                actionIcon = VaraghIcons.Add,
                onAction = onAddBook,
                modifier = Modifier.testTag("library_hero"),
            )
        }
        AppearAnimated(index = 1) {
            VaraghCard(title = stringResource(R.string.library_tips_title)) {
                Column(verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XLarge)) {
                    VaraghFeatureRow(
                        icon = VaraghIcons.Import,
                        title = stringResource(R.string.library_tip_import_title),
                        description = stringResource(R.string.library_tip_import_body),
                    )
                    VaraghFeatureRow(
                        icon = VaraghIcons.ReadingTheme,
                        title = stringResource(R.string.library_tip_themes_title),
                        description = stringResource(R.string.library_tip_themes_body),
                    )
                    VaraghFeatureRow(
                        icon = VaraghIcons.Streak,
                        title = stringResource(R.string.library_tip_streak_title),
                        description = stringResource(R.string.library_tip_streak_body),
                        highlight = true,
                    )
                }
            }
        }
    }
}
