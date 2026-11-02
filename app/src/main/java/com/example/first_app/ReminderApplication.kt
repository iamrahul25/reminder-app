package com.example.first_app

import android.app.Application
import com.example.first_app.alarm.AlarmService
import com.example.first_app.reminder.ReminderRepository

class ReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderRepository.init(this)
        AlarmService.createNotificationChannel(this)
    }
}
