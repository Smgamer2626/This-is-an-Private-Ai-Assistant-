package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ServerHealthResult(
    val isSuccess: Boolean,
    val latencyMs: Long = 0,
    val modelName: String? = null,
    val errorMessage: String? = null,
    val rawDetails: String? = null
)

data class ChatMessagePayload(
    val role: String,
    val content: String
)

class LocalLlamaClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun normalizeHost(host: String): String {
        var trimmed = host.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://$trimmed"
        }
        return trimmed.removeSuffix("/")
    }

    suspend fun checkHealth(rawHost: String): ServerHealthResult = withContext(Dispatchers.IO) {
        val base = normalizeHost(rawHost)
        val startTime = System.currentTimeMillis()

        // 1. Try /v1/models (OpenAI style standard)
        try {
            val request = Request.Builder()
                .url("$base/v1/models")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    var detectedModel: String? = null
                    try {
                        val json = JSONObject(body)
                        val dataArr = json.optJSONArray("data")
                        if (dataArr != null && dataArr.length() > 0) {
                            detectedModel = dataArr.getJSONObject(0).optString("id")
                        }
                    } catch (_: Exception) {}

                    return@withContext ServerHealthResult(
                        isSuccess = true,
                        latencyMs = elapsed,
                        modelName = detectedModel ?: "llama.cpp local model",
                        rawDetails = "Connected: HTTP ${response.code} (v1/models)"
                    )
                }
            }
        } catch (_: Exception) {
            // continue to fallback check
        }

        // 2. Try /health (native llama.cpp server health endpoint)
        try {
            val request = Request.Builder()
                .url("$base/health")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful || response.code == 200 || response.code == 503) {
                    val body = response.body?.string() ?: ""
                    return@withContext ServerHealthResult(
                        isSuccess = response.code == 200,
                        latencyMs = elapsed,
                        modelName = "llama.cpp server",
                        rawDetails = "Status ${response.code}: $body"
                    )
                }
            }
        } catch (_: Exception) {
            // continue to props check
        }

        // 3. Try /props (llama.cpp server properties)
        try {
            val request = Request.Builder()
                .url("$base/props")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    return@withContext ServerHealthResult(
                        isSuccess = true,
                        latencyMs = elapsed,
                        modelName = "llama.cpp server",
                        rawDetails = "Connected: HTTP ${response.code} (/props)"
                    )
                }
            }
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Failed to connect", ignoreCase = true) == true ||
                e.message?.contains("Connection refused", ignoreCase = true) == true ->
                    "Connection refused at $base. Please ensure Termux is running and 'llama-server' is listening on port 8080."
                e is java.net.SocketTimeoutException ->
                    "Connection timed out. The local server at $base did not respond in time."
                else ->
                    "Connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
            return@withContext ServerHealthResult(
                isSuccess = false,
                latencyMs = System.currentTimeMillis() - startTime,
                errorMessage = errorMsg
            )
        }

        ServerHealthResult(
            isSuccess = false,
            errorMessage = "Server reachable at $base, but endpoints (/v1/models, /health, /props) were not found."
        )
    }

    fun streamChat(
        rawHost: String,
        modelName: String,
        messages: List<ChatMessagePayload>,
        systemInstruction: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): Flow<String> = flow {
        val base = normalizeHost(rawHost)
        val endpoint = "$base/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("stream", true)
            put("temperature", temperature.toDouble())
            put("max_tokens", maxTokens)

            val messagesArr = JSONArray()
            // Prepend system prompt if provided
            if (systemInstruction.isNotBlank()) {
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
            }
            // Add user and assistant messages
            for (msg in messages) {
                messagesArr.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", messagesArr)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .header("Accept", "text/event-stream")
            .build()

        var response: Response? = null
        try {
            response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IOException("Server returned HTTP ${response.code}: $errBody")
            }

            val body = response.body ?: throw IOException("Empty response body from server")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (currentLine.isEmpty() || currentLine.startsWith(":")) {
                    continue // SSE comment / ping
                }
                if (currentLine.startsWith("data:")) {
                    val dataContent = currentLine.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") {
                        break
                    }
                    try {
                        val chunkObj = JSONObject(dataContent)
                        val choices = chunkObj.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            if (delta != null && delta.has("content")) {
                                val token = delta.getString("content")
                                if (token.isNotEmpty()) {
                                    emit(token)
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // ignore malformed chunks
                    }
                }
            }
        } finally {
            response?.close()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateChatDirect(
        rawHost: String,
        modelName: String,
        messages: List<ChatMessagePayload>,
        systemInstruction: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048
    ): String = withContext(Dispatchers.IO) {
        val base = normalizeHost(rawHost)
        val endpoint = "$base/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("stream", false)
            put("temperature", temperature.toDouble())
            put("max_tokens", maxTokens)

            val messagesArr = JSONArray()
            if (systemInstruction.isNotBlank()) {
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
            }
            for (msg in messages) {
                messagesArr.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", messagesArr)
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IOException("Server returned HTTP ${response.code}: $errBody")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from local AI server")
            val json = JSONObject(body)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val messageObj = choices.getJSONObject(0).getJSONObject("message")
                return@withContext messageObj.getString("content")
            }
            throw IOException("No completion choices returned by model")
        }
    }
}
