package com.mid.varagh.core.model

/**
 * Runtime view of the build-time backend switch (`USE_REMOTE_BACKEND` in core/data/build.gradle.kts).
 *
 * Provided once by Hilt from `:core:data` and injected wherever UI needs to decide whether a
 * server-only feature is visible. Nothing server-only may be shown when [useRemoteBackend] is false.
 */
data class FeatureFlags(
    val useRemoteBackend: Boolean,
    val apiBaseUrl: String,
    val isDebugBuild: Boolean,
) {
    /** Social feed, discover readers, "readers of this book". */
    val isSocialEnabled: Boolean get() = useRemoteBackend

    /** Login / register / logout. The local build has a single always-signed-in user. */
    val isAuthEnabled: Boolean get() = useRemoteBackend

    /** Public/private profile toggle and viewing other readers' public profiles. */
    val isPublicProfileEnabled: Boolean get() = useRemoteBackend

    /** Follow / unfollow other readers. */
    val isFollowEnabled: Boolean get() = useRemoteBackend

    /** Background synchronisation of metadata, progress and sessions (never PDF files). */
    val isSyncEnabled: Boolean get() = useRemoteBackend

    /** Debug-only screen that shows the flag values. */
    val showDeveloperInfo: Boolean get() = isDebugBuild

    companion object {
        /** Offline defaults, handy for previews and tests. */
        val Offline = FeatureFlags(useRemoteBackend = false, apiBaseUrl = "", isDebugBuild = false)
    }
}
