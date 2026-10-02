package com.mid.varagh.core.network.auth

import com.mid.varagh.core.network.model.RefreshRequest
import com.mid.varagh.core.network.model.TokenPairDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

data class AuthTokens(val accessToken: String, val refreshToken: String)

/** Secure token storage (implemented with encrypted DataStore in :core:data). */
interface TokenStore {
    val tokens: Flow<AuthTokens?>

    /** Current tokens; may block briefly on first access (called from OkHttp threads). */
    fun current(): AuthTokens?
    suspend fun save(tokens: AuthTokens)
    suspend fun clear()
}

/** Adds `Authorization: Bearer …` to every request except the public auth endpoints. */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.contains("/auth/")) return chain.proceed(request)
        val token = tokenStore.current()?.accessToken ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/**
 * On HTTP 401, refreshes the access token once (POST /auth/refresh with a separate client, so
 * this authenticator never recurses) and retries. If refreshing fails the tokens are cleared,
 * which signs the user out.
 */
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @param:Named(BASE_URL) private val baseUrl: String,
    @param:Named(REFRESH_CLIENT) private val refreshClient: OkHttpClient,
    private val json: Json,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null
        if (responseCount(response) >= 2) return null
        synchronized(this) {
            val current = tokenStore.current() ?: return null
            val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            // Another request already refreshed while we waited: just retry with the new token.
            if (sentToken != null && sentToken != current.accessToken) {
                return response.request.withToken(current.accessToken)
            }
            val refreshed = refresh(current.refreshToken)
            if (refreshed == null) {
                runBlocking { tokenStore.clear() }
                return null
            }
            runBlocking { tokenStore.save(refreshed) }
            return response.request.withToken(refreshed.accessToken)
        }
    }

    private fun refresh(refreshToken: String): AuthTokens? = runCatching {
        val body = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder().url(baseUrl.trimEnd('/') + "/auth/refresh").post(body).build()
        refreshClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val pair = json.decodeFromString(TokenPairDto.serializer(), response.body.string())
            AuthTokens(pair.accessToken, pair.refreshToken)
        }
    }.getOrNull()

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        const val BASE_URL = "varagh.baseUrl"
        const val REFRESH_CLIENT = "varagh.refreshClient"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
