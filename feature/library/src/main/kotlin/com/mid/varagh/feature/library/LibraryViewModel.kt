package com.mid.varagh.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.domain.usecase.ImportBookUseCase
import com.mid.varagh.core.domain.usecase.ImportResult
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.LibraryQuery
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.ReadingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val loading: Boolean = true,
    val books: List<BookWithProgress> = emptyList(),
    /** True when there are no books at all (not just none matching the filter). */
    val libraryEmpty: Boolean = false,
    val search: String = "",
    val searchActive: Boolean = false,
    val statusFilter: ReadingStatus? = null,
    val sort: LibrarySort = LibrarySort.LAST_OPENED,
    val grid: Boolean = true,
    val importing: Boolean = false,
)

sealed interface LibraryMessage {
    data class Added(val title: String) : LibraryMessage
    data class Duplicate(val title: String) : LibraryMessage
    data class Failed(val error: Throwable) : LibraryMessage
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val books: BookRepository,
    private val preferences: UserPreferencesRepository,
    private val importBook: ImportBookUseCase,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val searchActive = MutableStateFlow(false)
    private val statusFilter = MutableStateFlow<ReadingStatus?>(null)
    private val importing = MutableStateFlow(false)
    private val messages = Channel<LibraryMessage>(Channel.BUFFERED)

    val events: Flow<LibraryMessage> = messages.receiveAsFlow()

    private val prefs = preferences.preferences
        .map { it.libraryGrid to it.librarySort }
        .distinctUntilChanged()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val query: Flow<LibraryQuery> = combine(
        search.debounce(SEARCH_DEBOUNCE_MS).onStart { emit(search.value) }.distinctUntilChanged(),
        statusFilter,
        prefs,
    ) { text, status, (_, sort) -> LibraryQuery(search = text, status = status, sort = sort) }
        .distinctUntilChanged()

    private val filtered = query.flatMapLatest { books.observeLibrary(it) }
    private val total = books.observeLibrary(LibraryQuery()).map { it.size }.distinctUntilChanged()

    val uiState: StateFlow<LibraryUiState> = combine(
        combine(filtered, total, prefs) { list, count, layout -> Triple(list, count, layout) },
        search,
        searchActive,
        statusFilter,
        importing,
    ) { (list, count, layout), text, active, status, busy ->
        LibraryUiState(
            loading = false,
            books = list,
            libraryEmpty = count == 0,
            search = text,
            searchActive = active,
            statusFilter = status,
            sort = layout.second,
            grid = layout.first,
            importing = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun onSearchChange(text: String) = search.update { text }

    fun onSearchActiveChange(active: Boolean) {
        searchActive.value = active
        if (!active) search.value = ""
    }

    fun onStatusFilter(status: ReadingStatus?) = statusFilter.update { status }

    fun onSortChange(sort: LibrarySort) = viewModelScope.launch {
        preferences.update { it.copy(librarySort = sort) }
    }

    fun onToggleLayout() = viewModelScope.launch {
        preferences.update { it.copy(libraryGrid = !it.libraryGrid) }
    }

    fun onImport(uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            try {
                for (uri in uris) {
                    val message = runCatching { importBook(uri) }.fold(
                        onSuccess = { result ->
                            when (result) {
                                is ImportResult.Added -> LibraryMessage.Added(result.title)
                                is ImportResult.Duplicate -> LibraryMessage.Duplicate(result.existing.title)
                            }
                        },
                        onFailure = { LibraryMessage.Failed(it) },
                    )
                    messages.send(message)
                }
            } finally {
                importing.value = false
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
