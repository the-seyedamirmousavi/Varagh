package com.mid.varagh.feature.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.SocialRepository
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.FeedItem
import com.mid.varagh.core.model.PublicReader
import com.mid.varagh.feature.social.navigation.PublicProfileRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Generic load state for server-backed screens: graceful when the server is unreachable. */
sealed interface Remote<out T> {
    data object Loading : Remote<Nothing>
    data class Loaded<T>(val value: T) : Remote<T>
    data class Failed(val error: Throwable) : Remote<Nothing>
}

data class FeedUiState(
    val items: List<FeedItem> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: Throwable? = null,
    val signedIn: Boolean = true,
) {
    val endReached: Boolean get() = !loading && nextCursor == null
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val social: SocialRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    val authState: StateFlow<AuthState?> = auth.authState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var job: Job? = null

    init {
        viewModelScope.launch {
            authState.collect { auth ->
                val signedIn = auth is AuthState.SignedIn
                _state.update { it.copy(signedIn = signedIn) }
                if (signedIn) refresh() else _state.update { it.copy(loading = false, items = emptyList()) }
            }
        }
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { social.getFeed(cursor = null) }
                .onSuccess { page -> _state.update { it.copy(items = page.items, nextCursor = page.nextCursor, loading = false) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e, signedIn = e !is VaraghException.Unauthorized && it.signedIn) } }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loadingMore || job?.isActive == true) return
        job = viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            runCatching { social.getFeed(cursor) }
                .onSuccess { page ->
                    _state.update { s ->
                        s.copy(items = (s.items + page.items).distinctBy { it.id }, nextCursor = page.nextCursor, loadingMore = false)
                    }
                }
                .onFailure { e -> _state.update { it.copy(loadingMore = false, error = e) } }
        }
    }
}

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val social: SocialRepository,
) : ViewModel() {
    private val _result = MutableStateFlow<Remote<PublicReader>?>(null)
    val result: StateFlow<Remote<PublicReader>?> = _result.asStateFlow()

    fun search(username: String) {
        val query = username.trim().removePrefix("@")
        if (query.length < MIN_QUERY) return
        viewModelScope.launch {
            _result.value = Remote.Loading
            _result.value = runCatching { social.getReader(query) }.fold({ Remote.Loaded(it) }, { Remote.Failed(it) })
        }
    }

    private companion object {
        const val MIN_QUERY = 3
    }
}

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val social: SocialRepository,
) : ViewModel() {
    val username: String = savedStateHandle.toRoute<PublicProfileRoute>().username

    private val _reader = MutableStateFlow<Remote<PublicReader>>(Remote.Loading)
    val reader: StateFlow<Remote<PublicReader>> = _reader.asStateFlow()

    private val _followBusy = MutableStateFlow(false)
    val followBusy: StateFlow<Boolean> = _followBusy.asStateFlow()

    private val errors = Channel<Throwable>(Channel.BUFFERED)
    val errorFlow: Flow<Throwable> = errors.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _reader.value = Remote.Loading
            _reader.value = runCatching { social.getReader(username) }.fold({ Remote.Loaded(it) }, { Remote.Failed(it) })
        }
    }

    fun toggleFollow() {
        val current = (_reader.value as? Remote.Loaded)?.value ?: return
        viewModelScope.launch {
            _followBusy.value = true
            runCatching { if (current.isFollowedByMe) social.unfollow(current.id) else social.follow(current.id) }
                .onSuccess { _reader.value = Remote.Loaded(current.copy(isFollowedByMe = !current.isFollowedByMe)) }
                .onFailure { errors.send(it) }
            _followBusy.value = false
        }
    }
}

data class LoginUiState(
    val registering: Boolean = false,
    val busy: Boolean = false,
    val error: Throwable? = null,
    val done: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setRegistering(registering: Boolean) = _state.update { it.copy(registering = registering, error = null) }

    fun submit(email: String, password: String, username: String) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching {
                if (_state.value.registering) auth.register(username, email, password) else auth.login(email, password)
            }.onSuccess { _state.update { it.copy(busy = false, done = true) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e) } }
        }
    }

    companion object {
        private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val MIN_PASSWORD = 8

        fun isValid(registering: Boolean, email: String, password: String, username: String): Boolean =
            EmailPattern.matches(email.trim()) && password.length >= MIN_PASSWORD &&
                (!registering || Regex("^[a-z0-9_.]{3,20}$").matches(username.trim()))
    }
}

@HiltViewModel
class ReadersOfBookViewModel @Inject constructor(
    private val social: SocialRepository,
) : ViewModel() {
    private val _readers = MutableStateFlow<Remote<List<PublicReader>>>(Remote.Loading)
    val readers: StateFlow<Remote<List<PublicReader>>> = _readers.asStateFlow()
    private var loadedFor: String? = null

    fun load(remoteBookId: String, force: Boolean = false) {
        if (!force && loadedFor == remoteBookId) return
        loadedFor = remoteBookId
        viewModelScope.launch {
            _readers.value = Remote.Loading
            _readers.value = runCatching { social.readersOfBook(remoteBookId) }.fold({ Remote.Loaded(it) }, { Remote.Failed(it) })
        }
    }
}
