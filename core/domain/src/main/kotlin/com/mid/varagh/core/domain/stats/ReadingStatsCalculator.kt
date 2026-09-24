package com.mid.varagh.core.domain.stats

import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.MonthlyCount
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.ReadingStats
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Pure functions that turn books and sessions into [ReadingStats]. */
object ReadingStatsCalculator {

    fun calculate(
        finishedBooks: List<Book>,
        sessions: List<ReadingSession>,
        nowMillis: Long,
        zone: ZoneId,
    ): ReadingStats {
        val today = nowMillis.toLocalDate(zone)
        val finishedDates = finishedBooks.mapNotNull { it.finishedAt }.map { it.toLocalDate(zone) }
        return ReadingStats(
            totalBooksFinished = finishedBooks.size,
            booksFinishedThisYear = finishedDates.count { it.year == today.year },
            totalPagesRead = sessions.sumOf { it.pagesRead.coerceAtLeast(0) },
            totalMinutesRead = sessions.sumOf { it.durationMillis } / 60_000,
            currentStreakDays = currentStreak(sessions.map { it.startedAt.toLocalDate(zone) }.toSet(), today),
            booksPerMonth = booksPerMonth(finishedDates, YearMonth.from(today)),
        )
    }

    /**
     * Consecutive days with reading, ending today. If nothing was read yet today the streak still
     * counts up to yesterday (it only breaks once a whole day is missed).
     */
    fun currentStreak(readingDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in readingDays) today else today.minusDays(1)
        var streak = 0
        while (day in readingDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /** Last [months] months ending at [current], oldest first, zero-filled. */
    fun booksPerMonth(finishedDates: List<LocalDate>, current: YearMonth, months: Int = 12): List<MonthlyCount> {
        val counts = finishedDates.groupingBy { YearMonth.from(it) }.eachCount()
        return (months - 1 downTo 0).map { back ->
            val ym = current.minusMonths(back.toLong())
            MonthlyCount(year = ym.year, month = ym.monthValue, count = counts[ym] ?: 0)
        }
    }

    // Java 8 API on purpose (LocalDate.ofInstant is Java 9+ and not desugared on old Android).
    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
