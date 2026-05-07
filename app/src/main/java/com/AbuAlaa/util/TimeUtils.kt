package com.AbuAlaa.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    fun normalizeTime(raw: String): String {
        // Aladhan sometimes returns "04:12 (AST)" or "04:12"
        val cleaned = raw.trim()
            .takeWhile { it.isDigit() || it == ':' }
        return cleaned.take(5).ifBlank { raw }
    }

    fun todayDdMmYyyy(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val d = LocalDate.now(zoneId)
        return d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
    }

    fun todayIso(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val d = LocalDate.now(zoneId)
        return d.format(DateTimeFormatter.ISO_DATE)
    }

    fun toMillisToday(timeHHmm: String, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val t = LocalTime.parse(timeHHmm, DateTimeFormatter.ofPattern("HH:mm"))
        val dt = LocalDateTime.of(LocalDate.now(zoneId), t)
        return dt.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun toMillis(date: LocalDate, timeHHmm: String, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val t = LocalTime.parse(timeHHmm, DateTimeFormatter.ofPattern("HH:mm"))
        val dt = LocalDateTime.of(date, t)
        return dt.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun millisUntil(targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long =
        (targetMillis - nowMillis).coerceAtLeast(0)

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
