package com.mid.varagh.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.AvatarStore
import com.mid.varagh.core.domain.repository.UserProfileRepository
import com.mid.varagh.core.domain.usecase.ObserveCurrentlyReadingUseCase
import com.mid.varagh.core.domain.usecase.ObserveReadingStatsUseCase
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.core.model.ReadingStats
import com.mid.varagh.core.model.UserProfile
import com.mid.varagh.feature.profile.share.ShareCardRenderer
import com.mid.varagh.feature.profile.share.ShareCardText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile.DefaultLocal,
    val currentlyReading: BookWithProgress? = null,
    val stats: ReadingStats = ReadingStats.Empty,
    val auth: AuthState? = null,
    val sharing: Boolean = false,
)

sealed interface ProfileEvent {
    data class Share(val uri: Uri) : ProfileEvent
    data class Failed(val error: Throwable) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: UserProfileRepository,
    private val avatars: AvatarStore,
    private val shareCards: ShareCardRenderer,
    private val auth: AuthRepository,
    val featureFlags: FeatureFlags,
    observeCurrentlyReading: ObserveCurrentlyReadingUseCase,
    observeStats: ObserveReadingStatsUseCase,
) : ViewModel() {

    private val sharing = MutableStateFlow(false)
    private val events = Channel<ProfileEvent>(Channel.BUFFERED)
    val eventFlow: Flow<ProfileEvent> = events.receiveAsFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        profiles.observeProfile(),
        observeCurrentlyReading(),
        observeStats(),
        auth.authState,
        sharing,
    ) { profile, reading, stats, authState, isSharing ->
        ProfileUiState(false, profile, reading, stats, authState, isSharing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun onSaveProfile(displayName: String, username: String, bio: String) = launchSafely {
        profiles.updateProfile {
            it.copy(
                displayName = displayName.trim().take(MAX_NAME),
                username = normalizeUsername(username),
                bio = bio.trim().take(MAX_BIO),
            )
        }
    }

    fun onAvatarPicked(uri: String) = launchSafely {
        val old = uiState.value.profile.avatarPath
        val path = avatars.saveAvatar(uri)
        profiles.updateProfile { it.copy(avatarPath = path) }
        avatars.deleteAvatar(old)
    }

    fun onPublicChange(isPublic: Boolean) = launchSafely { profiles.updateProfile { it.copy(isPublic = isPublic) } }

    fun onLogout() = launchSafely { auth.logout() }

    fun onShare(text: ShareCardText) {
        val reading = uiState.value.currentlyReading ?: return
        launchSafely {
            sharing.value = true
            try {
                val uri = shareCards.render(text, reading.book.coverPath, reading.progress?.percent ?: 0f)
                events.send(ProfileEvent.Share(uri))
            } finally {
                sharing.value = false
            }
        }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() }.onFailure { events.send(ProfileEvent.Failed(it)) } }
    }

    companion object {
        const val MAX_NAME = 40
        const val MAX_BIO = 160
        private val UsernameAllowed = Regex("[^a-z0-9_.]")

        /** Lower-case latin letters, digits, '_' and '.', at most 20 characters. */
        fun normalizeUsername(input: String): String =
            input.trim().removePrefix("@").lowercase().replace(UsernameAllowed, "").take(20)

        fun isValidUsername(input: String): Boolean = input.isEmpty() || normalizeUsername(input).length >= 3
    }
}
