package com.AbuAlaa.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import android.media.MediaPlayer
class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(AlarmReceiver.EXTRA_ID, 1001)
        AzanMediaPlayer.player?.stop()
        AzanMediaPlayer.player?.release()
        AzanMediaPlayer.player = null
        NotificationManagerCompat.from(context).cancel(notifId)
    }
}
