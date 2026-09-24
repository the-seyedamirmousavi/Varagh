package com.mid.varagh.feature.settings

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.FeatureFlags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeveloperInfoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsBackendFlagAndBaseUrl() {
        val flags = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "https://api.example.com/v1/", isDebugBuild = true)
        composeRule.setContent { VaraghTheme { DeveloperInfoScreen(featureFlags = flags, onBack = {}) } }
        composeRule.onNodeWithTag("flag_value").assertTextEquals("false")
        composeRule.onNodeWithText("https://api.example.com/v1/").assertExists()
    }

    @Test
    fun developerEntryHiddenInReleaseBuilds() {
        val flags = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "", isDebugBuild = false)
        composeRule.setContent { VaraghTheme { SettingsScreen(featureFlags = flags, onOpenDeveloperInfo = {}) } }
        composeRule.onNodeWithText("اطلاعات توسعه‌دهنده").assertDoesNotExist()
    }

    @Test
    fun developerEntryShownInDebugBuilds() {
        val flags = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "", isDebugBuild = true)
        composeRule.setContent { VaraghTheme { SettingsScreen(featureFlags = flags, onOpenDeveloperInfo = {}) } }
        composeRule.onNodeWithText("اطلاعات توسعه‌دهنده").assertExists()
    }
}
