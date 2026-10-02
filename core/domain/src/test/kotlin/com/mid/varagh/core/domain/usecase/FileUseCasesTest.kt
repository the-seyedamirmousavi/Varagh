package com.mid.varagh.core.domain.usecase

import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BookFileRepository
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.PdfFileInfo
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUseCasesTest {

    private val files = mockk<BookFileRepository>(relaxUnitFun = true)
    private val books = mockk<BookRepository>(relaxUnitFun = true)
    private val import = ImportBookUseCase(files, books, AddBookUseCase(books))

    private val info = PdfFileInfo(displayName = "my_great-book.pdf", fileHash = "abc", pageCount = 321, sizeBytes = 10)
    private val book = Book(5, "t", null, "content://old", "abc", 321, null, 0, null, null, ReadingStatus.READING, null, null)

    @Test
    fun `title comes from the file name`() {
        assertEquals("my great-book", ImportBookUseCase.titleFromFileName("my_great-book.pdf"))
        assertEquals("شاهنامه فردوسی", ImportBookUseCase.titleFromFileName("شاهنامه  فردوسی.PDF"))
        assertEquals("noextension", ImportBookUseCase.titleFromFileName("noextension"))
        assertEquals(AddBookUseCase.UNTITLED, ImportBookUseCase.titleFromFileName(null))
        assertEquals(AddBookUseCase.UNTITLED, ImportBookUseCase.titleFromFileName(".pdf"))
    }

    @Test
    fun `import adds the book with its cover and page count`() = runTest {
        coEvery { files.inspect("content://new") } returns info
        coEvery { files.renderCover("content://new", "abc") } returns "/covers/abc.jpg"
        coEvery { books.findByHash("abc") } returns null
        coEvery { books.addBook(any()) } returns 11

        val result = import("content://new")

        assertEquals(ImportResult.Added(11, "my great-book"), result)
        coVerify {
            books.addBook(NewBook("my great-book", null, "content://new", "abc", 321, "/covers/abc.jpg"))
        }
    }

    @Test
    fun `import detects duplicates before rendering a cover`() = runTest {
        coEvery { files.inspect(any()) } returns info
        coEvery { books.findByHash("abc") } returns book
        assertEquals(ImportResult.Duplicate(book), import("content://copy"))
        coVerify(exactly = 0) { files.renderCover(any(), any()) }
    }

    @Test
    fun `relink accepts the same document and rejects a different one`() = runTest {
        val relink = RelinkBookFileUseCase(files, books)
        coEvery { books.getBook(5) } returns book
        coEvery { files.inspect("content://moved") } returns info
        coEvery { files.renderCover(any(), any()) } returns null
        relink(5, "content://moved")
        coVerify { books.updateFileUri(5, "content://moved") }

        coEvery { files.inspect("content://other") } returns info.copy(fileHash = "zzz")
        val error = runCatching { relink(5, "content://other") }.exceptionOrNull()
        assertTrue(error is VaraghException.DifferentFile)
        coVerify { files.releaseAccess("content://other") }
    }

    @Test
    fun `delete removes cover and releases permission but never the pdf`() = runTest {
        coEvery { books.getBook(5) } returns book.copy(coverPath = "/covers/abc.jpg")
        DeleteBookUseCase(books, files)(5)
        coVerify { books.deleteBook(5) }
        coVerify { files.deleteCover("/covers/abc.jpg") }
        coVerify { files.releaseAccess("content://old") }
    }
}
