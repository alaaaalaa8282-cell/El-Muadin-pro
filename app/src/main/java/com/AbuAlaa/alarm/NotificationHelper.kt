package com.AbuAlaa.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.AbuAlaa.R
import com.AbuAlaa.data.AdhanSound

object NotificationHelper {
    const val CHANNEL_PRAYER = "prayer_channel"
    const val CHANNEL_AZKAR = "azkar_channel"

    fun ensureChannels(context: Context, adhanSound: AdhanSound = AdhanSound.MAKKAH) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use selected adhan sound from settings
        val adhanSoundUri = Uri.parse("android.resource://${context.packageName}/${adhanSound.resId}")
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        // Delete existing channel to update sound
        nm.deleteNotificationChannel(CHANNEL_PRAYER)

        val prayer = NotificationChannel(
            CHANNEL_PRAYER,
            "تنبيهات الصلاة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات مواقيت الصلاة"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
        }

        val azkar = NotificationChannel(
            CHANNEL_AZKAR,
            "تذكير الأذكار",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تذكير أذكار الصباح والمساء"
        }

        nm.createNotificationChannel(prayer)
        nm.createNotificationChannel(azkar)
    }
    
    fun getAdhanSoundUri(context: Context, adhanSound: AdhanSound): Uri {
        return Uri.parse("android.resource://${context.packageName}/${adhanSound.resId}")
    }
}
