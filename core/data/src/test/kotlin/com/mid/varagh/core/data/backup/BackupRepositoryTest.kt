package com.mid.varagh.core.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.data.FakeTimeProvider
import com.mid.varagh.core.data.TestTransactionRunner
import com.mid.varagh.core.data.inMemoryDatabase
import com.mid.varagh.core.data.repository.local.LocalBookRepository
import com.mid.varagh.core.data.repository.local.LocalBookmarkRepository
import com.mid.varagh.core.data.repository.local.LocalReadingSessionRepository
import com.mid.varagh.core.data.repository.local.LocalUserProfileRepository
import com.mid.varagh.core.database.VaraghDatabase
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.UserPreferencesRepository
import com.mid.varagh.core.model.AppLanguage
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private val time = FakeTimeProvider(now = 1_000)
    private val databases = mutableListOf<VaraghDatabase>()

    private class FakePrefs(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
        val state = MutableStateFlow(initial)
        override val preferences = state
        override suspend fun update(transform: (UserPreferences) -> UserPreferences) = state.update(transform)
    }

    private fun backupRepo(db: VaraghDatabase, prefs: UserPreferencesRepository) = LocalBackupRepository(
        ApplicationProvider.getApplicationContext<Context>(),
        db.bookDao(), db.readingProgressDao(), db.readingSessionDao(), db.bookmarkDao(), db.userProfileDao(),
        prefs, TestTransactionRunner(db), time,
    )

    private fun newDb() = inMemoryDatabase().also { databases += it }

    @After
    fun tearDown() = databases.forEach { it.close() }

    @Test
    fun exportThenImportIntoEmptyDevice_restoresEverything_andReimportAddsNothing() = runTest {
        // Phone A
        val a = newDb()
        val tx = TestTransactionRunner(a)
        val books = LocalBookRepository(a.bookDao(), a.readingProgressDao(), tx, time)
        val id = books.addBook(NewBook("شاهنامه", "فردوسی", "content://a", "hash-1", 500, "/covers/x.jpg"))
        books.saveProgress(id, 120)
        books.setStatus(id, ReadingStatus.READING)
        LocalReadingSessionRepository(a.readingSessionDao()).addSession(id, 0, 600_000, 12)
        LocalBookmarkRepository(a.bookmarkDao(), time).addBookmark(id, 42, "بیت محبوب")
        LocalUserProfileRepository(a.userProfileDao(), tx, time).updateProfile { it.copy(displayName = "امیر", username = "amir") }
        val prefsA = FakePrefs(UserPreferences(language = AppLanguage.ENGLISH, warmFilter = 0.3f))
        val json = Json.encodeToString(BackupFile.serializer(), backupRepo(a, prefsA).buildBackup())

        // Phone B
        val b = newDb()
        val prefsB = FakePrefs()
        val restore = backupRepo(b, prefsB)
        val summary = restore.importJson(json)
        assertEquals(1, summary.books)
        assertEquals(1, summary.sessions)
        assertEquals(1, summary.bookmarks)

        val restored = b.bookDao().getAll().single()
        assertEquals("شاهنامه", restored.title)
        assertEquals(ReadingStatus.READING, restored.status)
        assertEquals("covers are regenerated on the new device", null, restored.coverPath)
        assertEquals(120, b.readingProgressDao().get(restored.id)!!.currentPage)
        assertEquals("بیت محبوب", b.bookmarkDao().getAll().single().note)
        assertEquals("امیر", b.userProfileDao().get(1)!!.displayName)
        assertEquals(AppLanguage.ENGLISH, prefsB.preferences.first().language)
        assertEquals(0.3f, prefsB.preferences.first().warmFilter, 0f)

        val again = restore.importJson(json)
        assertEquals(0, again.books)
        assertEquals(0, again.sessions)
        assertEquals(0, again.bookmarks)
    }

    @Test
    fun newerLocalProgressIsKept() = runTest {
        val db = newDb()
        val books = LocalBookRepository(db.bookDao(), db.readingProgressDao(), TestTransactionRunner(db), time)
        val id = books.addBook(NewBook("t", null, "content://a", "hash-1", 100, null))
        time.now = 5_000
        books.saveProgress(id, 80)
        val old = BackupFile(
            exportedAt = 0,
            books = listOf(BookBackup("hash-1", "t", null, "content://a", 100, 0, status = "READING", updatedAt = 0, progressPage = 10, progressUpdatedAt = 1)),
        )
        backupRepo(db, FakePrefs()).importJson(Json.encodeToString(BackupFile.serializer(), old))
        assertEquals(80, db.readingProgressDao().get(id)!!.currentPage)
    }

    @Test
    fun garbageIsRejectedAsInvalidBackup() = runTest {
        val repo = backupRepo(newDb(), FakePrefs())
        listOf("not json", """{"format":"something-else","version":1,"exportedAt":0}""", """{"format":"varagh-backup","version":99,"exportedAt":0}""")
            .forEach { text ->
                val error = runCatching { repo.importJson(text) }.exceptionOrNull()
                assertTrue("expected InvalidBackup for $text but was $error", error is VaraghException.InvalidBackup)
            }
    }
}
