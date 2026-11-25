package com.mathalarm.clock.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathalarm.clock.alarm.AlarmScheduler
import com.mathalarm.clock.alarm.AlarmService
import com.mathalarm.clock.data.MathQuestion
import com.mathalarm.clock.data.QuestionRepository
import com.mathalarm.clock.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MathAlarmActivity : ComponentActivity() {

    private lateinit var questionRepo: QuestionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeAndUnlockScreen()

        questionRepo = QuestionRepository(this)
        val questionsCount = intent.getIntExtra(AlarmScheduler.EXTRA_QUESTIONS_COUNT, 3)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "Morning Alarm"

        val questions = questionRepo.getQuestions(questionsCount.coerceAtLeast(1))

        // Intercept back button to prevent dismissing without solving
        onBackPressedDispatcher.addCallback(this) {
            // Do not allow dismissing by back press
        }

        setContent {
            MathAlarmTheme {
                MathAlarmScreen(
                    label = label,
                    questions = questions,
                    onAllSolved = {
                        dismissAlarmService()
                    },
                    onFinishActivity = {
                        finish()
                    }
                )
            }
        }
    }

    private fun wakeAndUnlockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismissAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS_ALARM
        }
        startService(stopIntent)
    }
}

@Composable
fun MathAlarmScreen(
    label: String,
    questions: List<MathQuestion>,
    onAllSolved: () -> Unit,
    onFinishActivity: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Formatted current time string
    var currentTimeStr by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        while (true) {
            currentTimeStr = sdf.format(Date())
            delay(1000)
        }
    }

    if (isSuccess) {
        // Success celebration view
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = AccentGreen,
                    modifier = Modifier.size(88.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Alarm Dismissed!",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You solved all ${questions.size} math problems.\nYour brain is awake!",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = onFinishActivity,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(54.dp)
                ) {
                    Text(
                        text = "Good Morning",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
        return
    }

    val currentQ = questions.getOrNull(currentIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Ringing Status & Time
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x22EF4444))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Ringing",
                    tint = AccentRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ALARM RINGING • SOLVE TO STOP",
                    color = AccentRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentTimeStr,
                color = TextPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = label,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }

        // Middle Section: Question Card & Answer Input
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Problem progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Problem ${currentIndex + 1} of ${questions.size}",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in questions.indices) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentIndex) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        i < currentIndex -> AccentGreen
                                        i == currentIndex -> AccentCyan
                                        else -> CardStroke
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Question Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentQ.category.uppercase(),
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = currentQ.question,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "questionText"
                    ) { qText ->
                        Text(
                            text = qText,
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Answer Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(
                        1.5.dp,
                        if (errorMessage != null) AccentRed else AccentBlue.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentInput.isEmpty()) "Enter answer" else currentInput,
                    color = if (currentInput.isEmpty()) TextTertiary else AccentCyan,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage ?: "",
                    color = AccentRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Section: Sleek Minimal Keypad
        CustomKeypad(
            onDigit = { d ->
                errorMessage = null
                if (currentInput.length < 8) {
                    currentInput += d
                }
            },
            onDelete = {
                errorMessage = null
                if (currentInput.isNotEmpty()) {
                    currentInput = currentInput.dropLast(1)
                }
            },
            onMinus = {
                errorMessage = null
                if (currentInput.isEmpty()) {
                    currentInput = "-"
                }
            },
            onSubmit = {
                val entered = currentInput.toIntOrNull()
                if (entered == currentQ.answer) {
                    // Correct answer!
                    errorMessage = null
                    currentInput = ""
                    if (currentIndex + 1 < questions.size) {
                        currentIndex++
                    } else {
                        // All solved!
                        isSuccess = true
                        onAllSolved()
                    }
                } else {
                    errorMessage = "Incorrect answer, try again!"
                    currentInput = ""
                }
            }
        )
    }
}

@Composable
fun CustomKeypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onMinus: () -> Unit,
    onSubmit: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("-", "0", "DEL")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (key) {
                                    "DEL" -> KeypadDel
                                    else -> KeypadBg
                                }
                            )
                            .clickable {
                                when (key) {
                                    "DEL" -> onDelete()
                                    "-" -> onMinus()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = when (key) {
                                "DEL" -> AccentRed
                                else -> TextPrimary
                            },
                            fontSize = if (key.length > 1) 15.sp else 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Submit Button Row
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KeypadEnter)
        ) {
            Text(
                text = "SUBMIT ANSWER",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
