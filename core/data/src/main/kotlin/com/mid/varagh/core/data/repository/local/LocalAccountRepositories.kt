package com.mid.varagh.core.data.repository.local

import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.domain.repository.AuthRepository
import com.mid.varagh.core.domain.repository.SocialRepository
import com.mid.varagh.core.model.AuthState
import com.mid.varagh.core.model.BookMeta
import com.mid.varagh.core.model.FeedPage
import com.mid.varagh.core.model.PublicReader
import com.mid.varagh.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Offline build: one local user who is always signed in. Account actions need the server. */
class LocalAuthRepository @Inject constructor() : AuthRepository {

    override val authState: Flow<AuthState> =
        flowOf(AuthState.SignedIn(userId = UserProfile.LOCAL_USER_ID.toString(), username = LOCAL_USERNAME))

    override suspend fun login(email: String, password: String) = unavailable()

    override suspend fun register(username: String, email: String, password: String) = unavailable()

    /** There is nothing to sign out of locally; a no-op keeps callers simple. */
    override suspend fun logout() = Unit

    private fun unavailable(): Nothing = throw VaraghException.FeatureUnavailable("Accounts")

    companion object {
        const val LOCAL_USERNAME = "local"
    }
}

/**
 * Offline build: social features do not exist. The UI hides them via FeatureFlags, so reaching
 * these methods is a programming error surfaced as [VaraghException.FeatureUnavailable].
 */
class LocalSocialRepository @Inject constructor() : SocialRepository {
    override suspend fun getFeed(cursor: String?): FeedPage = unavailable()
    override suspend fun getReader(username: String): PublicReader = unavailable()
    override suspend fun follow(readerId: String) = unavailable()
    override suspend fun unfollow(readerId: String) = unavailable()
    override suspend fun readersOfBook(remoteBookId: String): List<PublicReader> = unavailable()
    override suspend fun searchCatalog(query: String): List<BookMeta> = unavailable()

    private fun unavailable(): Nothing = throw VaraghException.FeatureUnavailable("Social")
}
