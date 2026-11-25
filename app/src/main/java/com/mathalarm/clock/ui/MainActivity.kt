package com.mathalarm.clock.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.mathalarm.clock.alarm.AlarmScheduler
import com.mathalarm.clock.alarm.AlarmService
import com.mathalarm.clock.data.AlarmItem
import com.mathalarm.clock.data.AlarmPreferences
import com.mathalarm.clock.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var alarmPreferences: AlarmPreferences
    private lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmPreferences = AlarmPreferences(this)
        alarmScheduler = AlarmScheduler(this)

        checkPermissions()

        setContent {
            MathAlarmTheme {
                MainAlarmScreen(
                    alarmPreferences = alarmPreferences,
                    alarmScheduler = alarmScheduler,
                    onTestAlarm = { questionsCount ->
                        startTestAlarm(questionsCount)
                    }
                )
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startTestAlarm(questionsCount: Int) {
        // Start foreground service to play alarm sound from system
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            putExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, questionsCount)
            putExtra(AlarmScheduler.EXTRA_LABEL, "Test Math Alarm")
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        // Launch challenge screen
        val intent = Intent(this, MathAlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            putExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, questionsCount)
            putExtra(AlarmScheduler.EXTRA_LABEL, "Test Math Alarm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun MainAlarmScreen(
    alarmPreferences: AlarmPreferences,
    alarmScheduler: AlarmScheduler,
    onTestAlarm: (questionsCount: Int) -> Unit
) {
    var alarms by remember { mutableStateOf(alarmPreferences.getAlarms()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Live clock time
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val secFormat = SimpleDateFormat("ss", Locale.getDefault())
    val nextAlarmMillis = remember(alarms) { alarmScheduler.getNextAlarmTime(alarms) }

    // Notification Permission launcher for Android 13+
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(62.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATH ALARM",
                    color = TextTertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "300 QUESTIONS",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Digital Clock
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = timeFormat.format(currentTime),
                    color = TextPrimary,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 64.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = secFormat.format(currentTime),
                    color = TextSecondary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Next Alarm Info
            val nextAlarmText = if (nextAlarmMillis != null) {
                val diff = (nextAlarmMillis - System.currentTimeMillis()).coerceAtLeast(0)
                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff / (1000 * 60)) % 60
                "Next alarm in ${hours}h ${minutes}m"
            } else {
                "No active alarms"
            }

            Text(
                text = nextAlarmText,
                color = if (nextAlarmMillis != null) AccentGreen else TextTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Test Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTestAlarm(3) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Test",
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Test Math Challenge Now",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Rings system sound & tests 3 questions",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "TEST",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Active Alarms
            Text(
                text = "Alarms",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alarms configured.\nTap + below to add your first alarm.",
                        color = TextTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { isEnabled ->
                                val updated = alarm.copy(isEnabled = isEnabled)
                                alarmPreferences.updateAlarm(updated)
                                if (isEnabled) {
                                    alarmScheduler.schedule(updated)
                                } else {
                                    alarmScheduler.cancel(updated.id)
                                }
                                alarms = alarmPreferences.getAlarms()
                            },
                            onDelete = {
                                alarmScheduler.cancel(alarm.id)
                                alarmPreferences.deleteAlarm(alarm.id)
                                alarms = alarmPreferences.getAlarms()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onSave = { hour, minute, label, questionsCount, days ->
                val newAlarm = AlarmItem(
                    id = System.currentTimeMillis(),
                    hour = hour,
                    minute = minute,
                    label = label.ifBlank { "Morning Alarm" },
                    questionsCount = questionsCount,
                    isEnabled = true,
                    daysOfWeek = days
                )
                alarmPreferences.addAlarm(newAlarm)
                alarmScheduler.schedule(newAlarm)
                alarms = alarmPreferences.getAlarms()
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) CardDark else SurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (alarm.isEnabled) CardStroke else Color(0x11FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime,
                        color = if (alarm.isEnabled) TextPrimary else TextTertiary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPm,
                        color = if (alarm.isEnabled) AccentCyan else TextTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alarm.label,
                        color = if (alarm.isEnabled) TextSecondary else TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${alarm.questionsCount} math problems",
                        color = if (alarm.isEnabled) AccentBlue else TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = alarm.daysSummary,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextTertiary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AccentBlue,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }
        }
    }
}

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, questionsCount: Int, days: Set<Int>) -> Unit
) {
    var hour by remember { mutableIntStateOf(6) }
    var minute by remember { mutableIntStateOf(30) }
    var isAm by remember { mutableStateOf(true) }
    var label by remember { mutableStateOf("Morning Awakening") }
    var questionsCount by remember { mutableIntStateOf(3) }
    val selectedDays = remember { mutableStateListOf<Int>(2, 3, 4, 5, 6) } // Weekdays default

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Math Alarm",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time adjustment row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour picker buttons
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▲",
                            color = AccentCyan,
                            modifier = Modifier
                                .clickable {
                                    hour = if (hour >= 12) 1 else hour + 1
                                }
                                .padding(6.dp)
                        )
                        Text(
                            text = String.format("%02d", hour),
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "▼",
                            color = AccentCyan,
                            modifier = Modifier
                                .clickable {
                                    hour = if (hour <= 1) 12 else hour - 1
                                }
                                .padding(6.dp)
                        )
                    }

                    Text(
                        text = ":",
                        color = TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute picker buttons
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▲",
                            color = AccentCyan,
                            modifier = Modifier
                                .clickable {
                                    minute = (minute + 5) % 60
                                }
                                .padding(6.dp)
                        )
                        Text(
                            text = String.format("%02d", minute),
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "▼",
                            color = AccentCyan,
                            modifier = Modifier
                                .clickable {
                                    minute = if (minute < 5) 55 else minute - 5
                                }
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM / PM toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isAm) AccentBlue else SurfaceDark)
                                .clickable { isAm = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "AM",
                                color = if (isAm) Color.White else TextTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isAm) AccentBlue else SurfaceDark)
                                .clickable { isAm = false }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "PM",
                                color = if (!isAm) Color.White else TextTertiary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Questions count selector
                Text(
                    text = "Math Questions to Dismiss: $questionsCount",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 2, 3, 5, 7).forEach { count ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (questionsCount == count) AccentBlue else SurfaceDark)
                                .clickable { questionsCount = count },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = if (questionsCount == count) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days selection
                val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                val dayIndices = listOf(1, 2, 3, 4, 5, 6, 7) // Calendar.SUNDAY to SATURDAY
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in dayIndices.indices) {
                        val dayVal = dayIndices[i]
                        val isSelected = dayVal in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.25f) else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedDays.remove(dayVal) else selectedDays.add(dayVal)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLetters[i],
                                color = if (isSelected) AccentCyan else TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val final24Hour = if (isAm) {
                                if (hour == 12) 0 else hour
                            } else {
                                if (hour == 12) 12 else hour + 12
                            }
                            onSave(
                                final24Hour,
                                minute,
                                label,
                                questionsCount,
                                selectedDays.toSet()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save Alarm", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
