package com.mid.varagh.core.data.backup

import kotlinx.serialization.Serializable

/**
 * On-disk backup format. Books are keyed by file hash so a backup restores onto a new phone where
 * the same PDFs get different URIs/ids. Never contains the PDFs themselves.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val books: List<BookBackup> = emptyList(),
    val sessions: List<SessionBackup> = emptyList(),
    val bookmarks: List<BookmarkBackup> = emptyList(),
    val profile: ProfileBackup? = null,
    val preferences: PreferencesBackup? = null,
) {
    companion object {
        const val FORMAT = "varagh-backup"
        const val VERSION = 1
    }
}

@Serializable
data class BookBackup(
    val fileHash: String,
    val title: String,
    val author: String? = null,
    val fileUri: String,
    val pageCount: Int,
    val addedAt: Long,
    val lastOpenedAt: Long? = null,
    val finishedAt: Long? = null,
    val status: String,
    val rating: Int? = null,
    val remoteId: String? = null,
    val updatedAt: Long,
    val progressPage: Int? = null,
    val progressPercent: Float? = null,
    val progressUpdatedAt: Long? = null,
)

@Serializable
data class SessionBackup(
    val bookHash: String,
    val startedAt: Long,
    val endedAt: Long,
    val pagesRead: Int,
)

@Serializable
data class BookmarkBackup(
    val bookHash: String,
    val page: Int,
    val note: String? = null,
    val createdAt: Long,
)

@Serializable
data class ProfileBackup(
    val displayName: String,
    val username: String,
    val bio: String,
    val isPublic: Boolean = false,
)

@Serializable
data class PreferencesBackup(
    val readingTheme: String? = null,
    val customBackgroundArgb: Int? = null,
    val customTextArgb: Int? = null,
    val readingMode: String? = null,
    val rightToLeftPaging: Boolean? = null,
    val keepScreenOn: Boolean? = null,
    val darkThemeConfig: String? = null,
    val useDynamicColor: Boolean? = null,
    val language: String? = null,
    val warmFilter: Float? = null,
    val libraryGrid: Boolean? = null,
    val librarySort: String? = null,
)
