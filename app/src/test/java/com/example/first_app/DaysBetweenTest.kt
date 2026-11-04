package com.example.first_app

import com.example.first_app.reminder.daysBetween
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DaysBetweenTest {
    private val newYork = TimeZone.getTimeZone("America/New_York")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(newYork).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun sameDayIsZero() {
        assertEquals(0, daysBetween(at(2026, 11, 4, 8), at(2026, 11, 4, 23, 59), newYork))
    }

    @Test
    fun countsCalendarDatesNotFullDays() {
        assertEquals(1, daysBetween(at(2026, 11, 4, 23), at(2026, 11, 5, 1), newYork))
        assertEquals(1, daysBetween(at(2026, 11, 4, 1), at(2026, 11, 5, 23), newYork))
    }

    @Test
    fun spansMonthsAndDstChange() {
        // DST ends in New York on 1 Nov 2026, making that day 25 hours long.
        assertEquals(10, daysBetween(at(2026, 10, 25, 9), at(2026, 11, 4, 9), newYork))
        assertEquals(30, daysBetween(at(2026, 10, 5, 12), at(2026, 11, 4, 12), newYork))
    }

    @Test
    fun neverNegative() {
        assertEquals(0, daysBetween(at(2026, 11, 5, 9), at(2026, 11, 4, 9), newYork))
    }
}
