package com.example.first_app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.first_app.reminder.ReminderScheduler

/** Receives AlarmManager alarms and starts the ringing service. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        val reminder = ReminderScheduler.onAlarmFired(context, id, snooze) ?: return
        try {
            AlarmService.start(context, reminder)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not start the alarm service for reminder $id", e)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_SNOOZE = "snooze"
        private const val TAG = "AlarmReceiver"
    }
}
