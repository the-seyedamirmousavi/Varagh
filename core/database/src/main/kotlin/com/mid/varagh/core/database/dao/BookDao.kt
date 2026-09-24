package com.mid.varagh.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.BookWithProgressRow
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /**
     * Library list. [search] must already be LIKE-escaped with '\' (see `escapeLike`); blank matches
     * everything. [status] null = all statuses. [sort] is a `LibrarySort` name.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM books
        WHERE sync_state != 'DELETED'
          AND (:search = '' OR title LIKE '%' || :search || '%' ESCAPE '\'
               OR IFNULL(author, '') LIKE '%' || :search || '%' ESCAPE '\')
          AND (:status IS NULL OR status = :status)
        ORDER BY
          CASE WHEN :sort = 'TITLE' THEN title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 'DATE_ADDED' THEN added_at END DESC,
          CASE WHEN :sort = 'LAST_OPENED' THEN COALESCE(last_opened_at, added_at) END DESC,
          id DESC
        """,
    )
    fun observeLibrary(search: String, status: ReadingStatus?, sort: String): Flow<List<BookWithProgressRow>>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id AND sync_state != 'DELETED'")
    fun observeBookWithProgress(id: Long): Flow<BookWithProgressRow?>

    @Transaction
    @Query(
        """
        SELECT * FROM books
        WHERE sync_state != 'DELETED' AND status = :status
        ORDER BY COALESCE(last_opened_at, added_at) DESC, id DESC
        """,
    )
    fun observeByStatus(status: ReadingStatus): Flow<List<BookWithProgressRow>>

    @Query("SELECT * FROM books WHERE id = :id AND sync_state != 'DELETED'")
    suspend fun getById(id: Long): BookEntity?

    /** Includes tombstoned rows so a re-import can revive them instead of violating the unique hash. */
    @Query("SELECT * FROM books WHERE file_hash = :hash LIMIT 1")
    suspend fun findByHashIncludingDeleted(hash: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET last_opened_at = :openedAt, updated_at = :openedAt, sync_state = :syncState WHERE id = :id")
    suspend fun updateLastOpened(id: Long, openedAt: Long, syncState: SyncState)

    @Query(
        """
        UPDATE books SET status = :status, finished_at = :finishedAt, updated_at = :updatedAt,
        sync_state = :syncState WHERE id = :id
        """,
    )
    suspend fun updateStatus(id: Long, status: ReadingStatus, finishedAt: Long?, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE books SET rating = :rating, updated_at = :updatedAt, sync_state = :syncState WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Int?, updatedAt: Long, syncState: SyncState)

    @Query(
        """
        UPDATE books SET title = :title, author = :author, updated_at = :updatedAt,
        sync_state = :syncState WHERE id = :id
        """,
    )
    suspend fun updateMetadata(id: Long, title: String, author: String?, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE books SET page_count = :pageCount, cover_path = :coverPath WHERE id = :id")
    suspend fun updateFileInfo(id: Long, pageCount: Int, coverPath: String?)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE books SET sync_state = 'DELETED', updated_at = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: Long, updatedAt: Long)

    @Query("SELECT * FROM books WHERE sync_state != 'SYNCED'")
    suspend fun getUnsynced(): List<BookEntity>

    @Query("SELECT * FROM books WHERE sync_state != 'DELETED' ORDER BY id")
    suspend fun getAll(): List<BookEntity>
}

/** Escapes LIKE wildcards so user input is matched literally (pair with `ESCAPE '\'`). */
fun escapeLike(input: String): String =
    input.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
