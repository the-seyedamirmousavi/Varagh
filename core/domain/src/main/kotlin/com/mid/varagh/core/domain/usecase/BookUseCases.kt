package com.mid.varagh.core.domain.usecase

import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.BookmarkRepository
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.ReadingStatus
import javax.inject.Inject

sealed interface AddBookResult {
    data class Added(val bookId: Long) : AddBookResult

    /** A book with the same file hash is already in the library. */
    data class Duplicate(val existing: Book) : AddBookResult
}

/** Adds a book unless the same file (by SHA-256) is already in the library. */
class AddBookUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(book: NewBook): AddBookResult {
        require(book.fileHash.isNotBlank()) { "fileHash is required" }
        books.findByHash(book.fileHash)?.let { return AddBookResult.Duplicate(it) }
        val title = book.title.trim().ifBlank { UNTITLED }
        return AddBookResult.Added(books.addBook(book.copy(title = title, author = book.author?.trim()?.ifBlank { null })))
    }

    companion object {
        const val UNTITLED = "Untitled"
    }
}

/**
 * Called when the reader opens a book: stamps `lastOpenedAt` and moves a WANT_TO_READ book to
 * READING. Returns the book, or null if it no longer exists.
 */
class OpenBookUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long): Book? {
        val book = books.getBook(bookId) ?: return null
        books.markOpened(bookId)
        if (book.status == ReadingStatus.WANT_TO_READ) books.setStatus(bookId, ReadingStatus.READING)
        return books.getBook(bookId)
    }
}

/** Saves the current page. Page numbers outside the book are clamped. */
class SaveReadingProgressUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long, page: Int) {
        val book = books.getBook(bookId) ?: return
        val clamped = page.coerceIn(0, (book.pageCount - 1).coerceAtLeast(0))
        books.saveProgress(bookId, clamped)
    }
}

class SetBookStatusUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long, status: ReadingStatus) = books.setStatus(bookId, status)
}

class SetBookRatingUseCase @Inject constructor(
    private val books: BookRepository,
) {
    /** [rating] 0..5 or null to clear. */
    suspend operator fun invoke(bookId: Long, rating: Int?) {
        require(rating == null || rating in 0..Book.MAX_RATING) { "rating must be 0..${Book.MAX_RATING}" }
        books.setRating(bookId, rating)
    }
}

class UpdateBookDetailsUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long, title: String, author: String?) {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "title must not be blank" }
        books.updateMetadata(bookId, cleanTitle, author?.trim()?.ifBlank { null })
    }
}

/** Removes a book from the library. The PDF file itself is never deleted. */
class DeleteBookUseCase @Inject constructor(
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long) = books.deleteBook(bookId)
}

/** Adds a bookmark on [page], or removes the existing one. Returns true if the page is now bookmarked. */
class ToggleBookmarkUseCase @Inject constructor(
    private val bookmarks: BookmarkRepository,
) {
    suspend operator fun invoke(bookId: Long, page: Int, note: String? = null): Boolean {
        val existing = bookmarks.findOnPage(bookId, page)
        return if (existing != null) {
            bookmarks.deleteBookmark(existing.id)
            false
        } else {
            bookmarks.addBookmark(bookId, page, note?.trim()?.ifBlank { null })
            true
        }
    }
}
