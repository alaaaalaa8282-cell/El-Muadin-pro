package com.AbuAlaa.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Islamic Gold/Warm palette
private val IslamicGold = Color(0xFF10171A)
private val IslamicGoldDark = Color(0xFF10171A)
private val WarmBeige = Color(0xFFFDFBF5)
private val WarmCream = Color(0xFFFDFBF5)
private val DeepBrown = Color(0xFF5D4037)
private val AccentTeal = Color(0xFF008080)
private val SoftOrange = Color(0xFFE6A23C)
private val ErrorRed = Color(0xFFEF4444)

private val LightColors = lightColorScheme(
    primary = IslamicGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF3CD),
    onPrimaryContainer = DeepBrown,

    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),

    tertiary = SoftOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFECB3),
    onTertiaryContainer = Color(0xFF663C00),

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF4C0519),

    background = WarmBeige,
    onBackground = DeepBrown,
    surface = WarmCream,
    onSurface = DeepBrown,
    surfaceVariant = Color(0xFFF5F0E6),
    onSurfaceVariant = Color(0xFF10171A),
    outline = Color(0xFF10171A).copy(alpha = 0.5f),

    surfaceContainerHighest = Color.White,
    surfaceContainerHigh = Color(0xFFFFFDF8),
    surfaceContainer = Color(0xFFFDFBF5),
    surfaceContainerLow = Color(0xFFFDFBF5),
    surfaceContainerLowest = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD4C4B0),
    onPrimary = Color(0xFF221B17),
    primaryContainer = Color(0xFF3D3229),
    onPrimaryContainer = Color(0xFFEDE0D4),

    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),

    tertiary = Color(0xFFD4C4B0),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFEDE0D4),

    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFFE4E6),

    background = Color(0xFF1A1410),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF221B17),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF3D3229),
    onSurfaceVariant = Color(0xFFD4C4B0),
    outline = Color(0xFF9E8E7E),

    surfaceContainerHighest = Color(0xFF3D3229),
    surfaceContainerHigh = Color(0xFF332A22),
    surfaceContainer = Color(0xFF2A221C),
    surfaceContainerLow = Color(0xFF221B17),
    surfaceContainerLowest = Color(0xFF1A1410),
)

@Composable
fun MuadhinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
