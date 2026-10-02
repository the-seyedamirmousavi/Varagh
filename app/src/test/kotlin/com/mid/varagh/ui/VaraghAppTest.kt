package com.mid.varagh.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.model.FeatureFlags
import com.mid.varagh.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The app shell's bottom bar. Default resources are Persian, so with the fa qualifier the layout is
 * RTL. (Feature screens have their own stateless screen tests.)
 */
@RunWith(AndroidJUnit4::class)
class VaraghAppTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val offline = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "https://api.example.com/v1/", isDebugBuild = true)
    private val online = offline.copy(useRemoteBackend = true)

    private fun setBar(flags: FeatureFlags, onDirection: (LayoutDirection) -> Unit = {}) {
        composeRule.setContent {
            onDirection(LocalLayoutDirection.current)
            VaraghTheme {
                var selected by remember { mutableStateOf(TopLevelDestination.LIBRARY) }
                VaraghBottomBar(
                    destinations = TopLevelDestination.visible(flags),
                    isSelected = { it == selected },
                    onNavigate = { selected = it },
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "fa")
    fun offlineBuild_hasNoSocialTab_andLayoutIsRtl() {
        var direction: LayoutDirection? = null
        setBar(offline) { direction = it }
        composeRule.onNodeWithTag("nav_library").assertExists().assertIsSelected()
        composeRule.onNodeWithTag("nav_social").assertDoesNotExist()
        assertEquals(LayoutDirection.Rtl, direction)
    }

    @Test
    fun onlineBuild_showsSocialTab_andSelectsIt() {
        setBar(online)
        composeRule.onNodeWithTag("nav_social").assertExists().performClick()
        composeRule.onNodeWithTag("nav_social").assertIsSelected()
        composeRule.onNodeWithTag("nav_library").assertIsNotSelected()
    }

    @Test
    fun historyTab_selects() {
        setBar(offline)
        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.onNodeWithTag("nav_history").assertIsSelected()
    }
}
