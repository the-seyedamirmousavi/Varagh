package com.mid.varagh.core.network

import com.mid.varagh.core.network.auth.AuthInterceptor
import com.mid.varagh.core.network.auth.AuthTokens
import com.mid.varagh.core.network.auth.TokenRefreshAuthenticator
import com.mid.varagh.core.network.auth.TokenStore
import com.mid.varagh.core.network.di.NetworkModule
import com.mid.varagh.core.network.model.LibraryPatchDto
import com.mid.varagh.core.network.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

class VaraghApiTest {

    private val server = MockWebServer()
    private val json = NetworkModule.provideJson()
    private val store = FakeTokenStore()
    private lateinit var api: VaraghApi

    private class FakeTokenStore : TokenStore {
        val state = MutableStateFlow<AuthTokens?>(null)
        override val tokens = state
        override fun current(): AuthTokens? = state.value
        override suspend fun save(tokens: AuthTokens) { state.value = tokens }
        override suspend fun clear() { state.value = null }
    }

    @Before
    fun setUp() {
        server.start()
        val baseUrl = server.url("/v1/").toString()
        val refreshClient = OkHttpClient()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store))
            .authenticator(TokenRefreshAuthenticator(store, baseUrl, refreshClient, json))
            .build()
        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(VaraghApi::class.java)
    }

    @After
    fun tearDown() = server.close()

    private fun respond(code: Int, body: String = "") =
        server.enqueue(MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json").build())

    @Test
    fun login_isPublic_andParsesUser_ignoringUnknownFields() = runTest {
        store.state.value = AuthTokens("old", "r")
        respond(200, """{"accessToken":"a","refreshToken":"b","user":{"id":"u1","username":"amir","extra":1}}""")
        val result = api.login(LoginRequest("a@b.c", "password1"))
        assertEquals("u1", result.user.id)
        val request = server.takeRequest()
        assertEquals("/v1/auth/login", request.url.encodedPath)
        assertNull("auth endpoints never carry a bearer token", request.headers["Authorization"])
    }

    @Test
    fun authenticatedCalls_sendBearer_andOmitNullPatchFields() = runTest {
        store.state.value = AuthTokens("access-1", "refresh-1")
        respond(200, """{"id":"e1","book":{"id":"b1","title":"T"},"status":"READING","updatedAt":5}""")
        api.updateLibraryEntry("e1", LibraryPatchDto(currentPage = 12))
        val request = server.takeRequest()
        assertEquals("Bearer access-1", request.headers["Authorization"])
        assertEquals("PATCH", request.method)
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"currentPage\":12"))
        assertFalse("null fields are not sent in PATCH bodies", body.contains("status"))
    }

    @Test
    fun expiredToken_isRefreshedOnce_andTheCallRetried() = runTest {
        store.state.value = AuthTokens("expired", "refresh-1")
        respond(401)
        respond(200, """{"accessToken":"fresh","refreshToken":"refresh-2"}""")
        respond(200, """[]""")

        assertEquals(emptyList<Any>(), api.library())

        assertEquals(AuthTokens("fresh", "refresh-2"), store.current())
        assertEquals("Bearer expired", server.takeRequest().headers["Authorization"])
        val refresh = server.takeRequest()
        assertEquals("/v1/auth/refresh", refresh.url.encodedPath)
        assertTrue(refresh.body!!.utf8().contains("refresh-1"))
        assertEquals("Bearer fresh", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun failedRefresh_signsOut() = runTest {
        store.state.value = AuthTokens("expired", "revoked")
        respond(401)
        respond(401)
        val error = runCatching { api.me() }.exceptionOrNull()
        assertTrue(error is HttpException && error.code() == 401)
        assertNull(store.current())
    }
}
