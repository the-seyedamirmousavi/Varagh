package com.mid.varagh.core.model

/** One continuous stretch of reading. Timestamps are epoch milliseconds. */
data class ReadingSession(
    val id: Long,
    val bookId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val pagesRead: Int,
) {
    val durationMillis: Long get() = (endedAt - startedAt).coerceAtLeast(0)
}

/** A saved page, optionally with a note. [page] is zero-based. */
data class Bookmark(
    val id: Long,
    val bookId: Long,
    val page: Int,
    val note: String?,
    val createdAt: Long,
)

/** Books finished in one calendar month. */
data class MonthlyCount(
    val year: Int,
    /** 1..12 */
    val month: Int,
    val count: Int,
)

/** Reading statistics derived from books and sessions. */
data class ReadingStats(
    val totalBooksFinished: Int,
    val booksFinishedThisYear: Int,
    val totalPagesRead: Int,
    val totalMinutesRead: Long,
    val currentStreakDays: Int,
    /** The last 12 months, oldest first, including months with zero books. */
    val booksPerMonth: List<MonthlyCount>,
) {
    companion object {
        val Empty = ReadingStats(0, 0, 0, 0, 0, emptyList())
    }
}
