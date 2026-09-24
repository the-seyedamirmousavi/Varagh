package com.mid.varagh.core.data.repository.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.mid.varagh.core.data.FakeTimeProvider
import com.mid.varagh.core.data.TestTransactionRunner
import com.mid.varagh.core.data.inMemoryDatabase
import com.mid.varagh.core.database.VaraghDatabase
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.usecase.AddBookResult
import com.mid.varagh.core.domain.usecase.AddBookUseCase
import com.mid.varagh.core.domain.usecase.ObserveReadingStatsUseCase
import com.mid.varagh.core.domain.usecase.OpenBookUseCase
import com.mid.varagh.core.domain.usecase.RecordReadingSessionUseCase
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.LibraryQuery
import com.mid.varagh.core.model.LibrarySort
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalRepositoriesTest {

    private lateinit var db: VaraghDatabase
    private val time = FakeTimeProvider()
    private lateinit var books: LocalBookRepository
    private lateinit var sessions: LocalReadingSessionRepository
    private lateinit var bookmarks: LocalBookmarkRepository
    private lateinit var profiles: LocalUserProfileRepository

    private fun newBook(hash: String, title: String = "Book $hash", pages: Int = 200) =
        NewBook(title = title, author = "Author", fileUri = "content://$hash", fileHash = hash, pageCount = pages, coverPath = null)

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val tx = TestTransactionRunner(db)
        books = LocalBookRepository(db.bookDao(), db.readingProgressDao(), tx, time)
        sessions = LocalReadingSessionRepository(db.readingSessionDao())
        bookmarks = LocalBookmarkRepository(db.bookmarkDao(), time)
        profiles = LocalUserProfileRepository(db.userProfileDao(), tx, time)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addBook_detectsDuplicates_throughUseCase() = runTest {
        val add = AddBookUseCase(books)
        val first = add(newBook("h1"))
        assertTrue(first is AddBookResult.Added)
        val again = add(newBook("h1", title = "Renamed copy"))
        assertEquals((first as AddBookResult.Added).bookId, (again as AddBookResult.Duplicate).existing.id)
    }

    @Test
    fun deletedBookCanBeReimported() = runTest {
        val id = books.addBook(newBook("h1"))
        books.deleteBook(id)
        assertNull(books.findByHash("h1"))
        val newId = books.addBook(newBook("h1"))
        assertNotNull(books.getBook(newId))
    }

    @Test
    fun remoteTombstoneIsRevivedInsteadOfViolatingUniqueHash() = runTest {
        val id = books.addBook(newBook("h1"))
        db.bookDao().markDeleted(id, updatedAt = 5)
        assertNull(books.findByHash("h1"))
        assertEquals(id, books.addBook(newBook("h1", title = "Back again")))
        val revived = db.bookDao().getById(id)!!
        assertEquals("Back again", revived.title)
        assertEquals(SyncState.SYNCED, revived.syncState)
    }

    @Test
    fun progressAndLibraryQueryFlowTogether() = runTest {
        time.now = 10
        val a = books.addBook(newBook("a", title = "Alef"))
        time.now = 20
        val b = books.addBook(newBook("b", title = "Be"))
        books.saveProgress(a, page = 199)

        val library = books.observeLibrary(LibraryQuery(sort = LibrarySort.DATE_ADDED)).first()
        assertEquals(listOf(b, a), library.map { it.book.id })
        assertEquals(1f, library.first { it.book.id == a }.progress!!.percent, 0f)
        assertEquals(listOf(a), books.observeLibrary(LibraryQuery(search = "alef")).first().map { it.book.id })
    }

    @Test
    fun finishingSetsFinishDate_andUnfinishingClearsIt() = runTest {
        val id = books.addBook(newBook("a"))
        time.now = 777
        books.setStatus(id, ReadingStatus.FINISHED)
        assertEquals(777L, books.getBook(id)!!.finishedAt)
        time.now = 999
        books.setStatus(id, ReadingStatus.FINISHED)
        assertEquals("re-finishing keeps the original date", 777L, books.getBook(id)!!.finishedAt)
        books.setStatus(id, ReadingStatus.READING)
        assertNull(books.getBook(id)!!.finishedAt)
    }

    @Test
    fun openBook_startsReading_andStampsLastOpened() = runTest {
        val id = books.addBook(newBook("a"))
        time.now = 5_000
        val opened = OpenBookUseCase(books)(id)!!
        assertEquals(ReadingStatus.READING, opened.status)
        assertEquals(5_000L, opened.lastOpenedAt)
    }

    @Test
    fun statsUseCase_readsFromRoom() = runTest {
        val id = books.addBook(newBook("a"))
        time.now = 2 * 86_400_000L
        val record = RecordReadingSessionUseCase(sessions)
        record(id, startedAt = time.now - 600_000, endedAt = time.now, pagesRead = 12)
        record(id, startedAt = time.now - 10_000, endedAt = time.now, pagesRead = 1) // too short
        books.setStatus(id, ReadingStatus.FINISHED)

        val stats = ObserveReadingStatsUseCase(books, sessions, time)().first()
        assertEquals(1, stats.totalBooksFinished)
        assertEquals(12, stats.totalPagesRead)
        assertEquals(10L, stats.totalMinutesRead)
        assertEquals(1, stats.currentStreakDays)
    }

    @Test
    fun bookmarks_addUpdateDelete() = runTest {
        val id = books.addBook(newBook("a"))
        bookmarks.observeBookmarks(id).test {
            assertTrue(awaitItem().isEmpty())
            val bm = bookmarks.addBookmark(id, page = 3, note = "quote")
            assertEquals("quote", awaitItem().single().note)
            bookmarks.updateNote(bm, "better quote")
            assertEquals("better quote", awaitItem().single().note)
            bookmarks.deleteBookmark(bm)
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun profile_defaultsThenPersistsEdits() = runTest {
        profiles.observeProfile().test {
            assertEquals(UserProfile.DefaultLocal, awaitItem())
            profiles.updateProfile { it.copy(displayName = "امیر", bio = "کتاب‌خوان") }
            val updated = awaitItem()
            assertEquals("امیر", updated.displayName)
            assertEquals(UserProfile.LOCAL_USER_ID, updated.id)
        }
    }

    @Test
    fun localAuth_isAlwaysSignedIn_andAccountsAreUnavailable() = runTest {
        val auth = LocalAuthRepository()
        assertTrue(auth.authState.first() is AuthState.SignedIn)
        val error = runCatching { auth.login("a@b.c", "pw") }.exceptionOrNull()
        assertTrue(error is VaraghException.FeatureUnavailable)
        val social = runCatching { LocalSocialRepository().getFeed(null) }.exceptionOrNull()
        assertTrue(social is VaraghException.FeatureUnavailable)
    }
}
