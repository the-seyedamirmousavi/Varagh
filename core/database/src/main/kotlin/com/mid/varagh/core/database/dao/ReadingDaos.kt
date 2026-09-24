package com.mid.varagh.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.mid.varagh.core.database.model.BookmarkEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.ReadingSessionEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId AND sync_state != 'DELETED'")
    fun observe(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId AND sync_state != 'DELETED'")
    suspend fun get(bookId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress")
    suspend fun getAll(): List<ReadingProgressEntity>
}

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity): Long

    @Query("SELECT * FROM reading_sessions WHERE book_id = :bookId AND sync_state != 'DELETED' ORDER BY started_at DESC")
    fun observeForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE sync_state != 'DELETED' ORDER BY started_at")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Query(
        """
        SELECT IFNULL(SUM(ended_at - started_at), 0) FROM reading_sessions
        WHERE book_id = :bookId AND sync_state != 'DELETED'
        """,
    )
    fun observeTotalMillisForBook(bookId: Long): Flow<Long>

    @Query("SELECT * FROM reading_sessions WHERE sync_state = 'PENDING'")
    suspend fun getPending(): List<ReadingSessionEntity>

    @Query("UPDATE reading_sessions SET sync_state = :state WHERE id IN (:ids)")
    suspend fun setSyncState(ids: List<Long>, state: SyncState)

    @Query("SELECT * FROM reading_sessions ORDER BY id")
    suspend fun getAll(): List<ReadingSessionEntity>
}

@Dao
interface BookmarkDao {
    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query(
        """
        UPDATE bookmarks SET note = :note, updated_at = :updatedAt, sync_state = :syncState
        WHERE id = :id
        """,
    )
    suspend fun updateNote(id: Long, note: String?, updatedAt: Long, syncState: SyncState)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE bookmarks SET sync_state = 'DELETED', updated_at = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long)

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId AND sync_state != 'DELETED' ORDER BY page, created_at")
    fun observeForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId AND page = :page AND sync_state != 'DELETED' LIMIT 1")
    suspend fun findOnPage(bookId: Long, page: Int): BookmarkEntity?

    @Query("SELECT * FROM bookmarks ORDER BY id")
    suspend fun getAll(): List<BookmarkEntity>
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observe(id: Long): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun get(id: Long): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}
