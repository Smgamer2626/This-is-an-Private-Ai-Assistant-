package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class YuriWakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningLoop = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID = "yuri_wake_word_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START_LISTENING = "com.example.service.ACTION_START_WAKE_WORD"
        const val ACTION_STOP_LISTENING = "com.example.service.ACTION_STOP_WAKE_WORD"
        const val ACTION_TRIGGER_EMERGE = "com.example.service.ACTION_TRIGGER_EMERGE"

        @Volatile
        var isServiceRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LISTENING -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_EMERGE -> {
                triggerEmerge()
            }
            else -> {
                startForegroundServiceWithNotification()
                startContinuousWakeListening()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YURI AI Background Wake Engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for 'Yuri' wake word to emerge the assistant"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        isServiceRunning = true

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val emergeIntent = Intent(this, YuriWakeWordService::class.java).apply {
            action = ACTION_TRIGGER_EMERGE
        }
        val emergePendingIntent = PendingIntent.getService(
            this, 1, emergeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, YuriWakeWordService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YURI AI • Wake Assistant Active")
            .setContentText("Say 'Yuri' anytime to emerge the AI bubble")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Summon Yuri", emergePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Deactivate", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startContinuousWakeListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        isListeningLoop = true
        initAndListen()
    }

    private fun initAndListen() {
        if (!isListeningLoop) return

        handler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        scheduleRestart(300)
                    }

                    override fun onError(error: Int) {
                        // Immediately schedule seamless restart for continuous background listening
                        scheduleRestart(600)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkAndProcessWakeWord(matches)
                        }
                        scheduleRestart(300)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkAndProcessWakeWord(matches)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (_: Exception) {
                scheduleRestart(1000)
            }
        }
    }

    private fun checkAndProcessWakeWord(matches: List<String>) {
        val wakeKeywords = listOf("yuri", "uri", "hey yuri", "ok yuri", "hello yuri", "assistant yuri")

        for (phrase in matches) {
            val lower = phrase.lowercase(Locale.ROOT)
            val matchedKey = wakeKeywords.firstOrNull { lower.contains(it) }

            if (matchedKey != null) {
                // Wake word detected!
                vibrateDevice()

                // Extract potential prompt after wake word
                val afterKeyword = lower.substringAfter(matchedKey).trim().removePrefix(",").trim()
                triggerEmerge(if (afterKeyword.isNotBlank()) afterKeyword else null)
                break
            }
        }
    }

    private fun triggerEmerge(prompt: String? = null) {
        val emergeIntent = Intent(applicationContext, YuriFloatingBubbleService::class.java).apply {
            action = YuriFloatingBubbleService.ACTION_EMERGE
            if (prompt != null) {
                putExtra("EXTRA_PROMPT", prompt)
            }
        }
        try {
            startService(emergeIntent)
        } catch (_: Exception) {}
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (_: Exception) {}
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isListeningLoop) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            initAndListen()
        }, delayMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isListeningLoop = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}
