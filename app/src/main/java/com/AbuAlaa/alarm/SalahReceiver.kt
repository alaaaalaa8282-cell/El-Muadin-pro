package com.AbuAlaa.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.AbuAlaa.R
import com.AbuAlaa.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.AbuAlaa.alarm.AzanMediaPlayer

@AndroidEntryPoint
class SalahReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val settings = runBlocking { settingsRepository.settingsFlow.first() }

        if (!settings.salahEnabled) return

        if (AzanMediaPlayer.player?.isPlaying == true) return
       
        // شغل الصوت
        context.startService(Intent(context, SalahSoundService::class.java))

        // اعرض notification
        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_AZKAR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("الصلاة على النبي ﷺ")
            .setContentText("اللهم صل وسلم على نبينا محمد")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(9001, notif)

        // جدول المرة الجاية
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, SalahReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 9000, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + (settings.salahInterval * 60 * 1000L)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
}
