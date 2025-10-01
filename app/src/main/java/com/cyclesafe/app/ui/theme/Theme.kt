package com.cyclesafe.app.ui.theme

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

// 🎨 CycleSafe boje
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00BFA5),      // tirkizna
    onPrimary = Color.White,
    secondary = Color(0xFFFFB300),    // žuta
    onSecondary = Color.Black,
    background = Color(0xFFF5F5F5),   // svetlo siva
    onBackground = Color(0xFF212121), // tamno siva
    surface = Color.White,
    onSurface = Color(0xFF212121),
    error = Color(0xFFD32F2F),        // crvena
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color.White,
    secondary = Color(0xFFFFB300),
    onSecondary = Color.Black,
    background = Color(0xFF121212),   // tamna pozadina
    onBackground = Color(0xFFE0E0E0), // svetlo siva za tekst
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun CycleSafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
