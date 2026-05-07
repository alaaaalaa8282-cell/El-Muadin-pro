package com.AbuAlaa.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App Color Palette
 * Central place for all app colors - easy to change themes
 */
object AppColors {
    // Primary Dark Theme Color
    val DarkPrimary = Color(0xFF10171A) // اللون الأساسي (داكن) للخلفية/الشريط العلوي
    val DarkPrimaryLight = Color(0xFF1A2329) // درجة أفتح من الأساسي (للبطاقات/السطوح)
    
    // Golden Accent (kept for accent elements)
    val GoldenAccent = Color(0xFFD4C4B0) // لون إبراز ذهبي (Accent) للأزرار/الأيقونات/العناصر المهمة
    val GoldenAccentDark = Color(0xFFBFAE98) // ذهبي غامق (للضغط/الحالات النشطة)
    
    // Background colors
    val BackgroundLight = Color(0xFFFDFBF5) // خلفية فاتحة للشاشات
    val BackgroundLighter = Color(0xFFFDFBF5) // خلفية أفتح/بديلة (لأقسام داخل الشاشة)
    
    // Text colors
    val TextDark = Color(0xFF5D4037) // نص داكن للعناوين/النص الأساسي على الخلفيات الفاتحة
    val TextLight = Color.White // نص فاتح على الخلفيات الداكنة
    val TextMuted = Color(0xFF10171A) // نص ثانوي/مخفف (وصف، تلميحات)
    
    // Success/Error
    val Success = Color(0xFF4CAF50) // نجاح (رسائل تأكيد/حالات صحيحة)
    val Error = Color(0xFFEF4444) // خطأ (تنبيهات/فشل عمليات)
    val Warning = Color(0xFFE65100) // تحذير (تنبيه متوسط الشدة)
}
