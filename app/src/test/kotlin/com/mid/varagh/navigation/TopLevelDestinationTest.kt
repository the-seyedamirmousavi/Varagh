package com.mid.varagh.navigation

import com.mid.varagh.core.model.FeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `social tab is hidden when the remote backend is off`() {
        val visible = TopLevelDestination.visible(FeatureFlags(useRemoteBackend = false, apiBaseUrl = "", isDebugBuild = true))
        assertFalse(TopLevelDestination.SOCIAL in visible)
        assertEquals(
            listOf(
                TopLevelDestination.LIBRARY,
                TopLevelDestination.HISTORY,
                TopLevelDestination.PROFILE,
                TopLevelDestination.SETTINGS,
            ),
            visible,
        )
    }

    @Test
    fun `social tab is shown when the remote backend is on`() {
        val visible = TopLevelDestination.visible(FeatureFlags(useRemoteBackend = true, apiBaseUrl = "", isDebugBuild = false))
        assertTrue(TopLevelDestination.SOCIAL in visible)
    }
}
