package com.mid.varagh.core.domain.usecase

import com.mid.varagh.core.domain.TimeProvider
import com.mid.varagh.core.domain.repository.BookRepository
import com.mid.varagh.core.domain.repository.ReadingSessionRepository
import com.mid.varagh.core.domain.repository.UserProfileRepository
import com.mid.varagh.core.domain.stats.ReadingStatsCalculator
import com.mid.varagh.core.model.BookWithProgress
import com.mid.varagh.core.model.ReadingStats
import com.mid.varagh.core.model.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Stores a finished reading session, ignoring very short ones (accidental opens). */
class RecordReadingSessionUseCase @Inject constructor(
    private val sessions: ReadingSessionRepository,
) {
    /** Returns the new session id, or null if the session was too short to count. */
    suspend operator fun invoke(bookId: Long, startedAt: Long, endedAt: Long, pagesRead: Int): Long? {
        if (endedAt - startedAt < MIN_SESSION_MILLIS) return null
        return sessions.addSession(bookId, startedAt, endedAt, pagesRead.coerceAtLeast(0))
    }

    companion object {
        const val MIN_SESSION_MILLIS = 30_000L
    }
}

/** Live reading statistics (finished books, pages, minutes, streak, books per month). */
class ObserveReadingStatsUseCase @Inject constructor(
    private val books: BookRepository,
    private val sessions: ReadingSessionRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<ReadingStats> =
        combine(
            books.observeBooksByStatus(ReadingStatus.FINISHED),
            sessions.observeAllSessions(),
        ) { finished, allSessions ->
            ReadingStatsCalculator.calculate(
                finishedBooks = finished.map { it.book },
                sessions = allSessions,
                nowMillis = time.nowMillis(),
                zone = time.zone(),
            )
        }
}

/**
 * The book for the profile's "currently reading" card: the explicitly chosen one if it is still
 * being read, otherwise the most recently opened READING book, otherwise null.
 */
class ObserveCurrentlyReadingUseCase @Inject constructor(
    private val books: BookRepository,
    private val profile: UserProfileRepository,
) {
    operator fun invoke(): Flow<BookWithProgress?> =
        combine(
            profile.observeProfile().map { it.currentlyReadingBookId }.distinctUntilChanged(),
            books.observeBooksByStatus(ReadingStatus.READING),
        ) { pickedId, reading ->
            reading.firstOrNull { it.book.id == pickedId } ?: reading.firstOrNull()
        }.distinctUntilChanged()
}
