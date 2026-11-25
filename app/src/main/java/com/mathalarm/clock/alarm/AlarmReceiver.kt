package com.mathalarm.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.mathalarm.clock.ui.MathAlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val questionsCount = intent.getIntExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, 3)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "Alarm"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MathAlarm:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(10_000L) // 10 seconds max

        // 1. Start Foreground Audio & Vibration Service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, questionsCount)
            putExtra(AlarmScheduler.EXTRA_LABEL, label)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // 2. Launch Math Challenge Ringing Activity directly
        val activityIntent = Intent(context, MathAlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, questionsCount)
            putExtra(AlarmScheduler.EXTRA_LABEL, label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(activityIntent)
    }
}
