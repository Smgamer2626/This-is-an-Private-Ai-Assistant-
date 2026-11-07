package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.YuriPreferences
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
import com.example.ui.theme.YuriStatusOffline
import com.example.ui.theme.YuriStatusOnline
import com.example.ui.theme.YuriStatusWarning
import com.example.ui.theme.YuriTextPrimary
import com.example.ui.theme.YuriTextSecondary
import com.example.ui.viewmodel.YuriViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: YuriViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serverHealth by viewModel.serverHealth.collectAsState()
    val isCheckingHealth by viewModel.isCheckingHealth.collectAsState()
    val memories by viewModel.userMemories.collectAsState()

    val currentHost by viewModel.preferences.serverHostFlow.collectAsState(initial = YuriPreferences.DEFAULT_SERVER_HOST)
    val currentModel by viewModel.preferences.modelNameFlow.collectAsState(initial = YuriPreferences.DEFAULT_MODEL_NAME)
    val currentTemp by viewModel.preferences.temperatureFlow.collectAsState(initial = YuriPreferences.DEFAULT_TEMPERATURE)
    val currentMaxTokens by viewModel.preferences.maxTokensFlow.collectAsState(initial = YuriPreferences.DEFAULT_MAX_TOKENS)
    val currentSysPrompt by viewModel.preferences.systemPromptFlow.collectAsState(initial = YuriPreferences.DEFAULT_SYSTEM_PROMPT)
    val currentPitch by viewModel.preferences.voicePitchFlow.collectAsState(initial = 1.0f)
    val currentSpeed by viewModel.preferences.voiceSpeedFlow.collectAsState(initial = 1.0f)
    val currentAutoSpeak by viewModel.preferences.autoSpeakFlow.collectAsState(initial = false)
    val currentStream by viewModel.preferences.streamResponseFlow.collectAsState(initial = true)

    var hostInput by remember(currentHost) { mutableStateOf(currentHost) }
    var modelInput by remember(currentModel) { mutableStateOf(currentModel) }
    var sysPromptInput by remember(currentSysPrompt) { mutableStateOf(currentSysPrompt) }

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showSetupGuide by remember { mutableStateOf(false) }

    val copyToClipboard: (String, String) -> Unit = { label, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YuriBackgroundDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YuriBackgroundSurface)
                    .border(1.dp, YuriBorderCyan)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = YuriCyanPrimary
                    )
                }
                Text(
                    text = "SYSTEM CONFIGURATION",
                    color = YuriTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // SECTION 0: BACKGROUND WAKE ENGINE & APK PACKAGE
                SettingsSectionCard(title = "BACKGROUND WAKE ASSISTANT & APK") {
                    Text(
                        text = "• Say 'Yuri' anytime to emerge the floating assistant (just like Siri, Gemini, or Bixby).\n• Can run continuously in the background via foreground service.\n• Can be set as the system Default Digital Assistant in Android Settings > Apps > Default apps > Digital assistant app.",
                        color = YuriTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YuriBackgroundElevated)
                            .border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "APPLICATION PACKAGE (APK):",
                                color = YuriCyanGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Package: com.aistudio.yuriai.xvrq\nVersion: 1.0 (Signed Release & Debug APK)\nGenerated file: app-debug.apk\nYou can export the APK or ZIP anytime from the project settings menu.",
                                color = YuriTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // SECTION 1: Termux & llama.cpp Local Server
                SettingsSectionCard(title = "LOCAL AI ENGINE (TERMUX / LLAMA.CPP)") {
                    Text(
                        text = "Yuri operates fully offline by communicating with llama.cpp running natively on your Android device via Termux.",
                        color = YuriTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = {
                            hostInput = it
                            scope.launch { viewModel.preferences.updateServerHost(it) }
                        },
                        label = { Text("Local API URL / Port", color = YuriCyanPrimary, fontSize = 12.sp) },
                        placeholder = { Text("http://127.0.0.1:8080") },
                        colors = outlinedColors(),
                        modifier = Modifier.fillMaxWidth().testTag("server_host_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = {
                            modelInput = it
                            scope.launch { viewModel.preferences.updateModelName(it) }
                        },
                        label = { Text("GGUF Model Identifier", color = YuriCyanPrimary, fontSize = 12.sp) },
                        placeholder = { Text("local-gguf") },
                        colors = outlinedColors(),
                        modifier = Modifier.fillMaxWidth().testTag("model_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Connection Button & Result
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.checkServerHealth() },
                            colors = ButtonDefaults.buttonColors(containerColor = YuriCyanPrimary),
                            enabled = !isCheckingHealth,
                            modifier = Modifier.testTag("test_connection_button")
                        ) {
                            if (isCheckingHealth) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "TEST CONNECTION",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        // Status indicator
                        serverHealth?.let { health ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (health.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (health.isSuccess) YuriStatusOnline else YuriStatusOffline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (health.isSuccess) "${health.latencyMs}ms" else "FAIL",
                                    color = if (health.isSuccess) YuriStatusOnline else YuriStatusOffline,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Diagnostic info or error
                    serverHealth?.let { health ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (health.isSuccess) YuriBackgroundElevated else Color(0xFF280B14))
                                .border(1.dp, if (health.isSuccess) YuriStatusOnline else YuriStatusOffline, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (health.isSuccess) {
                                    "Connected successfully: ${health.rawDetails ?: "Ready for local inference"}"
                                } else {
                                    health.errorMessage ?: "Connection refused. Termux engine is offline."
                                },
                                color = if (health.isSuccess) YuriStatusOnline else YuriStatusOffline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step-by-Step Termux Setup Guide button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YuriBackgroundElevated)
                            .clickable { showSetupGuide = !showSetupGuide }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = YuriCyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TERMUX SETUP INSTRUCTIONS",
                                color = YuriCyanGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (showSetupGuide) "▲ HIDE" else "▼ SHOW",
                            color = YuriTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (showSetupGuide) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(YuriCodeBackground)
                                .border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1. Install build tools in Termux:",
                                color = YuriTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            CodeSnippetRow(
                                code = "pkg update && pkg install clang cmake make git",
                                onCopy = { copyToClipboard("Command 1", it) }
                            )

                            Text(
                                text = "2. Clone & compile llama.cpp:",
                                color = YuriTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            CodeSnippetRow(
                                code = "git clone https://github.com/ggerganov/llama.cpp && cd llama.cpp && make -j4",
                                onCopy = { copyToClipboard("Command 2", it) }
                            )

                            Text(
                                text = "3. Run llama-server with your GGUF model:",
                                color = YuriTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            CodeSnippetRow(
                                code = "./llama-server -m /sdcard/model.gguf --host 127.0.0.1 --port 8080 -c 2048",
                                onCopy = { copyToClipboard("Command 3", it) }
                            )
                        }
                    }
                }

                // SECTION 2: Inference Parameters
                SettingsSectionCard(title = "INFERENCE PARAMETERS") {
                    // Temperature
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Temperature", color = YuriTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.2f", currentTemp), color = YuriCyanPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = currentTemp,
                        onValueChange = { scope.launch { viewModel.preferences.updateTemperature(it) } },
                        valueRange = 0.1f..1.5f,
                        colors = sliderColors()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Max Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Output Tokens", color = YuriTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("$currentMaxTokens", color = YuriCyanPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = currentMaxTokens.toFloat(),
                        onValueChange = { scope.launch { viewModel.preferences.updateMaxTokens(it.toInt()) } },
                        valueRange = 256f..4096f,
                        steps = 14,
                        colors = sliderColors()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Stream Response Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Real-Time Streaming", color = YuriTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("Stream tokens in real-time as they generate", color = YuriTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = currentStream,
                            onCheckedChange = { scope.launch { viewModel.preferences.updateStreamResponse(it) } },
                            colors = switchColors()
                        )
                    }
                }

                // SECTION 3: Yuri Persona & Directives
                SettingsSectionCard(title = "YURI'S PERSONALITY & DIRECTIVES") {
                    Text(
                        text = "Customize the JARVIS-inspired core system prompt:",
                        color = YuriTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = sysPromptInput,
                        onValueChange = {
                            sysPromptInput = it
                            scope.launch { viewModel.preferences.updateSystemPrompt(it) }
                        },
                        colors = outlinedColors(),
                        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("system_prompt_input"),
                        maxLines = 8
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = {
                            sysPromptInput = YuriPreferences.DEFAULT_SYSTEM_PROMPT
                            scope.launch { viewModel.preferences.updateSystemPrompt(YuriPreferences.DEFAULT_SYSTEM_PROMPT) }
                        }
                    ) {
                        Text("RESET TO OFFICIAL DIRECTIVE", color = YuriCyanPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // SECTION 4: Voice Synthesizer & Speech
                SettingsSectionCard(title = "VOICE & SPEECH SYNTHESIS") {
                    // Auto-speak toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Speak Responses", color = YuriTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("Read Yuri's answers aloud automatically", color = YuriTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = currentAutoSpeak,
                            onCheckedChange = { scope.launch { viewModel.preferences.updateAutoSpeak(it) } },
                            colors = switchColors()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pitch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice Pitch", color = YuriTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.1fx", currentPitch), color = YuriCyanPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = currentPitch,
                        onValueChange = {
                            scope.launch { viewModel.preferences.updateVoicePitch(it) }
                            viewModel.voiceManager.setSpeechParameters(it, currentSpeed)
                        },
                        valueRange = 0.5f..1.8f,
                        colors = sliderColors()
                    )

                    // Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate", color = YuriTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%.1fx", currentSpeed), color = YuriCyanPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = currentSpeed,
                        onValueChange = {
                            scope.launch { viewModel.preferences.updateVoiceSpeed(it) }
                            viewModel.voiceManager.setSpeechParameters(currentPitch, it)
                        },
                        valueRange = 0.5f..1.8f,
                        colors = sliderColors()
                    )
                }

                // SECTION 5: Memory System & Core Facts
                SettingsSectionCard(title = "MEMORY SYSTEM & BACKUP") {
                    Text(
                        text = "User-approved facts and preferences stored locally in Room database:",
                        color = YuriTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (memories.isEmpty()) {
                        Text(
                            text = "No saved memories. Add key facts below for Yuri to remember.",
                            color = YuriTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            memories.forEach { mem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YuriBackgroundElevated)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = mem.key, color = YuriCyanGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text(text = mem.value, color = YuriTextPrimary, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(mem.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = YuriStatusOffline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddMemoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD FACT", color = YuriCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val json = viewModel.exportMemoriesAndChatsJson()
                                    copyToClipboard("Memory Backup", json)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT", color = YuriCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IMPORT", color = YuriCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Reset to Default button
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.preferences.resetDefaults()
                            Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = YuriTextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTORE DEFAULT SETTINGS", color = YuriTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Add Memory Dialog
        if (showAddMemoryDialog) {
            var keyInput by remember { mutableStateOf("") }
            var valInput by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddMemoryDialog = false },
                title = { Text("REMEMBER PREFERENCE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("Fact Title / Key", color = YuriCyanPrimary) },
                            placeholder = { Text("User Name, Favorite Language, etc.") },
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = valInput,
                            onValueChange = { valInput = it },
                            label = { Text("Details / Value", color = YuriCyanPrimary) },
                            placeholder = { Text("e.g. Kotlin, Senior Embedded Engineer") },
                            colors = outlinedColors()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (keyInput.isNotBlank() && valInput.isNotBlank()) {
                                viewModel.addMemory(keyInput, valInput)
                                showAddMemoryDialog = false
                            }
                        }
                    ) {
                        Text("SAVE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemoryDialog = false }) {
                        Text("CANCEL", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface
            )
        }

        // Import Backup Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("RESTORE MEMORY BACKUP", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste your exported JSON backup text below:", color = YuriTextSecondary, fontSize = 12.sp)
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("{\"memories\": [...]}") },
                            colors = outlinedColors(),
                            modifier = Modifier.height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val success = viewModel.importMemoriesJson(importJsonText)
                            if (success) {
                                Toast.makeText(context, "Memories restored successfully", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "Invalid JSON backup format", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("RESTORE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("CANCEL", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface
            )
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(YuriBackgroundCard)
            .border(1.dp, YuriBorderCyan, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = YuriCyanPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
fun CodeSnippetRow(
    code: String,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF020712))
            .border(1.dp, YuriBorderCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onCopy(code) }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            color = YuriCodeText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy command",
            tint = YuriCyanPrimary,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = YuriTextPrimary,
    unfocusedTextColor = YuriTextPrimary,
    focusedBorderColor = YuriCyanPrimary,
    unfocusedBorderColor = YuriBorderCyan,
    focusedContainerColor = YuriBackgroundDeep,
    unfocusedContainerColor = YuriBackgroundDeep
)

@Composable
fun sliderColors() = SliderDefaults.colors(
    thumbColor = YuriCyanPrimary,
    activeTrackColor = YuriCyanPrimary,
    inactiveTrackColor = YuriBorderCyan
)

@Composable
fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.Black,
    checkedTrackColor = YuriCyanPrimary,
    uncheckedThumbColor = YuriTextSecondary,
    uncheckedTrackColor = YuriBackgroundElevated
)
