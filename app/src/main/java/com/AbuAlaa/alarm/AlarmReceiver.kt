package com.AbuAlaa.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.telephony.TelephonyManager
import com.AbuAlaa.data.AdhanSound
import com.AbuAlaa.ui.screens.AzanFullscreenActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (tm.callState != TelephonyManager.CALL_STATE_IDLE) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "حان وقت الصلاة"
        val adhanSoundName = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: AdhanSound.MAKKAH.name
        val notifId = intent.getIntExtra(EXTRA_ID, 1001)
        val isSilent = intent.getBooleanExtra(EXTRA_IS_SILENT, false)
        val volume = intent.getFloatExtra(EXTRA_VOLUME, 1f)

        val adhanSound = try { AdhanSound.valueOf(adhanSoundName) } catch (e: Exception) { AdhanSound.MAKKAH }

        if (!isSilent) {
            val mp = MediaPlayer.create(context, adhanSound.resId)
            mp?.setVolume(volume, volume)
            mp?.start()
            AzanMediaPlayer.player = mp
            mp?.setOnCompletionListener {
                it.release()
                AzanMediaPlayer.player = null
            }
        }

        val openIntent = Intent(context, AzanFullscreenActivity::class.java).apply {
            putExtra("prayer_name", title)
            putExtra("notif_id", notifId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(openIntent)
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
        const val EXTRA_IS_SILENT = "extra_is_silent"
        const val EXTRA_VOLUME = "extra_volume"
    }
}
