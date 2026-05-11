package com.AbuAlaa.ui.screens

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.AbuAlaa.alarm.PrayerAlarmScheduler
import com.AbuAlaa.data.AdhanSound
import com.AbuAlaa.data.CalculationMethod
import com.AbuAlaa.data.SalahSound
import com.AbuAlaa.data.LocationMode
import com.AbuAlaa.noor.config.ConfigManager
import com.AbuAlaa.noor.data.ApiProvider
import com.AbuAlaa.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors
import com.AbuAlaa.data.ZekrData
import com.AbuAlaa.data.ZekrPrefs
import com.AbuAlaa.alarm.ZekrScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val s by vm.ui.collectAsState()

    val appCtx = ctx.applicationContext
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(appCtx, SchedulerEntryPoint::class.java)
    }
    val scheduler = entryPoint.scheduler()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSound by remember { mutableStateOf<AdhanSound?>(null) }
    var playingSalahSound by remember { mutableStateOf<SalahSound?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled by system */ }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled by system */ }

    fun requestLocation() {
        locationLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${ctx.packageName}")
            }
            ctx.startActivity(intent)
        }
    }

    fun toggleAdhanPreview(sound: AdhanSound) {
        if (playingSound == sound) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playingSound = null
        } else {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(ctx, sound.resId).apply {
                setOnCompletionListener {
                    release()
                    playingSound = null
                    mediaPlayer = null
                }
                start()
            }
            playingSound = sound
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "الإعدادات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.AbuAlaa.R.drawable.icon),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Location Settings
            item {
                SettingsCard(
                    title = "الموقع",
                    icon = Icons.Default.LocationOn
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = s.locationMode == LocationMode.AUTO,
                            onClick = { vm.setLocationMode(LocationMode.AUTO); requestLocation() },
                            label = { Text("تلقائي (GPS)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = s.locationMode == LocationMode.MANUAL,
                            onClick = { vm.setLocationMode(LocationMode.MANUAL) },
                            label = { Text("يدوي") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (s.locationMode == LocationMode.MANUAL) {
                        Spacer(Modifier.height(12.dp))
                       var cityInput by remember(s.manualCity) { mutableStateOf(s.manualCity) }
                       var countryInput by remember(s.manualCountry) { mutableStateOf(s.manualCountry) }
                        OutlinedTextField(
                            value = cityInput,
                            onValueChange = { cityInput = it },
                            label = { Text("المدينة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = countryInput,
                            onValueChange = { countryInput = it },
                            label = { Text("الدولة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                 LaunchedEffect(cityInput, countryInput) {
    vm.setManual(cityInput, countryInput)
                 }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            requestLocation()
                            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                vm.setLocationMode(LocationMode.AUTO)
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("طلب صلاحية الموقع")
                        }
                    }
                }
            }

            // Calculation Method
            item {
                SettingsCard(
                    title = "طريقة الحساب",
                    icon = Icons.Default.Calculate
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = s.calculationMethod.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختيار الطريقة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            CalculationMethod.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.labelAr) },
                                    onClick = {
                                        vm.setMethod(m)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Adhan Sound Settings
            item {
                SettingsCard(
                    title = "صوت الأذان",
                    icon = Icons.Default.Notifications
                ) {
                    Text(
                        "اختر صوت الأذان المفضل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    AdhanSound.entries.forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { vm.setAdhanSound(sound) }
                                .background(
                                    if (s.adhanSound == sound) MaterialTheme.colorScheme.outline
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = s.adhanSound == sound,
                                    onClick = { vm.setAdhanSound(sound) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Column {
                                    Text(
                                        sound.labelAr,
                                        fontWeight = if (s.adhanSound == sound) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (sound.isFull) {
                                        Text(
                                            "أذان كامل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { toggleAdhanPreview(sound) }) {
                                Icon(
                                    imageVector = if (playingSound == sound) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                                    contentDescription = if (playingSound == sound) "إيقاف" else "تشغيل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تشغيل الأذان الكامل")
                            Text(
                                "عند تفعيل هذا الخيار، سيتم تشغيل الأذان كاملاً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = s.playFullAdhan,
                            onCheckedChange = { vm.setPlayFullAdhan(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Notifications
            item {
                SettingsCard(
                    title = "الإشعارات",
                    icon = Icons.Default.Notifications
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل الإشعارات")
                        Switch(
                            checked = s.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                vm.setNotifications(enabled)
                                if (enabled) requestNotifications()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (s.notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduler.canScheduleExactAlarms()) {
                            Spacer(Modifier.height(12.dp))
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "⚠️ ملاحظة",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "جهازك يحتاج تفعيل (Exact Alarms) لإرسال تنبيهات الأذان بدقة",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { openExactAlarmSettings() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("تفعيل الآن")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Silent Fajr Setting
            item {
                SettingsCard(
                    title = "إعداد الفجر",
                    icon = Icons.Default.NightlightRound
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "الفجر بدون أذان",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "إشعار صامت لصلاة الفجر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = s.silentFajr,
                            onCheckedChange = { vm.setSilentFajr(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }


            // Zekr Settings
            item {
                var zekrEnabled by remember { mutableStateOf(ZekrPrefs.isEnabled(ctx)) }
                var selectedInterval by remember { mutableStateOf(ZekrPrefs.getIntervalInMinutes(ctx)) }
                var playbackMode by remember { mutableStateOf(ZekrPrefs.getPlaybackMode(ctx)) }
                var selectedRepeatIndex by remember { mutableStateOf(ZekrPrefs.getRepeatIndex(ctx)) }
                var zekrVolume by remember { mutableStateOf(ZekrPrefs.getVolume(ctx)) }
                var dhikrMenuExpanded by remember { mutableStateOf(false) }
                val intervals = listOf(1, 5, 10, 15, 20, 30, 60, 120)

                SettingsCard(
                    title = "الأذكار الصوتية 🤲",
                    icon = Icons.Default.Favorite
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (zekrEnabled) "الأذكار مفعلة ✅" else "الأذكار متوقفة 🔴",
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = zekrEnabled,
                            onCheckedChange = { v ->
                                zekrEnabled = v
                                ZekrPrefs.setEnabled(ctx, v)
                                if (v) ZekrScheduler.schedule(ctx, selectedInterval.toLong())
                                else ZekrScheduler.cancel(ctx)
                            }
                        )
                    }

                    if (zekrEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text("الفترة الزمنية بين الأذكار", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        var expanded2 by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded2,
                            onExpandedChange = { expanded2 = it }
                        ) {
                            OutlinedTextField(
                                value = "$selectedInterval دقيقة",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded2,
                                onDismissRequest = { expanded2 = false }
                            ) {
                                intervals.forEach { min ->
                                    DropdownMenuItem(
                                        text = { Text("$min دقيقة") },
                                        onClick = {
                                            selectedInterval = min
                                            ZekrPrefs.setIntervalInMinutes(ctx, min)
                                            if (zekrEnabled) ZekrScheduler.schedule(ctx, min.toLong())
                                            expanded2 = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("طريقة التشغيل", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (playbackMode == 0) "تسلسل (دور)" else "تكرار ذكر محدد",
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = playbackMode == 1,
                                onCheckedChange = {
                                    playbackMode = if (it) 1 else 0
                                    ZekrPrefs.setPlaybackMode(ctx, playbackMode)
                                }
                            )
                        }

                        if (playbackMode == 1) {
                            Spacer(Modifier.height(8.dp))
                            Text("اختر الذكر للتكرار", style = MaterialTheme.typography.bodySmall)
                            ExposedDropdownMenuBox(
                                expanded = dhikrMenuExpanded,
                                onExpandedChange = { dhikrMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = if (selectedRepeatIndex < ZekrData.zekrList.size)
                                        ZekrData.zekrList[selectedRepeatIndex].name
                                    else "اختر ذكر...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dhikrMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dhikrMenuExpanded,
                                    onDismissRequest = { dhikrMenuExpanded = false }
                                ) {
                                    ZekrData.zekrList.forEachIndexed { index, zekr ->
                                        DropdownMenuItem(
                                            text = { Text(zekr.name) },
                                            onClick = {
                                                selectedRepeatIndex = index
                                                ZekrPrefs.setRepeatIndex(ctx, index)
                                                dhikrMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("مستوى الصوت 🔊", style = MaterialTheme.typography.bodySmall)
                        Slider(
                          value = zekrVolume,
                          onValueChange = { newVal ->
                          zekrVolume = newVal
                         ZekrPrefs.setVolume(ctx, newVal)
                        },
                     valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                     activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                  modifier = Modifier.fillMaxWidth()
                     )
                        Text(
                            "${(zekrVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            // ↑ هنا item الأذكار اتقفل صح — بدون } زيادة

            // App Info
            item {
                SettingsCard(
                    title = "حول التطبيق",
                    icon = Icons.Default.Info
                ) {
                    Text(
                        "المؤذن - تطبيق محمد عبد العظيم الطويل مواقيت الصلاة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "الإصدار 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "🕌 مواقيت الصلاة الدقيقة\n🧭 اتجاه القبلة\n📅 التقويم الهجري\n📿 الأذكار\n🌙 عداد رمضان",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "المطور: علاء محمد عبد العظيم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))

                    ElevatedCard(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/msr7799"))
                            ctx.startActivity(intent)
                        },
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Report Bug",
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {}
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(80.dp))
            }

        } // ← LazyColumn
    } // ← Box
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SchedulerEntryPoint {
    fun scheduler(): PrayerAlarmScheduler
}

@Composable
fun spacer(height: Int) {
    Spacer(Modifier.height(height.dp))
}

@Composable
fun spacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoorSettingsCard() {
    var selectedProvider by remember {
        mutableStateOf(
            try { ApiProvider.valueOf(ConfigManager.get(ConfigManager.Keys.API_PROVIDER, "HUGGINGFACE")) }
            catch (e: Exception) { ApiProvider.HUGGINGFACE }
        )
    }
    var hfToken by remember { mutableStateOf(ConfigManager.get(ConfigManager.Keys.OPENAI_API_KEY, "")) }
    var googleApiKey by remember { mutableStateOf(ConfigManager.get(ConfigManager.Keys.GOOGLE_STUDIO_API_KEY, "")) }
    var showHfToken by remember { mutableStateOf(false) }
    var showGoogleKey by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "إعدادات نور (AI)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                "مزود الخدمة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر المزود") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ApiProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                selectedProvider = provider
                                ConfigManager.set(ConfigManager.Keys.API_PROVIDER, provider.name)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            if (selectedProvider == ApiProvider.HUGGINGFACE) {
                Text(
                    "HuggingFace Token",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "احصل على token من huggingface.co/settings/tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = hfToken,
                    onValueChange = {
                        hfToken = it
                        ConfigManager.set(ConfigManager.Keys.OPENAI_API_KEY, it)
                    },
                    label = { Text("HF Token") },
                    placeholder = { Text("hf_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showHfToken)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showHfToken = !showHfToken }) {
                            Icon(
                                if (showHfToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showHfToken) "إخفاء" else "إظهار"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (selectedProvider == ApiProvider.GOOGLE_AI_STUDIO) {
                Text(
                    "Google AI Studio API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "احصل على API Key من aistudio.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = googleApiKey,
                    onValueChange = {
                        googleApiKey = it
                        ConfigManager.set(ConfigManager.Keys.GOOGLE_STUDIO_API_KEY, it)
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("AIza...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showGoogleKey)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGoogleKey = !showGoogleKey }) {
                            Icon(
                                if (showGoogleKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showGoogleKey) "إخفاء" else "إظهار"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            val isConfigured = when (selectedProvider) {
                ApiProvider.HUGGINGFACE -> hfToken.isNotBlank()
                ApiProvider.GOOGLE_AI_STUDIO -> googleApiKey.isNotBlank()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800))
                )
                Text(
                    text = if (isConfigured) "✓ تم الإعداد" else "⚠ يرجى إدخال المفتاح",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }
    }
}
