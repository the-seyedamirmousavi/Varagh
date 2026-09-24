package com.mid.varagh.core.database

import androidx.room.migration.Migration

/**
 * All hand-written migrations, oldest first. Version 1 is the initial schema, so there are none yet.
 *
 * Template for the next one:
 * ```
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE books ADD COLUMN ...")
 *     }
 * }
 * ```
 * then add it to [ALL] and cover it in `MigrationTest`.
 */
object VaraghMigrations {
    val ALL: Array<Migration> = arrayOf()
}
