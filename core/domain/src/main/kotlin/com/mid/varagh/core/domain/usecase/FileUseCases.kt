package com.mid.varagh.core.domain.usecase

import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.BookFileRepository
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.NewBook
import javax.inject.Inject

sealed interface ImportResult {
    data class Added(val bookId: Long, val title: String) : ImportResult
    data class Duplicate(val existing: Book) : ImportResult
}

/**
 * Imports a PDF picked with the Storage Access Framework: persists access, detects duplicates by
 * SHA-256, renders a cover from page 1 and adds the book. Throws [VaraghException.InvalidFile] or
 * [VaraghException.FileUnavailable].
 */
class ImportBookUseCase @Inject constructor(
    private val files: BookFileRepository,
    private val books: BookRepository,
    private val addBook: AddBookUseCase,
) {
    suspend operator fun invoke(fileUri: String): ImportResult {
        val info = files.inspect(fileUri)
        books.findByHash(info.fileHash)?.let { return ImportResult.Duplicate(it) }
        val cover = files.renderCover(fileUri, info.fileHash)
        val title = titleFromFileName(info.displayName)
        val result = addBook(
            NewBook(
                title = title,
                author = null,
                fileUri = fileUri,
                fileHash = info.fileHash,
                pageCount = info.pageCount,
                coverPath = cover,
            ),
        )
        return when (result) {
            is AddBookResult.Added -> ImportResult.Added(result.bookId, title)
            is AddBookResult.Duplicate -> ImportResult.Duplicate(result.existing)
        }
    }

    companion object {
        /** "my_book-name.pdf" -> "my book name". */
        fun titleFromFileName(name: String?): String =
            name.orEmpty()
                .substringBeforeLast('.', name.orEmpty())
                .replace('_', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { AddBookUseCase.UNTITLED }
    }
}

/**
 * Points a book at a new copy of its file (after the original was moved or permission was lost).
 * The new file must be the same document (same SHA-256).
 */
class RelinkBookFileUseCase @Inject constructor(
    private val files: BookFileRepository,
    private val books: BookRepository,
) {
    suspend operator fun invoke(bookId: Long, newFileUri: String) {
        val book = books.getBook(bookId) ?: throw VaraghException.NotFound("Book")
        val info = files.inspect(newFileUri)
        if (info.fileHash != book.fileHash) {
            files.releaseAccess(newFileUri)
            throw VaraghException.DifferentFile()
        }
        books.updateFileUri(bookId, newFileUri)
        if (book.coverPath == null) {
            books.updateFileInfo(bookId, info.pageCount, files.renderCover(newFileUri, info.fileHash))
        }
    }
}
