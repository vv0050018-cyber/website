package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = MedicalCyanLight,
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C70),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = MedicalTealLight,
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF005049),
    onSecondaryContainer = Color(0xFF70F7E6),
    tertiary = Color(0xFFA5C8FF),
    background = DeepNavyBg,
    onBackground = Color(0xFFE2E8F0),
    surface = CardNavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF163255),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334E68)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = MedicalTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF70F7E6),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF1D4ED8),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200
)

@Composable
fun KidneyAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Prefer tailored medical aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KidneyAiTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
