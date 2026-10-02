package com.mid.varagh.feature.reader

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mid.varagh.core.designsystem.component.VaraghEmptyState
import com.mid.varagh.core.designsystem.component.VaraghLoading
import com.mid.varagh.core.designsystem.component.VaraghTopAppBar
import com.mid.varagh.core.designsystem.component.currentLocale
import com.mid.varagh.core.designsystem.component.errorMessageRes
import com.mid.varagh.core.designsystem.component.formatNumber
import com.mid.varagh.core.designsystem.icon.VaraghIcons
import com.mid.varagh.core.designsystem.reading.ReadingPalette
import com.mid.varagh.core.designsystem.reading.warmFilter
import com.mid.varagh.core.designsystem.theme.VaraghDimens
import com.mid.varagh.core.designsystem.theme.VaraghSpacing
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.ReadingMode
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.feature.reader.pdf.PdfDocument
import com.mid.varagh.core.designsystem.R as DesignR

@Composable
internal fun ReaderScreenRoute(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val document by viewModel.document.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }
    val context = LocalContext.current
    DisposableEffect(viewModel) {
        val callbacks = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) = viewModel.onTrimMemory(level)
            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = viewModel.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }
        context.registerComponentCallbacks(callbacks)
        onDispose { context.unregisterComponentCallbacks(callbacks) }
    }
    val relinkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.relink(it.toString()) }
    }

    ReaderScreen(
        state = state,
        preferences = preferences,
        document = document,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLocateFile = { relinkPicker.launch(arrayOf("application/pdf")) },
        onPageChanged = viewModel::onPageChanged,
        onToggleBookmark = viewModel::onToggleBookmark,
        onSaveNote = viewModel::onAddBookmarkNote,
        onEditBookmark = viewModel::onEditBookmark,
        onDeleteBookmark = viewModel::onDeleteBookmark,
        onPreferencesChange = viewModel::updatePreferences,
    )
}

@Composable
internal fun ReaderScreen(
    state: ReaderUiState,
    preferences: UserPreferences,
    document: PdfDocument?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLocateFile: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveNote: (String) -> Unit,
    onEditBookmark: (Bookmark, String) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onPreferencesChange: ((UserPreferences) -> UserPreferences) -> Unit,
) {
    when (val load = state.load) {
        ReaderLoadState.Loading -> Scaffold(topBar = { ReaderPlainTopBar(state.title, onBack) }) { padding ->
            VaraghLoading(Modifier.padding(padding))
        }
        is ReaderLoadState.Failed -> Scaffold(topBar = { ReaderPlainTopBar(state.title, onBack) }) { padding ->
            val fileProblem = load.error is VaraghException.FileUnavailable || load.error is VaraghException.DifferentFile
            VaraghEmptyState(
                icon = VaraghIcons.Error,
                title = stringResource(R.string.reader_cannot_open),
                message = stringResource(errorMessageRes(load.error)),
                actionLabel = stringResource(if (fileProblem) R.string.reader_locate_file else DesignR.string.common_retry),
                actionIcon = if (fileProblem) VaraghIcons.Import else null,
                onAction = if (fileProblem) onLocateFile else onRetry,
                modifier = Modifier
                    .padding(padding)
                    .testTag("reader_error"),
            )
        }
        ReaderLoadState.Ready -> if (document != null) {
            ReadyReader(
                state = state,
                preferences = preferences,
                document = document,
                onBack = onBack,
                onPageChanged = onPageChanged,
                onToggleBookmark = onToggleBookmark,
                onSaveNote = onSaveNote,
                onEditBookmark = onEditBookmark,
                onDeleteBookmark = onDeleteBookmark,
                onPreferencesChange = onPreferencesChange,
            )
        }
    }
}

@Composable
private fun ReaderPlainTopBar(title: String, onBack: () -> Unit) {
    VaraghTopAppBar(
        title = title,
        centered = false,
        onBack = onBack,
        backContentDescription = stringResource(DesignR.string.common_back),
    )
}

