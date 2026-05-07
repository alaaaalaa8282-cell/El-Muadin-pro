package com.AbuAlaa.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.AbuAlaa.data.ZekrData
import com.AbuAlaa.data.ZekrPrefs

class ZekrReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ZekrPrefs.isEnabled(context)) return

        val playbackMode = ZekrPrefs.getPlaybackMode(context)
        val zekr = if (playbackMode == 1) {
            val index = ZekrPrefs.getRepeatIndex(context)
            if (index < ZekrData.zekrList.size) ZekrData.zekrList[index]
            else ZekrData.zekrList[0]
        } else {
            val index = ZekrPrefs.nextZekrIndex(context)
            ZekrData.zekrList[index]
        }

        val volume = ZekrPrefs.getVolume(context)
        val intervalMinutes = ZekrPrefs.getIntervalInMinutes(context)

        // تشغيل الصوت عبر Foreground Service عشان ما يتوقفش لما بتلمس ستارة الإشعارات
        val serviceIntent = Intent(context, ZekrSoundService::class.java).apply {
            putExtra(ZekrSoundService.EXTRA_RES_ID, zekr.resId)
            putExtra(ZekrSoundService.EXTRA_VOLUME, volume)
            putExtra(ZekrSoundService.EXTRA_INTERVAL, intervalMinutes.toLong())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
