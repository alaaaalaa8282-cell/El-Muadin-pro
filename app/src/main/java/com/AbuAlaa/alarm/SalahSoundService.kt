package com.AbuAlaa.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.AbuAlaa.R
import com.AbuAlaa.data.SettingsRepository
import com.AbuAlaa.data.UserSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SalahSoundService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var salahPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIF_ID = 9002
        private const val CHANNEL_ID = "salah_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings: UserSettings = runBlocking {
            settingsRepository.settingsFlow.first()
        }

        if (!settings.salahEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Acquire WakeLock عشان الصوت ما يتوقفش لما الشاشة تقفل
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "com.AbuAlaa:SalahSoundWakeLock"
        ).also { it.acquire(5 * 60 * 1000L) } // max 5 دقائق

        startForeground(NOTIF_ID, buildNotification())

        val soundResId: Int = settings.salahSound.resId
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            focusRequest = req
            audioManager?.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }

        if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            releaseWakeLock()
            stopSelf()
            return START_NOT_STICKY
        }

        salahPlayer?.release()
        salahPlayer = MediaPlayer.create(this, soundResId)
        salahPlayer?.apply {
            start()
            setOnCompletionListener {
                releaseAudioFocus()
                releaseWakeLock()
                it.release()
                salahPlayer = null
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("تذكير الصلاة على النبي")
            .setContentText("جاري التشغيل...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "خدمة الصلاة على النبي",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "يُستخدم لتشغيل صوت الصلاة على النبي"
                    setSound(null, null)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseAudioFocus()
        releaseWakeLock()
        salahPlayer?.release()
        salahPlayer = null
        super.onDestroy()
    }
}
