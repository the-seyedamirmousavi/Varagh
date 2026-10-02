package com.mid.varagh.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mid.varagh.core.domain.repository.BackupRepository
import com.mid.varagh.core.domain.repository.BackupSummary
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data class Exported(val summary: BackupSummary) : SettingsEvent
    data class Imported(val summary: BackupSummary) : SettingsEvent
    data class Failed(val error: Throwable) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val featureFlags: FeatureFlags,
    private val preferencesRepository: UserPreferencesRepository,
    private val backups: BackupRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow: Flow<SettingsEvent> = events.receiveAsFlow()

    fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch { preferencesRepository.update(transform) }
    }

    fun export(uri: String) = runBackup { SettingsEvent.Exported(backups.exportTo(uri)) }

    fun import(uri: String) = runBackup { SettingsEvent.Imported(backups.importFrom(uri)) }

    private fun runBackup(block: suspend () -> SettingsEvent) {
        viewModelScope.launch {
            _busy.value = true
            val event = runCatching { block() }.getOrElse { SettingsEvent.Failed(it) }
            _busy.value = false
            events.send(event)
        }
    }
}
