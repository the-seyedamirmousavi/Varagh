package com.mid.varagh.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Fast JVM checks of the migration policy (the full upgrade test is the instrumented
 * `MigrationTest`): a schema must be exported and checked in for every version, and there must be
 * a migration for every step, so release builds never need destructive fallback.
 */
class SchemaPolicyTest {

    private val schemaDir = File(System.getProperty("user.dir"), "schemas/${VaraghDatabase::class.qualifiedName}")

    @Test
    fun schemaIsExportedForEveryVersion() {
        for (version in 1..VaraghDatabase.VERSION) {
            val schema = File(schemaDir, "$version.json")
            assertTrue("missing exported schema ${schema.path}; build once and commit it", schema.isFile)
            assertTrue(schema.readText().contains("\"version\": $version"))
        }
    }

    @Test
    fun everyMigrationStepExists() {
        val covered = VaraghMigrations.ALL.map { it.startVersion to it.endVersion }.toSet()
        for (from in 1 until VaraghDatabase.VERSION) {
            assertEquals("missing migration $from -> ${from + 1}", true, (from to from + 1) in covered)
        }
    }
}
