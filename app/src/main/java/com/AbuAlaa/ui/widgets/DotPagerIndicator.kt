package com.AbuAlaa.ui.widgets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF10171A),
    inactiveColor: Color = Color(0xFF10171A).copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val size by animateDpAsState(
                targetValue = if (selected) 10.dp else 6.dp,
                label = "dot"
            )
            val color = if (selected) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .size(size)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}
