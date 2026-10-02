package com.mid.varagh.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mid.varagh.core.designsystem.component.BookCover
import com.mid.varagh.core.designsystem.component.VaraghCard
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.component.VaraghFeatureRow
import com.mid.varagh.core.designsystem.component.VaraghHeroCard
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghProgressBar
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.component.labelRes
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.illustration.BookStackIllustration
import com.mid.varagh.core.designsystem.motion.AppearAnimated
import com.mid.varagh.core.designsystem.theme.VaraghPillShape
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.ReadingStatus
import kotlin.math.roundToInt

@Composable
internal fun LibraryScreenRoute(
    onOpenBook: (bookId: Long) -> Unit,
    onOpenDetails: (bookId: Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.onImport(uris.map { it.toString() })
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            val text = when (message) {
                is LibraryMessage.Added -> context.getString(R.string.library_added, message.title)
                is LibraryMessage.Duplicate -> context.getString(R.string.library_duplicate, message.title)
                is LibraryMessage.Failed -> context.getString(errorMessageRes(message.error))
            }
            snackbar.showSnackbar(text)
        }
    }
    LibraryScreen(
        state = state,
        snackbarHostState = snackbar,
        onAddBook = { picker.launch(arrayOf(PDF_MIME)) },
        onOpenBook = onOpenBook,
        onOpenDetails = onOpenDetails,
        onSearchChange = viewModel::onSearchChange,
        onSearchActiveChange = viewModel::onSearchActiveChange,
        onStatusFilter = viewModel::onStatusFilter,
        onSortChange = viewModel::onSortChange,
        onToggleLayout = viewModel::onToggleLayout,
    )
}

