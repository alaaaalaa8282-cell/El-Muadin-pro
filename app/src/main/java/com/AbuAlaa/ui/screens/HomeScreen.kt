package com.AbuAlaa.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.AbuAlaa.R
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.ui.viewmodel.AzkarViewModel
import com.AbuAlaa.ui.viewmodel.HomeViewModel
import com.AbuAlaa.data.repo.ZikrItem
import com.AbuAlaa.ui.widgets.DotPagerIndicator
import java.time.LocalTime
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.min
import kotlinx.coroutines.delay
import com.AbuAlaa.ui.theme.AppColors

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onOpenAzkar: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val azkarVm: AzkarViewModel = hiltViewModel()
    val azkarState by azkarVm.state.collectAsState()

    val placeholderTime = "--:--"
    val placeholderText = "--"
    val placeholderCountdown = "--:--:--"

    val now = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = LocalTime.now()
            delay(30_000)
        }
    }
    val isMorning = now.value.hour in 3..11
    val spotlightList = if (isMorning) azkarState.morning else azkarState.evening
    val spotlightTitle = if (isMorning) "أذكار الصباح" else "أذكار المساء"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Title + App Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically // Ensures parallel alignment
                    ) {
                        Text(
                            "مواقيت الصلاة",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // App Icon (Increased size)
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(72.dp) // Increased from 48dp to 56dp
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    // Row 2: Hijri Date + Day Name
                    state.day?.let { day ->
                        if (day.hijriDate.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hijri Date (e.g., 3 رمضان 1447هـ)
                                Text(
                                    day.hijriDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                // Day Name (e.g., الأحد)
                                val dayOfWeek = java.time.LocalDate.now().dayOfWeek
                                val arabicDayName = when (dayOfWeek) {
                                    java.time.DayOfWeek.SUNDAY -> "الأحد"
                                    java.time.DayOfWeek.MONDAY -> "الاثنين"
                                    java.time.DayOfWeek.TUESDAY -> "الثلاثاء"
                                    java.time.DayOfWeek.WEDNESDAY -> "الأربعاء"
                                    java.time.DayOfWeek.THURSDAY -> "الخميس"
                                    java.time.DayOfWeek.FRIDAY -> "الجمعة"
                                    java.time.DayOfWeek.SATURDAY -> "السبت"
                                }
                                Text(
                                    arabicDayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Divider line
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Row 3: Azkar Carousel with light background
                    if (spotlightList.isNotEmpty()) {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            AzkarCarouselHeader(
                                azkarList = spotlightList.take(5),
                                isMorning = isMorning,
                                title = spotlightTitle
                            )
                        }
                    } else {
                        // Fallback
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                 Icon(
                                    if (isMorning) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = Color(0xFFD4C4B0),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    spotlightTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Loading indicator
            item {
                AnimatedVisibility(visible = state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }

            // Offline/Error Indicator
            item {
                AnimatedVisibility(visible = state.isOffline || state.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.error != null) 
                                MaterialTheme.colorScheme.errorContainer 
                            else 
                                Color(0xFFFFF3E0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (state.error != null) Icons.Default.Warning else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (state.error != null) MaterialTheme.colorScheme.error else Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val message = if (state.error != null) {
                                state.error ?: "خطأ غير معروف"
                            } else {
                                "لا يوجد اتصال بالانترنت"
                            }
                            
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.error != null) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFE65100),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (state.isOffline && state.error == null) {
                                state.lastUpdated?.let {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "| $it",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFE65100).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hero Card
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF101418)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Main Content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 1. Location Name (Larger Size now)
                            val locationText = if (state.settings.locationMode == LocationMode.MANUAL) {
                                 "${state.settings.manualCity}، ${state.settings.manualCountry}"
                            } else {
                            state.cityName ?: state.day?.timezone?.substringAfter("/")?.replace("_", " ") ?: "الموقع التلقائي"
                            }
                            
                            Text(
                                locationText,
                                style = MaterialTheme.typography.headlineMedium, // Significantly Larger
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f),
                                textAlign = TextAlign.Center,
                                maxLines = 1, // Prevent multiline if possible
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(Modifier.height(8.dp))

                            // 2. "Next Prayer" Label
                            Text(
                                "الصلاة القادمة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(Modifier.height(12.dp))

                            // 3. Next Prayer Name
                            Text(
                                state.nextPrayerName ?: placeholderText,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // 4. Next Prayer Time
                            Text(
                                state.nextPrayerTime ?: placeholderTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Spacer(Modifier.height(16.dp))

                            // 5. Countdown
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                AnimatedContent(
                                    targetState = state.countdown ?: placeholderCountdown,
                                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                                    label = "countdown"
                                ) { t ->
                                    Text(
                                        t,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 6. Footer (Method)
                            Text(
                                state.settings.calculationMethod.labelAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Refresh Button (Top-Left of the Box) - kept same
                        IconButton(
                            onClick = { vm.refresh() },
                            modifier = Modifier
                                .align(Alignment.TopEnd) 
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Prayer times grid
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "مواقيت اليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val day = state.day
                        val prayers = listOf(
                            Triple("الفجر", day?.fajr ?: placeholderTime, Icons.Default.NightsStay),
                            Triple("الشروق", day?.sunrise ?: placeholderTime, Icons.Default.WbSunny),
                            Triple("الظهر", day?.dhuhr ?: placeholderTime, Icons.Default.WbSunny),
                            Triple("العصر", day?.asr ?: placeholderTime, Icons.Default.WbTwilight),
                            Triple("المغرب", day?.maghrib ?: placeholderTime, Icons.Default.NightsStay),
                            Triple("العشاء", day?.isha ?: placeholderTime, Icons.Default.DarkMode)
                        )

                        prayers.forEachIndexed { index, (name, time, icon) ->
                            val isNext = state.nextPrayerName == name
                            PrayerTimeRow(
                                name = name,
                                time = time,
                                icon = icon,
                                isNext = isNext
                            )
                            if (index < prayers.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
    }
}
}

@Composable
fun AzkarCarouselHeader(azkarList: List<ZikrItem>, isMorning: Boolean, title: String) {
    if (azkarList.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { azkarList.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Auto-scroll logic block
    LaunchedEffect(isDragged) {
        if (!isDragged) {
            while (true) {
                delay(6000)
                val nextPage = (pagerState.currentPage + 1) % azkarList.size
                pagerState.animateScrollToPage(
                    nextPage, 
                    animationSpec = tween(durationMillis = 800)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                if (isMorning) Icons.Default.WbSunny else Icons.Default.NightsStay,
                contentDescription = null,
                tint = Color(0xFFD4C4B0),
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Auto-scrolling Text with full content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp, max = 280.dp)
                .padding(horizontal = 16.dp),
            userScrollEnabled = true,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
             Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(horizontal = 8.dp)
                     .verticalScroll(rememberScrollState()),
                 contentAlignment = Alignment.Center
             ) {
                Text(
                    text = azkarList[page].text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
         // Dots Indicator
        DotPagerIndicator(
            pageCount = azkarList.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PrayerTimeRow(
    name: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNext: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
            )
            if (isNext) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "القادمة",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
