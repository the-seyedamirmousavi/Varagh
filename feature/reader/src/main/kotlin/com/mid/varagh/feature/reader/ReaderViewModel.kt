package com.mid.varagh.feature.reader

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.domain.usecase.OpenBookUseCase
import com.mid.varagh.core.domain.usecase.RecordReadingSessionUseCase
import com.mid.varagh.core.domain.usecase.RelinkBookFileUseCase
import com.mid.varagh.core.domain.usecase.SaveReadingProgressUseCase
import com.mid.varagh.core.domain.usecase.ToggleBookmarkUseCase
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.feature.reader.navigation.ReaderRoute
import com.mid.varagh.feature.reader.pdf.PdfDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReaderLoadState {
    data object Loading : ReaderLoadState
    data class Failed(val error: Throwable) : ReaderLoadState
    data object Ready : ReaderLoadState
}

data class ReaderUiState(
    val load: ReaderLoadState = ReaderLoadState.Loading,
    val title: String = "",
    val pageCount: Int = 0,
    /** Page to show when the document first appears (the saved position). */
    val initialPage: Int = 0,
    val currentPage: Int = 0,
    /** Page sizes in PDF points; empty until measured. */
    val pageSizes: List<IntSize> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
) {
    val currentBookmark: Bookmark? get() = bookmarks.firstOrNull { it.page == currentPage }
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val opener: PdfDocument.Opener,
    private val openBook: OpenBookUseCase,
    private val books: BookRepository,
    private val bookmarks: BookmarkRepository,
    private val saveProgress: SaveReadingProgressUseCase,
    private val recordSession: RecordReadingSessionUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    private val relinkFile: RelinkBookFileUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReaderRoute>()
    val bookId: Long = route.bookId

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _document = MutableStateFlow<PdfDocument?>(null)
    val document: StateFlow<PdfDocument?> = _document.asStateFlow()

    private var saveJob: Job? = null
    private var sessionStart: Long? = null
    private val pagesThisSession = mutableSetOf<Int>()

    init {
        load()
        viewModelScope.launch {
            bookmarks.observeBookmarks(bookId).collect { list -> _uiState.update { it.copy(bookmarks = list) } }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(load = ReaderLoadState.Loading) }
            val result = runCatching {
                val book = openBook(bookId) ?: throw VaraghException.NotFound("Book")
                val savedPage = route.startPage.takeIf { it >= 0 }
                    ?: books.observeBook(bookId).first()?.progress?.currentPage ?: 0
                val doc = opener.open(book.fileUri)
                Triple(book, savedPage.coerceIn(0, (doc.pageCount - 1).coerceAtLeast(0)), doc)
            }
            result.onSuccess { (book, page, doc) ->
                _document.value?.close()
                _document.value = doc
                if (book.pageCount != doc.pageCount) books.updateFileInfo(bookId, doc.pageCount, book.coverPath)
                _uiState.update {
                    it.copy(
                        load = ReaderLoadState.Ready,
                        title = book.title,
                        pageCount = doc.pageCount,
                        initialPage = page,
                        currentPage = page,
                    )
                }
                pagesThisSession += page
                launch {
                    val sizes = runCatching { doc.allPageSizes() }.getOrDefault(emptyList())
                    _uiState.update { it.copy(pageSizes = sizes) }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(load = ReaderLoadState.Failed(error)) }
            }
        }
    }

    fun retry() = load()

    /** The user picked the moved file again; check it's the same book and reopen. */
    fun relink(fileUri: String) {
        viewModelScope.launch {
            runCatching { relinkFile(bookId, fileUri) }
                .onSuccess { load() }
                .onFailure { error -> _uiState.update { it.copy(load = ReaderLoadState.Failed(error)) } }
        }
    }

    fun onPageChanged(page: Int) {
        val state = _uiState.value
        if (page == state.currentPage || page !in 0 until state.pageCount) return
        _uiState.update { it.copy(currentPage = page) }
        pagesThisSession += page
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveProgress(bookId, page)
        }
    }

    fun onResume() {
        sessionStart = time.nowMillis()
        pagesThisSession.clear()
        pagesThisSession += _uiState.value.currentPage
    }

    /** Saves the exact position and closes the current reading session. */
    fun onPause() {
        val page = _uiState.value.currentPage
        val start = sessionStart
        val pages = pagesThisSession.size
        sessionStart = null
        saveJob?.cancel()
        if (_uiState.value.load != ReaderLoadState.Ready) return
        viewModelScope.launch {
            saveProgress(bookId, page)
            if (start != null) recordSession(bookId, start, time.nowMillis(), pages)
        }
    }

    fun onToggleBookmark() {
        val page = _uiState.value.currentPage
        viewModelScope.launch { toggleBookmark(bookId, page) }
    }

    fun onAddBookmarkNote(note: String) {
        val state = _uiState.value
        viewModelScope.launch {
            val existing = state.currentBookmark
            if (existing != null) {
                bookmarks.updateNote(existing.id, note.trim().ifBlank { null })
            } else {
                bookmarks.addBookmark(bookId, state.currentPage, note.trim().ifBlank { null })
            }
        }
    }

    fun onEditBookmark(bookmark: Bookmark, note: String) {
        viewModelScope.launch { bookmarks.updateNote(bookmark.id, note.trim().ifBlank { null }) }
    }

    fun onDeleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { bookmarks.deleteBookmark(bookmark.id) }
    }

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }

    fun onTrimMemory(level: Int) {
        _document.value?.trimMemory(level)
    }

    override fun onCleared() {
        _document.value?.close()
        _document.value = null
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 600L
    }
}
