package com.mid.varagh.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.CustomReadingColors
import com.mid.varagh.core.model.DarkThemeConfig
import com.mid.varagh.core.model.ReadingMode
import com.mid.varagh.core.model.ReadingTheme
import com.mid.varagh.core.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val dataStore = PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
        tmp.newFile("prefs.preferences_pb")
    }

    private val repository = DataStoreUserPreferencesRepository(dataStore)

    @Test
    fun defaultsWhenEmpty() = testScope.runTest {
        assertEquals(UserPreferences(), repository.preferences.first())
    }

    @Test
    fun roundTripsEveryField() = testScope.runTest {
        val custom = UserPreferences(
            readingTheme = ReadingTheme.CUSTOM,
            customReadingColors = CustomReadingColors(0xFF101010.toInt(), 0xFFEEEEEE.toInt()),
            readingMode = ReadingMode.HORIZONTAL_PAGED,
            rightToLeftPaging = false,
            keepScreenOn = false,
            darkThemeConfig = DarkThemeConfig.DARK,
            useDynamicColor = true,
            language = AppLanguage.ENGLISH,
            warmFilter = 0.4f,
            readerBrightness = 0.3f,
        )
        repository.preferences.test {
            assertEquals(UserPreferences(), awaitItem())
            repository.update { custom }
            assertEquals(custom, awaitItem())
            repository.update { it.copy(readerBrightness = null, warmFilter = 7f) }
            val cleared = awaitItem()
            assertNull(cleared.readerBrightness)
            assertEquals(1f, cleared.warmFilter, 0f)
        }
    }

    @Test
    fun unknownEnumValuesFallBackToDefaults() = testScope.runTest {
        dataStore.edit { it[stringPreferencesKey("reading_theme")] = "NEON" }
        assertEquals(UserPreferences().readingTheme, repository.preferences.first().readingTheme)
    }
}
