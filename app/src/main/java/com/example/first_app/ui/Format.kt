package com.example.first_app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.first_app.R
import com.example.first_app.reminder.Reminder
import com.example.first_app.reminder.Repeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat

/** Time of day in the user's 12/24-hour preference. */
@Composable
fun formatTime(millis: Long): String =
    AndroidDateFormat.getTimeFormat(LocalContext.current).format(Date(millis))

fun formatDate(millis: Long): String {
    val pattern = AndroidDateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMMyyyy")
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

@Composable
fun formatDateTime(millis: Long): String = "${formatDate(millis)}, ${formatTime(millis)}"

@Composable
fun repeatLabel(reminder: Reminder): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = reminder.startAt }
    return when (reminder.repeat) {
        Repeat.NONE -> stringResource(R.string.repeat_once)
        Repeat.DAILY -> stringResource(R.string.repeat_daily_label)
        Repeat.WEEKLY -> stringResource(
            R.string.repeat_weekly_label,
            calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "",
        )
        Repeat.MONTHLY -> stringResource(R.string.repeat_monthly_label, calendar.get(Calendar.DAY_OF_MONTH))
        Repeat.EVERY_X_DAYS -> pluralStringResource(
            R.plurals.repeat_every_x_days_label,
            reminder.intervalDays,
            reminder.intervalDays,
        )
    }
}
