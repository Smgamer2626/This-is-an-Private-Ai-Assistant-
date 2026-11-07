package com.example.ui.screens

import android.Manifest
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun DashboardScreen(
    viewModel: YuriViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val status by viewModel.status.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val speechError by viewModel.voiceManager.speechError.collectAsState()

    val currentHost by viewModel.preferences.serverHostFlow.collectAsState(initial = YuriPreferences.DEFAULT_SERVER_HOST)
    val currentModel by viewModel.preferences.modelNameFlow.collectAsState(initial = "qwen2.5")
    val wakeWordEnabled by viewModel.preferences.wakeWordEnabledFlow.collectAsState(initial = false)
    val floatingBubbleEnabled by viewModel.preferences.floatingBubbleEnabledFlow.collectAsState(initial = false)

    var showConfigDialog by remember { mutableStateOf(false) }
    var hostInput by remember(currentHost) { mutableStateOf(currentHost) }
    var modelInput by remember(currentModel) { mutableStateOf(currentModel) }

    // Check permission states
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        )
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isBatteryOptimized by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            isBatteryOptimized = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
    }

    val refreshPermissions = {
        hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        hasOverlayPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        hasNotifPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            isBatteryOptimized = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        }
    }

    // Permission Launchers
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
            startBubbleService(context)
        }
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        refreshPermissions()
        val micOk = perms[Manifest.permission.RECORD_AUDIO] == true || hasMicPermission
        if (micOk) {
            checkAndRequestRemainingPerms(context, overlayLauncher, batteryLauncher)
        }
    }

    val isProtocolActive = wakeWordEnabled && floatingBubbleEnabled && YuriWakeWordService.isServiceRunning

    // Function to activate background protocol
    val activateProtocol = {
        refreshPermissions()
        if (!hasMicPermission || !hasNotifPermission) {
            val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionsLauncher.launch(list.toTypedArray())
        } else if (!hasOverlayPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                overlayLauncher.launch(intent)
                Toast.makeText(context, "Grant 'Display over other apps' to enable floating bubble", Toast.LENGTH_LONG).show()
            }
        } else {
            // Start services
            scope.launch {
                viewModel.preferences.updateWakeWordEnabled(true)
                viewModel.preferences.updateFloatingBubbleEnabled(true)
            }
            startWakeService(context)
            startBubbleService(context)

            // Request battery optimization exclusion if not already granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimized) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    batteryLauncher.launch(intent)
                } catch (_: Exception) {}
            }

            Toast.makeText(context, "YURI Background Protocol Activated!", Toast.LENGTH_SHORT).show()
        }
    }

    val deactivateProtocol = {
        scope.launch {
            viewModel.preferences.updateWakeWordEnabled(false)
            viewModel.preferences.updateFloatingBubbleEnabled(false)
        }
        stopWakeService(context)
        stopBubbleService(context)
        Toast.makeText(context, "Background Protocol Deactivated", Toast.LENGTH_SHORT).show()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DashboardPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ProtocolPulse"
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // 1. Header: Name and AI Engine
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "YURI",
                            color = YuriCyanPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 3.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI",
                            color = YuriTextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "OFFLINE ASSISTANT • QWEN CORE",
                        color = YuriCyanGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Server Status Pill
                val isConnected = serverHealth?.isSuccess == true
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(YuriBackgroundCard)
                        .border(1.dp, if (isConnected) YuriStatusOnline.copy(alpha = 0.6f) else YuriStatusOffline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { showConfigDialog = true }
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
                        text = if (isConnected) "QWEN ONLINE" else "QWEN OFFLINE",
                        color = if (isConnected) YuriStatusOnline else YuriStatusOffline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Center: The Floating AI Bubble
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                YuriAiCore(
                    status = status,
                    audioRms = audioRms,
                    onClick = {
                        if (hasMicPermission) {
                            viewModel.toggleVoiceListening()
                        } else {
                            permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (status) {
                        YuriStatus.LISTENING -> "Listening to audio input..."
                        YuriStatus.THINKING -> "Qwen is thinking..."
                        YuriStatus.SPEAKING -> "Transmitting response..."
                        else -> "Say 'Yuri' or tap the bubble"
                    },
                    color = YuriTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Personal Offline AI Assistant powered by Qwen GGUF",
                    color = YuriTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

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
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. BACKGROUND PROTOCOL ACTIVATION CARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(YuriBackgroundCard)
                    .border(1.dp, if (isProtocolActive) YuriCyanPrimary else YuriBorderCyan, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BACKGROUND PROTOCOL",
                            color = if (isProtocolActive) YuriCyanPrimary else YuriTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isProtocolActive) "PROTOCOL ENGAGED • STANDING BY" else "ENABLE WAKE-WORD & FLOATING BUBBLE",
                            color = if (isProtocolActive) YuriStatusOnline.copy(alpha = pulseAlpha) else YuriTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Icon(
                        imageVector = if (isProtocolActive) Icons.Default.Hearing else Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isProtocolActive) YuriCyanPrimary else YuriTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Checklist
                PermissionCheckRow(label = "Microphone Access", isGranted = hasMicPermission)
                PermissionCheckRow(label = "Draw Over Other Apps (Floating Bubble)", isGranted = hasOverlayPermission)
                PermissionCheckRow(label = "Background Service Notification", isGranted = hasNotifPermission)
                PermissionCheckRow(label = "Battery Optimization Disabled (Stays Active)", isGranted = isBatteryOptimized)

                Spacer(modifier = Modifier.height(14.dp))

                // Big Activation Button
                Button(
                    onClick = {
                        if (isProtocolActive) {
                            deactivateProtocol()
                        } else {
                            activateProtocol()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProtocolActive) Color(0xFF280B14) else YuriCyanPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("activate_protocol_button")
                ) {
                    Text(
                        text = if (isProtocolActive) "DEACTIVATE BACKGROUND PROTOCOL" else "ACTIVATE BACKGROUND PROTOCOL",
                        color = if (isProtocolActive) YuriStatusOffline else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick test actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasOverlayPermission) {
                                activateProtocol()
                            } else {
                                startBubbleService(context)
                                Toast.makeText(context, "Floating bubble summoned!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHOW BUBBLE", color = YuriCyanGlow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            if (!hasOverlayPermission) {
                                activateProtocol()
                            } else {
                                val emergeIntent = Intent(context, YuriFloatingBubbleService::class.java).apply {
                                    action = YuriFloatingBubbleService.ACTION_EMERGE
                                }
                                context.startService(emergeIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TEST 'YURI'", color = YuriCyanGlow, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Bottom Controls: Open Chat / Terminal & Config
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateToChat,
                    colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, YuriBorderCyan, RoundedCornerShape(10.dp))
                ) {
                    Text("OPEN TERMINAL CHAT", color = YuriTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(YuriBackgroundElevated)
                        .border(1.dp, YuriBorderCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Qwen Settings",
                        tint = YuriCyanPrimary
                    )
                }
            }
        }

        // Qwen Server Configuration Dialog
        if (showConfigDialog) {
            AlertDialog(
                onDismissRequest = { showConfigDialog = false },
                title = {
                    Text("LOCAL QWEN / TERMUX SETTINGS", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Connects to your local Qwen GGUF model running in Termux:",
                            color = YuriTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = {
                                hostInput = it
                                scope.launch { viewModel.preferences.updateServerHost(it) }
                            },
                            label = { Text("Local Server URL", color = YuriCyanPrimary) },
                            placeholder = { Text("http://127.0.0.1:8080") }
                        )

                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = {
                                modelInput = it
                                scope.launch { viewModel.preferences.updateModelName(it) }
                            },
                            label = { Text("Qwen Model Identifier", color = YuriCyanPrimary) },
                            placeholder = { Text("qwen2.5") }
                        )

                        // Connection test
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
                                text = if (health.isSuccess) "Connected! Latency: ${health.latencyMs}ms" else (health.errorMessage ?: "Failed to connect"),
                                color = if (health.isSuccess) YuriStatusOnline else YuriStatusOffline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConfigDialog = false }) {
                        Text("DONE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun PermissionCheckRow(label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isGranted) YuriTextPrimary else YuriTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isGranted) YuriStatusOnline else YuriTextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun startWakeService(context: Context) {
    val intent = Intent(context, YuriWakeWordService::class.java).apply {
        action = YuriWakeWordService.ACTION_START_LISTENING
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun stopWakeService(context: Context) {
    val intent = Intent(context, YuriWakeWordService::class.java).apply {
        action = YuriWakeWordService.ACTION_STOP_LISTENING
    }
    context.startService(intent)
}

private fun startBubbleService(context: Context) {
    val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
        action = YuriFloatingBubbleService.ACTION_SHOW
    }
    context.startService(intent)
}

private fun stopBubbleService(context: Context) {
    val intent = Intent(context, YuriFloatingBubbleService::class.java).apply {
        action = YuriFloatingBubbleService.ACTION_HIDE
    }
    context.startService(intent)
}

private fun checkAndRequestRemainingPerms(
    context: Context,
    overlayLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    batteryLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlayLauncher.launch(intent)
        Toast.makeText(context, "Grant 'Display over other apps' to enable floating bubble", Toast.LENGTH_LONG).show()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (pm?.isIgnoringBatteryOptimizations(context.packageName) != true) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                batteryLauncher.launch(intent)
            } catch (_: Exception) {}
        }
    }
}
