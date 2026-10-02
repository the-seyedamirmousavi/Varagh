package com.mid.varagh.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mid.varagh.core.designsystem.component.BookCover
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghPrimaryButton
import com.mid.varagh.core.designsystem.component.VaraghProgressRing
import com.mid.varagh.core.designsystem.component.VaraghStatRow
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.component.labelRes
import com.mid.varagh.core.designsystem.component.rememberDateFormatter
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.theme.VaraghPillShape
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.ReadingStatus
import kotlin.math.roundToInt
import com.mid.varagh.core.designsystem.R as DesignR

@Composable
internal fun BookDetailScreenRoute(
    onBack: () -> Unit,
    onRead: (bookId: Long, startPage: Int) -> Unit,
    readersOfBook: (@Composable (remoteBookId: String) -> Unit)?,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                BookDetailEvent.Deleted -> onBack()
                is BookDetailEvent.Failed -> snackbar.showSnackbar(context.getString(errorMessageRes(event.error)))
            }
        }
    }
    BookDetailScreen(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onRead = { page -> onRead(viewModel.bookId, page) },
        onStatus = viewModel::onStatus,
        onRating = viewModel::onRating,
        onSaveDetails = viewModel::onSaveDetails,
        onDelete = viewModel::onDelete,
        readersOfBook = readersOfBook,
    )
}

@Composable
internal fun BookDetailScreen(
    state: BookDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRead: (startPage: Int) -> Unit,
    onStatus: (ReadingStatus) -> Unit,
    onRating: (Int?) -> Unit,
    onSaveDetails: (String, String?) -> Unit,
    onDelete: () -> Unit,
    readersOfBook: (@Composable (remoteBookId: String) -> Unit)? = null,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val item = state.book
    Scaffold(
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.detail_title),
                centered = false,
                onBack = onBack,
                backContentDescription = stringResource(DesignR.string.common_back),
                actions = {
                    if (item != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(VaraghIcons.Edit, contentDescription = stringResource(R.string.detail_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete_book")) {
                            Icon(VaraghIcons.Delete, contentDescription = stringResource(R.string.detail_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.loading -> VaraghLoading(Modifier.padding(padding))
            item == null -> VaraghEmptyState(
                icon = VaraghIcons.Error,
                title = stringResource(R.string.detail_missing),
                modifier = Modifier.padding(padding),
            )
            else -> DetailContent(
                state = state,
                book = item.book,
                percent = item.progress?.percent ?: 0f,
                currentPage = item.progress?.currentPage,
                onRead = onRead,
                onStatus = onStatus,
                onRating = onRating,
                readersOfBook = readersOfBook,
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (editing && item != null) {
        EditBookDialog(
            book = item.book,
            onSave = { title, author ->
                editing = false
                onSaveDetails(title, author)
            },
            onDismiss = { editing = false },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }, modifier = Modifier.testTag("confirm_delete")) {
                    Text(stringResource(DesignR.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(DesignR.string.common_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    state: BookDetailUiState,
    book: Book,
    percent: Float,
    currentPage: Int?,
    onRead: (Int) -> Unit,
    onStatus: (ReadingStatus) -> Unit,
    onRating: (Int?) -> Unit,
    readersOfBook: (@Composable (String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val dates = rememberDateFormatter()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VaraghSpacing.ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
    ) {
        VaraghCard {
            Row(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Large)) {
                BookCover(book.coverPath, book.title, Modifier.width(112.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Small)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("detail_title"))
                    book.author?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text(
                        pluralStringResource(DesignR.plurals.pages_count, book.pageCount, formatNumber(book.pageCount.toLong(), locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    book.finishedAt?.let {
                        Text(stringResource(R.string.history_finished_on, dates.date(it)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            RatingBar(rating = book.rating, onRating = onRating)
            VaraghPrimaryButton(
                text = stringResource(if (currentPage == null) R.string.detail_start_reading else R.string.detail_continue_reading),
                onClick = { onRead(-1) },
                leadingIcon = VaraghIcons.Pages,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = VaraghSpacing.XLarge)
                    .testTag("read_button"),
            )
        }

        VaraghCard(title = stringResource(R.string.detail_status)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small)) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = book.status == status,
                        onClick = { onStatus(status) },
                        label = { Text(stringResource(status.labelRes())) },
                        shape = VaraghPillShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.testTag("status_${status.name}"),
                    )
                }
            }
        }

        VaraghCard(title = stringResource(R.string.detail_progress), horizontalAlignment = Alignment.CenterHorizontally) {
            val pct = (percent * 100).roundToInt()
            VaraghProgressRing(
                progress = percent,
                value = formatNumber(pct.toLong(), locale),
                caption = stringResource(R.string.detail_percent_caption),
                modifier = Modifier.fillMaxWidth(0.6f),
            )
            val minutes = state.totalMillis / 60_000
            VaraghStatRow(
                stats = listOf(
                    formatNumber(minutes, locale) to stringResource(R.string.history_stat_minutes),
                    formatNumber(state.sessions.size.toLong(), locale) to stringResource(R.string.detail_sessions),
                    formatNumber(state.pagesRead.toLong(), locale) to stringResource(R.string.history_stat_pages),
                ),
                modifier = Modifier.padding(top = VaraghSpacing.XLarge),
            )
        }

        if (state.bookmarks.isNotEmpty()) {
            VaraghCard(title = stringResource(R.string.detail_bookmarks), contentPadding = PaddingValues(vertical = VaraghSpacing.Small)) {
                state.bookmarks.forEach { bookmark ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onRead(bookmark.page) }
                            .padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(VaraghIcons.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_page, formatNumber(bookmark.page + 1L, locale)), style = MaterialTheme.typography.titleSmall)
                            bookmark.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }

        if (state.sessions.isNotEmpty()) {
            VaraghCard(title = stringResource(R.string.detail_recent_sessions), contentPadding = PaddingValues(vertical = VaraghSpacing.Small)) {
                state.sessions.take(MAX_SESSIONS).forEach { session ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VaraghSpacing.CardPadding, vertical = VaraghSpacing.Small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(dates.dateTime(session.startedAt), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            pluralStringResource(
                                DesignR.plurals.minutes_count,
                                (session.durationMillis / 60_000).toInt(),
                                formatNumber((session.durationMillis / 60_000).coerceAtLeast(1), locale),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        book.remoteId?.let { remoteId -> readersOfBook?.invoke(remoteId) }
    }
}

@Composable
private fun RatingBar(rating: Int?, onRating: (Int?) -> Unit) {
    val locale = currentLocale()
    Row {
        for (star in 1..Book.MAX_RATING) {
            val filled = rating != null && star <= rating
            val label = stringResource(R.string.detail_rate, formatNumber(star.toLong(), locale))
            Icon(
                imageVector = if (filled) VaraghIcons.Star else VaraghIcons.StarOutline,
                contentDescription = label,
                tint = if (filled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .selectable(selected = filled, role = Role.RadioButton) {
                        // Tapping the current rating again clears it.
                        onRating(if (rating == star) null else star)
                    }
                    .padding(VaraghSpacing.XSmall)
                    .testTag("star_$star"),
            )
        }
    }
}

@Composable
private fun EditBookDialog(book: Book, onSave: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(book.title) }
    var author by rememberSaveable { mutableStateOf(book.author.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.detail_field_title)) },
                    isError = title.isBlank(),
                    singleLine = true,
                    modifier = Modifier.testTag("edit_title"),
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.detail_field_author)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, author) }, modifier = Modifier.testTag("save_edit")) {
                Text(stringResource(DesignR.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.common_cancel)) } },
    )
}

private const val MAX_SESSIONS = 10
