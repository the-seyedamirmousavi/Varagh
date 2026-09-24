package com.mid.varagh.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.CustomReadingColors
import com.mid.varagh.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Device settings in Preferences DataStore. Unknown/corrupt values fall back to defaults. */
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toUserPreferences() }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toUserPreferences())) }
    }

    private object Keys {
        val readingTheme = stringPreferencesKey("reading_theme")
        val customBackground = intPreferencesKey("custom_background_argb")
        val customText = intPreferencesKey("custom_text_argb")
        val readingMode = stringPreferencesKey("reading_mode")
        val rtlPaging = booleanPreferencesKey("rtl_paging")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val darkTheme = stringPreferencesKey("dark_theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val language = stringPreferencesKey("language")
        val warmFilter = floatPreferencesKey("warm_filter")
        val readerBrightness = floatPreferencesKey("reader_brightness")
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val d = UserPreferences()
        return UserPreferences(
            readingTheme = enumOrDefault(this[Keys.readingTheme], d.readingTheme),
            customReadingColors = CustomReadingColors(
                backgroundArgb = this[Keys.customBackground] ?: d.customReadingColors.backgroundArgb,
                textArgb = this[Keys.customText] ?: d.customReadingColors.textArgb,
            ),
            readingMode = enumOrDefault(this[Keys.readingMode], d.readingMode),
            rightToLeftPaging = this[Keys.rtlPaging] ?: d.rightToLeftPaging,
            keepScreenOn = this[Keys.keepScreenOn] ?: d.keepScreenOn,
            darkThemeConfig = enumOrDefault(this[Keys.darkTheme], d.darkThemeConfig),
            useDynamicColor = this[Keys.dynamicColor] ?: d.useDynamicColor,
            language = this[Keys.language]?.let(AppLanguage::fromTag) ?: d.language,
            warmFilter = (this[Keys.warmFilter] ?: d.warmFilter).coerceIn(0f, 1f),
            readerBrightness = this[Keys.readerBrightness]?.coerceIn(MIN_BRIGHTNESS, 1f),
        )
    }

    private fun MutablePreferences.write(p: UserPreferences) {
        this[Keys.readingTheme] = p.readingTheme.name
        this[Keys.customBackground] = p.customReadingColors.backgroundArgb
        this[Keys.customText] = p.customReadingColors.textArgb
        this[Keys.readingMode] = p.readingMode.name
        this[Keys.rtlPaging] = p.rightToLeftPaging
        this[Keys.keepScreenOn] = p.keepScreenOn
        this[Keys.darkTheme] = p.darkThemeConfig.name
        this[Keys.dynamicColor] = p.useDynamicColor
        this[Keys.language] = p.language.tag
        this[Keys.warmFilter] = p.warmFilter.coerceIn(0f, 1f)
        val brightness = p.readerBrightness
        if (brightness == null) remove(Keys.readerBrightness) else this[Keys.readerBrightness] = brightness.coerceIn(MIN_BRIGHTNESS, 1f)
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

    private companion object {
        const val MIN_BRIGHTNESS = 0.01f
    }
}
