package com.AbuAlaa.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object ZekrScheduler {
    fun schedule(ctx: Context, minutes: Long) {
        val intent = Intent(ctx, ZekrReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 3001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + minutes * 60 * 1000,
            pi
        )
    }

    fun cancel(ctx: Context) {
        val intent = Intent(ctx, ZekrReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, 3001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}
