package com.mid.varagh.ui

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.FeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Screen-level smoke tests of the app shell. Default resources are Persian, so these run in fa/RTL
 * (Robolectric's default locale has no values-xx match -> Persian strings).
 */
@RunWith(AndroidJUnit4::class)
class VaraghAppTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val offline = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "https://api.example.com/v1/", isDebugBuild = true)
    private val online = offline.copy(useRemoteBackend = true)

    @Test
    @Config(qualifiers = "fa")
    fun offlineBuild_hasNoSocialTab_andLayoutIsRtl() {
        var direction: LayoutDirection? = null
        composeRule.setContent {
            direction = LocalLayoutDirection.current
            VaraghTheme { VaraghApp(featureFlags = offline) }
        }
        composeRule.onNodeWithTag("nav_library").assertExists().assertIsSelected()
        composeRule.onNodeWithTag("nav_social").assertDoesNotExist()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun onlineBuild_showsSocialTab_andNavigatesToIt() {
        composeRule.setContent {
            VaraghTheme { VaraghApp(featureFlags = online) }
        }
        composeRule.onNodeWithTag("nav_social").assertExists().performClick()
        composeRule.onNodeWithTag("nav_social").assertIsSelected()
    }

    @Test
    fun historyTab_navigates() {
        composeRule.setContent {
            VaraghTheme { VaraghApp(featureFlags = offline) }
        }
        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.onNodeWithTag("nav_history").assertIsSelected()
    }
}
