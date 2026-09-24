package com.mid.varagh.core.data

import com.mid.varagh.core.domain.TimeProvider
import java.time.ZoneId
import javax.inject.Inject

internal class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
