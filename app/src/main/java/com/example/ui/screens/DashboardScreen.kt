package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.service.YuriFloatingBubbleService
import com.example.service.YuriWakeWordService
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.HardwareTelemetryDashboard
import com.example.ui.components.TopHudClockBar
import com.example.ui.components.YuriAiCore
import com.example.ui.theme.YuriBackgroundCard
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.theme.YuriBackgroundSurface
import com.example.ui.theme.YuriBorderBright
import com.example.ui.theme.YuriBorderCyan
import com.example.ui.theme.YuriCyanGlow
import com.example.ui.theme.YuriCyanPrimary
import com.example.ui.theme.YuriStatusListening
import com.example.ui.theme.YuriStatusOffline
import com.example.ui.theme.YuriStatusOnline
import com.example.ui.theme.YuriStatusThinking
import com.example.ui.theme.YuriTextPrimary
import com.example.ui.theme.YuriTextSecondary
import com.example.ui.viewmodel.YuriStatus
import com.example.ui.viewmodel.YuriViewModel

@Composable
fun DashboardScreen(
    viewModel: YuriViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val systemReport by viewModel.systemReport.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val speechError by viewModel.voiceManager.speechError.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceManager.startListening()
        }
    }

    val requestMicAndToggle = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleVoiceListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DashboardGlow")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        YuriBackgroundDeep,
                        YuriBackgroundSurface,
                        Color(0xFF02050D)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD Clock & Status Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TopHudClockBar(
                    serverHealth = serverHealth,
                    onStatusClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Hardware Telemetry Card (Battery, RAM, CPU)
                HardwareTelemetryDashboard(
                    battery = batteryInfo,
                    report = systemReport,
                    onExpandClick = onNavigateToTools
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Central Bobbing AI Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                YuriAiCore(
                    status = status,
                    audioRms = audioRms,
                    onClick = requestMicAndToggle
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Greeting & Yuri status line
                val greetingTitle = when (status) {
                    YuriStatus.LISTENING -> "Listening to audio input..."
                    YuriStatus.THINKING -> "Synthesizing neural response..."
                    YuriStatus.SPEAKING -> "Transmitting audio stream..."
                    YuriStatus.OFFLINE -> "Offline: Termux engine waiting"
                    YuriStatus.ONLINE -> "Hello! I am Yuri. How can I assist you?"
                }

                Text(
                    text = greetingTitle,
                    color = YuriTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitleText = when (status) {
                    YuriStatus.LISTENING -> "Speak your command or query clearly."
                    YuriStatus.THINKING -> "Local llama.cpp inference in progress."
                    YuriStatus.OFFLINE -> "Tap Settings to test or start the Termux server."
                    else -> "Personal Offline Assistant • Mathematics • Science • Code"
                }

                Text(
                    text = subtitleText,
                    color = YuriTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                // Speech error or info if present
                AnimatedVisibility(
                    visible = speechError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = speechError ?: "",
                        color = YuriStatusOffline,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3.5 WAKE ENGINE & FLOATING BUBBLE HUD PANEL
            val wakeWordEnabled by viewModel.preferences.wakeWordEnabledFlow.collectAsState(initial = false)
            val floatingBubbleEnabled by viewModel.preferences.floatingBubbleEnabledFlow.collectAsState(initial = false)
            val scope = rememberCoroutineScope()

            val overlayPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                    scope.launch { viewModel.preferences.updateFloatingBubbleEnabled(true) }
                    val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                        action = YuriFloatingBubbleService.ACTION_SHOW
                    }
                    context.startService(intent)
                    Toast.makeText(context, "Floating AI Bubble Active!", Toast.LENGTH_SHORT).show()
                }
            }

            val wakeMicLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
                if (micGranted) {
                    scope.launch { viewModel.preferences.updateWakeWordEnabled(true) }
                    val intent = Intent(context, YuriWakeWordService::class.java).apply {
                        action = YuriWakeWordService.ACTION_START_LISTENING
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    Toast.makeText(context, "Wake Engine Active: Say 'Yuri' anytime!", Toast.LENGTH_LONG).show()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(YuriBackgroundCard.copy(alpha = 0.9f))
                    .border(1.dp, YuriBorderCyan, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (wakeWordEnabled) YuriStatusOnline else YuriCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WAKE WORD: 'YURI'",
                                color = YuriTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (wakeWordEnabled) "Listening in background (like Siri / Gemini)" else "Say 'Yuri' to summon assistant",
                                color = YuriTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val hasMic = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasMic && hasNotif) {
                                    scope.launch { viewModel.preferences.updateWakeWordEnabled(true) }
                                    val intent = Intent(context, YuriWakeWordService::class.java).apply {
                                        action = YuriWakeWordService.ACTION_START_LISTENING
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    Toast.makeText(context, "Wake Engine Active: Say 'Yuri'!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val reqs = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        reqs.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    wakeMicLauncher.launch(reqs.toTypedArray())
                                }
                            } else {
                                scope.launch { viewModel.preferences.updateWakeWordEnabled(false) }
                                val intent = Intent(context, YuriWakeWordService::class.java).apply {
                                    action = YuriWakeWordService.ACTION_STOP_LISTENING
                                }
                                context.startService(intent)
                                Toast.makeText(context, "Wake Engine Deactivated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = YuriCyanPrimary,
                            uncheckedThumbColor = YuriTextSecondary,
                            uncheckedTrackColor = YuriBackgroundSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (floatingBubbleEnabled) YuriCyanGlow else YuriCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FLOATING AI BUBBLE",
                                color = YuriTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Draggable holographic bubble over other apps",
                                color = YuriTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Switch(
                        checked = floatingBubbleEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                    Toast.makeText(context, "Please allow 'Display over other apps' for Yuri", Toast.LENGTH_LONG).show()
                                } else {
                                    scope.launch { viewModel.preferences.updateFloatingBubbleEnabled(true) }
                                    val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                                        action = YuriFloatingBubbleService.ACTION_SHOW
                                    }
                                    context.startService(intent)
                                }
                            } else {
                                scope.launch { viewModel.preferences.updateFloatingBubbleEnabled(false) }
                                val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                                    action = YuriFloatingBubbleService.ACTION_HIDE
                                }
                                context.startService(intent)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = YuriCyanPrimary,
                            uncheckedThumbColor = YuriTextSecondary,
                            uncheckedTrackColor = YuriBackgroundSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Summon Button
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                            Toast.makeText(context, "Please grant overlay permission first", Toast.LENGTH_LONG).show()
                        } else {
                            val emergeIntent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                                action = YuriFloatingBubbleService.ACTION_EMERGE
                            }
                            context.startService(emergeIntent)
                            Toast.makeText(context, "AI Bubble Emerging...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, YuriCyanPrimary, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = YuriCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEST EMERGE AI BUBBLE OVERLAY",
                        color = YuriCyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Quick Action Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "Explain Quantum Computing",
                    "Ohm's Law Calculation",
                    "Debug Python Code",
                    "What are Black Holes?",
                    "System Diagnostics"
                )
                chips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.sendMessage(chip)
                                onNavigateToChat()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("chip_${chip.replace(' ', '_').lowercase()}")
                    ) {
                        Text(
                            text = chip,
                            color = YuriCyanGlow,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Futuristic Bottom Control Deck
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onNavigateToChat)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Open Chat",
                            tint = YuriCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TERMINAL",
                        color = YuriTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Large Central Glowing Microphone Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = requestMicAndToggle)
                ) {
                    val micColor = if (isListening) YuriStatusListening else YuriCyanPrimary
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(if (isListening) micPulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        micColor.copy(alpha = 0.35f),
                                        Color(0xFF0B1933)
                                    )
                                )
                            )
                            .border(2.dp, micColor, CircleShape)
                            .testTag("main_mic_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                            tint = micColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isListening) "LISTENING" else "VOICE",
                        color = if (isListening) YuriStatusListening else YuriCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Tools Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onNavigateToTools)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Tools",
                            tint = YuriCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MODULES",
                        color = YuriTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Settings Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onNavigateToSettings)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = YuriCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SETTINGS",
                        color = YuriTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
