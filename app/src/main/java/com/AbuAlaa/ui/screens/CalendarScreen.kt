package com.AbuAlaa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Hijri month names in Arabic
private val HIJRI_MONTHS = listOf(
    "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
    "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
)

// Arabic day names
private val ARABIC_DAYS = listOf(
    "الأحد", "الاثنين", "الثلاثاء", "الأربعاء",
    "الخميس", "الجمعة", "السبت"
)

// Arabic day abbreviations
private val ARABIC_DAY_ABBR = listOf("أح", "اث", "ثل", "أر", "خم", "جم", "سب")

// Gregorian month names in Arabic
private val GREGORIAN_MONTHS = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

data class CalendarDay(
    val gregorianDate: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val isToday: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    contentPadding: PaddingValues,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    // Fetch prayer times when selected date changes
    LaunchedEffect(selectedDate) {
        vm.fetchPrayerTimesForDate(selectedDate)
    }

    // Calculate days for current month view
    val daysInMonth = remember(currentMonth) {
        val firstDay = currentMonth
        val daysBefore = (firstDay.dayOfWeek.value % 7) // Sunday=0, etc.
        val startDate = firstDay.minusDays(daysBefore.toLong())

        (0 until 42).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val hijri = approximateHijriDate(date)
            CalendarDay(
                gregorianDate = date,
                hijriDay = hijri.first,
                hijriMonth = hijri.second,
                hijriYear = hijri.third,
                isToday = date == today
            )
        }
    }

    // Week view for quick navigation
    val currentWeek = remember(today) {
        (-3..3).map { offset ->
            val date = today.plusDays(offset.toLong())
            val hijri = approximateHijriDate(date)
            CalendarDay(
                gregorianDate = date,
                hijriDay = hijri.first,
                hijriMonth = hijri.second,
                hijriYear = hijri.third,
                isToday = date == today
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        "التقويم الهجري",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val todayHijri = approximateHijriDate(today)
                    Text(
                        "${todayHijri.first} ${HIJRI_MONTHS.getOrElse(todayHijri.second - 1) { "" }} ${todayHijri.third}هـ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Ramadan Countdown
            item {
                val todayHijri = approximateHijriDate(today)
                if (todayHijri.second < 9) { // Before Ramadan
                    val daysToRamadan = estimateDaysToRamadan(today)
                    if (daysToRamadan > 0) {
                        RamadanCountdownCard(daysToRamadan)
                    }
                } else if (todayHijri.second == 9) { // During Ramadan
                    RamadanActiveCard(todayHijri.first)
                }
            }

            // 3. Month View Card (Full Calendar)
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Month Navigation Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Next")
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${GREGORIAN_MONTHS.getOrElse(currentMonth.monthValue - 1) { "" }} ${currentMonth.year}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val monthHijri = approximateHijriDate(currentMonth)
                                Text(
                                    "${HIJRI_MONTHS.getOrElse(monthHijri.second - 1) { "" }} ${monthHijri.third}هـ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Prev")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Day Names Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ARABIC_DAY_ABBR.forEach { day ->
                                Text(
                                    day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            daysInMonth.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    week.forEach { day ->
                                        CalendarDayCell(
                                            day = day,
                                            isCurrentMonth = day.gregorianDate.month == currentMonth.month,
                                            isSelected = day.gregorianDate == selectedDate,
                                            onClick = { selectedDate = day.gregorianDate },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Week View (Quick Navigation)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "عرض سريع للأسبوع",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentWeek) { day ->
                            WeekDayCard(
                                day = day,
                                isSelected = day.gregorianDate == selectedDate,
                                onClick = { selectedDate = day.gregorianDate }
                            )
                        }
                    }
                }
            }

            // 5. Prayer Times for Selected Date
            item {
                AnimatedVisibility(visible = state.day != null) {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp),
                         elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Date Header and Location
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        ARABIC_DAYS.getOrElse(selectedDate.dayOfWeek.value % 7) { "" },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${selectedDate.dayOfMonth} ${GREGORIAN_MONTHS.getOrElse(selectedDate.monthValue - 1) { "" }} ${selectedDate.year}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val selectedHijri = approximateHijriDate(selectedDate)
                                    Text(
                                        "${selectedHijri.first} ${HIJRI_MONTHS.getOrElse(selectedHijri.second - 1) { "" }} ${selectedHijri.third}هـ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                // Location Info - Added as requested
                                Column(horizontalAlignment = Alignment.End) {
                                     val locationText = if (state.settings.locationMode == LocationMode.MANUAL) {
                                         "${state.settings.manualCity}، ${state.settings.manualCountry}"
                                     } else {
                                         state.day?.timezone?.substringAfter("/")?.replace("_", " ") ?: "الموقع تلقائي"
                                     }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            locationText,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Prayer Times List
                            val prayerDay = state.selectedDateDay ?: state.day
                            if (prayerDay != null) {
                                Text(
                                    "مواقيت الصلاة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val prayers = listOf(
                                    "الفجر" to prayerDay.fajr,
                                    "الشروق" to prayerDay.sunrise,
                                    "الظهر" to prayerDay.dhuhr,
                                    "العصر" to prayerDay.asr,
                                    "المغرب" to prayerDay.maghrib,
                                    "العشاء" to prayerDay.isha
                                )
                                
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    prayers.forEach { (name, time) ->
                                        PrayerTimeRow(name, time)
                                    }
                                }
                            }

                            // Return to Today Button
                            if (selectedDate != today) {
                                OutlinedButton(
                                    onClick = {
                                        selectedDate = today
                                        currentMonth = today.withDayOfMonth(1)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("العودة لليوم")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayCard(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        day.isToday -> Color.White
        isSelected -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .width(55.dp) // Fixed width for uniformity
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (isSelected && !day.isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            ARABIC_DAY_ABBR.getOrElse(day.gregorianDate.dayOfWeek.value % 7) { "" },
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
        Text(
            "${day.gregorianDate.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val textAlpha = if (isCurrentMonth) 1f else 0.4f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${day.gregorianDate.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    day.isToday -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                }
            )
        }
    }
}

@Composable
private fun PrayerTimeRow(name: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            time,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RamadanCountdownCard(daysRemaining: Int) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface // Changed to surface color
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "العد التنازلي لرمضان",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "باقي $daysRemaining يوم على الشهر الفضيل",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "🌙",
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun RamadanActiveCard(currentDay: Int) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "🌙 رمضان كريم 🌙",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "اليوم $currentDay من رمضان",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CountdownBox(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Approximate Hijri date calculation
private fun approximateHijriDate(gregorian: LocalDate): Triple<Int, Int, Int> {
    // Reference date: 1 Muharram 1446 = July 7, 2024
    val referenceGregorian = LocalDate.of(2024, 7, 7)
    val referenceHijri = Triple(1, 1, 1446) // Day, Month, Year

    val daysDiff = ChronoUnit.DAYS.between(referenceGregorian, gregorian)

    // Hijri months alternate between 30 and 29 days
    val hijriMonthLengths = listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
    // val hijriYearLength = 354 // Unused

    var remainingDays = daysDiff.toInt()
    var year = referenceHijri.third
    var month = referenceHijri.second
    var day = referenceHijri.first

    if (remainingDays >= 0) {
        while (remainingDays > 0) {
            val monthLength = hijriMonthLengths[(month - 1) % 12]
            val daysLeftInMonth = monthLength - day + 1

            if (remainingDays >= daysLeftInMonth) {
                remainingDays -= daysLeftInMonth
                day = 1
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            } else {
                day += remainingDays
                remainingDays = 0
            }
        }
    } else {
        remainingDays = -remainingDays
        while (remainingDays > 0) {
            if (remainingDays >= day) {
                remainingDays -= day
                month--
                if (month < 1) {
                    month = 12
                    year--
                }
                day = hijriMonthLengths[(month - 1) % 12]
            } else {
                day -= remainingDays
                remainingDays = 0
            }
        }
    }

    return Triple(day, month, year)
}

private fun estimateDaysToRamadan(from: LocalDate): Int {
    val currentHijri = approximateHijriDate(from)

    // If we're past Ramadan this year, calculate for next year
    val targetMonth = 9 // Ramadan
    var targetYear = currentHijri.third

    if (currentHijri.second > 9 || (currentHijri.second == 9 && currentHijri.first >= 30)) {
        targetYear++
    }

    // Approximate days remaining
    var days = 0
    val hijriMonthLengths = listOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)

    // Days left in current month
    val currentMonthLength = hijriMonthLengths[(currentHijri.second - 1) % 12]
    days += currentMonthLength - currentHijri.first

    // Full months between
    var m = currentHijri.second + 1
    var y = currentHijri.third
    // Loop until we reach Start of Ramadan (1/9/TargetYear)
    while (y < targetYear || (y == targetYear && m < targetMonth)) {
        if (m > 12) {
            m = 1
            y++
        }
        days += hijriMonthLengths[(m - 1) % 12]
        m++
    }

    return days
}
