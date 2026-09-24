package com.mid.varagh.core.model

/**
 * The reader's own profile. In the offline build there is exactly one, local, always-signed-in user.
 *
 * @property isPublic only meaningful with the remote backend (the toggle is hidden otherwise).
 * @property currentlyReadingBookId explicit pick; when null the most recently opened READING book is used.
 */
data class UserProfile(
    val id: Long,
    val displayName: String,
    val username: String,
    val bio: String,
    val avatarPath: String?,
    val isPublic: Boolean,
    val currentlyReadingBookId: Long?,
    val remoteId: String?,
) {
    companion object {
        const val LOCAL_USER_ID = 1L

        val DefaultLocal = UserProfile(
            id = LOCAL_USER_ID,
            displayName = "",
            username = "",
            bio = "",
            avatarPath = null,
            isPublic = false,
            currentlyReadingBookId = null,
            remoteId = null,
        )
    }
}

/** Authentication state. The local build is always [SignedIn] as the local user. */
sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val userId: String, val username: String) : AuthState
}

// ---- Remote-only models (the social features need a server; used from phase 8) ----

/** Another reader as seen publicly. */
data class PublicReader(
    val id: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUrl: String?,
    val currentlyReading: BookMeta?,
    val booksFinished: Int,
    val isFollowedByMe: Boolean,
)

/** Shared-catalog book metadata (never the PDF). */
data class BookMeta(
    val remoteId: String,
    val title: String,
    val author: String?,
    val pageCount: Int?,
    val coverUrl: String?,
)

/** "Reader X started / is reading / finished book Y". */
data class FeedItem(
    val id: String,
    val reader: PublicReader,
    val book: BookMeta,
    val type: FeedItemType,
    val progressPercent: Float?,
    val createdAt: Long,
)

enum class FeedItemType {
    STARTED,
    READING,
    FINISHED,
}

/** A page of the social feed. [nextCursor] is null on the last page. */
data class FeedPage(
    val items: List<FeedItem>,
    val nextCursor: String?,
)

/** A follow relationship between two readers. */
data class Follow(
    val followerId: String,
    val followeeId: String,
    val createdAt: Long,
)
