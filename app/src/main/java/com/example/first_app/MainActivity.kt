package com.example.first_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.first_app.reminder.ReminderRepository
import com.example.first_app.reminder.ReminderScheduler
import com.example.first_app.reminder.Repeat
import com.example.first_app.ui.ReminderEditorScreen
import com.example.first_app.ui.ReminderListScreen
import com.example.first_app.ui.theme.FirstAppTheme

private const val NOT_EDITING = -1
private const val NEW_REMINDER = 0

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // Restores alarms that a force-stop may have cleared.
            ReminderScheduler.rescheduleAll(this)
        }
        enableEdgeToEdge()
        setContent {
            FirstAppTheme {
                ReminderApp(resumeTick)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun ReminderApp(resumeTick: Int) {
    val context = LocalContext.current
    val reminders by ReminderRepository.reminders.collectAsState()
    var editingId by rememberSaveable { mutableIntStateOf(NOT_EDITING) }

    if (editingId == NOT_EDITING) {
        ReminderListScreen(
            reminders = reminders,
            resumeTick = resumeTick,
            onAdd = { editingId = NEW_REMINDER },
            onEdit = { editingId = it.id },
            onToggle = { reminder, enabled ->
                val expiredOneTime = reminder.repeat == Repeat.NONE && reminder.startAt <= System.currentTimeMillis()
                if (enabled && expiredOneTime) {
                    editingId = reminder.id
                } else {
                    ReminderScheduler.setEnabled(context, reminder.id, enabled)
                }
            },
            onComplete = { ReminderScheduler.markCompleted(context, it.id) },
            onRestore = { reminder, enabled -> ReminderScheduler.restore(context, reminder.id, enabled) },
            onDelete = { ReminderScheduler.delete(context, it.id) },
        )
    } else {
        BackHandler { editingId = NOT_EDITING }
        key(editingId) {
            ReminderEditorScreen(
                initial = reminders.firstOrNull { it.id == editingId },
                onSave = {
                    ReminderScheduler.save(context, it)
                    editingId = NOT_EDITING
                },
                onCancel = { editingId = NOT_EDITING },
            )
        }
    }
}
