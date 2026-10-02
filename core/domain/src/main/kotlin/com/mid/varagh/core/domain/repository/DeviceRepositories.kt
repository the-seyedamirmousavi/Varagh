package com.mid.varagh.core.domain.repository

// Device-side storage that only exists on the phone (files, images, backups). These have one
// implementation in both builds: PDFs and covers never leave the device.

/** What we learn about a PDF before adding it. */
data class PdfFileInfo(
    val displayName: String?,
    val fileHash: String,
    val pageCount: Int,
    val sizeBytes: Long,
)

/** Access to the user's PDF files (Storage Access Framework URIs) and their cover thumbnails. */
interface BookFileRepository {
    /** Takes persistable read permission, hashes (SHA-256) and opens the PDF to count pages. */
    suspend fun inspect(fileUri: String): PdfFileInfo

    /** Renders page 1 as a thumbnail into app storage. Returns its path, or null if it failed. */
    suspend fun renderCover(fileUri: String, fileHash: String): String?

    suspend fun deleteCover(coverPath: String?)

    /** Releases the persisted permission (the file itself is never deleted). */
    suspend fun releaseAccess(fileUri: String)

    suspend fun isReadable(fileUri: String): Boolean
}

/** Profile picture storage. */
interface AvatarStore {
    /** Copies and downsizes the picked image into app storage; returns the new path. */
    suspend fun saveAvatar(sourceUri: String): String
    suspend fun deleteAvatar(path: String?)
}

data class BackupSummary(
    val books: Int,
    val sessions: Int,
    val bookmarks: Int,
)

/**
 * Exports/imports the whole local library (metadata, progress, sessions, bookmarks, profile,
 * settings) as JSON to a user-chosen document. PDFs are not included.
 */
interface BackupRepository {
    suspend fun exportTo(documentUri: String): BackupSummary

    /** Merges the backup into the current data (never deletes anything). */
    suspend fun importFrom(documentUri: String): BackupSummary
}
