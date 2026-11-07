package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.MessageEntity
import com.example.ui.theme.YuriBackgroundCard
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.theme.YuriBackgroundElevated
import com.example.ui.theme.YuriBackgroundSurface
import com.example.ui.theme.YuriBluePrimary
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
import com.example.ui.viewmodel.YuriViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: YuriViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val messages by viewModel.currentMessages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeId by viewModel.activeConversationId.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val serverHealth by viewModel.serverHealth.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive or while streaming
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val totalItems = messages.size + if (isStreaming) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceManager.startListening()
        }
    }

    val toggleVoice = {
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

    val copyToClipboard: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Yuri Response", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val activeConvTitle = conversations.find { it.id == activeId }?.title ?: "Chat Terminal"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YuriBackgroundDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YuriBackgroundSurface)
                    .border(width = 1.dp, color = YuriBorderCyan)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = YuriCyanPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHistoryDialog = true }
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = activeConvTitle,
                        color = YuriTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = if (serverHealth?.isSuccess == true) "TERMUX ONLINE • TAP FOR SESSIONS" else "OFFLINE • TAP TO CONFIGURE",
                        color = if (serverHealth?.isSuccess == true) YuriStatusOnline else YuriStatusOffline,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // New Session button
                IconButton(
                    onClick = { viewModel.createNewConversation() },
                    modifier = Modifier.testTag("chat_new_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        tint = YuriCyanPrimary
                    )
                }

                // Sessions List button
                IconButton(
                    onClick = { showHistoryDialog = true },
                    modifier = Modifier.testTag("chat_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Session History",
                        tint = YuriTextSecondary
                    )
                }

                // Clear button
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("chat_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Conversation",
                        tint = YuriTextSecondary
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onCopy = { copyToClipboard(msg.content) },
                        onSpeak = { viewModel.voiceManager.speak(msg.content) }
                    )
                }

                // Live Streaming message if active
                if (isStreaming) {
                    item {
                        StreamingBubble(
                            text = streamingText,
                            onStop = { viewModel.stopGenerating() }
                        )
                    }
                }
            }

            // Input Dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(YuriBackgroundSurface)
                    .border(width = 1.dp, color = YuriBorderCyan)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button
                IconButton(
                    onClick = toggleVoice,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isListening) YuriStatusListening.copy(alpha = 0.2f) else YuriBackgroundCard)
                        .border(1.dp, if (isListening) YuriStatusListening else YuriBorderCyan, CircleShape)
                        .testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (isListening) YuriStatusListening else YuriCyanPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening..." else "Ask Yuri anything...",
                            color = YuriTextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = YuriTextPrimary,
                        unfocusedTextColor = YuriTextPrimary,
                        focusedBorderColor = YuriCyanPrimary,
                        unfocusedBorderColor = YuriBorderCyan,
                        focusedContainerColor = YuriBackgroundDeep,
                        unfocusedContainerColor = YuriBackgroundDeep
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send or Stop Button
                if (isStreaming) {
                    IconButton(
                        onClick = { viewModel.stopGenerating() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(YuriStatusOffline.copy(alpha = 0.2f))
                            .border(1.dp, YuriStatusOffline, CircleShape)
                            .testTag("chat_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Generation",
                            tint = YuriStatusOffline
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) YuriCyanPrimary else YuriBackgroundCard)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.Black else YuriTextSecondary
                        )
                    }
                }
            }
        }

        // Sessions Dialog
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = {
                    Text(
                        text = "CONVERSATION SESSIONS",
                        color = YuriCyanPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations) { conv ->
                            val isSelected = conv.id == activeId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) YuriBackgroundElevated else YuriBackgroundCard)
                                    .border(1.dp, if (isSelected) YuriCyanPrimary else YuriBorderCyan, RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectConversation(conv.id)
                                        showHistoryDialog = false
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        color = if (isSelected) YuriCyanGlow else YuriTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(conv.updatedAt)),
                                        color = YuriTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteConversation(conv.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        tint = YuriStatusOffline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.createNewConversation()
                            showHistoryDialog = false
                        }
                    ) {
                        Text("NEW SESSION", color = YuriCyanPrimary, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("CLOSE", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Clear Dialog
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = {
                    Text(
                        text = "PURGE SESSION MESSAGES?",
                        color = YuriStatusOffline,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                },
                text = {
                    Text(
                        text = "This will erase all messages in this conversation from the local Room database.",
                        color = YuriTextPrimary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearCurrentConversation()
                            showClearDialog = false
                        }
                    ) {
                        Text("PURGE", color = YuriStatusOffline, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("CANCEL", color = YuriTextSecondary, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = YuriBackgroundSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role & Timestamp header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isUser) "COMMANDER" else "YURI",
                color = if (isUser) YuriBluePrimary else YuriCyanPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = timeStr,
                color = YuriTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Message Content Bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.horizontalGradient(listOf(Color(0xFF0F3260), Color(0xFF13417C)))
                    } else {
                        Brush.horizontalGradient(listOf(YuriBackgroundCard, YuriBackgroundSurface))
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) YuriBluePrimary.copy(alpha = 0.5f) else YuriBorderCyan,
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 14.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                RenderMarkdownContent(content = message.content)

                // Yuri action footer (copy, speak)
                if (!isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = YuriTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = YuriTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingBubble(
    text: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingWave")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingAlpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "YURI [SYNTHESIZING]",
                color = YuriStatusThinking,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(YuriStatusThinking.copy(alpha = dotAlpha))
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 2.dp))
                .background(YuriBackgroundCard)
                .border(1.dp, YuriStatusThinking.copy(alpha = 0.6f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 2.dp))
                .padding(12.dp)
        ) {
            Column {
                if (text.isNotBlank()) {
                    RenderMarkdownContent(content = text)
                } else {
                    Text(
                        text = "Thinking and generating local neural tokens...",
                        color = YuriTextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "STOP",
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
}

@Composable
fun RenderMarkdownContent(content: String) {
    // Parse code blocks vs regular text
    val parts = content.split("```")
    for (i in parts.indices) {
        val segment = parts[i]
        if (i % 2 == 1) {
            // Code block
            val lines = segment.trim().lines()
            val language = if (lines.isNotEmpty() && lines[0].matches(Regex("^[a-zA-Z0-9#+_-]+$"))) {
                lines[0]
            } else {
                "code"
            }
            val codeBody = if (language != "code" && lines.size > 1) {
                lines.drop(1).joinToString("\n")
            } else {
                segment.trim()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(YuriCodeBackground)
                    .border(1.dp, YuriBorderCyan, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = language.uppercase(Locale.ROOT),
                            color = YuriCyanGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = codeBody,
                        color = YuriCodeText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Regular text with basic formatting
            val cleanText = segment.trim()
            if (cleanText.isNotEmpty()) {
                Text(
                    text = cleanText,
                    color = YuriTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
