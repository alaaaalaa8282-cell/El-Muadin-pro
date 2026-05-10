package com.AbuAlaa.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import com.AbuAlaa.data.AdhanSound

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // لو في مكالمة - ما نشغلش
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (tm.callState != TelephonyManager.CALL_STATE_IDLE) return

        val title          = intent.getStringExtra(EXTRA_TITLE) ?: "حان وقت الصلاة"
        val adhanSoundName = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: AdhanSound.MAKKAH.name
        val notifId        = intent.getIntExtra(EXTRA_ID, 1001)
        val isSilent       = intent.getBooleanExtra(EXTRA_IS_SILENT, false)
        val volume         = intent.getFloatExtra(EXTRA_VOLUME, 1f)

        if (!isSilent) {
            // شغّل الأذان عبر Service صح - مش مباشرة من الـ Receiver
            val serviceIntent = Intent(context, AzanSoundService::class.java).apply {
                putExtra(AzanSoundService.EXTRA_ADHAN_SOUND, adhanSoundName)
                putExtra(AzanSoundService.EXTRA_PRAYER_NAME, title)
                putExtra(AzanSoundService.EXTRA_NOTIF_ID, notifId)
                putExtra(AzanSoundService.EXTRA_VOLUME, volume)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val EXTRA_ID          = "extra_id"
        const val EXTRA_TITLE       = "extra_title"
        const val EXTRA_BODY        = "extra_body"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
        const val EXTRA_IS_SILENT   = "extra_is_silent"
        const val EXTRA_VOLUME      = "extra_volume"
    }
}
