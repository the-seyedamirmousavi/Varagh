package com.mid.varagh.core.domain

import java.time.ZoneId

/** Injectable clock so time-dependent logic (streaks, "this year", sessions) is testable. */
interface TimeProvider {
    fun nowMillis(): Long
    fun zone(): ZoneId
}
