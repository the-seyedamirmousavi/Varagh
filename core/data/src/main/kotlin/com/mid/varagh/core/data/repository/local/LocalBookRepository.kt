package com.mid.varagh.core.data.repository.local

import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.dao.BookDao
import com.mid.varagh.core.database.dao.ReadingProgressDao
import com.mid.varagh.core.database.dao.escapeLike
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.asExternalModel
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.LibraryQuery
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingProgress
import com.mid.varagh.core.model.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-only [BookRepository]. Nothing to sync, so rows are written as SYNCED and hard-deleted.
 * `RemoteBookRepository` reuses this logic and only changes [writeState], [afterWrite] and deletion.
 */
open class LocalBookRepository @Inject constructor(
    protected val bookDao: BookDao,
    protected val progressDao: ReadingProgressDao,
    protected val transaction: TransactionRunner,
    protected val time: TimeProvider,
) : BookRepository {

    /** Sync state written with every edit. */
    protected open val writeState: SyncState get() = SyncState.SYNCED

    /** Called after every successful edit (the remote build schedules a sync here). */
    protected open suspend fun afterWrite() = Unit

    override fun observeLibrary(query: LibraryQuery): Flow<List<BookWithProgress>> =
        bookDao.observeLibrary(escapeLike(query.search), query.status, query.sort.name)
            .map { rows -> rows.map { it.asExternalModel() } }

    override fun observeBook(bookId: Long): Flow<BookWithProgress?> =
        bookDao.observeBookWithProgress(bookId).map { it?.asExternalModel() }

    override fun observeBooksByStatus(status: ReadingStatus): Flow<List<BookWithProgress>> =
        bookDao.observeByStatus(status).map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun getBook(bookId: Long): Book? = bookDao.getById(bookId)?.asExternalModel()

    override suspend fun findByHash(fileHash: String): Book? =
        bookDao.findByHashIncludingDeleted(fileHash)
            ?.takeIf { it.syncState != SyncState.DELETED }
            ?.asExternalModel()

    override suspend fun addBook(book: NewBook): Long = transaction {
        val now = time.nowMillis()
        val tombstone = bookDao.findByHashIncludingDeleted(book.fileHash)
        val entity = BookEntity(
            id = tombstone?.id ?: 0,
            title = book.title,
            author = book.author,
            fileUri = book.fileUri,
            fileHash = book.fileHash,
            pageCount = book.pageCount,
            coverPath = book.coverPath,
            addedAt = now,
            lastOpenedAt = null,
            finishedAt = if (book.status == ReadingStatus.FINISHED) now else null,
            status = book.status,
            rating = null,
            remoteId = tombstone?.remoteId,
            updatedAt = now,
            syncState = writeState,
            remoteEntryId = tombstone?.remoteEntryId,
        )
        if (tombstone != null) {
            bookDao.update(entity)
            tombstone.id
        } else {
            bookDao.insert(entity)
        }
    }.also { afterWrite() }

    override suspend fun updateMetadata(bookId: Long, title: String, author: String?) {
        bookDao.updateMetadata(bookId, title, author, time.nowMillis(), writeState)
        afterWrite()
    }

    override suspend fun updateFileInfo(bookId: Long, pageCount: Int, coverPath: String?) =
        bookDao.updateFileInfo(bookId, pageCount, coverPath)

    override suspend fun updateFileUri(bookId: Long, fileUri: String) = bookDao.updateFileUri(bookId, fileUri)

    override suspend fun setStatus(bookId: Long, status: ReadingStatus) {
        transaction {
            val book = bookDao.getById(bookId) ?: return@transaction
            val now = time.nowMillis()
            val finishedAt = when {
                status != ReadingStatus.FINISHED -> null
                book.status == ReadingStatus.FINISHED -> book.finishedAt ?: now
                else -> now
            }
            bookDao.updateStatus(bookId, status, finishedAt, now, writeState)
        }
        afterWrite()
    }

    override suspend fun setRating(bookId: Long, rating: Int?) {
        bookDao.updateRating(bookId, rating, time.nowMillis(), writeState)
        afterWrite()
    }

    /** Opening a book is local bookkeeping and is never synced on its own. */
    override suspend fun markOpened(bookId: Long) =
        bookDao.updateLastOpened(bookId, time.nowMillis(), bookDao.getById(bookId)?.syncState ?: SyncState.SYNCED)

    override suspend fun saveProgress(bookId: Long, page: Int) {
        val book = bookDao.getById(bookId) ?: return
        progressDao.upsert(
            ReadingProgressEntity(
                bookId = bookId,
                currentPage = page,
                percent = ReadingProgress.percentOf(page, book.pageCount),
                updatedAt = time.nowMillis(),
                syncState = writeState,
            ),
        )
        onProgressSaved(bookId)
    }

    /** Hook for the remote build, which pushes progress together with its library entry. */
    protected open suspend fun onProgressSaved(bookId: Long) = Unit

    override suspend fun deleteBook(bookId: Long) = bookDao.deleteById(bookId)
}
