package com.AbuAlaa.data.remote

import com.squareup.moshi.Json

data class AladhanResponse(
    val code: Int,
    val status: String,
    val data: AladhanData
)

data class AladhanData(
    val timings: Timings,
    val date: AladhanDate,
    val meta: Meta
)

data class Timings(
    @Json(name = "Imsak") val imsak: String,
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String
)

data class AladhanDate(
    val readable: String,
    val hijri: Hijri,
    val gregorian: Gregorian? = null
)

data class Gregorian(
    val date: String,
    val day: String,
    val month: GregorianMonth? = null,
    val year: String
)

data class GregorianMonth(
    val number: Int,
    val en: String
)

data class Hijri(
    val date: String,
    val day: String,
    val year: String,
    val month: HijriMonth
)

data class HijriMonth(
    val number: Int,
    val en: String,
    val ar: String
)

data class Meta(
    val timezone: String
)

// Calendar Response - returns entire month
data class CalendarResponse(
    val code: Int,
    val data: List<CalendarDayData>
)

data class CalendarDayData(
    val timings: Timings,
    val date: AladhanDate,
    val meta: Meta
)
