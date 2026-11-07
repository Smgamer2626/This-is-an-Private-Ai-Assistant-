package com.example.service

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import com.example.data.local.YuriDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.network.ChatMessagePayload
import com.example.data.network.LocalLlamaClient
import com.example.data.preferences.YuriPreferences
import com.example.data.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class YuriFloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var bubbleContainer: FrameLayout? = null
    private var expandedContainer: LinearLayout? = null
    private var bubbleImage: ImageView? = null
    private var emergentOrb: ImageView? = null
    private var txtStatus: TextView? = null
    private var txtQuery: TextView? = null
    private var txtResponse: TextView? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isExpanded = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val llamaClient = LocalLlamaClient()
    private var voiceManager: VoiceManager? = null
    private var activeInferenceJob: Job? = null

    companion object {
        const val ACTION_SHOW = "com.example.service.ACTION_SHOW_BUBBLE"
        const val ACTION_HIDE = "com.example.service.ACTION_HIDE_BUBBLE"
        const val ACTION_EMERGE = "com.example.service.ACTION_EMERGE_BUBBLE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        voiceManager = VoiceManager(this)
        initFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBubble()
            ACTION_HIDE -> hideBubble()
            ACTION_EMERGE -> emergeAssistant(prompt = intent.getStringExtra("EXTRA_PROMPT"))
            else -> showBubble()
        }
        return START_STICKY
    }

    private fun initFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        bubbleContainer = floatingView?.findViewById(R.id.bubble_container)
        expandedContainer = floatingView?.findViewById(R.id.expanded_container)
        bubbleImage = floatingView?.findViewById(R.id.bubble_image)
        emergentOrb = floatingView?.findViewById(R.id.emergent_orb)
        txtStatus = floatingView?.findViewById(R.id.txt_emergent_status)
        txtQuery = floatingView?.findViewById(R.id.txt_emergent_query)
        txtResponse = floatingView?.findViewById(R.id.txt_emergent_response)

        val btnClose = floatingView?.findViewById<TextView>(R.id.btn_close_expanded)
        val btnListenAgain = floatingView?.findViewById<Button>(R.id.btn_listen_again)
        val btnOpenApp = floatingView?.findViewById<Button>(R.id.btn_open_app)

        // Bobbing animation for the bubble
        startOrbBobbing(bubbleImage)
        startOrbBobbing(emergentOrb)

        // Drag & Touch gestures on the floating bubble
        bubbleContainer?.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = kotlin.math.abs(event.rawX - initialTouchX)
                    val diffY = kotlin.math.abs(event.rawY - initialTouchY)
                    // If tap rather than drag
                    if (diffX < 15 && diffY < 15) {
                        emergeAssistant()
                    }
                    true
                }
                else -> false
            }
        }

        btnClose?.setOnClickListener {
            minimizeToBubble()
        }

        btnListenAgain?.setOnClickListener {
            startEmergentListening()
        }

        btnOpenApp?.setOnClickListener {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
            minimizeToBubble()
        }

        try {
            windowManager?.addView(floatingView, params)
        } catch (_: Exception) {}
    }

    private fun startOrbBobbing(view: ImageView?) {
        if (view == null) return
        val animator = ObjectAnimator.ofFloat(view, "translationY", -8f, 8f).apply {
            duration = 1800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        animator.start()
    }

    private fun showBubble() {
        floatingView?.visibility = View.VISIBLE
        bubbleContainer?.visibility = View.VISIBLE
        expandedContainer?.visibility = View.GONE
        isExpanded = false
    }

    private fun hideBubble() {
        floatingView?.visibility = View.GONE
    }

    private fun minimizeToBubble() {
        isExpanded = false
        bubbleContainer?.visibility = View.VISIBLE
        expandedContainer?.visibility = View.GONE
        voiceManager?.stopListening()
        activeInferenceJob?.cancel()
    }

    private fun emergeAssistant(prompt: String? = null) {
        isExpanded = true
        bubbleContainer?.visibility = View.GONE
        expandedContainer?.visibility = View.VISIBLE

        // Make window focusable so keyboard or speech can register
        val p = params ?: return
        p.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        windowManager?.updateViewLayout(floatingView, p)

        if (prompt.isNullOrBlank()) {
            startEmergentListening()
        } else {
            handleUserQuery(prompt)
        }
    }

    private fun startEmergentListening() {
        txtStatus?.text = "LISTENING (OFFLINE)..."
        txtStatus?.setTextColor(0xFF00FF9D.toInt())
        txtQuery?.text = "Listening for your query..."
        txtResponse?.text = ""

        voiceManager?.onPartialTranscript = { partial ->
            txtQuery?.text = partial
        }

        voiceManager?.onFinalTranscript = { finalResult ->
            if (finalResult.isNotBlank()) {
                handleUserQuery(finalResult)
            }
        }

        voiceManager?.startListening()
    }

    private fun handleUserQuery(query: String) {
        txtQuery?.text = query
        txtStatus?.text = "YURI SYNTHESIZING RESPONSE..."
        txtStatus?.setTextColor(0xFFFFB300.toInt())
        txtResponse?.text = ""

        activeInferenceJob?.cancel()
        activeInferenceJob = serviceScope.launch {
            val prefs = YuriPreferences(applicationContext)
            var host = YuriPreferences.DEFAULT_SERVER_HOST
            var model = YuriPreferences.DEFAULT_MODEL_NAME
            var temp = YuriPreferences.DEFAULT_TEMPERATURE
            var maxTokens = YuriPreferences.DEFAULT_MAX_TOKENS
            var sysPrompt = YuriPreferences.DEFAULT_SYSTEM_PROMPT

            prefs.serverHostFlow.collectLatest { host = it }
            prefs.modelNameFlow.collectLatest { model = it }
            prefs.temperatureFlow.collectLatest { temp = it }
            prefs.maxTokensFlow.collectLatest { maxTokens = it }
            prefs.systemPromptFlow.collectLatest { sysPrompt = it }

            val responseBuilder = StringBuilder()

            try {
                llamaClient.streamChat(
                    rawHost = host,
                    modelName = model,
                    messages = listOf(ChatMessagePayload("user", query)),
                    systemInstruction = sysPrompt,
                    temperature = temp,
                    maxTokens = maxTokens
                ).collect { token ->
                    responseBuilder.append(token)
                    withContext(Dispatchers.Main) {
                        txtResponse?.text = responseBuilder.toString()
                    }
                }

                val finalAnswer = responseBuilder.toString().ifBlank { "Operation complete." }
                withContext(Dispatchers.Main) {
                    txtStatus?.text = "ONLINE"
                    txtStatus?.setTextColor(0xFF00FF9D.toInt())
                    txtResponse?.text = finalAnswer
                    voiceManager?.speak(finalAnswer)
                }

                // Save to Room database session
                withContext(Dispatchers.IO) {
                    val db = YuriDatabase.getDatabase(applicationContext)
                    val convId = UUID.randomUUID().toString()
                    db.chatDao().insertConversation(
                        ConversationEntity(
                            id = convId,
                            title = if (query.length > 24) query.take(24) + "..." else query,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    db.chatDao().insertMessage(
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = convId,
                            role = "user",
                            content = query,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    db.chatDao().insertMessage(
                        MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = convId,
                            role = "assistant",
                            content = finalAnswer,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtStatus?.text = "OFFLINE"
                    txtStatus?.setTextColor(0xFFFF3366.toInt())
                    val errMsg = "Cannot connect to local Termux server at $host: ${e.localizedMessage}"
                    txtResponse?.text = errMsg
                    voiceManager?.speak("Cannot reach Termux local engine.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager?.release()
        activeInferenceJob?.cancel()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
}
