package com.AbuAlaa.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.AbuAlaa.data.PrayerDay
import com.AbuAlaa.data.SettingsRepository
import com.AbuAlaa.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import java.time.ZoneId

@Singleton
class PrayerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val settingsRepository: SettingsRepository
) {
    data class AlarmSpec(val id: Int, val name: String, val time: String)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
    }

    fun scheduleForToday(day: PrayerDay, adhanSoundName: String, zoneId: ZoneId = ZoneId.systemDefault(), silentFajr: Boolean = false) {
        val today = LocalDate.now(zoneId)
        scheduleForDate(today, day, adhanSoundName, zoneId, silentFajr)
    }

    fun scheduleForDate(date: LocalDate, day: PrayerDay, adhanSoundName: String, zoneId: ZoneId = ZoneId.systemDefault(), silentFajr: Boolean = false) {
        val list = listOf(
            AlarmSpec(2001, "Fajr", day.fajr),
            AlarmSpec(2002, "Dhuhr", day.dhuhr),
            AlarmSpec(2003, "Asr", day.asr),
            AlarmSpec(2004, "Maghrib", day.maghrib),
            AlarmSpec(2005, "Isha", day.isha)
        )

        for (spec in list) {
            scheduleExact(spec.id, spec.name, date, spec.time, adhanSoundName, zoneId, silentFajr)
        }
    }

    fun cancelAll() {
        for (id in 2001..2005) {
            val pi = pendingIntent(id, "Prayer", "Prayer", "MAKKAH", false)
            alarmManager.cancel(pi)
        }
    }

    private fun scheduleExact(id: Int, prayerName: String, date: LocalDate, time: String, adhanSoundName: String, zoneId: ZoneId, silentFajr: Boolean = false) {
        val triggerAt = TimeUtils.toMillis(date, time, zoneId)
        val now = System.currentTimeMillis()
        val actualTrigger = if (triggerAt <= now) {
            TimeUtils.toMillis(date.plusDays(1), time, zoneId)
        } else triggerAt

        val arabicName = when (prayerName) {
            "Fajr" -> "الفجر"
            "Dhuhr" -> "الظهر"
            "Asr" -> "العصر"
            "Maghrib" -> "المغرب"
            "Isha" -> "العشاء"
            else -> prayerName
        }

        val isSilent = prayerName == "Fajr" && silentFajr

        val pi = pendingIntent(
            id = id,
            title = "حان وقت صلاة $arabicName",
            body = "الوقت: $time",
            adhanSoundName = adhanSoundName,
            isSilent = isSilent
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
    AlarmManager.AlarmClockInfo(actualTrigger, pi),
    pi
)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, actualTrigger, pi)
        }
    }

    private fun pendingIntent(id: Int, title: String, body: String, adhanSoundName: String, isSilent: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ID, id)
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
            putExtra(AlarmReceiver.EXTRA_ADHAN_SOUND, adhanSoundName)
            putExtra(AlarmReceiver.EXTRA_IS_SILENT, isSilent)
        }
        return PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun rescheduleAll() {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.notificationsEnabled) return

        val stored = settingsRepository.storedPrayerDayFlow.first() ?: return
        val day = stored.second
        scheduleForToday(day, settings.adhanSound.name, silentFajr = settings.silentFajr)
    }
fun scheduleSalah(intervalMinutes: Int) {
    val intent = Intent(context, SalahReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 9000, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val intervalMs = intervalMinutes * 60 * 1000L
    alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    System.currentTimeMillis() + intervalMs,
    pi
)
}

fun cancelSalah() {
    val intent = Intent(context, SalahReceiver::class.java)
    val pi = PendingIntent.getBroadcast(
        context, 9000, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pi)
}
}

