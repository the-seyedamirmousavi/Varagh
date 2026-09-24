package com.mid.varagh.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookModelTest {

    @Test
    fun `percent is zero on first page and one on last page`() {
        assertEquals(0f, ReadingProgress.percentOf(page = 0, pageCount = 500), 0f)
        assertEquals(1f, ReadingProgress.percentOf(page = 499, pageCount = 500), 0f)
        assertEquals(0.5f, ReadingProgress.percentOf(page = 50, pageCount = 101), 0.0001f)
    }

    @Test
    fun `percent clamps out of range pages and handles tiny books`() {
        assertEquals(1f, ReadingProgress.percentOf(page = 900, pageCount = 10), 0f)
        assertEquals(0f, ReadingProgress.percentOf(page = -3, pageCount = 10), 0f)
        assertEquals(1f, ReadingProgress.percentOf(page = 0, pageCount = 1), 0f)
        assertEquals(1f, ReadingProgress.percentOf(page = 0, pageCount = 0), 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rating above five is rejected`() {
        Book(1, "t", null, "u", "h", 1, null, 0, null, null, ReadingStatus.READING, rating = 6, remoteId = null)
    }

    @Test
    fun `session duration never negative`() {
        assertEquals(0L, ReadingSession(1, 1, startedAt = 10, endedAt = 5, pagesRead = 0).durationMillis)
    }
}
