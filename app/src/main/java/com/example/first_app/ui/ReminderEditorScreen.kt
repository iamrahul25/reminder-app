package com.example.first_app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.first_app.R
import com.example.first_app.reminder.Reminder
import com.example.first_app.reminder.Repeat
import com.example.first_app.reminder.nextOccurrence
import java.util.Calendar
import java.util.TimeZone

private val UTC = TimeZone.getTimeZone("UTC")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorScreen(
    initial: Reminder?,
    onSave: (Reminder) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val defaultTitle = stringResource(R.string.default_title)
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var dateTime by rememberSaveable {
        mutableLongStateOf(initial?.let { if (it.nextAt > 0) it.nextAt else it.startAt } ?: defaultStartTime())
    }
    var repeat by rememberSaveable { mutableStateOf(initial?.repeat ?: Repeat.NONE) }
    var intervalText by rememberSaveable {
        mutableStateOf(initial?.intervalDays?.takeIf { initial.repeat == Repeat.EVERY_X_DAYS }?.toString() ?: "2")
    }
    var soundUri by rememberSaveable { mutableStateOf(initial?.soundUri) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val interval = intervalText.toIntOrNull()
    val intervalValid = repeat != Repeat.EVERY_X_DAYS || (interval != null && interval in 1..365)
    val next = if (intervalValid) {
        nextOccurrence(dateTime, repeat, interval ?: 1, System.currentTimeMillis())
    } else {
        null
    }
    val error = when {
        !intervalValid -> stringResource(R.string.error_interval)
        next == null -> stringResource(R.string.error_past_time)
        else -> null
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            soundUri = picked?.takeUnless { it == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) }?.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (initial == null) R.string.new_reminder else R.string.edit_reminder)) },
                navigationIcon = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
                actions = {
                    TextButton(
                        enabled = error == null,
                        onClick = {
                            onSave(
                                Reminder(
                                    id = initial?.id ?: 0,
                                    title = title.trim().ifEmpty { defaultTitle },
                                    startAt = dateTime,
                                    repeat = repeat,
                                    intervalDays = if (repeat == Repeat.EVERY_X_DAYS) interval ?: 1 else 1,
                                    enabled = true,
                                    soundUri = soundUri,
                                ),
                            )
                        },
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title_label)) },
                placeholder = { Text(stringResource(R.string.title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel(stringResource(R.string.date_and_time))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(formatDate(dateTime))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                    Text(formatTime(dateTime))
                }
            }

            SectionLabel(stringResource(R.string.repeat))
            Column(modifier = Modifier.selectableGroup()) {
                listOf(
                    Repeat.NONE to R.string.repeat_once,
                    Repeat.DAILY to R.string.repeat_daily,
                    Repeat.WEEKLY to R.string.repeat_weekly,
                    Repeat.MONTHLY to R.string.repeat_monthly,
                    Repeat.EVERY_X_DAYS to R.string.repeat_every_x_days,
                ).forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = repeat == option,
                                onClick = { repeat = option },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = repeat == option, onClick = null)
                        Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            if (repeat == Repeat.EVERY_X_DAYS) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { text -> intervalText = text.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.interval_label)) },
                    suffix = { Text(stringResource(R.string.days)) },
                    singleLine = true,
                    isError = !intervalValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionLabel(stringResource(R.string.sound))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(soundName(context, soundUri), modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri?.let(Uri::parse) ?: defaultUri)
                    try {
                        soundPicker.launch(intent)
                    } catch (_: ActivityNotFoundException) {
                        soundUri = null
                    }
                }) { Text(stringResource(R.string.change)) }
            }

            Text(
                text = error ?: stringResource(R.string.next_ring, formatDateTime(next!!)),
                style = MaterialTheme.typography.bodyLarge,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showDatePicker) {
        val todayUtc = remember { toUtcDate(System.currentTimeMillis()) }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = toUtcDate(dateTime),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateTime = withDate(dateTime, it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val calendar = remember { Calendar.getInstance().apply { timeInMillis = dateTime } }
        val state = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateTime = withTime(dateTime, state.hour, state.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun soundName(context: Context, soundUri: String?): String {
    val default = stringResource(R.string.default_sound)
    val custom = stringResource(R.string.custom_sound)
    return remember(soundUri) {
        if (soundUri == null) {
            default
        } else {
            runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }
                .getOrNull() ?: custom
        }
    }
}

/** Next 5-minute mark at least 5 minutes from now. */
private fun defaultStartTime(): Long = Calendar.getInstance().apply {
    add(Calendar.MINUTE, 5)
    set(Calendar.MINUTE, (get(Calendar.MINUTE) + 4) / 5 * 5)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** The Material date picker works with UTC midnight; convert a local moment to that form. */
private fun toUtcDate(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(UTC).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withDate(localMillis: Long, utcDate: Long): Long {
    val date = Calendar.getInstance(UTC).apply { timeInMillis = utcDate }
    return Calendar.getInstance().apply {
        timeInMillis = localMillis
        set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withTime(localMillis: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    timeInMillis = localMillis
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
