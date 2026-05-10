package com.AbuAlaa.ui.screens

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.AbuAlaa.R
import com.AbuAlaa.alarm.AzanMediaPlayer
import com.AbuAlaa.alarm.AzanSoundService

class AzanFullscreenActivity : AppCompatActivity() {

    private val autoHandler = Handler(Looper.getMainLooper())

    private val athanCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            closeScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إظهار فوق شاشة القفل وتشغيل الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
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
        findViewById<TextView>(R.id.prayer_name_text).text = prayerName
        findViewById<TextView>(R.id.athan_text).text = "حان وقت صلاة $prayerName"

        // إيقاف الأذان عبر الـ Service
        findViewById<Button>(R.id.stop_athan_button).setOnClickListener {
            stopAzanService()
            closeScreen()
        }

        // إغلاق تلقائي بعد 10 دقائق
        autoHandler.postDelayed({ closeScreen() }, 10 * 60 * 1000L)
    }

    private fun stopAzanService() {
        startService(Intent(this, AzanSoundService::class.java).apply {
            action = AzanSoundService.ACTION_STOP
        })
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                athanCompleteReceiver,
                IntentFilter("com.AbuAlaa.ATHAN_COMPLETE"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(athanCompleteReceiver, IntentFilter("com.AbuAlaa.ATHAN_COMPLETE"))
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(athanCompleteReceiver) } catch (_: Exception) {}
    }

    private fun closeScreen() {
        autoHandler.removeCallbacksAndMessages(null)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        autoHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        stopAzanService()
        closeScreen()
    }
}
