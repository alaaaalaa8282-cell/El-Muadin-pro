
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
import com.AbuAlaa.R

class ZekrSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var intervalMinutes: Long = 0L
    private var focusLost = false

    companion object {
        const val EXTRA_RES_ID    = "zekr_res_id"
        const val EXTRA_VOLUME    = "zekr_volume"
        const val EXTRA_INTERVAL  = "zekr_interval"
        const val NOTIF_ID        = 9001
        const val CHANNEL_ID      = "zekr_service_channel"
    }

    // ============================================================
    // AudioFocus listener - قلب الحل
    // ============================================================
    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {

            // سحب كامل للفوكس (مكالمة، تطبيق آخر...)
            AudioManager.AUDIOFOCUS_LOSS -> {
                focusLost = true
                stopPlaybackAndReschedule()
            }

            // سحب مؤقت (مكالمة واردة مثلاً)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
    focusLost = true
    mediaPlayer?.pause()
}
            

            // تخفيض الصوت مؤقتاً - نكمل بصوت أقل
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }

            // استرجاع الفوكس - الذكر انتهى أصلاً وأُعيدت الجدولة، لا نفعل شيئاً
            AudioManager.AUDIOFOCUS_GAIN -> {
    mediaPlayer?.start()
    focusLost = false
}
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resId        = intent?.getIntExtra(EXTRA_RES_ID, -1) ?: -1
        val volume       = intent?.getFloatExtra(EXTRA_VOLUME, 1f) ?: 1f
        intervalMinutes  = intent?.getLongExtra(EXTRA_INTERVAL, 0L) ?: 0L
        focusLost        = false

        if (resId == -1) { stopSelf(); return START_NOT_STICKY }

        startForeground(NOTIF_ID, buildNotification())

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // طلب AudioFocus مع تسجيل الـ listener
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)  // ← المهم
                .setAcceptsDelayedFocusGain(false)
                .build()
            focusRequest = req
            audioManager?.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                focusListener,                                   // ← المهم
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }

        if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // ما قدرناش نأخذ الفوكس (مكالمة شغّالة مثلاً) → نعيد الجدولة ونمشي
            if (intervalMinutes > 0) ZekrScheduler.schedule(this, intervalMinutes)
            stopSelf()
            return START_NOT_STICKY
        }

        val logVolume = if (volume == 0f) 0f else Math.pow(volume.toDouble(), 4.0).toFloat()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer?.apply {
            setVolume(logVolume, logVolume)
            setOnCompletionListener {
                // اكتمل طبيعياً
                releaseAudioFocus()
                it.release()
                mediaPlayer = null
                if (intervalMinutes > 0 && !focusLost) {
                    ZekrScheduler.schedule(this@ZekrSoundService, intervalMinutes)
                }
                stopSelf()
            }
            setOnErrorListener { _, _, _ ->
                // خطأ في التشغيل → أعد الجدولة كمان
                releaseAudioFocus()
                release()
                mediaPlayer = null
                if (intervalMinutes > 0) ZekrScheduler.schedule(this@ZekrSoundService, intervalMinutes)
                stopSelf()
                true
            }
            start()
        }

        return START_NOT_STICKY
    }

    /**
     * يوقف التشغيل ويعيد الجدولة - يُستدعى من focusListener عند مكالمة
     */
    private fun stopPlaybackAndReschedule() {
        mediaPlayer?.let {
            try { if (it.isPlaying) it.stop() } catch (_: Exception) {}
            it.release()
        }
        mediaPlayer = null
        releaseAudioFocus()
        if (intervalMinutes > 0) {
            ZekrScheduler.schedule(this, intervalMinutes)
        }
        stopSelf()
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
                val ch = NotificationChannel(
                    CHANNEL_ID, "خدمة الأذكار",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "يُستخدم لتشغيل صوت الأذكار بشكل مستمر"
                    setSound(null, null)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(focusListener)
        }
        focusRequest = null
    }

    override fun onDestroy() {
        releaseAudioFocus()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
