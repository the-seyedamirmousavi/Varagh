package com.mid.varagh.core.domain.repository

import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookMeta
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.FeedPage
import com.mid.varagh.core.model.LibraryQuery
import com.mid.varagh.core.model.NewBook
import com.mid.varagh.core.model.PublicReader
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.ReadingStatus
import com.mid.varagh.core.model.UserPreferences
import com.mid.varagh.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

// Every repository below has a Local… (Room) implementation and, from phase 8, a Remote…
// (Retrofit + Room cache) implementation in :core:data. Which one is bound is decided once, by
// the USE_REMOTE_BACKEND build flag. Room stays the source of truth for the UI in both cases.

/** Books and their reading progress. */
interface BookRepository {
    fun observeLibrary(query: LibraryQuery = LibraryQuery()): Flow<List<BookWithProgress>>
    fun observeBook(bookId: Long): Flow<BookWithProgress?>
    fun observeBooksByStatus(status: ReadingStatus): Flow<List<BookWithProgress>>
    suspend fun getBook(bookId: Long): Book?

    /** Existing, non-deleted book with this SHA-256, if any. */
    suspend fun findByHash(fileHash: String): Book?

    /** Inserts the book (or revives a previously deleted one with the same hash). Returns its id. */
    suspend fun addBook(book: NewBook): Long
    suspend fun updateMetadata(bookId: Long, title: String, author: String?)
    suspend fun updateFileInfo(bookId: Long, pageCount: Int, coverPath: String?)

    /** Sets the status; `finishedAt` is set when moving to FINISHED and cleared otherwise. */
    suspend fun setStatus(bookId: Long, status: ReadingStatus)
    suspend fun setRating(bookId: Long, rating: Int?)
    suspend fun markOpened(bookId: Long)

    /** [page] is zero-based; percent is derived from the book's page count. */
    suspend fun saveProgress(bookId: Long, page: Int)

    /** Removes the book from the library (and its progress/sessions/bookmarks). Never touches the PDF. */
    suspend fun deleteBook(bookId: Long)
}

interface ReadingSessionRepository {
    fun observeSessions(bookId: Long): Flow<List<ReadingSession>>
    fun observeAllSessions(): Flow<List<ReadingSession>>
    fun observeTotalReadingMillis(bookId: Long): Flow<Long>
    suspend fun addSession(bookId: Long, startedAt: Long, endedAt: Long, pagesRead: Int): Long
}

interface BookmarkRepository {
    fun observeBookmarks(bookId: Long): Flow<List<Bookmark>>
    suspend fun findOnPage(bookId: Long, page: Int): Bookmark?
    suspend fun addBookmark(bookId: Long, page: Int, note: String?): Long
    suspend fun updateNote(bookmarkId: Long, note: String?)
    suspend fun deleteBookmark(bookmarkId: Long)
}

interface UserProfileRepository {
    /** Always emits a profile; a default empty one before the user edits it. */
    fun observeProfile(): Flow<UserProfile>
    suspend fun updateProfile(transform: (UserProfile) -> UserProfile)
}

/**
 * Local build: a single always-signed-in local user; login/register are unsupported.
 * Remote build: JWT login/register/refresh with tokens in encrypted storage.
 */
interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun login(email: String, password: String)
    suspend fun register(username: String, email: String, password: String)
    suspend fun logout()
}

/**
 * Server-only features. The local implementation returns empty data / throws
 * `VaraghException.FeatureUnavailable`, and the UI never calls it while the flag is off.
 */
interface SocialRepository {
    suspend fun getFeed(cursor: String?): FeedPage
    suspend fun getReader(username: String): PublicReader
    suspend fun follow(readerId: String)
    suspend fun unfollow(readerId: String)
    suspend fun readersOfBook(remoteBookId: String): List<PublicReader>
    suspend fun searchCatalog(query: String): List<BookMeta>
}

/**
 * Device settings (DataStore). Deliberately device-local in both builds: reading theme, language
 * etc. are per-device choices and are not synced.
 */
interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}
