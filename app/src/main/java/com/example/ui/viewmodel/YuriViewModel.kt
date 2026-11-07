package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.YuriDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.network.ChatMessagePayload
import com.example.data.network.LocalLlamaClient
import com.example.data.network.ServerHealthResult
import com.example.data.preferences.YuriPreferences
import com.example.data.system.BatteryTelemetry
import com.example.data.system.SystemStatusReport
import com.example.data.system.SystemTelemetryProvider
import com.example.data.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class YuriStatus {
    ONLINE,
    LISTENING,
    THINKING,
    SPEAKING,
    OFFLINE
}

class YuriViewModel(application: Application) : AndroidViewModel(application) {

    private val database = YuriDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    private val memoryDao = database.memoryDao()
    private val noteDao = database.noteDao()

    val preferences = YuriPreferences(application)
    private val llamaClient = LocalLlamaClient()
    private val telemetryProvider = SystemTelemetryProvider(application)
    val voiceManager = VoiceManager(application)

    private val _status = MutableStateFlow(YuriStatus.ONLINE)
    val status: StateFlow<YuriStatus> = _status.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _serverHealth = MutableStateFlow<ServerHealthResult?>(null)
    val serverHealth: StateFlow<ServerHealthResult?> = _serverHealth.asStateFlow()

    private val _isCheckingHealth = MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth.asStateFlow()

    private val _systemReport = MutableStateFlow(SystemStatusReport())
    val systemReport: StateFlow<SystemStatusReport> = _systemReport.asStateFlow()

    private val _batteryInfo = MutableStateFlow(BatteryTelemetry())
    val batteryInfo: StateFlow<BatteryTelemetry> = _batteryInfo.asStateFlow()

    private val _userNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val userNotes: StateFlow<List<NoteEntity>> = _userNotes.asStateFlow()

    private val _userMemories = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val userMemories: StateFlow<List<MemoryEntity>> = _userMemories.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = chatDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private var activeStreamJob: Job? = null

    init {
        // Collect active conversation messages
        viewModelScope.launch {
            _activeConversationId.collectLatest { convId ->
                if (convId != null) {
                    chatDao.getMessagesForConversation(convId).collectLatest { msgs ->
                        _currentMessages.value = msgs
                    }
                } else {
                    _currentMessages.value = emptyList()
                }
            }
        }

        // Initialize or create initial conversation
        viewModelScope.launch {
            conversations.collectLatest { list ->
                if (list.isNotEmpty() && _activeConversationId.value == null) {
                    _activeConversationId.value = list.first().id
                } else if (list.isEmpty() && _activeConversationId.value == null) {
                    createDefaultConversation()
                }
            }
        }

        // Collect notes and memories
        viewModelScope.launch {
            noteDao.getAllNotes().collectLatest { _userNotes.value = it }
        }
        viewModelScope.launch {
            memoryDao.getAllMemories().collectLatest { _userMemories.value = it }
        }

        // Observe hardware telemetry
        viewModelScope.launch {
            telemetryProvider.observeBattery().collectLatest {
                _batteryInfo.value = it
            }
        }
        viewModelScope.launch {
            telemetryProvider.observeTelemetryPolling().collectLatest {
                _systemReport.value = it
            }
        }

        // Observe speech events
        voiceManager.onFinalTranscript = { transcript ->
            if (transcript.isNotBlank()) {
                sendMessage(transcript)
            }
        }

        // Track voice speaking state
        viewModelScope.launch {
            combine(voiceManager.isListening, voiceManager.isSpeaking, _isStreaming) { listening, speaking, streaming ->
                when {
                    listening -> YuriStatus.LISTENING
                    streaming -> YuriStatus.THINKING
                    speaking -> YuriStatus.SPEAKING
                    _serverHealth.value?.isSuccess == false -> YuriStatus.OFFLINE
                    else -> YuriStatus.ONLINE
                }
            }.collectLatest { newStatus ->
                _status.value = newStatus
            }
        }

        // Check Termux server health on launch
        checkServerHealth()
    }

