package com.mid.varagh.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.mid.varagh.core.model.ReadingStatus

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["file_hash"], unique = true),
        Index(value = ["status"]),
        Index(value = ["last_opened_at"]),
        Index(value = ["remote_id"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    @ColumnInfo(name = "file_uri") val fileUri: String,
    @ColumnInfo(name = "file_hash") val fileHash: String,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long?,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    val status: ReadingStatus,
    val rating: Int?,
    @ColumnInfo(name = "remote_id") val remoteId: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.SYNCED,
)

@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReadingProgressEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "current_page") val currentPage: Int,
    val percent: Float,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.SYNCED,
)

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["started_at"]),
    ],
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "pages_read") val pagesRead: Int,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.SYNCED,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["book_id", "page"])],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    val page: Int,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.SYNCED,
)

@Entity(
    tableName = "user_profile",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["currently_reading_book_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["currently_reading_book_id"])],
)
data class UserProfileEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "display_name") val displayName: String,
    val username: String,
    val bio: String,
    @ColumnInfo(name = "avatar_path") val avatarPath: String?,
    @ColumnInfo(name = "is_public") val isPublic: Boolean,
    @ColumnInfo(name = "currently_reading_book_id") val currentlyReadingBookId: Long?,
    @ColumnInfo(name = "remote_id") val remoteId: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: SyncState = SyncState.SYNCED,
)

/** A book row joined with its progress row (if any). */
data class BookWithProgressRow(
    @Embedded val book: BookEntity,
    @Relation(parentColumn = "id", entityColumn = "book_id")
    val progress: ReadingProgressEntity?,
)
