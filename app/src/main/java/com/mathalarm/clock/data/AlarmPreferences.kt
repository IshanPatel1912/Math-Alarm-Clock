package com.mathalarm.clock.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlarmPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("alarms_storage_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAlarms(): List<AlarmItem> {
        val json = prefs.getString("saved_alarms_json", null) ?: return getDefaultAlarms()
        return try {
            val type = object : TypeToken<List<AlarmItem>>() {}.type
            gson.fromJson<List<AlarmItem>>(json, type) ?: getDefaultAlarms()
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultAlarms()
        }
    }

    fun saveAlarms(alarms: List<AlarmItem>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString("saved_alarms_json", json).apply()
    }

    fun addAlarm(alarm: AlarmItem) {
        val list = getAlarms().toMutableList()
        list.add(alarm)
        saveAlarms(list)
    }

    fun updateAlarm(alarm: AlarmItem) {
        val list = getAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            list[index] = alarm
            saveAlarms(list)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        val list = getAlarms().filter { it.id != alarmId }
        saveAlarms(list)
    }

    private fun getDefaultAlarms(): List<AlarmItem> {
        val defaultList = listOf(
            AlarmItem(
                id = 1001L,
                hour = 6,
                minute = 30,
                label = "Morning Math Awakening",
                questionsCount = 3,
                isEnabled = true,
                daysOfWeek = setOf(2, 3, 4, 5, 6)
            ),
            AlarmItem(
                id = 1002L,
                hour = 7,
                minute = 45,
                label = "Weekend Fitness",
                questionsCount = 5,
                isEnabled = false,
                daysOfWeek = setOf(1, 7)
            )
        )
        saveAlarms(defaultList)
        return defaultList
    }
}