    private suspend fun createDefaultConversation(): String {
        val id = UUID.randomUUID().toString()
        val defaultConv = ConversationEntity(
            id = id,
            title = "YURI Primary Directives",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertConversation(defaultConv)
        // Add initial greeting from Yuri
        val greeting = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = id,
            role = "assistant",
            content = "Hello! I am Yuri, your personal offline AI assistant. All local systems are operational. How can I assist you with programming, electronics, science, or analysis today?",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(greeting)
        _activeConversationId.value = id
        return id
    }

    fun selectConversation(id: String) {
        _activeConversationId.value = id
    }

    fun createNewConversation(title: String = "New Session") {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val conv = ConversationEntity(
                id = id,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatDao.insertConversation(conv)
            _activeConversationId.value = id
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatDao.deleteConversation(id)
            if (_activeConversationId.value == id) {
                val remaining = conversations.value.filter { it.id != id }
                _activeConversationId.value = remaining.firstOrNull()?.id
            }
        }
    }

    fun clearCurrentConversation() {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            chatDao.deleteMessagesForConversation(convId)
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            var host = YuriPreferences.DEFAULT_SERVER_HOST
            preferences.serverHostFlow.collectLatest { host = it }
            val result = llamaClient.checkHealth(host)
            _serverHealth.value = result
            _isCheckingHealth.value = false
            if (!result.isSuccess && _status.value != YuriStatus.LISTENING && _status.value != YuriStatus.THINKING) {
                _status.value = YuriStatus.OFFLINE
            } else if (result.isSuccess && _status.value == YuriStatus.OFFLINE) {
                _status.value = YuriStatus.ONLINE
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        viewModelScope.launch {
            var convId = _activeConversationId.value
            if (convId == null) {
                convId = createDefaultConversation()
            }

            // Save user message
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "user",
                content = userText.trim(),
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(userMsg)

            // Update conversation title if first user message
            val existing = chatDao.getMessagesList(convId)
            if (existing.size <= 2) {
                val summaryTitle = if (userText.length > 28) userText.take(28) + "..." else userText
                chatDao.updateConversation(
                    ConversationEntity(
                        id = convId,
                        title = summaryTitle,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Gather context & preferences
            var host = YuriPreferences.DEFAULT_SERVER_HOST
            var model = YuriPreferences.DEFAULT_MODEL_NAME
            var temp = YuriPreferences.DEFAULT_TEMPERATURE
            var maxTokens = YuriPreferences.DEFAULT_MAX_TOKENS
            var sysPrompt = YuriPreferences.DEFAULT_SYSTEM_PROMPT
            var stream = true
            var autoSpeak = false

            preferences.serverHostFlow.collectLatest { host = it }
            preferences.modelNameFlow.collectLatest { model = it }
            preferences.temperatureFlow.collectLatest { temp = it }
            preferences.maxTokensFlow.collectLatest { maxTokens = it }
            preferences.systemPromptFlow.collectLatest { sysPrompt = it }
            preferences.streamResponseFlow.collectLatest { stream = it }
            preferences.autoSpeakFlow.collectLatest { autoSpeak = it }

            // Include user memories if any exist
            val memories = memoryDao.getAllMemoriesList()
            val effectiveSystemPrompt = if (memories.isNotEmpty()) {
                val memorySummary = memories.joinToString("; ") { "${it.key}: ${it.value}" }
                "$sysPrompt\n\n[User Memories & Preferences]: $memorySummary"
            } else {
                sysPrompt
            }

            // Prepare recent message context (last 10 messages)
            val history = chatDao.getMessagesList(convId).takeLast(10).map {
                ChatMessagePayload(role = it.role, content = it.content)
            }

            _isStreaming.value = true
            _streamingText.value = ""
            _status.value = YuriStatus.THINKING

            val responseBuilder = StringBuilder()

            activeStreamJob?.cancel()
            activeStreamJob = launch {
                try {
                    if (stream) {
                        llamaClient.streamChat(
                            rawHost = host,
                            modelName = model,
                            messages = history,
                            systemInstruction = effectiveSystemPrompt,
                            temperature = temp,
                            maxTokens = maxTokens
                        ).collect { token ->
                            responseBuilder.append(token)
                            _streamingText.value = responseBuilder.toString()
                        }
                    } else {
                        val reply = llamaClient.generateChatDirect(
                            rawHost = host,
                            modelName = model,
                            messages = history,
                            systemInstruction = effectiveSystemPrompt,
                            temperature = temp,
                            maxTokens = maxTokens
                        )
                        responseBuilder.append(reply)
                        _streamingText.value = reply
                    }

                    val finalContent = responseBuilder.toString().ifBlank {
                        "Operation complete. No textual output produced by the local inference engine."
                    }

                    // Save assistant message to Room
                    val assistantMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = convId,
                        role = "assistant",
                        content = finalContent,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(assistantMsg)

                    // Auto speak if enabled
                    if (autoSpeak) {
                        voiceManager.speak(finalContent)
                    }
                } catch (e: Exception) {
                    val errorAdvice = buildErrorMessage(e, host)
                    val errorMsg = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        conversationId = convId,
                        role = "assistant",
                        content = errorAdvice,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(errorMsg)
                } finally {
                    _isStreaming.value = false
                    _streamingText.value = ""
                    _status.value = YuriStatus.ONLINE
                }
            }
        }
    }

    private fun buildErrorMessage(e: Exception, host: String): String {
        return """
            ⚠️ **Local AI Engine Communication Failure**
            
            Unable to connect to the local inference engine at `$host`.
            
            **Diagnostic:**
            ${e.localizedMessage ?: e.javaClass.simpleName}
            
            **Troubleshooting Steps for Termux:**
            1. Open Termux on this device.
            2. Verify `llama-server` is running:
               ```bash
               ./llama-server -m /path/to/your/model.gguf --host 127.0.0.1 --port 8080 -c 2048
               ```
            3. Ensure the address and port in **Settings** match your Termux server.
            4. Tap **Test Connection** in Settings to verify connectivity.
        """.trimIndent()
    }

    fun stopGenerating() {
        activeStreamJob?.cancel()
        _isStreaming.value = false
        val currentText = _streamingText.value
        val convId = _activeConversationId.value
        if (currentText.isNotBlank() && convId != null) {
            viewModelScope.launch {
                val partialMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    role = "assistant",
                    content = "$currentText\n\n*(Generation halted by user)*",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(partialMsg)
                _streamingText.value = ""
            }
        }
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    // Memory operations
    fun addMemory(key: String, value: String, category: String = "general") {
        viewModelScope.launch {
            val mem = MemoryEntity(
                id = UUID.randomUUID().toString(),
                key = key.trim(),
                value = value.trim(),
                category = category.trim(),
                timestamp = System.currentTimeMillis()
            )
            memoryDao.insertMemory(mem)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryDao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryDao.clearAllMemories()
        }
    }

    suspend fun exportMemoriesAndChatsJson(): String {
        val root = JSONObject()
        val mems = memoryDao.getAllMemoriesList()
        val memArr = JSONArray()
        for (m in mems) {
            memArr.put(JSONObject().apply {
                put("key", m.key)
                put("value", m.value)
                put("category", m.category)
                put("timestamp", m.timestamp)
            })
        }
        root.put("memories", memArr)

        val convs = chatDao.getAllConversations()
        val convArr = JSONArray()
        // export active session messages
        val activeId = _activeConversationId.value
        if (activeId != null) {
            val msgs = chatDao.getMessagesList(activeId)
            val msgArr = JSONArray()
            for (msg in msgs) {
                msgArr.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                    put("timestamp", msg.timestamp)
                })
            }
            root.put("active_messages", msgArr)
        }
        return root.toString(2)
    }

    fun importMemoriesJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val memArr = root.optJSONArray("memories")
            if (memArr != null) {
                viewModelScope.launch {
                    val list = mutableListOf<MemoryEntity>()
                    for (i in 0 until memArr.length()) {
                        val item = memArr.getJSONObject(i)
                        list.add(
                            MemoryEntity(
                                id = UUID.randomUUID().toString(),
                                key = item.optString("key", "Fact"),
                                value = item.optString("value", ""),
                                category = item.optString("category", "general"),
                                timestamp = item.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    memoryDao.insertMemories(list)
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    // Notes operations
    fun addNote(title: String, content: String, tag: String = "Directive") {
        viewModelScope.launch {
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                content = content.trim(),
                tag = tag.trim(),
                timestamp = System.currentTimeMillis()
            )
            noteDao.insertNote(note)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            noteDao.deleteNote(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
