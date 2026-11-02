package com.example.first_app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.first_app.MainActivity
import com.example.first_app.alarm.AlarmReceiver
import com.example.first_app.alarm.AlarmService

/** High-level reminder operations that keep storage and AlarmManager in sync. */
object ReminderScheduler {
    private const val MISSED_RING_DELAY_MS = 3_000L

    fun save(context: Context, reminder: Reminder): Reminder {
        val saved = ReminderRepository.upsert(reminder.copy(snoozeUntil = 0))
        cancelAlarm(context, saved.id, snooze = true)
        return scheduleNext(context, saved, after = System.currentTimeMillis())
    }

    fun setEnabled(context: Context, id: Int, enabled: Boolean) {
        val reminder = ReminderRepository.get(id) ?: return
        if (enabled) {
            save(context, reminder.copy(enabled = true))
        } else {
            cancelAlarm(context, id, snooze = false)
            cancelAlarm(context, id, snooze = true)
            ReminderRepository.upsert(reminder.copy(enabled = false, nextAt = 0, snoozeUntil = 0))
        }
    }

    fun delete(context: Context, id: Int) {
        cancelAlarm(context, id, snooze = false)
        cancelAlarm(context, id, snooze = true)
        ReminderRepository.delete(id)
        if (AlarmService.ringing.value?.reminderId == id) AlarmService.stop(context)
    }

    fun snooze(context: Context, id: Int, minutes: Int) {
        val reminder = ReminderRepository.get(id) ?: return
        val at = System.currentTimeMillis() + minutes * 60_000L
        ReminderRepository.upsert(reminder.copy(snoozeUntil = at))
        setAlarm(context, id, at, snooze = true)
    }

    /**
     * Called when an alarm goes off. Updates the reminder's state (and schedules the next
     * occurrence for recurring reminders). Returns the reminder that should ring, or null.
     */
    fun onAlarmFired(context: Context, id: Int, snooze: Boolean): Reminder? {
        val reminder = ReminderRepository.get(id) ?: return null
        if (snooze) {
            return ReminderRepository.upsert(reminder.copy(snoozeUntil = 0))
        }
        if (!reminder.enabled) return null
        val now = System.currentTimeMillis()
        return if (reminder.repeat == Repeat.NONE) {
            ReminderRepository.upsert(reminder.copy(enabled = false, nextAt = 0))
        } else {
            scheduleNext(context, reminder, after = maxOf(now, reminder.nextAt))
        }
    }

    /**
     * Re-registers every alarm, e.g. after a reboot, a time change or a force-stop (all of which
     * clear AlarmManager). One-time reminders and snoozes missed meanwhile ring right away.
     */
    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        for (reminder in ReminderRepository.reminders.value) {
            if (reminder.snoozeUntil > 0) {
                setAlarm(context, reminder.id, maxOf(reminder.snoozeUntil, now + MISSED_RING_DELAY_MS), snooze = true)
            }
            if (!reminder.enabled) continue
            if (reminder.repeat == Repeat.NONE && reminder.nextAt in 1..now) {
                setAlarm(context, reminder.id, now + MISSED_RING_DELAY_MS, snooze = false)
            } else {
                scheduleNext(context, reminder, after = now)
            }
        }
    }

    private fun scheduleNext(context: Context, reminder: Reminder, after: Long): Reminder {
        val next = if (reminder.enabled) {
            nextOccurrence(reminder.startAt, reminder.repeat, reminder.intervalDays, after)
        } else {
            null
        }
        if (next == null) {
            cancelAlarm(context, reminder.id, snooze = false)
            return ReminderRepository.upsert(reminder.copy(enabled = false, nextAt = 0))
        }
        setAlarm(context, reminder.id, next, snooze = false)
        return ReminderRepository.upsert(reminder.copy(nextAt = next))
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setAlarm(context: Context, id: Int, triggerAt: Long, snooze: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = alarmIntent(context, id, snooze)
        if (canScheduleExactAlarms(context)) {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun cancelAlarm(context: Context, id: Int, snooze: Boolean) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, id, snooze))
    }

    private fun alarmIntent(context: Context, id: Int, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setData(Uri.parse("reminder://alarm/$id/${if (snooze) "snooze" else "regular"}"))
            .putExtra(AlarmReceiver.EXTRA_REMINDER_ID, id)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze)
        return PendingIntent.getBroadcast(
            context,
            id * 2 + if (snooze) 1 else 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
