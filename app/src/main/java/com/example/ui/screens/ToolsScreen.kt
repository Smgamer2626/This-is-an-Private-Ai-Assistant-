package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YuriBackgroundCard
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.theme.YuriBackgroundElevated
import com.example.ui.theme.YuriBackgroundSurface
import com.example.ui.theme.YuriBorderCyan
import com.example.ui.theme.YuriCodeBackground
import com.example.ui.theme.YuriCodeText
import com.example.ui.theme.YuriCyanGlow
import com.example.ui.theme.YuriCyanPrimary
import com.example.ui.theme.YuriStatusOffline
import com.example.ui.theme.YuriStatusOnline
import com.example.ui.theme.YuriTextPrimary
import com.example.ui.theme.YuriTextSecondary
import com.example.ui.viewmodel.YuriViewModel
import java.util.Locale
import kotlin.math.sqrt

enum class ToolModule(val label: String) {
    CALCULATOR("CALCULATOR"),
    NOTES("NOTES"),
    PROGRAMMING("CODE ASSIST"),
    ELECTRONICS("ELECTRONICS"),
    SYSTEM_INFO("DIAGNOSTICS")
}

@Composable
fun ToolsScreen(
    viewModel: YuriViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedModule by remember { mutableStateOf(ToolModule.CALCULATOR) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YuriBackgroundDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YuriBackgroundSurface)
                    .border(1.dp, YuriBorderCyan)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = YuriCyanPrimary
                    )
                }
                Text(
                    text = "HUD SUBSYSTEMS & MODULES",
                    color = YuriTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Subsystem Tab Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YuriBackgroundSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolModule.values().forEach { module ->
                    val isSelected = selectedModule == module
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) YuriCyanPrimary else YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
                            .clickable { selectedModule = module }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("tab_${module.name.lowercase()}")
                    ) {
                        Text(
                            text = module.label,
                            color = if (isSelected) Color.Black else YuriTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Subsystem Content View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                when (selectedModule) {
                    ToolModule.CALCULATOR -> CalculatorSubsystem()
                    ToolModule.NOTES -> NotesSubsystem(
                        viewModel = viewModel,
                        onAskYuri = { noteText ->
                            viewModel.sendMessage("Review and analyze this directive: $noteText")
                            onNavigateToChat()
                        }
                    )
                    ToolModule.PROGRAMMING -> ProgrammingSubsystem(
                        onSendCodeToYuri = { prompt ->
                            viewModel.sendMessage(prompt)
                            onNavigateToChat()
                        }
                    )
                    ToolModule.ELECTRONICS -> ElectronicsSubsystem()
                    ToolModule.SYSTEM_INFO -> SystemDiagnosticsSubsystem(viewModel = viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 1: SCI-FI / PROGRAMMER CALCULATOR
// -------------------------------------------------------------
@Composable
fun CalculatorSubsystem() {
    var displayExpr by remember { mutableStateOf("") }
    var displayResult by remember { mutableStateOf("0") }

    fun evaluateSimple(expr: String) {
        try {
            val clean = expr.replace("×", "*").replace("÷", "/")
            if (clean.isBlank()) {
                displayResult = "0"
                return
            }
            // Simple arithmetic evaluator
            val res = evaluateMath(clean)
            displayResult = if (res % 1.0 == 0.0) res.toLong().toString() else String.format(Locale.US, "%.4f", res)
        } catch (_: Exception) {
            displayResult = "SYNTAX ERROR"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Display Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(YuriCodeBackground)
                .border(1.dp, YuriBorderCyan, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = displayExpr.ifEmpty { "ENTER EXPRESSION" },
                    color = YuriTextSecondary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayResult,
                    color = YuriCyanGlow,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                // Programmer HEX & BIN conversion if integer
                displayResult.toLongOrNull()?.let { num ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HEX: 0x${num.toString(16).uppercase(Locale.ROOT)} • BIN: ${num.toString(2)}",
                        color = YuriStatusOnline,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Keypad Grid
        val buttonRows = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "DEL", "=")
        )

        buttonRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isAction = key in listOf("=", "÷", "×", "-", "+")
                    val isClear = key in listOf("C", "DEL")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    key == "=" -> YuriCyanPrimary
                                    isAction -> YuriBackgroundElevated
                                    isClear -> Color(0xFF2B1019)
                                    else -> YuriBackgroundCard
                                }
                            )
                            .border(1.dp, YuriBorderCyan, RoundedCornerShape(10.dp))
                            .clickable {
                                when (key) {
                                    "C" -> {
                                        displayExpr = ""
                                        displayResult = "0"
                                    }
                                    "DEL" -> {
                                        if (displayExpr.isNotEmpty()) displayExpr = displayExpr.dropLast(1)
                                    }
                                    "=" -> evaluateSimple(displayExpr)
                                    else -> displayExpr += key
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = when {
                                key == "=" -> Color.Black
                                isAction -> YuriCyanGlow
                                isClear -> YuriStatusOffline
                                else -> YuriTextPrimary
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private fun evaluateMath(expr: String): Double {
    val tokens = expr.replace(" ", "")
    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < tokens.length) tokens[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < tokens.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = tokens.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }
            return x
        }
    }.parse()
}

// -------------------------------------------------------------
// MODULE 2: NOTES & DIRECTIVES
// -------------------------------------------------------------
@Composable
fun NotesSubsystem(
    viewModel: YuriViewModel,
    onAskYuri: (String) -> Unit
) {
    val notes by viewModel.userNotes.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED DIRECTIVES & NOTES (${notes.size})",
                color = YuriCyanPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = YuriCyanPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NEW NOTE", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No notes recorded yet.\nTap NEW NOTE to save local directives.",
                    color = YuriTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notes, key = { it.id }) { note ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(YuriBackgroundCard)
                            .border(1.dp, YuriBorderCyan, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.title,
                                color = YuriCyanGlow,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            IconButton(
                                onClick = { viewModel.deleteNote(note.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = YuriStatusOffline, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            color = YuriTextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onAskYuri("${note.title}: ${note.content}") }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ANALYZE WITH YURI", color = YuriCyanPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("NEW DIRECTIVE / NOTE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Title", color = YuriCyanPrimary) },
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = contentInput,
                            onValueChange = { contentInput = it },
                            label = { Text("Content / Notes", color = YuriCyanPrimary) },
                            colors = outlinedColors(),
                            modifier = Modifier.height(140.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (titleInput.isNotBlank() && contentInput.isNotBlank()) {
                                viewModel.addNote(titleInput, contentInput)
                                titleInput = ""
                                contentInput = ""
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("SAVE NOTE", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("CANCEL", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface
            )
        }
    }
}

// -------------------------------------------------------------
// MODULE 3: PROGRAMMING ASSISTANT
// -------------------------------------------------------------
@Composable
fun ProgrammingSubsystem(
    onSendCodeToYuri: (String) -> Unit
) {
    var codeSnippet by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Python") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "CODE ANALYSIS & ASSISTANT",
            color = YuriCyanPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Text(
            text = "Paste code snippet below and dispatch directly to Yuri for offline review.",
            color = YuriTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        // Language selection chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Python", "Kotlin", "C / C++", "JavaScript", "Rust", "SQL").forEach { lang ->
                val isSel = selectedLanguage == lang
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) YuriCyanPrimary else YuriBackgroundCard)
                        .clickable { selectedLanguage = lang }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lang,
                        color = if (isSel) Color.Black else YuriTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        OutlinedTextField(
            value = codeSnippet,
            onValueChange = { codeSnippet = it },
            placeholder = { Text("def solve_problem():\n    # Paste code here\n    pass", color = YuriTextSecondary) },
            colors = outlinedColors(),
            modifier = Modifier.fillMaxWidth().height(180.dp),
            maxLines = 10
        )

        // Action Prompts
        Text(
            text = "DISPATCH DIRECTIVE TO YURI:",
            color = YuriTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        val actions = listOf(
            "Explain How This Code Works",
            "Detect Bugs & Security Vulnerabilities",
            "Optimize Time & Space Complexity",
            "Generate Unit Tests",
            "Refactor to Idiomatic Style"
        )

        actions.forEach { action ->
            Button(
                onClick = {
                    val prompt = "$action for the following $selectedLanguage code:\n\n```$selectedLanguage\n$codeSnippet\n```"
                    onSendCodeToYuri(prompt)
                },
                enabled = codeSnippet.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = YuriBackgroundElevated),
                modifier = Modifier.fillMaxWidth().border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = YuriCyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(action, color = YuriCyanGlow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// -------------------------------------------------------------
// MODULE 4: ELECTRONICS LEARNING ASSISTANT
// -------------------------------------------------------------
@Composable
fun ElectronicsSubsystem() {
    var voltageInput by remember { mutableStateOf("5.0") }
    var currentInput by remember { mutableStateOf("0.02") }
    var resistanceInput by remember { mutableStateOf("250.0") }

    // LED series resistor calculator
    var sourceVoltage by remember { mutableStateOf("5.0") }
    var ledForwardVoltage by remember { mutableStateOf("2.0") } // Red/Green LED
    var ledCurrentMa by remember { mutableStateOf("20.0") } // 20mA

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Ohm's Law Calculator
        SettingsSectionCard(title = "OHM'S LAW CALCULATOR (V = I × R)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = voltageInput,
                    onValueChange = { voltageInput = it },
                    label = { Text("Voltage (V)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    label = { Text("Current (A)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = resistanceInput,
                    onValueChange = { resistanceInput = it },
                    label = { Text("Resistance (Ω)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            val v = voltageInput.toDoubleOrNull() ?: 0.0
            val i = currentInput.toDoubleOrNull() ?: 0.0
            val power = v * i

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Calculated Power Dissipation: ${String.format(Locale.US, "%.3f", power)} Watts (P = V × I)",
                color = YuriCyanGlow,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Section: LED Series Resistor Calculator
        SettingsSectionCard(title = "LED CURRENT LIMITING RESISTOR") {
            Text(
                text = "Formula: R = (V_source - V_forward) / I_led",
                color = YuriTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sourceVoltage,
                    onValueChange = { sourceVoltage = it },
                    label = { Text("V Source (e.g. 5V)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ledForwardVoltage,
                    onValueChange = { ledForwardVoltage = it },
                    label = { Text("LED V_f (e.g. 2V)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ledCurrentMa,
                    onValueChange = { ledCurrentMa = it },
                    label = { Text("LED I (mA)", fontSize = 10.sp) },
                    colors = outlinedColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            val vs = sourceVoltage.toDoubleOrNull() ?: 5.0
            val vf = ledForwardVoltage.toDoubleOrNull() ?: 2.0
            val ima = ledCurrentMa.toDoubleOrNull() ?: 20.0
            val rOhms = if (ima > 0 && vs > vf) (vs - vf) / (ima / 1000.0) else 0.0

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Required Resistor: ${String.format(Locale.US, "%.1f", rOhms)} Ω (Standard: ~${nearestStandardResistor(rOhms)} Ω)",
                color = YuriStatusOnline,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Section: Microcontroller Quick Reference
        SettingsSectionCard(title = "MICROCONTROLLER HARDWARE SPECS") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MicrocontrollerCard(
                    name = "ESP32 (WROOM-32)",
                    cpu = "Xtensa Dual-Core 32-bit LX6 @ 240MHz",
                    ram = "520 KB SRAM • 4MB Flash",
                    wireless = "Wi-Fi 802.11 b/g/n + Bluetooth 4.2 BLE",
                    gpio = "36 GPIO pins • 12-bit ADC • DAC • PWM"
                )
                MicrocontrollerCard(
                    name = "Raspberry Pi Pico (RP2040)",
                    cpu = "Dual ARM Cortex-M0+ @ 133MHz",
                    ram = "264 KB SRAM • 2MB Flash",
                    wireless = "Pico W adds 2.4GHz Wi-Fi / BLE",
                    gpio = "26 multi-function GPIO • 8 × PIO state machines"
                )
                MicrocontrollerCard(
                    name = "Arduino Uno (ATmega328P)",
                    cpu = "8-bit AVR RISC @ 16MHz",
                    ram = "2 KB SRAM • 32 KB Flash • 1 KB EEPROM",
                    wireless = "Requires external shield",
                    gpio = "14 Digital I/O (6 PWM) • 6 Analog In"
                )
            }
        }
    }
}

private fun nearestStandardResistor(ohms: Double): Int {
    val standardE12 = listOf(10, 12, 15, 18, 22, 27, 33, 39, 47, 56, 68, 82, 100, 120, 150, 180, 220, 270, 330, 390, 470, 560, 680, 820, 1000)
    return standardE12.minByOrNull { kotlin.math.abs(it - ohms) } ?: 220
}

@Composable
fun MicrocontrollerCard(
    name: String,
    cpu: String,
    ram: String,
    wireless: String,
    gpio: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(YuriBackgroundElevated)
            .border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(text = name, color = YuriCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "CPU: $cpu", color = YuriTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Memory: $ram", color = YuriTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = "Radio: $wireless", color = YuriTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = "I/O: $gpio", color = YuriTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

// -------------------------------------------------------------
// MODULE 5: SYSTEM DIAGNOSTICS DEEP DIVE
// -------------------------------------------------------------
@Composable
fun SystemDiagnosticsSubsystem(viewModel: YuriViewModel) {
    val battery by viewModel.batteryInfo.collectAsState()
    val report by viewModel.systemReport.collectAsState()

    val ramUsedGb = report.ram.usedBytes / (1024f * 1024f * 1024f)
    val ramTotalGb = report.ram.totalBytes / (1024f * 1024f * 1024f)
    val storageFreeGb = report.storage.freeBytes / (1024f * 1024f * 1024f)
    val storageTotalGb = report.storage.totalBytes / (1024f * 1024f * 1024f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "HARDWARE TELEMETRY & DIAGNOSTICS",
            color = YuriCyanPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        SettingsSectionCard(title = "BATTERY SUBSYSTEM") {
            TelemetryRow(label = "Charge Level", value = "${battery.percentage}%")
            TelemetryRow(label = "Power State", value = if (battery.isCharging) "Charging (${battery.plugType})" else "Discharging")
            TelemetryRow(label = "Internal Temperature", value = "${battery.temperatureCelsius} °C")
            TelemetryRow(label = "Cell Voltage", value = "${battery.voltageMv} mV")
            TelemetryRow(label = "Health Status", value = battery.statusText)
        }

        SettingsSectionCard(title = "MEMORY & STORAGE SUBSYSTEM") {
            TelemetryRow(label = "System RAM Utilization", value = "${report.ram.percentageUsed}% (%.1f / %.1f GB)".format(Locale.US, ramUsedGb, ramTotalGb))
            TelemetryRow(label = "Internal Storage Free", value = "%.1f GB free of %.1f GB".format(Locale.US, storageFreeGb, storageTotalGb))
        }

        SettingsSectionCard(title = "PROCESSOR & HOST OS") {
            TelemetryRow(label = "CPU Core Count", value = "${report.cpu.coreCount} Processors")
            TelemetryRow(label = "Instruction Architecture", value = report.cpu.architecture)
            TelemetryRow(label = "Live CPU Load Estimate", value = "${report.cpu.estimatedUsagePercent}%")
            TelemetryRow(label = "Device Hardware", value = report.cpu.hardwareModel)
            TelemetryRow(label = "System Uptime", value = "${report.uptimeSeconds / 3600}h ${(report.uptimeSeconds % 3600) / 60}m")
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = YuriTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = YuriTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
