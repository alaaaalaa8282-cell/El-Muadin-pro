package com.AbuAlaa.data

import com.AbuAlaa.R

enum class LocationMode { AUTO, MANUAL }

enum class CalculationMethod(val apiId: Int, val label: String, val labelAr: String) {
    UMM_AL_QURA(4, "Umm Al-Qura", "أم القرى"),
    MWL(3, "MWL", "رابطة العالم الإسلامي"),
    ISNA(2, "ISNA", "أمريكا الشمالية"),
    EGYPT(5, "Egypt", "الهيئة المصرية")
}

enum class AdhanSound(val resId: Int, val labelAr: String, val isFull: Boolean = false) {
    MAKKAH(R.raw.adhan_makkah, "أذان مكة المكرمة"),
    MADINAH(R.raw.adhan_madinah, "أذان المدينة المنورة"),
    ALAQSA(R.raw.adhan_alaqsa, "أذان المسجد الأقصى"),
    MAKKAH_FULL(R.raw.adhan_makkah_full, "أذان مكة الكامل", isFull = true),
    ELHOSARY(R.raw.adhan_elhosary, "الشيخ الحصري"),
    ELNAKSHBANDY(R.raw.adhan_elnakshbandy, "الشيخ النقشبندي"),
    MOHAMED_REFAT(R.raw.adhan_mohamed_refat, "الشيخ محمد رفعت"),
    ABDEL_BASET(R.raw.abdel_baset, "الشيخ عبد الباسط"),
    ABDEL_BASET_SAMAD(R.raw.abd_elbasit_abdel_samad, "الشيخ عبد الباسط عبد الصمد")
}

enum class SalahSound(val resId: Int, val labelAr: String) {
    NOZAKER(R.raw.nozaker_salt_ala_habib, "نذكركم بالصلاة على الحبيب"),
    AYAH(R.raw.ayah_elahzab, "آية الأحزاب"),
    SOBHANALLAH(R.raw.sobhanallah_wabehamdeh, "سبحان الله وبحمده"),
    ALHAMDO(R.raw.alhamdo_lelah, "الحمد لله"),
    LAHAWLA(R.raw.lahawla_wlaqowat, "لا حول ولا قوة إلا بالله"),
    ALLAHOM_ALHAMD(R.raw.allahom_lk_alhamd, "اللهم لك الحمد"),
    RBNA_IGHFER(R.raw.rbna_ighfer_li, "ربنا اغفر لي")
   
}
data class UserSettings(
    val locationMode: LocationMode = LocationMode.AUTO,
    val manualCity: String = "",
    val manualCountry: String = "",
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA,
    val notificationsEnabled: Boolean = true,
    val adhanSound: AdhanSound = AdhanSound.MAKKAH,
    val silentFajr: Boolean = false,
    val playFullAdhan: Boolean = false,
    val salahEnabled: Boolean = false,
    val salahSound: SalahSound = SalahSound.NOZAKER,
    val salahInterval: Int = 30,
    val salahSequential: Boolean = false,
    val salahCurrentIndex: Int = 0,
    val salahVolume: Float = 1.0f,
)

data class PrayerDay(
    val imsak: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val timezone: String,
    val gregorianDate: String,
    val hijriDate: String,
    val hijriMonthNumber: Int
)
