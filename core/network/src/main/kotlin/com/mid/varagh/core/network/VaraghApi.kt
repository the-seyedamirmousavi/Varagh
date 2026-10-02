package com.mid.varagh.core.network

import com.mid.varagh.core.network.model.AuthResponseDto
import com.mid.varagh.core.network.model.AvatarResponseDto
import com.mid.varagh.core.network.model.BookMetaDto
import com.mid.varagh.core.network.model.FeedPageDto
import com.mid.varagh.core.network.model.LibraryEntryDto
import com.mid.varagh.core.network.model.LibraryEntryRequest
import com.mid.varagh.core.network.model.LibraryPatchDto
import com.mid.varagh.core.network.model.LoginRequest
import com.mid.varagh.core.network.model.PublicReaderDto
import com.mid.varagh.core.network.model.ReadingSessionDto
import com.mid.varagh.core.network.model.ReadingStatsDto
import com.mid.varagh.core.network.model.RefreshRequest
import com.mid.varagh.core.network.model.RegisterRequest
import com.mid.varagh.core.network.model.TokenPairDto
import com.mid.varagh.core.network.model.UserProfileDto
import com.mid.varagh.core.network.model.UserProfilePatchDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The Varagh REST API, exactly as specified in docs/api-contract.yaml. Only used when
 * USE_REMOTE_BACKEND is true. PDF files are never uploaded: only metadata, progress and stats.
 */
interface VaraghApi {
    // Auth (no bearer token)
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenPairDto

    // Me
    @GET("me")
    suspend fun me(): UserProfileDto

    @PATCH("me")
    suspend fun updateMe(@Body body: UserProfilePatchDto): UserProfileDto

    @Multipart
    @POST("me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): AvatarResponseDto

    @GET("me/stats")
    suspend fun myStats(): ReadingStatsDto

    // Catalog & library
    @GET("books/search")
    suspend fun searchBooks(@Query("q") query: String): List<BookMetaDto>

    @POST("library")
    suspend fun addToLibrary(@Body body: LibraryEntryRequest): LibraryEntryDto

    @GET("library")
    suspend fun library(): List<LibraryEntryDto>

    @PATCH("library/{id}")
    suspend fun updateLibraryEntry(@Path("id") id: String, @Body body: LibraryPatchDto): LibraryEntryDto

    @DELETE("library/{id}")
    suspend fun deleteLibraryEntry(@Path("id") id: String)

    // Sessions (batch sync)
    @POST("sessions")
    suspend fun uploadSessions(@Body sessions: List<ReadingSessionDto>)

    // Social
    @GET("users/{username}")
    suspend fun user(@Path("username") username: String): PublicReaderDto

    @POST("users/{id}/follow")
    suspend fun follow(@Path("id") id: String)

    @DELETE("users/{id}/follow")
    suspend fun unfollow(@Path("id") id: String)

    @GET("feed")
    suspend fun feed(@Query("cursor") cursor: String?): FeedPageDto

    @GET("books/{remoteId}/readers")
    suspend fun readersOfBook(@Path("remoteId") remoteId: String): List<PublicReaderDto>
}
