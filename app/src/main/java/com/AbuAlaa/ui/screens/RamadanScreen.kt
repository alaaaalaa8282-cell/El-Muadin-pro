package com.AbuAlaa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.AbuAlaa.ui.viewmodel.RamadanViewModel

@Composable
fun RamadanScreen(
    contentPadding: PaddingValues,
    vm: RamadanViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    val placeholderText = "--"
    val placeholderTime = "--:--"
    val placeholderCountdown = "--:--:--"

    val ramadanStatus = if (state.isRamadan) "رمضان 🌙" else "ليس رمضان"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("رمضان", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { vm.refresh() }) { Text("تحديث") }
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let { err ->
            Text(err, color = MaterialTheme.colorScheme.error)
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("التاريخ الهجري: ${state.hijriDate ?: placeholderText}")
                Text("حالة اليوم: $ramadanStatus")
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الإمساك: ${state.suhoorEndsAt ?: placeholderTime}")
                Text("الإفطار: ${state.iftarAt ?: placeholderTime}")
                HorizontalDivider()
                Text(state.nextEventTitle ?: placeholderText)
                Text(
                    text = state.countdown ?: placeholderCountdown,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        if (!state.isRamadan) {
            Text(
                "ملاحظة: العدّادات تعتمد على (الإمساك/المغرب) حتى لو كان الشهر غير رمضان — تقدر تعتبرها وضع تجهيز مسبق.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
