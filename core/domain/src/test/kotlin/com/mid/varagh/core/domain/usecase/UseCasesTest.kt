package com.mid.varagh.core.domain.usecase

import app.cash.turbine.test
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.repository.UserProfileRepository
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {

    private val books = mockk<BookRepository>(relaxUnitFun = true)

    private fun book(id: Long, status: ReadingStatus = ReadingStatus.WANT_TO_READ, pages: Int = 100) = Book(
        id = id, title = "Book $id", author = null, fileUri = "content://$id", fileHash = "hash$id",
        pageCount = pages, coverPath = null, addedAt = 0, lastOpenedAt = null, finishedAt = null,
        status = status, rating = null, remoteId = null,
    )

    private val newBook = NewBook(title = "  Shahnameh ", author = " ", fileUri = "content://x", fileHash = "abc", pageCount = 10, coverPath = null)

    @Test
    fun `add book reports duplicates by hash`() = runTest {
        coEvery { books.findByHash("abc") } returns book(7)
        val result = AddBookUseCase(books)(newBook)
        assertEquals(AddBookResult.Duplicate(book(7)), result)
        coVerify(exactly = 0) { books.addBook(any()) }
    }

    @Test
    fun `add book trims title and blanks author`() = runTest {
        coEvery { books.findByHash("abc") } returns null
        coEvery { books.addBook(any()) } returns 42
        val result = AddBookUseCase(books)(newBook)
        assertEquals(AddBookResult.Added(42), result)
        coVerify { books.addBook(newBook.copy(title = "Shahnameh", author = null)) }
    }

    @Test
    fun `opening a want-to-read book starts reading it`() = runTest {
        coEvery { books.getBook(1) } returnsMany listOf(book(1), book(1, ReadingStatus.READING))
        val opened = OpenBookUseCase(books)(1)
        assertEquals(ReadingStatus.READING, opened?.status)
        coVerify { books.markOpened(1) }
        coVerify { books.setStatus(1, ReadingStatus.READING) }
    }

    @Test
    fun `opening a finished book keeps its status`() = runTest {
        coEvery { books.getBook(1) } returns book(1, ReadingStatus.FINISHED)
        OpenBookUseCase(books)(1)
        coVerify(exactly = 0) { books.setStatus(any(), any()) }
    }

    @Test
    fun `progress is clamped to the book`() = runTest {
        coEvery { books.getBook(1) } returns book(1, pages = 50)
        SaveReadingProgressUseCase(books)(1, page = 80)
        coVerify { books.saveProgress(1, 49) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rating outside 0 to 5 is rejected`() = runTest {
        SetBookRatingUseCase(books)(1, 9)
    }

    @Test
    fun `sessions shorter than 30 seconds are ignored`() = runTest {
        val sessions = mockk<ReadingSessionRepository>()
        coEvery { sessions.addSession(any(), any(), any(), any()) } returns 5
        val record = RecordReadingSessionUseCase(sessions)
        assertNull(record(1, startedAt = 0, endedAt = 29_999, pagesRead = 3))
        assertEquals(5L, record(1, startedAt = 0, endedAt = 30_000, pagesRead = -2))
        coVerify(exactly = 1) { sessions.addSession(1, 0, 30_000, 0) }
    }

    @Test
    fun `toggle bookmark adds then removes`() = runTest {
        val bookmarks = mockk<BookmarkRepository>(relaxUnitFun = true)
        val toggle = ToggleBookmarkUseCase(bookmarks)
        coEvery { bookmarks.findOnPage(1, 3) } returns null
        coEvery { bookmarks.addBookmark(1, 3, "note") } returns 9
        assertTrue(toggle(1, 3, " note "))
        coEvery { bookmarks.findOnPage(1, 3) } returns Bookmark(9, 1, 3, "note", 0)
        assertFalse(toggle(1, 3))
        coVerify { bookmarks.deleteBookmark(9) }
    }

    @Test
    fun `currently reading prefers the explicit pick then the most recent`() = runTest {
        val profiles = mockk<UserProfileRepository>()
        val profile = MutableStateFlow(UserProfile.DefaultLocal)
        val reading = MutableStateFlow(listOf(BookWithProgress(book(1, ReadingStatus.READING), null), BookWithProgress(book(2, ReadingStatus.READING), null)))
        every { profiles.observeProfile() } returns profile
        every { books.observeBooksByStatus(ReadingStatus.READING) } returns reading

        ObserveCurrentlyReadingUseCase(books, profiles)().test {
            assertEquals(1L, awaitItem()?.book?.id)
            profile.value = profile.value.copy(currentlyReadingBookId = 2)
            assertEquals(2L, awaitItem()?.book?.id)
            reading.value = emptyList()
            assertNull(awaitItem())
        }
    }
}
