package com.AbuAlaa.ui.screens

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.AbuAlaa.R

class AzanFullscreenActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val autoHandler = Handler(Looper.getMainLooper())

    // Receiver لما الأذان يخلص تلقائي
    private val athanCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            closeScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إظهار على شاشة القفل وتشغيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContentView(R.layout.activity_azan_fullscreen)

        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val soundResId = intent.getIntExtra("sound_res_id", -1)

        // ضبط النصوص
        findViewById<TextView>(R.id.prayer_name_text).text = prayerName
        findViewById<TextView>(R.id.athan_text).text = "حان وقت صلاة $prayerName"

        // زر الإيقاف
        findViewById<Button>(R.id.stop_athan_button).setOnClickListener {
            closeScreen()
        }

        // تشغيل الأذان
        if (soundResId != -1) {
            playAdhan(soundResId)
        }

        // إغلاق تلقائي بعد 10 دقائق لو ما حدش ضغط
        autoHandler.postDelayed({ closeScreen() }, 10 * 60 * 1000L)
    }

    private fun playAdhan(resId: Int) {
        try {
            // ضبط مستوى الصوت على الأعلى للأذان
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )

            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.apply {
                setOnCompletionListener {
                    // الأذان خلص تلقائي — اقفل الشاشة
                    closeScreen()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeScreen() {
        // وقف الصوت
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        autoHandler.removeCallbacksAndMessages(null)

        // ده هو السر — مش بيرجع للتطبيق
        // بيرجع للحالة اللي كانت قبل الأذان
        finishAndRemoveTask()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            athanCompleteReceiver,
            IntentFilter("com.AbuAlaa.ATHAN_COMPLETE")
        )
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(athanCompleteReceiver) } catch (e: Exception) {}
    }

    override fun onDestroy() {
        autoHandler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    // منع زر Back من الرجوع للتطبيق
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        closeScreen()
    }
}
