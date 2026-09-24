package com.mid.varagh.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs several DAO calls atomically without exposing [VaraghDatabase] to repositories. */
interface TransactionRunner {
    suspend operator fun <T> invoke(block: suspend () -> T): T
}

internal class RoomTransactionRunner @Inject constructor(
    private val database: VaraghDatabase,
) : TransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = database.withTransaction { block() }
}
