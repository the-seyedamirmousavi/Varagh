package com.mid.varagh.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {

    @Test
    fun `offline build hides every server-only feature`() {
        val flags = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "https://x/", isDebugBuild = false)
        assertFalse(flags.isSocialEnabled)
        assertFalse(flags.isAuthEnabled)
        assertFalse(flags.isPublicProfileEnabled)
        assertFalse(flags.isFollowEnabled)
        assertFalse(flags.isSyncEnabled)
    }

    @Test
    fun `remote build enables every server-only feature`() {
        val flags = FeatureFlags(useRemoteBackend = true, apiBaseUrl = "https://x/", isDebugBuild = false)
        assertTrue(flags.isSocialEnabled)
        assertTrue(flags.isAuthEnabled)
        assertTrue(flags.isPublicProfileEnabled)
        assertTrue(flags.isFollowEnabled)
        assertTrue(flags.isSyncEnabled)
    }

    @Test
    fun `developer info only in debug builds`() {
        assertTrue(FeatureFlags(false, "", isDebugBuild = true).showDeveloperInfo)
        assertFalse(FeatureFlags(false, "", isDebugBuild = false).showDeveloperInfo)
    }

    @Test
    fun `language falls back to persian`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.PERSIAN, AppLanguage.fromTag("fa-IR"))
        assertEquals(AppLanguage.PERSIAN, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.PERSIAN, AppLanguage.fromTag("de"))
    }
}
