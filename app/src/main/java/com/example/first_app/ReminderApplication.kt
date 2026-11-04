package com.example.first_app

import android.app.Application
import com.example.first_app.alarm.AlarmService
import com.example.first_app.reminder.ReminderRepository
import com.example.first_app.ui.theme.ThemeSettings

class ReminderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderRepository.init(this)
        ThemeSettings.init(this)
        AlarmService.createNotificationChannel(this)
    }
}
