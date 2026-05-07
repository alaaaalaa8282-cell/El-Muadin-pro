package com.AbuAlaa.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import com.AbuAlaa.R

class ZekrSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    companion object {
        const val EXTRA_RES_ID = "zekr_res_id"
        const val EXTRA_VOLUME = "zekr_volume"
        const val EXTRA_INTERVAL = "zekr_interval"
        const val NOTIF_ID = 9001
        const val CHANNEL_ID = "zekr_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resId = intent?.getIntExtra(EXTRA_RES_ID, -1) ?: -1
        val volume = intent?.getFloatExtra(EXTRA_VOLUME, 1f) ?: 1f
        val intervalMinutes = intent?.getLongExtra(EXTRA_INTERVAL, 0L) ?: 0L

        if (resId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start as foreground so Android doesn't kill us when notification shade pulled
        startForeground(NOTIF_ID, buildNotification())

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
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
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }

        val logVolume = if (volume == 0f) 0f
        else (1 - (Math.log((1 + (1 - volume) * 99).toDouble()) / Math.log(100.0))).toFloat()

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.apply {
            setVolume(logVolume, logVolume)
            setOnCompletionListener {
                releaseAudioFocus()
                it.release()
                mediaPlayer = null
                // إعادة الجدولة للمرة القادمة
                if (intervalMinutes > 0) {
                    ZekrScheduler.schedule(this@ZekrSoundService, intervalMinutes)
                }
                stopSelf()
            }
            start()
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
            .setContentTitle("الأذكار")
            .setContentText("جاري تشغيل الذكر...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "خدمة الأذكار",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "يُستخدم لتشغيل صوت الأذكار بشكل مستمر"
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

    override fun onDestroy() {
        releaseAudioFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
