package com.mid.varagh.core.data.repository.remote

import com.mid.varagh.core.data.remote.apiCall
import com.mid.varagh.core.data.repository.local.LocalBookRepository
import com.mid.varagh.core.data.repository.local.LocalBookmarkRepository
import com.mid.varagh.core.data.repository.local.LocalReadingSessionRepository
import com.mid.varagh.core.data.repository.local.LocalUserProfileRepository
import com.mid.varagh.core.data.sync.SyncScheduler
import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.dao.BookDao
import com.mid.varagh.core.database.dao.BookmarkDao
import com.mid.varagh.core.database.dao.ReadingProgressDao
import com.mid.varagh.core.database.dao.ReadingSessionDao
import com.mid.varagh.core.database.dao.UserProfileDao
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.database.model.UserProfileEntity
import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.SocialRepository
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.BookMeta
import com.mid.varagh.core.model.FeedItem
import com.mid.varagh.core.model.FeedItemType
import com.mid.varagh.core.model.FeedPage
import com.mid.varagh.core.model.PublicReader
import com.mid.varagh.core.model.UserProfile
import com.mid.varagh.core.network.VaraghApi
import com.mid.varagh.core.network.auth.AuthTokens
import com.mid.varagh.core.network.auth.TokenStore
import com.mid.varagh.core.network.model.AuthResponseDto
import com.mid.varagh.core.network.model.BookMetaDto
import com.mid.varagh.core.network.model.FeedItemDto
import com.mid.varagh.core.network.model.LoginRequest
import com.mid.varagh.core.network.model.PublicReaderDto
import com.mid.varagh.core.network.model.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

// Remote repositories: Room stays the source of truth for the UI. Writes go to Room first marked
// PENDING (deletions as tombstones) and a background SyncWorker pushes/pulls them through
// Retrofit, so everything keeps working offline even with USE_REMOTE_BACKEND = true.

class RemoteBookRepository @Inject constructor(
    bookDao: BookDao,
    progressDao: ReadingProgressDao,
    transaction: TransactionRunner,
    time: TimeProvider,
    private val sync: SyncScheduler,
) : LocalBookRepository(bookDao, progressDao, transaction, time) {

    override val writeState: SyncState get() = SyncState.PENDING

    override suspend fun afterWrite() = sync.requestSync()

    override suspend fun onProgressSaved(bookId: Long) {
        bookDao.markPending(bookId, time.nowMillis())
        sync.requestSync()
    }

    /** Tombstone until the server confirms the deletion (then the SyncWorker removes the row). */
    override suspend fun deleteBook(bookId: Long) {
        bookDao.markDeleted(bookId, time.nowMillis())
        sync.requestSync()
    }
}

class RemoteReadingSessionRepository @Inject constructor(
    sessionDao: ReadingSessionDao,
    private val sync: SyncScheduler,
) : LocalReadingSessionRepository(sessionDao) {
    override val writeState: SyncState get() = SyncState.PENDING
    override suspend fun afterWrite() = sync.requestSync()
}

/** The API contract has no bookmark endpoints: bookmarks are personal notes and stay on the device. */
class RemoteBookmarkRepository @Inject constructor(
    bookmarkDao: BookmarkDao,
    time: TimeProvider,
) : LocalBookmarkRepository(bookmarkDao, time)

class RemoteUserProfileRepository @Inject constructor(
    profileDao: UserProfileDao,
    transaction: TransactionRunner,
    time: TimeProvider,
    private val sync: SyncScheduler,
) : LocalUserProfileRepository(profileDao, transaction, time) {
    override val writeState: SyncState get() = SyncState.PENDING
    override suspend fun afterWrite() = sync.requestSync()
}

/** JWT login/register; tokens live in [TokenStore] (encrypted DataStore). */
class RemoteAuthRepository @Inject constructor(
    private val api: VaraghApi,
    private val tokens: TokenStore,
    private val profileDao: UserProfileDao,
    private val transaction: TransactionRunner,
    private val time: TimeProvider,
    private val sync: SyncScheduler,
) : AuthRepository {

    override val authState: Flow<AuthState> =
        combine(tokens.tokens, profileDao.observe(UserProfile.LOCAL_USER_ID)) { saved, profile ->
            if (saved == null) {
                AuthState.SignedOut
            } else {
                AuthState.SignedIn(userId = profile?.remoteId.orEmpty(), username = profile?.username.orEmpty())
            }
        }.distinctUntilChanged()

    override suspend fun login(email: String, password: String) =
        signedIn(apiCall { api.login(LoginRequest(email.trim(), password)) })

    override suspend fun register(username: String, email: String, password: String) =
        signedIn(apiCall { api.register(RegisterRequest(username.trim(), email.trim(), password)) })

    override suspend fun logout() {
        tokens.clear()
        sync.cancelAll()
    }

    private suspend fun signedIn(response: AuthResponseDto) {
        tokens.save(AuthTokens(response.accessToken, response.refreshToken))
        val user = response.user
        transaction {
            val current = profileDao.get(UserProfile.LOCAL_USER_ID)
            profileDao.upsert(
                UserProfileEntity(
                    id = UserProfile.LOCAL_USER_ID,
                    displayName = user.displayName.ifBlank { current?.displayName.orEmpty() },
                    username = user.username,
                    bio = user.bio.ifBlank { current?.bio.orEmpty() },
                    avatarPath = current?.avatarPath,
                    isPublic = user.isPublic,
                    currentlyReadingBookId = current?.currentlyReadingBookId,
                    remoteId = user.id,
                    updatedAt = time.nowMillis(),
                    syncState = SyncState.SYNCED,
                ),
            )
        }
        sync.requestSync()
        sync.schedulePeriodicSync()
    }
}

class RemoteSocialRepository @Inject constructor(
    private val api: VaraghApi,
) : SocialRepository {
    override suspend fun getFeed(cursor: String?): FeedPage = apiCall {
        api.feed(cursor).let { page -> FeedPage(page.items.map { it.asModel() }, page.nextCursor) }
    }

    override suspend fun getReader(username: String): PublicReader =
        apiCall { api.user(username.trim().removePrefix("@")).asModel() }

    override suspend fun follow(readerId: String) = apiCall { api.follow(readerId) }

    override suspend fun unfollow(readerId: String) = apiCall { api.unfollow(readerId) }

    override suspend fun readersOfBook(remoteBookId: String): List<PublicReader> =
        apiCall { api.readersOfBook(remoteBookId).map { it.asModel() } }

    override suspend fun searchCatalog(query: String): List<BookMeta> =
        apiCall { api.searchBooks(query).mapNotNull { it.asModel() } }
}

internal fun BookMetaDto.asModel(): BookMeta? =
    id?.let { BookMeta(remoteId = it, title = title, author = author, pageCount = pageCount, coverUrl = coverUrl) }

internal fun PublicReaderDto.asModel() = PublicReader(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    currentlyReading = currentlyReading?.asModel(),
    booksFinished = booksFinished,
    isFollowedByMe = isFollowedByMe,
)

internal fun FeedItemDto.asModel() = FeedItem(
    id = id,
    reader = reader.asModel(),
    book = book.asModel() ?: BookMeta(remoteId = "", title = book.title, author = book.author, pageCount = book.pageCount, coverUrl = book.coverUrl),
    type = runCatching { FeedItemType.valueOf(type) }.getOrDefault(FeedItemType.READING),
    progressPercent = progressPercent,
    createdAt = createdAt,
)
