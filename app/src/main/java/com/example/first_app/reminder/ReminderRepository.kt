package com.example.first_app.reminder

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Stores reminders as JSON in SharedPreferences and exposes them as a [StateFlow]. */
object ReminderRepository {
    private const val PREFS = "reminders"
    private const val KEY_LIST = "list"
    private const val KEY_NEXT_ID = "next_id"

    private lateinit var prefs: SharedPreferences
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _reminders.value = load()
    }

    fun get(id: Int): Reminder? = _reminders.value.firstOrNull { it.id == id }

    /** Inserts or replaces a reminder; a reminder with id 0 gets a new id. Returns the saved copy. */
    @Synchronized
    fun upsert(reminder: Reminder): Reminder {
        val saved = if (reminder.id == 0) reminder.copy(id = allocateId()) else reminder
        val list = _reminders.value.filterNot { it.id == saved.id } + saved
        persist(list.sortedBy { it.startAt })
        return saved
    }

    @Synchronized
    fun delete(id: Int) {
        persist(_reminders.value.filterNot { it.id == id })
    }

    private fun allocateId(): Int {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    private fun persist(list: List<Reminder>) {
        val json = JSONArray()
        list.forEach { json.put(it.toJson()) }
        prefs.edit().putString(KEY_LIST, json.toString()).apply()
        _reminders.value = list
    }

    private fun load(): List<Reminder> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            List(json.length()) { json.getJSONObject(it).toReminder() }
        }.getOrDefault(emptyList())
    }

    private fun Reminder.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("startAt", startAt)
        .put("repeat", repeat.name)
        .put("intervalDays", intervalDays)
        .put("enabled", enabled)
        .put("nextAt", nextAt)
        .put("snoozeUntil", snoozeUntil)
        .put("soundUri", soundUri ?: JSONObject.NULL)

    private fun JSONObject.toReminder() = Reminder(
        id = getInt("id"),
        title = getString("title"),
        startAt = getLong("startAt"),
        repeat = runCatching { Repeat.valueOf(getString("repeat")) }.getOrDefault(Repeat.NONE),
        intervalDays = optInt("intervalDays", 1),
        enabled = optBoolean("enabled", true),
        nextAt = optLong("nextAt", 0),
        snoozeUntil = optLong("snoozeUntil", 0),
        soundUri = if (isNull("soundUri")) null else optString("soundUri"),
    )
}
