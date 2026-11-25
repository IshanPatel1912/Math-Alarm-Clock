package com.mathalarm.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mathalarm.clock.data.AlarmPreferences

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = AlarmPreferences(context)
            val scheduler = AlarmScheduler(context)
            val alarms = prefs.getAlarms()

            for (alarm in alarms) {
                if (alarm.isEnabled) {
                    scheduler.schedule(alarm)
                }
            }
        }
    }
}
