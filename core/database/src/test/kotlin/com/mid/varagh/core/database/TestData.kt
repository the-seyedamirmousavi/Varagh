package com.mid.varagh.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mid.varagh.core.database.model.BookEntity
import com.mid.varagh.core.database.model.SyncState
import com.mid.varagh.core.model.ReadingStatus

fun inMemoryDatabase(): VaraghDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), VaraghDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun bookEntity(
    title: String,
    hash: String = title,
    author: String? = null,
    addedAt: Long = 0,
    lastOpenedAt: Long? = null,
    status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    syncState: SyncState = SyncState.SYNCED,
    pageCount: Int = 100,
) = BookEntity(
    title = title,
    author = author,
    fileUri = "content://books/$hash",
    fileHash = hash,
    pageCount = pageCount,
    coverPath = null,
    addedAt = addedAt,
    lastOpenedAt = lastOpenedAt,
    finishedAt = null,
    status = status,
    rating = null,
    remoteId = null,
    updatedAt = addedAt,
    syncState = syncState,
)
