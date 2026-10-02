package com.mid.varagh.core.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.data.FakeTimeProvider
import com.mid.varagh.core.data.TestTransactionRunner
import com.mid.varagh.core.data.inMemoryDatabase
import com.mid.varagh.core.data.repository.remote.RemoteBookRepository
import com.mid.varagh.core.data.repository.remote.RemoteReadingSessionRepository
import com.mid.varagh.core.database.VaraghDatabase
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.network.VaraghApi
import com.mid.varagh.core.network.auth.AuthTokens
import com.mid.varagh.core.network.auth.TokenStore
import com.mid.varagh.core.network.model.BookMetaDto
import com.mid.varagh.core.network.model.LibraryEntryDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class SyncEngineTest {

    private lateinit var db: VaraghDatabase
    private val time = FakeTimeProvider(now = 1_000)
    private val api = mockk<VaraghApi>(relaxed = true)
    private val tokens = object : TokenStore {
        val state = MutableStateFlow<AuthTokens?>(AuthTokens("a", "r"))
        override val tokens = state
        override fun current() = state.value
        override suspend fun save(tokens: AuthTokens) { state.value = tokens }
        override suspend fun clear() { state.value = null }
    }
    private val scheduler = object : SyncScheduler {
        var requests = 0
        override fun requestSync() { requests++ }
        override fun schedulePeriodicSync() = Unit
        override fun cancelAll() = Unit
    }
    private lateinit var books: RemoteBookRepository
    private lateinit var sessions: RemoteReadingSessionRepository
    private lateinit var engine: SyncEngine

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        val tx = TestTransactionRunner(db)
        books = RemoteBookRepository(db.bookDao(), db.readingProgressDao(), tx, time, scheduler)
        sessions = RemoteReadingSessionRepository(db.readingSessionDao(), scheduler)
        engine = SyncEngine(api, tokens, db.bookDao(), db.readingProgressDao(), db.readingSessionDao(), db.userProfileDao(), tx)
        coEvery { api.library() } returns emptyList()
    }

    @After
    fun tearDown() = db.close()

    private fun entry(id: String, status: String = "READING", updatedAt: Long = 1_000, page: Int? = null) =
        LibraryEntryDto(id = id, book = BookMetaDto(id = "cat-$id", title = "T"), status = status, updatedAt = updatedAt, currentPage = page)

    private suspend fun addBook(hash: String = "h1") =
        books.addBook(NewBook("شاهنامه", "فردوسی", "content://$hash", hash, 100, null))

    @Test
    fun remoteWritesArePending_andScheduleSync() = runTest {
        val id = addBook()
        books.setStatus(id, ReadingStatus.READING)
        assertEquals(SyncState.PENDING, db.bookDao().getById(id)!!.syncState)
        assertTrue(scheduler.requests >= 2)
    }

    @Test
    fun newBook_isAdded_thenPatchedWithProgress_andMarkedSynced() = runTest {
        val id = addBook()
        books.saveProgress(id, 49)
        coEvery { api.addToLibrary(any()) } returns entry("e1")
        coEvery { api.updateLibraryEntry(any(), any()) } returns entry("e1")

        val report = engine.sync()

        assertEquals(1, report.booksPushed)
        val row = db.bookDao().getById(id)!!
        assertEquals("e1", row.remoteEntryId)
        assertEquals("cat-e1", row.remoteId)
        assertEquals(SyncState.SYNCED, row.syncState)
        coVerify { api.updateLibraryEntry("e1", match { it.currentPage == 49 && it.status == "WANT_TO_READ" }) }
    }

    @Test
    fun deletedBook_isDeletedOnServer_thenRemovedLocally() = runTest {
        val id = addBook()
        db.bookDao().setRemoteIds(id, "cat-e9", "e9", SyncState.SYNCED)
        books.deleteBook(id)
        assertTrue("tombstone hidden from the UI", books.observeLibrary().first().isEmpty())

        engine.sync()

        coVerify { api.deleteLibraryEntry("e9") }
        assertNull(db.bookDao().findByHashIncludingDeleted("h1"))
    }

    @Test
    fun sessions_waitUntilTheirBookIsKnownToTheServer() = runTest {
        val id = addBook()
        sessions.addSession(id, 0, 60_000, 3)
        coEvery { api.addToLibrary(any()) } throws VaraghException.Network(IOException())

        runCatching { engine.sync() }
        coVerify(exactly = 0) { api.uploadSessions(any()) }

        coEvery { api.addToLibrary(any()) } returns entry("e1")
        coEvery { api.updateLibraryEntry(any(), any()) } returns entry("e1")
        val captured = slot<List<com.mid.varagh.core.network.model.ReadingSessionDto>>()
        coEvery { api.uploadSessions(capture(captured)) } returns Unit
        engine.sync()
        assertEquals("cat-e1", captured.captured.single().bookId)
        assertTrue(db.readingSessionDao().getPending().isEmpty())
    }

    @Test
    fun pull_isLastWriteWins() = runTest {
        val a = addBook("ha")
        val b = addBook("hb")
        db.bookDao().setRemoteIds(a, "cat-a", "ea", SyncState.SYNCED)
        time.now = 9_000
        books.setRating(b, 4)
        db.bookDao().setRemoteIds(b, "cat-b", "eb", SyncState.PENDING)
        db.bookDao().markSynced(b, 9_000)
        coEvery { api.library() } returns listOf(
            entry("ea", status = "FINISHED", updatedAt = 5_000, page = 99),
            entry("eb", status = "ABANDONED", updatedAt = 5_000),
        )

        val report = engine.sync()

        assertEquals(1, report.booksPulled)
        val bookA = db.bookDao().getById(a)!!
        assertEquals("server is newer", ReadingStatus.FINISHED, bookA.status)
        assertEquals(5_000L, bookA.finishedAt)
        assertEquals(99, db.readingProgressDao().get(a)!!.currentPage)
        val bookB = db.bookDao().getById(b)!!
        assertEquals("local is newer", ReadingStatus.WANT_TO_READ, bookB.status)
        assertEquals(4, bookB.rating)
    }

    @Test
    fun signedOut_doesNothing() = runTest {
        tokens.state.value = null
        addBook()
        assertEquals(SyncReport(), engine.sync())
        coVerify(exactly = 0) { api.addToLibrary(any()) }
    }
}