@Composable
private fun ReadyReader(
    state: ReaderUiState,
    preferences: UserPreferences,
    document: PdfDocument,
    onBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onSaveNote: (String) -> Unit,
    onEditBookmark: (Bookmark, String) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onPreferencesChange: ((UserPreferences) -> UserPreferences) -> Unit,
) {
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showBookmarks by rememberSaveable { mutableStateOf(false) }
    var showGoTo by rememberSaveable { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<NoteTarget?>(null) }
    var jump by remember { mutableStateOf<JumpRequest?>(null) }
    val palette = ReadingPalette.forTheme(preferences.readingTheme, preferences.customReadingColors)

    KeepScreenOn(preferences.keepScreenOn)
    WindowBrightness(preferences.readerBrightness)
    ImmersiveMode(immersive = !controlsVisible)

    Box(
        Modifier
            .fillMaxSize()
            .warmFilter(preferences.warmFilter),
    ) {
        when (preferences.readingMode) {
            ReadingMode.VERTICAL_SCROLL -> VerticalReader(
                document = document,
                state = state,
                palette = palette,
                jumpRequest = jump,
                onJumpHandled = { jump = null },
                onPageChanged = onPageChanged,
                onTap = { controlsVisible = !controlsVisible },
            )
            ReadingMode.HORIZONTAL_PAGED -> PagedReader(
                document = document,
                state = state,
                palette = palette,
                rightToLeft = preferences.rightToLeftPaging,
                jumpRequest = jump,
                onJumpHandled = { jump = null },
                onPageChanged = onPageChanged,
                onTap = { controlsVisible = !controlsVisible },
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopControls(
                title = state.title,
                bookmarked = state.currentBookmark != null,
                onBack = onBack,
                onToggleBookmark = onToggleBookmark,
                onEditNote = { noteFor = NoteTarget.Current(state.currentBookmark?.note.orEmpty()) },
                onShowBookmarks = { showBookmarks = true },
                onShowSettings = { showSettings = true },
            )
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderBottomControls(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                onSeek = { page ->
                    onPageChanged(page)
                    jump = JumpRequest(page)
                },
                onGoToPage = { showGoTo = true },
            )
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(preferences = preferences, onChange = onPreferencesChange, onDismiss = { showSettings = false })
    }
    if (showBookmarks) {
        BookmarksSheet(
            bookmarks = state.bookmarks,
            onOpen = {
                showBookmarks = false
                jump = JumpRequest(it.page)
            },
            onEdit = { noteFor = NoteTarget.Existing(it) },
            onDelete = onDeleteBookmark,
            onDismiss = { showBookmarks = false },
        )
    }
    if (showGoTo) {
        GoToPageDialog(
            pageCount = state.pageCount,
            onGo = {
                showGoTo = false
                jump = JumpRequest(it)
            },
            onDismiss = { showGoTo = false },
        )
    }
    noteFor?.let { target ->
        NoteDialog(
            initial = when (target) {
                is NoteTarget.Current -> target.note
                is NoteTarget.Existing -> target.bookmark.note.orEmpty()
            },
            onSave = { note ->
                when (target) {
                    is NoteTarget.Current -> onSaveNote(note)
                    is NoteTarget.Existing -> onEditBookmark(target.bookmark, note)
                }
                noteFor = null
            },
            onDismiss = { noteFor = null },
        )
    }
}

private sealed interface NoteTarget {
    data class Current(val note: String) : NoteTarget
    data class Existing(val bookmark: Bookmark) : NoteTarget
}

@Composable
private fun ReaderTopControls(
    title: String,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEditNote: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowSettings: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = CONTROLS_ALPHA),
        shadowElevation = VaraghDimens.BarElevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = VaraghSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(VaraghIcons.Back, contentDescription = stringResource(DesignR.string.common_back))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val bookmarkLabel = stringResource(if (bookmarked) R.string.reader_remove_bookmark else R.string.reader_add_bookmark)
            IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("bookmark_toggle")) {
                Icon(
                    if (bookmarked) VaraghIcons.Bookmark else VaraghIcons.BookmarkBorder,
                    contentDescription = bookmarkLabel,
                    tint = if (bookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onEditNote) {
                Icon(VaraghIcons.Edit, contentDescription = stringResource(R.string.reader_bookmark_note))
            }
            IconButton(onClick = onShowBookmarks) {
                Icon(VaraghIcons.Bookmarks, contentDescription = stringResource(R.string.reader_bookmarks))
            }
            IconButton(onClick = onShowSettings, modifier = Modifier.testTag("reader_settings_button")) {
                Icon(VaraghIcons.Tune, contentDescription = stringResource(R.string.reader_settings))
            }
        }
    }
}

@Composable
private fun ReaderBottomControls(
    currentPage: Int,
    pageCount: Int,
    onSeek: (Int) -> Unit,
    onGoToPage: () -> Unit,
) {
    val locale = currentLocale()
    var dragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(currentPage.toFloat()) }
    if (!dragging) sliderValue = currentPage.toFloat()
    val shownPage = sliderValue.toInt() + 1
    val pageLabel = stringResource(
        R.string.reader_page_of,
        formatNumber(shownPage.toLong(), locale),
        formatNumber(pageCount.toLong(), locale),
    )
    val sliderLabel = stringResource(R.string.reader_page_slider)
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = CONTROLS_ALPHA),
        shadowElevation = VaraghDimens.BarElevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = VaraghSpacing.ScreenGutter, vertical = VaraghSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(VaraghSpacing.XSmall),
        ) {
            Text(
                text = pageLabel,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.reader_go_to_page), onClick = onGoToPage)
                    .padding(vertical = VaraghSpacing.XSmall)
                    .testTag("page_label"),
            )
            if (pageCount > 1) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        dragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        dragging = false
                        onSeek(sliderValue.toInt())
                    },
                    valueRange = 0f..(pageCount - 1).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = sliderLabel
                            stateDescription = pageLabel
                        },
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/** In-app brightness, independent of the system setting; null = follow the system. */
@Composable
private fun WindowBrightness(brightness: Float?) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, brightness) {
        window.attributes = window.attributes.apply {
            screenBrightness = brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }
}

@Composable
private fun ImmersiveMode(immersive: Boolean) {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    DisposableEffect(window, immersive) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) controller.hide(WindowInsetsCompat.Type.systemBars()) else controller.show(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private const val CONTROLS_ALPHA = 0.96f
