package com.example.first_app

import com.example.first_app.reminder.Repeat
import com.example.first_app.reminder.nextOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NextOccurrenceTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val newYork = TimeZone.getTimeZone("America/New_York")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: TimeZone = utc): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun next(start: Long, repeat: Repeat, after: Long, interval: Int = 1, zone: TimeZone = utc) =
        nextOccurrence(start, repeat, interval, after, zone)

    @Test
    fun futureStartIsReturnedForAnyRepeat() {
        val start = at(2026, 11, 10, 9, 0)
        val now = at(2026, 11, 3, 12, 0)
        Repeat.entries.forEach { assertEquals(start, next(start, it, now)) }
    }

    @Test
    fun pastOneTimeReminderHasNoNextOccurrence() {
        assertNull(next(at(2026, 11, 3, 9, 0), Repeat.NONE, at(2026, 11, 3, 9, 0)))
    }

    @Test
    fun dailyMovesToTomorrowOnceTodayHasPassed() {
        val start = at(2026, 11, 3, 9, 0)
        assertEquals(at(2026, 11, 4, 9, 0), next(start, Repeat.DAILY, at(2026, 11, 3, 9, 0)))
        assertEquals(at(2026, 11, 4, 9, 0), next(start, Repeat.DAILY, at(2026, 11, 3, 15, 0)))
        assertEquals(at(2027, 3, 1, 9, 0), next(start, Repeat.DAILY, at(2027, 2, 28, 9, 30)))
    }

    @Test
    fun weeklyKeepsTheWeekday() {
        val start = at(2026, 11, 3, 7, 30)
        assertEquals(at(2026, 11, 10, 7, 30), next(start, Repeat.WEEKLY, at(2026, 11, 5, 0, 0)))
        assertEquals(at(2026, 12, 1, 7, 30), next(start, Repeat.WEEKLY, at(2026, 11, 24, 7, 30)))
    }

    @Test
    fun monthlyOnThe31stClampsToShortMonthsAndReturnsTo31st() {
        val start = at(2027, 1, 31, 8, 0)
        assertEquals(at(2027, 2, 28, 8, 0), next(start, Repeat.MONTHLY, at(2027, 2, 1, 0, 0)))
        assertEquals(at(2027, 3, 31, 8, 0), next(start, Repeat.MONTHLY, at(2027, 2, 28, 8, 0)))
        assertEquals(at(2027, 4, 30, 8, 0), next(start, Repeat.MONTHLY, at(2027, 4, 1, 0, 0)))
    }

    @Test
    fun everyXDaysStepsFromTheStartDate() {
        val start = at(2026, 11, 1, 20, 0)
        assertEquals(at(2026, 11, 4, 20, 0), next(start, Repeat.EVERY_X_DAYS, at(2026, 11, 1, 21, 0), interval = 3))
        assertEquals(at(2026, 11, 7, 20, 0), next(start, Repeat.EVERY_X_DAYS, at(2026, 11, 4, 20, 0), interval = 3))
        assertEquals(at(2027, 11, 1, 20, 0), next(start, Repeat.EVERY_X_DAYS, at(2027, 10, 31, 0, 0), interval = 365))
    }

    @Test
    fun dailyKeepsWallClockTimeAcrossDaylightSaving() {
        val start = at(2026, 3, 7, 9, 0, newYork)
        val afterDstStarts = at(2026, 3, 8, 12, 0, newYork)
        assertEquals(at(2026, 3, 9, 9, 0, newYork), next(start, Repeat.DAILY, afterDstStarts, zone = newYork))
    }

    @Test
    fun farPastStartStillFindsTheVeryNextOccurrence() {
        val start = at(2020, 1, 1, 6, 0)
        val now = at(2026, 11, 3, 5, 59)
        assertEquals(at(2026, 11, 3, 6, 0), next(start, Repeat.DAILY, now))
        assertEquals(at(2026, 12, 1, 6, 0), next(start, Repeat.MONTHLY, now))
    }
}
