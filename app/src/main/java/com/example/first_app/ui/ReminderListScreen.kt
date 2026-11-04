package com.example.first_app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.first_app.R
import com.example.first_app.alarm.AlarmActivity
import com.example.first_app.alarm.AlarmService
import com.example.first_app.reminder.Reminder
import com.example.first_app.reminder.ReminderScheduler
import com.example.first_app.reminder.daysBetween
import com.example.first_app.ui.theme.ThemeMode
import com.example.first_app.ui.theme.ThemeSettings
import com.example.first_app.ui.theme.appColors
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAB_ACTIVE = 0
private const val TAB_DONE = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    reminders: List<Reminder>,
    resumeTick: Int,
    onAdd: () -> Unit,
    onEdit: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onComplete: (Reminder) -> Unit,
    onRestore: (Reminder, enabled: Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    val resources = LocalResources.current
    var pendingDelete by remember { mutableStateOf<Reminder?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_ACTIVE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val activeListState = rememberLazyListState()
    val doneListState = rememberLazyListState()
    val listState = if (selectedTab == TAB_ACTIVE) activeListState else doneListState
    val fabExpanded by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val active = remember(reminders) {
        reminders.filterNot { it.isCompleted }.sortedWith(
            compareBy<Reminder> { !(it.enabled && it.nextAt > 0) }
                .thenBy { if (it.nextAt > 0) it.nextAt else it.startAt },
        )
    }
    val done = remember(reminders) { reminders.filter { it.isCompleted }.sortedByDescending { it.completedAt } }
    val now = remember(resumeTick, reminders) { System.currentTimeMillis() }
    val ringing by AlarmService.ringing.collectAsState()
    val permissionIssues = rememberPermissionIssues(resumeTick)
    val context = LocalContext.current

    val complete: (Reminder) -> Unit = { reminder ->
        onComplete(reminder)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.marked_completed, reminder.title),
                actionLabel = resources.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(reminder, reminder.enabled)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = { ThemeMenu() },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    listOf(
                        Triple(TAB_ACTIVE, R.string.tab_active, active.size),
                        Triple(TAB_DONE, R.string.tab_done, done.size),
                    ).forEach { (tab, label, count) ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(stringResource(R.string.tab_with_count, stringResource(label), count)) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                expanded = fabExpanded,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.new_reminder)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ringing?.let { current ->
                item(key = "ringing") {
                    Banner(
                        icon = R.drawable.ic_alarm,
                        text = stringResource(R.string.ringing_banner, current.title),
                        action = stringResource(R.string.open),
                        onAction = {
                            context.startActivity(
                                Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                }
            }
            items(permissionIssues, key = { it.settingsAction }) { issue ->
                Banner(
                    icon = R.drawable.ic_info,
                    text = stringResource(issue.message),
                    action = stringResource(R.string.allow),
                    onAction = { openAppSettings(context, issue.settingsAction) },
                )
            }

            if (selectedTab == TAB_ACTIVE) {
                if (active.isEmpty()) {
                    item(key = "empty_active") {
                        EmptyState(
                            icon = R.drawable.ic_alarm,
                            title = stringResource(R.string.empty_active_title),
                            body = stringResource(R.string.empty_active_body),
                        )
                    }
                }
                items(active, key = { it.id }) { reminder ->
                    ActiveReminderCard(
                        reminder = reminder,
                        now = now,
                        onClick = { onEdit(reminder) },
                        onToggle = { onToggle(reminder, it) },
                        onComplete = { complete(reminder) },
                        onDelete = { pendingDelete = reminder },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                if (done.isEmpty()) {
                    item(key = "empty_done") {
                        EmptyState(
                            icon = R.drawable.ic_task_alt,
                            title = stringResource(R.string.empty_done_title),
                            body = stringResource(R.string.empty_done_body),
                        )
                    }
                } else {
                    item(key = "speed") { SpeedSummaryCard(done) }
                }
                items(done, key = { it.id }) { reminder ->
                    CompletedReminderCard(
                        reminder = reminder,
                        onRestore = { onRestore(reminder, true) },
                        onDelete = { pendingDelete = reminder },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    pendingDelete?.let { reminder ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, reminder.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(reminder)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ThemeMenu() {
    val mode by ThemeSettings.mode.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(mode.icon), contentDescription = stringResource(R.string.theme))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ThemeMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label)) },
                    leadingIcon = { Icon(painterResource(option.icon), contentDescription = null) },
                    trailingIcon = if (option == mode) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        ThemeSettings.setMode(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val ThemeMode.icon: Int
    @DrawableRes get() = when (this) {
        ThemeMode.SYSTEM -> R.drawable.ic_brightness_auto
        ThemeMode.LIGHT -> R.drawable.ic_light_mode
        ThemeMode.DARK -> R.drawable.ic_dark_mode
    }

private val ThemeMode.label: Int
    @StringRes get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveReminderCard(
    reminder: Reminder,
    now: Long,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheduled = reminder.enabled && reminder.nextAt > 0
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = R.drawable.ic_alarm,
                    containerColor = if (scheduled) colors.primaryContainer else colors.surfaceContainerHighest,
                    contentColor = if (scheduled) colors.onPrimaryContainer else colors.onSurfaceVariant,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (scheduled) formatDateTime(reminder.nextAt) else stringResource(R.string.off),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (scheduled) colors.primary else colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = reminder.enabled, onCheckedChange = onToggle)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoChip(icon = R.drawable.ic_repeat, text = repeatLabel(reminder))
                if (reminder.snoozeUntil > 0) {
                    InfoChip(
                        icon = R.drawable.ic_schedule,
                        text = stringResource(R.string.snoozed_until, formatTime(reminder.snoozeUntil)),
                        containerColor = colors.tertiaryContainer,
                        contentColor = colors.onTertiaryContainer,
                    )
                }
                InfoChip(
                    icon = R.drawable.ic_calendar,
                    text = stringResource(R.string.created_on, formatShortDate(reminder.createdAt)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = openForLabel(reminder, now),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                TextButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.success),
                ) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.mark_completed))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompletedReminderCard(
    reminder: Reminder,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = R.drawable.ic_check,
                    containerColor = appColors.successContainer,
                    contentColor = appColors.onSuccessContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.completed_on, formatDateTime(reminder.completedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InfoChip(
                    icon = R.drawable.ic_timer,
                    text = completionLabel(reminder),
                    containerColor = appColors.successContainer,
                    contentColor = appColors.onSuccessContainer,
                )
                InfoChip(
                    icon = R.drawable.ic_calendar,
                    text = stringResource(R.string.created_on, formatShortDate(reminder.createdAt)),
                )
                InfoChip(icon = R.drawable.ic_repeat, text = repeatLabel(reminder))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRestore) {
                    Icon(painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.restore))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Completion speed across all done reminders: count, average and fastest number of days. */
@Composable
private fun SpeedSummaryCard(done: List<Reminder>) {
    val days = remember(done) { done.map { daysBetween(it.createdAt, it.completedAt) } }
    val average = String.format(Locale.getDefault(), "%.1f", days.average())
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.speed_title), style = MaterialTheme.typography.titleSmall, color = onContainer)
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Stat(done.size.toString(), stringResource(R.string.stat_completed), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_days_value, average), stringResource(R.string.stat_average), Modifier.weight(1f))
                Stat(
                    stringResource(R.string.stat_days_value, days.min().toString()),
                    stringResource(R.string.stat_fastest),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = onContainer)
        Text(label, style = MaterialTheme.typography.labelMedium, color = onContainer.copy(alpha = 0.75f))
    }
}

@Composable
private fun IconBadge(@DrawableRes icon: Int, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InfoChip(
    @DrawableRes icon: Int,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
private fun EmptyState(@DrawableRes icon: Int, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Banner(@DrawableRes icon: Int, text: String, action: String, onAction: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colors.onTertiaryContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onTertiaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
            ) { Text(action) }
        }
    }
}

private data class PermissionIssue(@StringRes val message: Int, val settingsAction: String)

/** Missing permissions that would stop reminders from ringing reliably. */
@Composable
private fun rememberPermissionIssues(resumeTick: Int): List<PermissionIssue> {
    val context = LocalContext.current
    var notificationsGranted by remember(resumeTick) { mutableStateOf(hasNotificationPermission(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    LaunchedEffect(Unit) {
        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exactAlarms = remember(resumeTick) { ReminderScheduler.canScheduleExactAlarms(context) }
    val fullScreen = remember(resumeTick) { canUseFullScreenIntent(context) }

    return buildList {
        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionIssue(R.string.permission_notifications, Settings.ACTION_APP_NOTIFICATION_SETTINGS))
        }
        if (!exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(PermissionIssue(R.string.permission_exact_alarm, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
        if (!fullScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(PermissionIssue(R.string.permission_full_screen, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun canUseFullScreenIntent(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

private fun openAppSettings(context: Context, action: String) {
    val intent = Intent(action)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // The notification settings screen does not accept a data URI; the other two require one.
    if (action != Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
        intent.data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(intent) }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
