package com.mid.varagh.core.domain.stats

import com.mid.varagh.core.model.Book
import com.mid.varagh.core.model.ReadingSession
import com.mid.varagh.core.model.ReadingStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class ReadingStatsCalculatorTest {

    private val zone = ZoneId.of("Asia/Tehran")
    private val today = LocalDate.of(2026, 9, 24)

    private fun millis(date: LocalDate, hour: Int = 12) =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun finishedBook(id: Long, finished: LocalDate) = Book(
        id = id, title = "b$id", author = null, fileUri = "u$id", fileHash = "h$id", pageCount = 100,
        coverPath = null, addedAt = 0, lastOpenedAt = null, finishedAt = millis(finished),
        status = ReadingStatus.FINISHED, rating = null, remoteId = null,
    )

    private fun session(day: LocalDate, minutes: Long, pages: Int) =
        ReadingSession(0, 1, millis(day), millis(day) + minutes * 60_000, pages)

    @Test
    fun `streak counts consecutive days ending today`() {
        val days = setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4))
        assertEquals(3, ReadingStatsCalculator.currentStreak(days, today))
    }

    @Test
    fun `streak survives until the end of today if yesterday was read`() {
        val days = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, ReadingStatsCalculator.currentStreak(days, today))
    }

    @Test
    fun `streak is zero after a missed day`() {
        assertEquals(0, ReadingStatsCalculator.currentStreak(setOf(today.minusDays(2)), today))
        assertEquals(0, ReadingStatsCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `books per month covers last twelve months oldest first with zeros`() {
        val result = ReadingStatsCalculator.booksPerMonth(
            finishedDates = listOf(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20), LocalDate.of(2025, 10, 5), LocalDate.of(2024, 1, 1)),
            current = YearMonth.of(2026, 9),
        )
        assertEquals(12, result.size)
        assertEquals(2025 to 10, result.first().year to result.first().month)
        assertEquals(1, result.first().count)
        assertEquals(2026 to 9, result.last().year to result.last().month)
        assertEquals(2, result.last().count)
        assertEquals(3, result.sumOf { it.count })
    }

    @Test
    fun `calculate aggregates totals and this year`() {
        val stats = ReadingStatsCalculator.calculate(
            finishedBooks = listOf(finishedBook(1, today.minusDays(3)), finishedBook(2, LocalDate.of(2025, 12, 30))),
            sessions = listOf(session(today, 30, 12), session(today.minusDays(1), 45, 20), session(today.minusDays(1), 1, -5)),
            nowMillis = millis(today, hour = 20),
            zone = zone,
        )
        assertEquals(2, stats.totalBooksFinished)
        assertEquals(1, stats.booksFinishedThisYear)
        assertEquals(32, stats.totalPagesRead)
        assertEquals(76, stats.totalMinutesRead)
        assertEquals(2, stats.currentStreakDays)
    }

    @Test
    fun `days are computed in the user's time zone`() {
        // 00:30 in Tehran yesterday is 21:00 UTC the day before. Counted in UTC the streak would be
        // broken (0); counted locally, yesterday was a reading day (1).
        val justAfterMidnight = LocalDateTime.of(today.minusDays(1), java.time.LocalTime.of(0, 30)).atZone(zone).toInstant().toEpochMilli()
        val stats = ReadingStatsCalculator.calculate(
            finishedBooks = emptyList(),
            sessions = listOf(ReadingSession(0, 1, justAfterMidnight, justAfterMidnight + 60_000, 1)),
            nowMillis = millis(today, hour = 9),
            zone = zone,
        )
        assertEquals(1, stats.currentStreakDays)
    }
}
