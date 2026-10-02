package com.mid.varagh.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.usecase.DeleteBookUseCase
import com.mid.varagh.core.domain.usecase.ObserveReadingStatsUseCase
import com.mid.varagh.core.domain.usecase.SetBookRatingUseCase
import com.mid.varagh.core.domain.usecase.SetBookStatusUseCase
import com.mid.varagh.core.domain.usecase.UpdateBookDetailsUseCase
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.ReadingStats
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.feature.history.navigation.BookDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val stats: ReadingStats = ReadingStats.Empty,
    val reading: List<BookWithProgress> = emptyList(),
    val finished: List<BookWithProgress> = emptyList(),
    val wantToRead: List<BookWithProgress> = emptyList(),
    val abandoned: List<BookWithProgress> = emptyList(),
) {
    val isEmpty: Boolean get() = reading.isEmpty() && finished.isEmpty() && wantToRead.isEmpty() && abandoned.isEmpty()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    books: BookRepository,
    observeStats: ObserveReadingStatsUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        observeStats(),
        books.observeBooksByStatus(ReadingStatus.READING),
        books.observeBooksByStatus(ReadingStatus.FINISHED),
        books.observeBooksByStatus(ReadingStatus.WANT_TO_READ),
        books.observeBooksByStatus(ReadingStatus.ABANDONED),
    ) { stats, reading, finished, want, abandoned ->
        HistoryUiState(
            loading = false,
            stats = stats,
            reading = reading,
            // Most recently finished first.
            finished = finished.sortedByDescending { it.book.finishedAt ?: 0L },
            wantToRead = want,
            abandoned = abandoned,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}

data class BookDetailUiState(
    val loading: Boolean = true,
    val book: BookWithProgress? = null,
    val sessions: List<ReadingSession> = emptyList(),
    val totalMillis: Long = 0,
    val bookmarks: List<Bookmark> = emptyList(),
) {
    val pagesRead: Int get() = sessions.sumOf { it.pagesRead }
}

sealed interface BookDetailEvent {
    data object Deleted : BookDetailEvent
    data class Failed(val error: Throwable) : BookDetailEvent
}

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    books: BookRepository,
    sessions: ReadingSessionRepository,
    bookmarks: BookmarkRepository,
    private val setStatus: SetBookStatusUseCase,
    private val setRating: SetBookRatingUseCase,
    private val updateDetails: UpdateBookDetailsUseCase,
    private val deleteBook: DeleteBookUseCase,
) : ViewModel() {

    val bookId: Long = savedStateHandle.toRoute<BookDetailRoute>().bookId

    private val events = Channel<BookDetailEvent>(Channel.BUFFERED)
    val eventFlow: Flow<BookDetailEvent> = events.receiveAsFlow()

    val uiState: StateFlow<BookDetailUiState> = combine(
        books.observeBook(bookId),
        sessions.observeSessions(bookId),
        sessions.observeTotalReadingMillis(bookId),
        bookmarks.observeBookmarks(bookId),
    ) { book, sessionList, total, marks ->
        BookDetailUiState(loading = false, book = book, sessions = sessionList, totalMillis = total, bookmarks = marks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailUiState())

    fun onStatus(status: ReadingStatus) = launchSafely { setStatus(bookId, status) }

    fun onRating(rating: Int?) = launchSafely { setRating(bookId, rating) }

    fun onSaveDetails(title: String, author: String?) = launchSafely { updateDetails(bookId, title, author) }

    fun onDelete() = launchSafely {
        deleteBook(bookId)
        events.send(BookDetailEvent.Deleted)
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { events.send(BookDetailEvent.Failed(it)) }
        }
    }
}
