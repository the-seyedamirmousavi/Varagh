package com.mid.varagh.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mid.varagh.core.designsystem.component.BookCover
import com.mid.varagh.core.designsystem.component.VaraghAccentChip
import com.mid.varagh.core.designsystem.component.VaraghBarChart
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghCountStat
import com.mid.varagh.core.designsystem.component.VaraghDateFormatter
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghProgressBar
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.component.rememberDateFormatter
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.motion.AppearAnimated
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.ReadingStats
import kotlin.math.roundToInt

@Composable
internal fun HistoryScreenRoute(
    onOpenBook: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onOpenBook = onOpenBook)
}

@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dates = rememberDateFormatter()
    Scaffold(
        modifier = modifier,
        topBar = { VaraghTopAppBar(title = stringResource(R.string.history_title)) },
    ) { padding ->
        if (state.loading) {
            VaraghLoading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("history_list"),
            contentPadding = PaddingValues(VaraghSpacing.ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        ) {
            item(key = "stats") { AppearAnimated(0) { StatsCard(state.stats) } }
            item(key = "chart") { AppearAnimated(1) { MonthlyChartCard(state.stats, dates) } }
            if (state.isEmpty) {
                item(key = "empty") {
                    AppearAnimated(2) {
                        VaraghCard(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.history_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                stringResource(R.string.history_empty_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = VaraghSpacing.Small),
                            )
                        }
                    }
                }
            }
            section("reading", R.string.history_section_reading, state.reading) { BookRow(it, dates, onOpenBook) }
            section("finished", R.string.history_section_finished, state.finished) { BookRow(it, dates, onOpenBook) }
            section("want", R.string.history_section_want, state.wantToRead) { BookRow(it, dates, onOpenBook) }
            section("abandoned", R.string.history_section_abandoned, state.abandoned) { BookRow(it, dates, onOpenBook) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    key: String,
    @StringRes title: Int,
    books: List<BookWithProgress>,
    row: @Composable (BookWithProgress) -> Unit,
) {
    if (books.isEmpty()) return
    item(key = key) {
        VaraghCard(title = stringResource(title), contentPadding = PaddingValues(vertical = VaraghSpacing.Small)) {
            books.forEach { row(it) }
        }
    }
}

@Composable
private fun StatsCard(stats: ReadingStats) {
    val locale = currentLocale()
    VaraghCard(
        title = stringResource(R.string.history_this_year),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VaraghAccentChip(
            text = stringResource(R.string.history_streak_days, formatNumber(stats.currentStreakDays.toLong(), locale)),
            icon = VaraghIcons.Streak,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = VaraghSpacing.XLarge),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            VaraghCountStat(target = stats.booksFinishedThisYear.toLong(), label = stringResource(R.string.history_stat_books), modifier = Modifier.weight(1f))
            VaraghCountStat(target = stats.totalPagesRead.toLong(), label = stringResource(R.string.history_stat_pages), modifier = Modifier.weight(1f))
            VaraghCountStat(target = stats.totalMinutesRead, label = stringResource(R.string.history_stat_minutes), modifier = Modifier.weight(1f))
        }
        Text(
            text = stringResource(R.string.history_total_finished, formatNumber(stats.totalBooksFinished.toLong(), locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = VaraghSpacing.Medium),
        )
    }
}

@Composable
private fun MonthlyChartCard(stats: ReadingStats, dates: VaraghDateFormatter) {
    val locale = currentLocale()
    val months = stats.booksPerMonth
    val labels = months.map { dates.monthLabel(it.year, it.month) }
    val summary = stringResource(R.string.history_chart_title) + ": " +
        months.zip(labels).joinToString { (m, label) -> "$label ${formatNumber(m.count.toLong(), locale)}" }
    VaraghCard(title = stringResource(R.string.history_chart_title)) {
        if (months.isEmpty()) {
            Text(stringResource(R.string.history_chart_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            VaraghBarChart(
                values = months.map { it.count },
                labels = labels,
                description = summary,
                formatValue = { formatNumber(it.toLong(), locale) },
                modifier = Modifier.testTag("monthly_chart"),
            )
        }
    }
}

@Composable
private fun BookRow(item: BookWithProgress, dates: VaraghDateFormatter, onOpen: (Long) -> Unit) {
    val book = item.book
    val locale = currentLocale()
    val subtitle = when {
        book.finishedAt != null -> stringResource(R.string.history_finished_on, dates.date(book.finishedAt!!))
        item.progress != null -> stringResource(
            R.string.history_progress,
            formatNumber(((item.progress!!.percent) * 100).roundToInt().toLong(), locale),
        )
        else -> book.author.orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(book.id) }
            .padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book.coverPath, book.title, Modifier.width(44.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall)) {
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (book.finishedAt == null && item.progress != null) VaraghProgressBar(item.progress!!.percent)
        }
        Icon(VaraghIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