@Composable
internal fun LibraryScreen(
    state: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onAddBook: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onOpenDetails: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onStatusFilter: (ReadingStatus?) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            VaraghTopAppBar(
                title = stringResource(R.string.library_title),
                actions = {
                    if (!state.libraryEmpty) {
                        IconButton(onClick = { onSearchActiveChange(!state.searchActive) }) {
                            Icon(
                                if (state.searchActive) VaraghIcons.Close else VaraghIcons.Search,
                                contentDescription = stringResource(R.string.library_search),
                            )
                        }
                        SortMenu(current = state.sort, onSortChange = onSortChange)
                        IconButton(onClick = onToggleLayout, modifier = Modifier.testTag("layout_toggle")) {
                            Icon(
                                if (state.grid) VaraghIcons.List else VaraghIcons.Grid,
                                contentDescription = stringResource(
                                    if (state.grid) R.string.library_show_list else R.string.library_show_grid,
                                ),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.libraryEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onAddBook,
                    icon = { Icon(VaraghIcons.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.library_add_book)) },
                    shape = VaraghPillShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_book_fab"),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.importing) {
                LinearProgressIndicator(Modifier.fillMaxWidth().testTag("importing"))
            }
            when {
                state.loading -> VaraghLoading()
                state.libraryEmpty -> LibraryEmptyContent(onAddBook = onAddBook)
                else -> {
                    AnimatedVisibility(visible = state.searchActive) {
                        SearchField(value = state.search, onValueChange = onSearchChange)
                    }
                    StatusFilterRow(selected = state.statusFilter, onSelect = onStatusFilter)
                    if (state.books.isEmpty()) {
                        VaraghEmptyState(
                            icon = VaraghIcons.Search,
                            title = stringResource(R.string.library_no_results_title),
                            message = stringResource(R.string.library_no_results_message),
                        )
                    } else if (state.grid) {
                        BookGrid(state.books, onOpenBook, onOpenDetails)
                    } else {
                        BookList(state.books, onOpenBook, onOpenDetails)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.library_search_hint)) },
        leadingIcon = { Icon(VaraghIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(VaraghIcons.Close, contentDescription = stringResource(R.string.library_clear_search))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(),
        shape = VaraghPillShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VaraghSpacing.ScreenGutter, vertical = VaraghSpacing.Small)
            .focusRequester(focus)
            .testTag("search_field"),
    )
}

@Composable
private fun StatusFilterRow(selected: ReadingStatus?, onSelect: (ReadingStatus?) -> Unit) {
    val options: List<ReadingStatus?> = listOf(null) + ReadingStatus.entries
    LazyRow(
        contentPadding = PaddingValues(horizontal = VaraghSpacing.ScreenGutter, vertical = VaraghSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Small),
    ) {
        items(options) { status ->
            val isSelected = status == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(status) },
                label = { Text(stringResource(status?.labelRes() ?: R.string.library_filter_all)) },
                leadingIcon = if (isSelected) {
                    { Icon(VaraghIcons.Check, contentDescription = null) }
                } else {
                    null
                },
                shape = VaraghPillShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
                modifier = Modifier.testTag("filter_${status?.name ?: "ALL"}"),
            )
        }
    }
}

@Composable
private fun SortMenu(current: LibrarySort, onSortChange: (LibrarySort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(VaraghIcons.Sort, contentDescription = stringResource(R.string.library_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LibrarySort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes())) },
                    onClick = {
                        open = false
                        onSortChange(sort)
                    },
                    trailingIcon = if (sort == current) {
                        { Icon(VaraghIcons.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.LAST_OPENED -> R.string.library_sort_last_opened
    LibrarySort.TITLE -> R.string.library_sort_title
    LibrarySort.DATE_ADDED -> R.string.library_sort_added
}

/** Extra space under the last row so the floating action button never covers a book. */
private val ListBottomPadding = 96.dp

@Composable
private fun BookGrid(books: List<BookWithProgress>, onOpen: (Long) -> Unit, onDetails: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(
            start = VaraghSpacing.ScreenGutter,
            end = VaraghSpacing.ScreenGutter,
            top = VaraghSpacing.Small,
            bottom = ListBottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(VaraghSpacing.Large),
        modifier = Modifier.testTag("book_grid"),
    ) {
        items(books, key = { it.book.id }) { item ->
            BookGridItem(item, onOpen, onDetails)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridItem(item: BookWithProgress, onOpen: (Long) -> Unit, onDetails: (Long) -> Unit) {
    val book = item.book
    val openLabel = stringResource(R.string.library_open_book, book.title)
    val detailsLabel = stringResource(R.string.library_book_details)
    Column(
        modifier = Modifier
            .combinedClickable(
                onClickLabel = openLabel,
                onLongClickLabel = detailsLabel,
                onLongClick = { onDetails(book.id) },
                onClick = { onOpen(book.id) },
            )
            .testTag("book_${book.id}"),
        verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall),
    ) {
        BookCover(coverPath = book.coverPath, title = book.title, modifier = Modifier.fillMaxWidth())
        item.progress?.let { VaraghProgressBar(it.percent) }
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        book.author?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BookList(books: List<BookWithProgress>, onOpen: (Long) -> Unit, onDetails: (Long) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = VaraghSpacing.ScreenGutter,
            end = VaraghSpacing.ScreenGutter,
            top = VaraghSpacing.Small,
            bottom = ListBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(VaraghSpacing.CardGap),
        modifier = Modifier.testTag("book_list"),
    ) {
        items(books, key = { it.book.id }) { item -> BookListItem(item, onOpen, onDetails) }
    }
}

@Composable
private fun BookListItem(item: BookWithProgress, onOpen: (Long) -> Unit, onDetails: (Long) -> Unit) {
    val book = item.book
    val locale = currentLocale()
    val percent = ((item.progress?.percent ?: 0f) * 100).roundToInt()
    VaraghCard(
        onClick = { onOpen(book.id) },
        contentPadding = PaddingValues(VaraghSpacing.Medium),
        modifier = Modifier.testTag("book_${book.id}"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(VaraghSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
            BookCover(coverPath = book.coverPath, title = book.title, modifier = Modifier.width(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                book.author?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    text = stringResource(book.status.labelRes()) + " · " +
                        stringResource(R.string.library_percent, formatNumber(percent.toLong(), locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VaraghProgressBar(item.progress?.percent ?: 0f)
            }
            IconButton(onClick = { onDetails(book.id) }) {
                Icon(VaraghIcons.More, contentDescription = stringResource(R.string.library_book_details))
            }
        }
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

private const val PDF_MIME = "application/pdf"
