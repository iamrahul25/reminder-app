package com.example.first_app.reminder

import java.util.Calendar
import java.util.TimeZone

enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY, EVERY_X_DAYS }

data class Reminder(
    val id: Int = 0,
    val title: String,
    /** First chosen date/time. Recurring reminders repeat relative to this moment. */
    val startAt: Long,
    val repeat: Repeat = Repeat.NONE,
    /** Used only when [repeat] is [Repeat.EVERY_X_DAYS]. */
    val intervalDays: Int = 1,
    val enabled: Boolean = true,
    /** Next regular ring time, or 0 when nothing is scheduled. */
    val nextAt: Long = 0,
    /** Pending snooze ring time, or 0 when not snoozed. */
    val snoozeUntil: Long = 0,
    /** Ringtone URI string, or null for the device's default alarm sound. */
    val soundUri: String? = null,
    /** When the reminder was first saved; 0 until the repository assigns it. */
    val createdAt: Long = 0,
    /** When the reminder was marked as completed, or 0 while it is still active. */
    val completedAt: Long = 0,
) {
    val isCompleted: Boolean get() = completedAt > 0
}

private const val DAY_MS = 24L * 60 * 60 * 1000

/**
 * Number of calendar days between the local dates of [from] and [to] (0 when both fall on the
 * same day). Counting dates rather than 24-hour spans keeps the result stable across DST changes.
 */
fun daysBetween(from: Long, to: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
    fun startOfDay(millis: Long) = Calendar.getInstance(timeZone).apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val days = Math.round((startOfDay(to) - startOfDay(from)).toDouble() / DAY_MS).toInt()
    return days.coerceAtLeast(0)
}

/**
 * Returns the first occurrence of a reminder strictly after [after], or null when a one-time
 * reminder is already in the past. Steps are calendar-based, so the wall-clock time is kept
 * across DST changes and monthly reminders on the 29th-31st clamp to the month's last day.
 */
fun nextOccurrence(
    startAt: Long,
    repeat: Repeat,
    intervalDays: Int,
    after: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): Long? {
    if (startAt > after) return startAt
    if (repeat == Repeat.NONE) return null

    val calendar = Calendar.getInstance(timeZone)
    fun occurrence(k: Int): Long {
        calendar.timeInMillis = startAt
        when (repeat) {
            Repeat.DAILY -> calendar.add(Calendar.DATE, k)
            Repeat.WEEKLY -> calendar.add(Calendar.DATE, 7 * k)
            Repeat.MONTHLY -> calendar.add(Calendar.MONTH, k)
            Repeat.EVERY_X_DAYS -> calendar.add(Calendar.DATE, intervalDays.coerceAtLeast(1) * k)
            Repeat.NONE -> Unit
        }
        return calendar.timeInMillis
    }

    val longestStepMs = when (repeat) {
        Repeat.DAILY -> DAY_MS
        Repeat.WEEKLY -> 7 * DAY_MS
        Repeat.MONTHLY -> 31 * DAY_MS
        Repeat.EVERY_X_DAYS -> intervalDays.coerceAtLeast(1) * DAY_MS
        Repeat.NONE -> DAY_MS
    }
    var k = ((after - startAt) / longestStepMs).toInt().coerceAtLeast(1)
    while (k > 1 && occurrence(k - 1) > after) k--
    while (occurrence(k) <= after) k++
    return occurrence(k)
}
