package com.AbuAlaa.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AppGradientBackground(content: @Composable () -> Unit) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFDFBF5),  // Warm beige
            Color(0xFFFDFBF5),  // Light cream
            Color(0xFFFDFBF5)   // Warm beige
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}
