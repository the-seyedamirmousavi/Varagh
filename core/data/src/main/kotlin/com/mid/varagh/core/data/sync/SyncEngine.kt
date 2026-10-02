package com.mid.varagh.core.data.sync

import com.mid.varagh.core.data.remote.apiCall
import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.dao.BookDao
import com.mid.varagh.core.database.dao.ReadingProgressDao
import com.mid.varagh.core.database.dao.ReadingSessionDao
import com.mid.varagh.core.database.dao.UserProfileDao
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.ReadingProgress
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserProfile
import com.mid.varagh.core.network.VaraghApi
import com.mid.varagh.core.network.auth.TokenStore
import com.mid.varagh.core.network.model.BookMetaDto
import com.mid.varagh.core.network.model.LibraryEntryRequest
import com.mid.varagh.core.network.model.LibraryPatchDto
import com.mid.varagh.core.network.model.ReadingSessionDto
import com.mid.varagh.core.network.model.UserProfilePatchDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class SyncReport(
    val booksPushed: Int = 0,
    val booksDeleted: Int = 0,
    val sessionsPushed: Int = 0,
    val booksPulled: Int = 0,
    val profilePushed: Boolean = false,
)

/**
 * Two-way sync between Room (the UI's source of truth) and the server.
 *
 * Push: every row with sync_state PENDING/DELETED is sent (POST/PATCH/DELETE /library,
 * POST /sessions, PATCH /me) and then marked SYNCED, guarded by updated_at so an edit made while a
 * request was in flight stays PENDING for the next run.
 * Pull: GET /library; for entries we already have locally, the newer side wins (last-write-wins
 * by updatedAt). Local PENDING rows always win because they are pushed first.
 *
 * Every step is idempotent, so a cancelled or retried sync is safe. PDFs are never uploaded.
 */
class SyncEngine @Inject constructor(
    private val api: VaraghApi,
    private val tokens: TokenStore,
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val sessionDao: ReadingSessionDao,
    private val profileDao: UserProfileDao,
    private val transaction: TransactionRunner,
) {
    /** Throws [VaraghException] (Network/Unauthorized/Server) so the worker can decide to retry. */
    suspend fun sync(): SyncReport {
        if (tokens.current() == null) return SyncReport()
        val profilePushed = pushProfile()
        val (pushed, deleted) = pushBooks()
        val sessions = pushSessions()
        val pulled = pullLibrary()
        return SyncReport(pushed, deleted, sessions, pulled, profilePushed)
    }

    private suspend fun pushProfile(): Boolean {
        val profile = profileDao.get(UserProfile.LOCAL_USER_ID) ?: return false
        if (profile.syncState != SyncState.PENDING) return false
        val currentRemoteBook = profile.currentlyReadingBookId?.let { bookDao.getById(it)?.remoteId }
        val dto = apiCall {
            api.updateMe(
                UserProfilePatchDto(
                    displayName = profile.displayName,
                    username = profile.username.ifBlank { null },
                    bio = profile.bio,
                    isPublic = profile.isPublic,
                    currentlyReadingBookId = currentRemoteBook,
                ),
            )
        }
        profile.avatarPath?.let(::File)?.takeIf { it.isFile }?.let { file ->
            val part = MultipartBody.Part.createFormData("avatar", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            apiCall { api.uploadAvatar(part) }
        }
        profileDao.markSynced(profile.id, dto.id, profile.updatedAt)
        return true
    }

    private suspend fun pushBooks(): Pair<Int, Int> {
        var pushed = 0
        var deleted = 0
        for (book in bookDao.getUnsynced()) {
            when {
                book.syncState == SyncState.DELETED -> {
                    book.remoteEntryId?.let { entryId ->
                        try {
                            apiCall { api.deleteLibraryEntry(entryId) }
                        } catch (_: VaraghException.NotFound) {
                            // Already gone on the server.
                        }
                    }
                    bookDao.deleteById(book.id)
                    deleted++
                }
                book.remoteEntryId == null -> {
                    val entry = apiCall {
                        api.addToLibrary(
                            LibraryEntryRequest(
                                bookMeta = BookMetaDto(title = book.title, author = book.author, pageCount = book.pageCount),
                                status = book.status.name,
                                rating = book.rating,
                            ),
                        )
                    }
                    bookDao.setRemoteIds(book.id, entry.book.id, entry.id, SyncState.PENDING)
                    patch(book.copy(remoteId = entry.book.id, remoteEntryId = entry.id))
                    pushed++
                }
                else -> {
                    patch(book)
                    pushed++
                }
            }
        }
        return pushed to deleted
    }

    private suspend fun patch(book: BookEntity) {
        val entryId = book.remoteEntryId ?: return
        val progress = progressDao.get(book.id)
        try {
            apiCall {
                api.updateLibraryEntry(
                    entryId,
                    LibraryPatchDto(
                        status = book.status.name,
                        rating = book.rating,
                        progressPercent = progress?.percent,
                        currentPage = progress?.currentPage,
                    ),
                )
            }
        } catch (_: VaraghException.NotFound) {
            // Deleted on another device: re-create it on the next run.
            bookDao.setRemoteIds(book.id, book.remoteId, null, SyncState.PENDING)
            return
        }
        bookDao.markSynced(book.id, book.updatedAt)
        progress?.let { progressDao.setSyncState(book.id, SyncState.SYNCED) }
    }

    private suspend fun pushSessions(): Int {
        val pending = sessionDao.getPending()
        if (pending.isEmpty()) return 0
        val remoteIds = pending.map { it.bookId }.distinct().associateWith { bookDao.getById(it)?.remoteId }
        // Sessions of books the server doesn't know yet wait for the next run.
        val ready = pending.filter { remoteIds[it.bookId] != null }
        if (ready.isEmpty()) return 0
        apiCall {
            api.uploadSessions(
                ready.map { ReadingSessionDto(remoteIds.getValue(it.bookId)!!, it.startedAt, it.endedAt, it.pagesRead) },
            )
        }
        sessionDao.setSyncState(ready.map { it.id }, SyncState.SYNCED)
        return ready.size
    }

    private suspend fun pullLibrary(): Int {
        val entries = apiCall { api.library() }
        var pulled = 0
        transaction {
            for (entry in entries) {
                val local = bookDao.findByRemoteEntryId(entry.id) ?: continue
                // Local edits not yet pushed win; otherwise the newer side wins.
                if (local.syncState != SyncState.SYNCED || entry.updatedAt <= local.updatedAt) continue
                val status = runCatching { ReadingStatus.valueOf(entry.status) }.getOrDefault(local.status)
                val finishedAt = when {
                    status != ReadingStatus.FINISHED -> null
                    local.finishedAt != null -> local.finishedAt
                    else -> entry.updatedAt
                }
                bookDao.updateStatus(local.id, status, finishedAt, entry.updatedAt, SyncState.SYNCED)
                bookDao.updateRating(local.id, entry.rating?.coerceIn(0, 5), entry.updatedAt, SyncState.SYNCED)
                val page = entry.currentPage
                if (page != null) {
                    val current = progressDao.get(local.id)
                    if (current == null || current.updatedAt < entry.updatedAt) {
                        val clamped = page.coerceIn(0, (local.pageCount - 1).coerceAtLeast(0))
                        progressDao.upsert(
                            ReadingProgressEntity(
                                bookId = local.id,
                                currentPage = clamped,
                                percent = entry.progressPercent ?: ReadingProgress.percentOf(clamped, local.pageCount),
                                updatedAt = entry.updatedAt,
                            ),
                        )
                    }
                }
                pulled++
            }
        }
        return pulled
    }
}
