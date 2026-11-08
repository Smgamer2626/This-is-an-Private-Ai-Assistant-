package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.YuriCameraScanner
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.preferences.YuriPreferences
import com.example.service.YuriFloatingBubbleService
import com.example.service.YuriWakeWordService
import com.example.ui.components.YuriAiCore
import com.example.ui.theme.YuriBackgroundCard
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.theme.YuriBackgroundElevated
import com.example.ui.theme.YuriBackgroundSurface
import com.example.ui.theme.YuriBorderBright
import com.example.ui.theme.YuriBorderCyan
import com.example.ui.theme.YuriCodeBackground
import com.example.ui.theme.YuriCodeText
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
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: YuriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val status by viewModel.status.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isCameraScannerOpen by viewModel.isCameraScannerOpen.collectAsState()

    val currentHost by viewModel.preferences.serverHostFlow.collectAsState(initial = YuriPreferences.DEFAULT_SERVER_HOST)
    val currentModel by viewModel.preferences.modelNameFlow.collectAsState(initial = "qwen2.5")
    val wakeWordEnabled by viewModel.preferences.wakeWordEnabledFlow.collectAsState(initial = false)
    val floatingBubbleEnabled by viewModel.preferences.floatingBubbleEnabledFlow.collectAsState(initial = false)

    var inputPrompt by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProtocolPrompt by remember { mutableStateOf(false) }

    var hostEdit by remember(currentHost) { mutableStateOf(currentHost) }
    var modelEdit by remember(currentModel) { mutableStateOf(currentModel) }

    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasOverlay = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    // Permission launchers for seamless activation
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
            startBubbleOverlay(context)
            startWakeEngine(context)
            scope.launch {
                viewModel.preferences.updateFloatingBubbleEnabled(true)
                viewModel.preferences.updateWakeWordEnabled(true)
            }
            Toast.makeText(context, "YURI Background Protocol Active!", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val micGranted = perms[Manifest.permission.RECORD_AUDIO] == true || hasMic
        if (micGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayLauncher.launch(intent)
            } else {
                startBubbleOverlay(context)
                startWakeEngine(context)
                scope.launch {
                    viewModel.preferences.updateFloatingBubbleEnabled(true)
                    viewModel.preferences.updateWakeWordEnabled(true)
                }
                Toast.makeText(context, "YURI Background Protocol Active!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-prompt once on first launch if background protocol is inactive
    LaunchedEffect(Unit) {
        if (!hasMic || !hasOverlay) {
            showProtocolPrompt = true
        } else {
            // Already permitted: ensure background services run smoothly
            startBubbleOverlay(context)
            startWakeEngine(context)
        }
    }

    val requestVoiceAndToggle = {
        if (hasMic) {
            viewModel.toggleVoiceListening()
        } else {
            permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Yuri Response", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // Latest user message and latest assistant response
    val lastUserMessage = messages.findLast { it.role == "user" }
    val lastAssistantMessage = messages.findLast { it.role == "assistant" }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF061026),
                        Color(0xFF020610)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // 1. Sleek JARVIS Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "YURI",
                            color = YuriCyanPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "JARVIS PROTOCOL • QWEN CORE",
                        color = YuriCyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Optical Vision Scanner icon
                    IconButton(
                        onClick = { viewModel.openCameraScanner() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, CircleShape)
                            .testTag("header_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Optical Camera Vision",
                            tint = YuriCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Floating Bubble summon icon
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                overlayLauncher.launch(intent)
                            } else {
                                val emergeIntent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                                    action = YuriFloatingBubbleService.ACTION_EMERGE
                                }
                                context.startService(emergeIntent)
                                Toast.makeText(context, "Holographic Bubble Summoned", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BubbleChart,
                            contentDescription = "Summon Floating Bubble",
                            tint = YuriCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Qwen status pill
                    val isConnected = serverHealth?.isSuccess == true
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(YuriBackgroundCard)
                            .border(1.dp, if (isConnected) YuriStatusOnline.copy(alpha = 0.6f) else YuriStatusOffline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) YuriStatusOnline else YuriStatusOffline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "QWEN 2.5" else "OFFLINE",
                            color = if (isConnected) YuriStatusOnline else YuriStatusOffline,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Central Holographic Bobbing AI Core
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.height(10.dp))

                // The Central Animated AI Core
                YuriAiCore(
                    status = status,
                    audioRms = audioRms,
                    onClick = requestVoiceAndToggle
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Audio / Status Display
                val statusTitle = when (status) {
                    YuriStatus.LISTENING -> "LISTENING TO COMMAND..."
                    YuriStatus.THINKING -> "SYNTHESIZING WITH QWEN..."
                    YuriStatus.SPEAKING -> "TRANSMITTING RESPONSE..."
                    YuriStatus.OFFLINE -> "YURI STANDING BY (OFFLINE)"
                    YuriStatus.ONLINE -> "YURI STANDING BY"
                }

                Text(
                    text = statusTitle,
                    color = when (status) {
                        YuriStatus.LISTENING -> YuriStatusListening
                        YuriStatus.THINKING -> YuriStatusThinking
                        YuriStatus.SPEAKING -> YuriCyanGlow
                        YuriStatus.OFFLINE -> YuriStatusOffline
                        else -> YuriCyanPrimary
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Live Conversational Display (No terminal chat clutter)
                if (isStreaming || streamingText.isNotBlank()) {
                    // Streaming response card
                    JarvisResponseCard(
                        userQuery = lastUserMessage?.content,
                        responseText = streamingText,
                        isStreaming = true,
                        onCopy = { copyToClipboard(streamingText) },
                        onSpeak = { viewModel.voiceManager.speak(streamingText) },
                        onStop = { viewModel.stopGenerating() }
                    )
                } else if (lastAssistantMessage != null && lastUserMessage != null) {
                    // Most recent conversation card
                    JarvisResponseCard(
                        userQuery = lastUserMessage.content,
                        responseText = lastAssistantMessage.content,
                        isStreaming = false,
                        onCopy = { copyToClipboard(lastAssistantMessage.content) },
                        onSpeak = { viewModel.voiceManager.speak(lastAssistantMessage.content) },
                        onStop = {}
                    )
                } else {
                    // Idle greeting & Prompt chips
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "“Good day, Commander. All offline neural systems are nominal. Say 'Yuri' or tap the core to command.”",
                            color = YuriTextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick prompt chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chips = listOf(
                                "Explain Quantum Physics",
                                "What is Dark Matter?",
                                "Write a Python Quicksort",
                                "Calculate Escape Velocity",
                                "Ohm's Law Formula"
                            )
                            chips.forEach { chip ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(YuriBackgroundCard)
                                        .border(1.dp, YuriBorderCyan, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.sendMessage(chip) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
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
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Bottom Futuristic Console Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(YuriBackgroundSurface)
                    .border(1.dp, YuriBorderCyan, RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice button
                IconButton(
                    onClick = requestVoiceAndToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) YuriStatusListening.copy(alpha = 0.25f) else YuriBackgroundCard)
                        .border(1.dp, if (isListening) YuriStatusListening else YuriBorderCyan, CircleShape)
                        .testTag("console_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) YuriStatusListening else YuriCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Optical Camera Scanner button
                IconButton(
                    onClick = { viewModel.openCameraScanner() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(YuriBackgroundCard)
                        .border(1.dp, YuriBorderCyan, CircleShape)
                        .testTag("console_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Camera Vision Scanner",
                        tint = YuriCyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening for your voice..." else "Command Yuri...",
                            color = YuriTextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YuriTextPrimary,
                        unfocusedTextColor = YuriTextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("console_input"),
                    singleLine = true
                )

                if (isStreaming) {
                    IconButton(
                        onClick = { viewModel.stopGenerating() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(YuriStatusOffline.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Halt Generation",
                            tint = YuriStatusOffline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputPrompt.isNotBlank()) {
                                viewModel.sendMessage(inputPrompt)
                                inputPrompt = ""
                            }
                        },
                        enabled = inputPrompt.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (inputPrompt.isNotBlank()) YuriCyanPrimary else YuriBackgroundCard)
                            .testTag("console_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputPrompt.isNotBlank()) Color.Black else YuriTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Qwen Server Configuration Modal
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text(
                        text = "QWEN OFFLINE ENGINE",
                        color = YuriCyanPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Connects locally to Qwen running in Termux:",
                            color = YuriTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        OutlinedTextField(
                            value = hostEdit,
                            onValueChange = {
                                hostEdit = it
                                scope.launch { viewModel.preferences.updateServerHost(it) }
                            },
                            label = { Text("Local API Endpoint", color = YuriCyanPrimary) },
                            placeholder = { Text("http://127.0.0.1:8080") }
                        )

                        OutlinedTextField(
                            value = modelEdit,
                            onValueChange = {
                                modelEdit = it
                                scope.launch { viewModel.preferences.updateModelName(it) }
                            },
                            label = { Text("Qwen Model Identifier", color = YuriCyanPrimary) },
                            placeholder = { Text("qwen2.5") }
                        )

                        Button(
                            onClick = { viewModel.checkServerHealth() },
                            colors = ButtonDefaults.buttonColors(containerColor = YuriCyanPrimary),
                            enabled = !isCheckingHealth,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCheckingHealth) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("TEST CONNECTION", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        serverHealth?.let { health ->
                            Text(
                                text = if (health.isSuccess) "Connected (${health.latencyMs}ms)" else (health.errorMessage ?: "Failed to connect"),
                                color = if (health.isSuccess) YuriStatusOnline else YuriStatusOffline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("DONE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clean One-Time Protocol Permission Modal (No clutter on screen)
        if (showProtocolPrompt) {
            AlertDialog(
                onDismissRequest = { showProtocolPrompt = false },
                title = {
                    Text(
                        text = "ACTIVATE YURI PROTOCOL",
                        color = YuriCyanPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Allow Yuri to run in the background with the floating holographic bubble and respond whenever you say 'Yuri' (just like JARVIS).",
                        color = YuriTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showProtocolPrompt = false
                            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(perms.toTypedArray())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YuriCyanPrimary)
                    ) {
                        Text("ENGAGE PROTOCOL", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProtocolPrompt = false }) {
                        Text("LATER", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Fullscreen Optical Camera Scanner HUD Overlay
        if (isCameraScannerOpen) {
            YuriCameraScanner(
                onDismiss = { viewModel.closeCameraScanner() },
                onAnalyzeImage = { bitmap, prompt ->
                    viewModel.analyzeCameraVision(bitmap, prompt)
                }
            )
        }
    }
}

@Composable
fun JarvisResponseCard(
    userQuery: String?,
    responseText: String,
    isStreaming: Boolean,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YuriBackgroundCard.copy(alpha = 0.95f))
            .border(1.dp, YuriBorderCyan, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        if (!userQuery.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMANDER:",
                    color = YuriCyanGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "“$userQuery”",
                    color = YuriTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isStreaming) "YURI [SYNTHESIZING...]" else "YURI [QWEN]:",
                color = if (isStreaming) YuriStatusThinking else YuriCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row {
                IconButton(onClick = onSpeak, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Read Aloud",
                        tint = YuriCyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = YuriTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = responseText,
            color = YuriTextPrimary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        if (isStreaming) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "HALT",
                    color = YuriStatusOffline,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable(onClick = onStop)
                        .padding(4.dp)
                )
            }
        }
    }
}

private fun startBubbleOverlay(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return
    val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
        action = YuriFloatingBubbleService.ACTION_SHOW
    }
    context.startService(intent)
}

private fun startWakeEngine(context: Context) {
    val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    if (!hasMic) return

    val intent = Intent(context, YuriWakeWordService::class.java).apply {
        action = YuriWakeWordService.ACTION_START_LISTENING
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
