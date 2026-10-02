package com.mid.varagh.core.designsystem.component

import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Date
import java.util.Locale

/**
 * Locale-aware date formatting. Persian uses the Solar Hijri calendar (e.g. "۲۹ مهر ۱۴۰۴"),
 * other languages the Gregorian one. Backed by android.icu (API 24+).
 */
class VaraghDateFormatter(locale: Locale) {
    private val uLocale: ULocale =
        if (locale.language == "fa") ULocale("${locale.language}_${locale.country.ifBlank { "IR" }}@calendar=persian") else ULocale.forLocale(locale)

    private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, uLocale)
    private val monthFormat = SimpleDateFormat("MMM", uLocale)
    private val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, uLocale)

    fun date(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

    fun dateTime(epochMillis: Long): String = date(epochMillis) + " · " + timeFormat.format(Date(epochMillis))

    /**
     * Short month label for a Gregorian [year]/[month]. For Persian this is the Solar Hijri month
     * containing the 15th of that Gregorian month (the two calendars' months overlap by ~half).
     */
    fun monthLabel(year: Int, month: Int): String {
        val midMonth = java.util.GregorianCalendar(year, month - 1, MID_MONTH_DAY).time
        return monthFormat.format(midMonth)
    }

    private companion object {
        const val MID_MONTH_DAY = 15
    }
}

@Composable
fun rememberDateFormatter(): VaraghDateFormatter {
    val locale = currentLocale()
    return remember(locale) { VaraghDateFormatter(locale) }
}
