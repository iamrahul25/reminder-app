package com.example.first_app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.first_app.reminder.ReminderScheduler

/** Handles the Stop / Snooze buttons on the ringing notification. */
class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        when (intent.action) {
            ACTION_STOP -> AlarmService.stop(context)
            ACTION_SNOOZE -> {
                ReminderScheduler.snooze(context, id, intent.getIntExtra(EXTRA_MINUTES, 5))
                AlarmService.stop(context)
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.example.first_app.action.STOP"
        const val ACTION_SNOOZE = "com.example.first_app.action.SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MINUTES = "minutes"
    }
}
