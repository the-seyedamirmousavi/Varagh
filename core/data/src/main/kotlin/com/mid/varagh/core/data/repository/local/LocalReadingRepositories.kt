package com.mid.varagh.core.data.repository.local

import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.dao.BookmarkDao
import com.mid.varagh.core.database.dao.ReadingSessionDao
import com.mid.varagh.core.database.dao.UserProfileDao
import com.mid.varagh.core.database.model.BookmarkEntity
import com.mid.varagh.core.database.model.ReadingSessionEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.asEntity
import com.mid.varagh.core.database.model.asExternalModel
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.repository.UserProfileRepository
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalReadingSessionRepository @Inject constructor(
    private val sessionDao: ReadingSessionDao,
) : ReadingSessionRepository {

    override fun observeSessions(bookId: Long): Flow<List<ReadingSession>> =
        sessionDao.observeForBook(bookId).map { rows -> rows.map { it.asExternalModel() } }

    override fun observeAllSessions(): Flow<List<ReadingSession>> =
        sessionDao.observeAll().map { rows -> rows.map { it.asExternalModel() } }

    override fun observeTotalReadingMillis(bookId: Long): Flow<Long> = sessionDao.observeTotalMillisForBook(bookId)

    override suspend fun addSession(bookId: Long, startedAt: Long, endedAt: Long, pagesRead: Int): Long =
        sessionDao.insert(
            ReadingSessionEntity(
                bookId = bookId,
                startedAt = startedAt,
                endedAt = endedAt,
                pagesRead = pagesRead,
                syncState = SyncState.SYNCED,
            ),
        )
}

class LocalBookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val time: TimeProvider,
) : BookmarkRepository {

    override fun observeBookmarks(bookId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun findOnPage(bookId: Long, page: Int): Bookmark? =
        bookmarkDao.findOnPage(bookId, page)?.asExternalModel()

    override suspend fun addBookmark(bookId: Long, page: Int, note: String?): Long {
        val now = time.nowMillis()
        return bookmarkDao.insert(
            BookmarkEntity(bookId = bookId, page = page, note = note, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun updateNote(bookmarkId: Long, note: String?) =
        bookmarkDao.updateNote(bookmarkId, note, time.nowMillis(), SyncState.SYNCED)

    override suspend fun deleteBookmark(bookmarkId: Long) = bookmarkDao.deleteById(bookmarkId)
}

/** The single local user. The row is created lazily on first edit. */
class LocalUserProfileRepository @Inject constructor(
    private val profileDao: UserProfileDao,
    private val transaction: TransactionRunner,
    private val time: TimeProvider,
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile> =
        profileDao.observe(UserProfile.LOCAL_USER_ID).map { it?.asExternalModel() ?: UserProfile.DefaultLocal }

    override suspend fun updateProfile(transform: (UserProfile) -> UserProfile) = transaction {
        val current = profileDao.get(UserProfile.LOCAL_USER_ID)?.asExternalModel() ?: UserProfile.DefaultLocal
        val updated = transform(current).copy(id = UserProfile.LOCAL_USER_ID)
        profileDao.upsert(updated.asEntity(updatedAt = time.nowMillis()))
    }
}
