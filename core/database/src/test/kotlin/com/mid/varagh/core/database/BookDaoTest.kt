package com.mid.varagh.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.mid.varagh.core.database.dao.escapeLike
import com.mid.varagh.core.database.model.BookmarkEntity
import com.mid.varagh.core.database.model.ReadingProgressEntity
import com.mid.varagh.core.database.model.ReadingSessionEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.UserProfileEntity
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.ReadingStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDaoTest {

    private lateinit var db: VaraghDatabase

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun library(search: String = "", status: ReadingStatus? = null, sort: LibrarySort = LibrarySort.LAST_OPENED) =
        db.bookDao().observeLibrary(escapeLike(search), status, sort.name).first().map { it.book.title }

    @Test
    fun sortsByLastOpenedThenAdded_titleCaseInsensitive_andDateAdded() = runTest {
        val dao = db.bookDao()
        dao.insert(bookEntity("banana", addedAt = 10, lastOpenedAt = 100))
        dao.insert(bookEntity("Apple", addedAt = 50))
        dao.insert(bookEntity("cherry", addedAt = 30, lastOpenedAt = 20))

        assertEquals(listOf("banana", "Apple", "cherry"), library(sort = LibrarySort.LAST_OPENED))
        assertEquals(listOf("Apple", "banana", "cherry"), library(sort = LibrarySort.TITLE))
        assertEquals(listOf("Apple", "cherry", "banana"), library(sort = LibrarySort.DATE_ADDED))
    }

    @Test
    fun searchesTitleAndAuthor_includingPersian_andEscapesWildcards() = runTest {
        val dao = db.bookDao()
        dao.insert(bookEntity("شاهنامه", author = "فردوسی"))
        dao.insert(bookEntity("Bustan", author = "Saadi"))
        dao.insert(bookEntity("100% Kotlin", hash = "k"))

        assertEquals(listOf("شاهنامه"), library(search = "فردوسی"))
        assertEquals(listOf("Bustan"), library(search = "saadi"))
        assertEquals(listOf("100% Kotlin"), library(search = "100%"))
        assertEquals(emptyList<String>(), library(search = "_"))
    }

    @Test
    fun filtersByStatus_andHidesTombstones() = runTest {
        val dao = db.bookDao()
        dao.insert(bookEntity("reading", status = ReadingStatus.READING))
        dao.insert(bookEntity("done", status = ReadingStatus.FINISHED))
        dao.insert(bookEntity("gone", status = ReadingStatus.READING, syncState = SyncState.DELETED))

        assertEquals(listOf("reading"), library(status = ReadingStatus.READING))
        assertEquals(2, library().size)
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateFileHashIsRejected() = runTest {
        db.bookDao().insert(bookEntity("a", hash = "same"))
        db.bookDao().insert(bookEntity("b", hash = "same"))
    }

    @Test
    fun libraryJoinsProgress_andUpdatesLive() = runTest {
        val id = db.bookDao().insert(bookEntity("a"))
        db.bookDao().observeBookWithProgress(id).test {
            assertNull(awaitItem()!!.progress)
            db.readingProgressDao().upsert(ReadingProgressEntity(bookId = id, currentPage = 9, percent = 0.1f, updatedAt = 1))
            assertEquals(9, awaitItem()!!.progress!!.currentPage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingBookCascadesAndClearsProfilePick() = runTest {
        val id = db.bookDao().insert(bookEntity("a"))
        db.readingProgressDao().upsert(ReadingProgressEntity(id, 1, 0f, 0))
        db.readingSessionDao().insert(ReadingSessionEntity(bookId = id, startedAt = 0, endedAt = 60_000, pagesRead = 2))
        db.bookmarkDao().insert(BookmarkEntity(bookId = id, page = 1, note = null, createdAt = 0, updatedAt = 0))
        db.userProfileDao().upsert(UserProfileEntity(1, "n", "u", "", null, false, id, null, 0))

        db.bookDao().deleteById(id)

        assertNull(db.readingProgressDao().get(id))
        assertTrue(db.readingSessionDao().getAll().isEmpty())
        assertTrue(db.bookmarkDao().getAll().isEmpty())
        assertNull(db.userProfileDao().get(1)!!.currentlyReadingBookId)
    }

    @Test
    fun statusUpdateAndSessionTotals() = runTest {
        val id = db.bookDao().insert(bookEntity("a"))
        db.bookDao().updateStatus(id, ReadingStatus.FINISHED, finishedAt = 5, updatedAt = 5, syncState = SyncState.SYNCED)
        assertEquals(ReadingStatus.FINISHED, db.bookDao().getById(id)!!.status)
        assertEquals(listOf(id), db.bookDao().observeByStatus(ReadingStatus.FINISHED).first().map { it.book.id })

        db.readingSessionDao().insert(ReadingSessionEntity(bookId = id, startedAt = 0, endedAt = 60_000, pagesRead = 2))
        db.readingSessionDao().insert(ReadingSessionEntity(bookId = id, startedAt = 100_000, endedAt = 160_000, pagesRead = 3))
        assertEquals(120_000L, db.readingSessionDao().observeTotalMillisForBook(id).first())
    }

    @Test
    fun escapeLikeEscapesWildcardsAndBackslash() {
        assertEquals("50\\%\\_a\\\\b", escapeLike(" 50%_a\\b "))
    }
}
