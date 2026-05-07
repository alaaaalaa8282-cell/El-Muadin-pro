package com.AbuAlaa.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.AbuAlaa.R
import com.AbuAlaa.data.AdhanSound
import com.AbuAlaa.ui.screens.AzanFullscreenActivity
import android.telephony.TelephonyManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.notif_prayer_title)
        val body = intent.getStringExtra(EXTRA_BODY) ?: context.getString(R.string.notif_prayer_body)
        val adhanSoundName = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: AdhanSound.MAKKAH.name
        val notifId = intent.getIntExtra(EXTRA_ID, 1001)
        val isSilent = intent.getBooleanExtra(EXTRA_IS_SILENT, false)

        val adhanSound = try {
            AdhanSound.valueOf(adhanSoundName)
        } catch (e: Exception) {
            AdhanSound.MAKKAH
        }

        if (!isSilent) {
            val mp = MediaPlayer.create(context, adhanSound.resId)
            mp?.isLooping = false
            mp?.start()
            AzanMediaPlayer.player = mp
            mp?.setOnCompletionListener {
                it.release()
                AzanMediaPlayer.player = null
                // ← هنا التعديل: إشارة للشاشة إن الأذان خلص
                context.sendBroadcast(
                    Intent("com.AbuAlaa.ATHAN_COMPLETE")
                )
            }
        }

        NotificationHelper.ensureChannels(context, adhanSound)

        val openIntent = Intent(context, AzanFullscreenActivity::class.java).apply {
            putExtra("prayer_name", title)
            putExtra("notif_id", notifId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, DismissReceiver::class.java)
        dismissIntent.putExtra(EXTRA_ID, notifId)
        val dismissPi = PendingIntent.getBroadcast(
            context, notifId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenPi = PendingIntent.getActivity(
            context, notifId + 100, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = NotificationHelper.getAdhanSoundUri(context, adhanSound)
        val largeBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeBitmap)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(0, "إيقاف الأذان", dismissPi)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notif)
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
        const val EXTRA_IS_SILENT = "extra_is_silent"
    }
}
