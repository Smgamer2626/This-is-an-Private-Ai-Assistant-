package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale
import kotlin.math.abs

class YuriWakeWordService : Service() {

    private val tag = "YuriWakeWordService"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningLoop = false
    private var isRecognitionRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private var audioRecord: AudioRecord? = null
    private var isAudioRecordRunning = false
    private var audioThread: Thread? = null

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
                isListeningLoop = false
                stopAudioRecord()
                destroyRecognizer()
                stopForeground(true)
                stopSelf()
                isServiceRunning = false
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_EMERGE -> {
                triggerEmerge()
            }
            else -> {
                startForegroundServiceWithNotification()
                startWakeEngine()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YURI AI Background Wake Protocol",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps YURI AI voice wake protocol and floating bubble active in background"
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
            .setContentTitle("YURI AI • Protocol Active")
            .setContentText("Say 'Yuri' or tap the floating bubble")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Summon Yuri", emergePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Stop Protocol", stopPendingIntent)
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

    private fun startWakeEngine() {
        isListeningLoop = true
        startSpeechRecognitionLoop()
        startAudioRecordMonitor()
    }

    private fun startSpeechRecognitionLoop() {
        handler.post {
            if (!isListeningLoop) return@post
            setupAndStartRecognizer()
        }
    }

    private fun setupAndStartRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(tag, "SpeechRecognizer is not available on this device")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isRecognitionRunning = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isRecognitionRunning = false
                        restartRecognition(200)
                    }

                    override fun onError(error: Int) {
                        isRecognitionRunning = false
                        // Recover cleanly based on error
                        val delay = when (error) {
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                            else -> 500L
                        }
                        restartRecognition(delay)
                    }

                    override fun onResults(results: Bundle?) {
                        isRecognitionRunning = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkAndProcessWakeWord(matches)
                        }
                        restartRecognition(200)
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
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            speechRecognizer?.startListening(intent)
            isRecognitionRunning = true
        } catch (e: Exception) {
            Log.e(tag, "Error starting speech recognition", e)
            restartRecognition(1000)
        }
    }

    private fun restartRecognition(delayMs: Long) {
        if (!isListeningLoop) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (isListeningLoop && !isRecognitionRunning) {
                setupAndStartRecognizer()
            }
        }, delayMs)
    }

    private fun destroyRecognizer() {
        handler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            isRecognitionRunning = false
        }
    }

    /**
     * Fallback AudioRecord level monitor:
     * Reads microphone sound pressure in the background. If user says something loudly
     * while recognizer is idle, it wakes up the recognizer and floating bubble immediately!
     */
    private fun startAudioRecordMonitor() {
        if (isAudioRecordRunning) return
        isAudioRecordRunning = true

        audioThread = Thread {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBufSize.coerceAtLeast(2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    return@Thread
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isAudioRecordRunning && isListeningLoop) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        var sum = 0L
                        for (i in 0 until read) {
                            sum += abs(buffer[i].toInt())
                        }
                        val avg = sum / read

                        // If significant speech energy detected while speech recognizer is idle
                        if (avg > 2500 && !isRecognitionRunning) {
                            handler.post {
                                if (isListeningLoop && !isRecognitionRunning) {
                                    setupAndStartRecognizer()
                                }
                            }
                        }
                    }
                    Thread.sleep(50)
                }
            } catch (e: Exception) {
                Log.e(tag, "AudioRecord monitor exception", e)
            } finally {
                stopAudioRecord()
            }
        }.apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    private fun stopAudioRecord() {
        isAudioRecordRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    private fun checkAndProcessWakeWord(matches: List<String>) {
        val wakeKeywords = listOf("yuri", "uri", "hey yuri", "ok yuri", "hello yuri", "assistant yuri", "qwen", "hey qwen")

        for (phrase in matches) {
            val lower = phrase.lowercase(Locale.ROOT)
            val matchedKey = wakeKeywords.firstOrNull { lower.contains(it) }

            if (matchedKey != null) {
                vibrateDevice()
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
                    VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isListeningLoop = false
        handler.removeCallbacksAndMessages(null)
        stopAudioRecord()
        destroyRecognizer()
    }
}
