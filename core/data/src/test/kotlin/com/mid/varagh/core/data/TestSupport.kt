package com.mid.varagh.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.mid.varagh.core.database.TransactionRunner
import com.mid.varagh.core.database.VaraghDatabase
import com.mid.varagh.core.domain.TimeProvider
import java.time.ZoneId

class FakeTimeProvider(var now: Long = 1_000_000L, private val zone: ZoneId = ZoneId.of("UTC")) : TimeProvider {
    override fun nowMillis(): Long = now
    override fun zone(): ZoneId = zone
}

fun inMemoryDatabase(): VaraghDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), VaraghDatabase::class.java)
        .allowMainThreadQueries()
        .build()

class TestTransactionRunner(private val db: VaraghDatabase) : TransactionRunner {
    override suspend fun <T> invoke(block: suspend () -> T): T = db.withTransaction { block() }
}
