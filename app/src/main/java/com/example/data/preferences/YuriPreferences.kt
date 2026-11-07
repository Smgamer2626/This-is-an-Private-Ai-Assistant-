package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yuri_preferences")

class YuriPreferences(private val context: Context) {

    companion object {
        val KEY_SERVER_HOST = stringPreferencesKey("server_host")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_SPEED = floatPreferencesKey("voice_speed")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_STREAM_RESPONSE = booleanPreferencesKey("stream_response")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")

        const val DEFAULT_SERVER_HOST = "http://127.0.0.1:8080"
        const val DEFAULT_MODEL_NAME = "local-gguf"
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MAX_TOKENS = 2048
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Yuri, a personal AI assistant with a JARVIS-inspired personality. You are intelligent, respectful, concise, curious and technically knowledgeable. You specialize in mathematics, science, electronics, programming, inventions and general assistance. Explain complicated concepts clearly. Never pretend to have access to devices, files or information you cannot access."
    }

    val serverHostFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_SERVER_HOST] ?: DEFAULT_SERVER_HOST
    }

    val modelNameFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME
    }

    val temperatureFlow: Flow<Float> = context.dataStore.data.map {
        it[KEY_TEMPERATURE] ?: DEFAULT_TEMPERATURE
    }

    val maxTokensFlow: Flow<Int> = context.dataStore.data.map {
        it[KEY_MAX_TOKENS] ?: DEFAULT_MAX_TOKENS
    }

    val systemPromptFlow: Flow<String> = context.dataStore.data.map {
        it[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
    }

    val voicePitchFlow: Flow<Float> = context.dataStore.data.map {
        it[KEY_VOICE_PITCH] ?: 1.0f
    }

    val voiceSpeedFlow: Flow<Float> = context.dataStore.data.map {
        it[KEY_VOICE_SPEED] ?: 1.0f
    }

    val autoSpeakFlow: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_AUTO_SPEAK] ?: false
    }

    val streamResponseFlow: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_STREAM_RESPONSE] ?: true
    }

    val wakeWordEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_WAKE_WORD_ENABLED] ?: false
    }

    val floatingBubbleEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_FLOATING_BUBBLE_ENABLED] ?: false
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateFloatingBubbleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLOATING_BUBBLE_ENABLED] = enabled }
    }

    suspend fun updateServerHost(host: String) {
        context.dataStore.edit { it[KEY_SERVER_HOST] = host.trim() }
    }

    suspend fun updateModelName(model: String) {
        context.dataStore.edit { it[KEY_MODEL_NAME] = model.trim() }
    }

    suspend fun updateTemperature(temp: Float) {
        context.dataStore.edit { it[KEY_TEMPERATURE] = temp }
    }

    suspend fun updateMaxTokens(tokens: Int) {
        context.dataStore.edit { it[KEY_MAX_TOKENS] = tokens }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { it[KEY_SYSTEM_PROMPT] = prompt.trim() }
    }

    suspend fun updateVoicePitch(pitch: Float) {
        context.dataStore.edit { it[KEY_VOICE_PITCH] = pitch }
    }

    suspend fun updateVoiceSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_VOICE_SPEED] = speed }
    }

    suspend fun updateAutoSpeak(autoSpeak: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPEAK] = autoSpeak }
    }

    suspend fun updateStreamResponse(stream: Boolean) {
        context.dataStore.edit { it[KEY_STREAM_RESPONSE] = stream }
    }

    suspend fun resetDefaults() {
        context.dataStore.edit {
            it[KEY_SERVER_HOST] = DEFAULT_SERVER_HOST
            it[KEY_MODEL_NAME] = DEFAULT_MODEL_NAME
            it[KEY_TEMPERATURE] = DEFAULT_TEMPERATURE
            it[KEY_MAX_TOKENS] = DEFAULT_MAX_TOKENS
            it[KEY_SYSTEM_PROMPT] = DEFAULT_SYSTEM_PROMPT
            it[KEY_VOICE_PITCH] = 1.0f
            it[KEY_VOICE_SPEED] = 1.0f
            it[KEY_AUTO_SPEAK] = false
            it[KEY_STREAM_RESPONSE] = true
        }
    }
}
