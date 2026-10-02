package com.mid.varagh

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.DarkThemeConfig
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.ui.VaraghApp
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {
    /** Null until DataStore has been read, so the first frame already uses the right theme. */
    val preferences: StateFlow<UserPreferences?> = preferences.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/**
 * Single activity. Extends AppCompatActivity (a ComponentActivity) so per-app language switching
 * via AppCompatDelegate works on API < 33 as well.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var featureFlags: FeatureFlags

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val prefs = preferences ?: return@setContent
            val darkTheme = when (prefs.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                DarkThemeConfig.LIGHT -> false
                DarkThemeConfig.DARK -> true
            }
            // Status/navigation bar icons follow the app theme, not only the system setting.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            LaunchedEffect(prefs.language) { applyLanguage(prefs.language) }
            VaraghTheme(darkTheme = darkTheme, dynamicColor = prefs.useDynamicColor) {
                VaraghApp(featureFlags = featureFlags)
            }
        }
    }

    /** Persian is the default UI language; the choice is stored by AppCompat and survives restarts. */
    private fun applyLanguage(language: AppLanguage) {
        val desired = LocaleListCompat.forLanguageTags(language.tag)
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != desired.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
