package com.mid.varagh.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mid.varagh.core.database.dao.BookDao
import com.mid.varagh.core.database.dao.BookmarkDao
import com.mid.varagh.core.database.dao.ReadingProgressDao
import com.mid.varagh.core.database.dao.ReadingSessionDao
import com.mid.varagh.core.database.dao.UserProfileDao
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.BookmarkEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.ReadingSessionEntity
import com.mid.varagh.core.database.model.UserProfileEntity

/**
 * Local source of truth for the UI (also when the remote backend is on).
 *
 * Migration policy: every schema change bumps [version], exports a new JSON schema into
 * core/database/schemas (checked in), and adds a `Migration` to [VaraghMigrations.ALL] with a test
 * in `MigrationTest`. Release builds never fall back to destructive migration.
 */
@Database(
    entities = [
        BookEntity::class,
        ReadingProgressEntity::class,
        ReadingSessionEntity::class,
        BookmarkEntity::class,
        UserProfileEntity::class,
    ],
    version = VaraghDatabase.VERSION,
    exportSchema = true,
)
abstract class VaraghDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val VERSION = 1
        const val NAME = "varagh.db"
    }
}
