package com.mid.varagh.core.database.model

import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.Bookmark
import com.mid.varagh.core.model.ReadingProgress
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.UserProfile

fun BookEntity.asExternalModel() = Book(
    id = id,
    title = title,
    author = author,
    fileUri = fileUri,
    fileHash = fileHash,
    pageCount = pageCount,
    coverPath = coverPath,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    finishedAt = finishedAt,
    status = status,
    rating = rating,
    remoteId = remoteId,
)

fun ReadingProgressEntity.asExternalModel() = ReadingProgress(
    bookId = bookId,
    currentPage = currentPage,
    percent = percent,
    updatedAt = updatedAt,
)

fun BookWithProgressRow.asExternalModel() = BookWithProgress(
    book = book.asExternalModel(),
    // A tombstoned progress row is treated as "no progress".
    progress = progress?.takeIf { it.syncState != SyncState.DELETED }?.asExternalModel(),
)

fun ReadingSessionEntity.asExternalModel() = ReadingSession(
    id = id,
    bookId = bookId,
    startedAt = startedAt,
    endedAt = endedAt,
    pagesRead = pagesRead,
)

fun BookmarkEntity.asExternalModel() = Bookmark(
    id = id,
    bookId = bookId,
    page = page,
    note = note,
    createdAt = createdAt,
)

fun UserProfileEntity.asExternalModel() = UserProfile(
    id = id,
    displayName = displayName,
    username = username,
    bio = bio,
    avatarPath = avatarPath,
    isPublic = isPublic,
    currentlyReadingBookId = currentlyReadingBookId,
    remoteId = remoteId,
)

fun UserProfile.asEntity(updatedAt: Long, syncState: SyncState = SyncState.SYNCED) = UserProfileEntity(
    id = id,
    displayName = displayName,
    username = username,
    bio = bio,
    avatarPath = avatarPath,
    isPublic = isPublic,
    currentlyReadingBookId = currentlyReadingBookId,
    remoteId = remoteId,
    updatedAt = updatedAt,
    syncState = syncState,
)
