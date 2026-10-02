package com.mid.varagh.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All hand-written migrations, oldest first. Every schema change bumps VaraghDatabase.VERSION,
 * exports core/database/schemas/<version>.json, adds a migration here and is covered by
 * `MigrationTest` (instrumented) and `SchemaPolicyTest` (JVM).
 */
object VaraghMigrations {

    /** v2: server library-entry id for syncing books with the remote backend. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE books ADD COLUMN remote_entry_id TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
