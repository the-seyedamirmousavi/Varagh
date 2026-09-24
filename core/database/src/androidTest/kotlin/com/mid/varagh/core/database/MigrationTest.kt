package com.mid.varagh.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the migration policy on a real device: every exported schema version must upgrade to the
 * current one via [VaraghMigrations.ALL] without destructive fallback, and Room must accept the
 * result. Run with `./gradlew :core:database:connectedDebugAndroidTest`.
 * (Instrumented because Room's MigrationTestHelper does not work under Robolectric on Windows.)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VaraghDatabase::class.java,
    )

    @Test
    fun migrateFromEveryVersionToLatest() {
        for (version in 1..VaraghDatabase.VERSION) {
            helper.createDatabase("$dbName-$version", version).close()
            if (version < VaraghDatabase.VERSION) {
                helper.runMigrationsAndValidate("$dbName-$version", VaraghDatabase.VERSION, true, *VaraghMigrations.ALL)
                    .close()
            }
            // Opening through Room validates the schema against the entities (no destructive fallback).
            Room.databaseBuilder(ApplicationProvider.getApplicationContext(), VaraghDatabase::class.java, "$dbName-$version")
                .addMigrations(*VaraghMigrations.ALL)
                .build()
                .apply {
                    assertEquals(VaraghDatabase.VERSION, openHelper.writableDatabase.version)
                    close()
                }
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
