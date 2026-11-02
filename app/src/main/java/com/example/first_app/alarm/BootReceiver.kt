package com.example.first_app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.first_app.reminder.ReminderScheduler

/** AlarmManager forgets alarms on reboot and app updates, and times shift on clock changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            -> ReminderScheduler.rescheduleAll(context)
        }
    }
}
