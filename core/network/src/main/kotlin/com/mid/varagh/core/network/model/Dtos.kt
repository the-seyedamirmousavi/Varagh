package com.mid.varagh.core.network.model

import kotlinx.serialization.Serializable

// DTOs for docs/api-contract.yaml. Timestamps are epoch milliseconds (UTC). Unknown fields are
// ignored so the server can evolve without breaking old clients.

@Serializable
data class RegisterRequest(val username: String, val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthResponseDto(val accessToken: String, val refreshToken: String, val user: UserProfileDto)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String)

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isPublic: Boolean = false,
    val currentlyReadingBookId: String? = null,
)

/** PATCH /me body: only non-null fields are sent. */
@Serializable
data class UserProfilePatchDto(
    val displayName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val isPublic: Boolean? = null,
    val currentlyReadingBookId: String? = null,
)

@Serializable
data class AvatarResponseDto(val avatarUrl: String)

/** Shared catalog metadata. Never the PDF. */
@Serializable
data class BookMetaDto(
    val id: String? = null,
    val title: String,
    val author: String? = null,
    val pageCount: Int? = null,
    val coverUrl: String? = null,
)

@Serializable
data class LibraryEntryRequest(val bookMeta: BookMetaDto, val status: String, val rating: Int? = null)

@Serializable
data class LibraryEntryDto(
    val id: String,
    val book: BookMetaDto,
    val status: String,
    val rating: Int? = null,
    val progressPercent: Float? = null,
    val currentPage: Int? = null,
    val updatedAt: Long,
)

/** PATCH /library/{id} body: only non-null fields are sent. */
@Serializable
data class LibraryPatchDto(
    val status: String? = null,
    val rating: Int? = null,
    val progressPercent: Float? = null,
    val currentPage: Int? = null,
)

@Serializable
data class ReadingSessionDto(
    /** Catalog book id (BookMetaDto.id). */
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long,
    val pagesRead: Int,
)

@Serializable
data class ReadingStatsDto(
    val totalBooksFinished: Int,
    val totalPagesRead: Int,
    val totalMinutesRead: Long,
    val currentStreakDays: Int,
)

@Serializable
data class PublicReaderDto(
    val id: String,
    val username: String,
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val currentlyReading: BookMetaDto? = null,
    val booksFinished: Int = 0,
    val isFollowedByMe: Boolean = false,
)

@Serializable
data class FeedItemDto(
    val id: String,
    val reader: PublicReaderDto,
    val book: BookMetaDto,
    /** STARTED | READING | FINISHED */
    val type: String,
    val progressPercent: Float? = null,
    val createdAt: Long,
)

@Serializable
data class FeedPageDto(val items: List<FeedItemDto>, val nextCursor: String? = null)

@Serializable
data class ErrorDto(val message: String? = null, val code: String? = null)
