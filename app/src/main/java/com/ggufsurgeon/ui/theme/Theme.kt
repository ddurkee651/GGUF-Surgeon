package com.ggufsurgeon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A5BD4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A5BD4).copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF6D7CF0),
    secondary = Color(0xFF7EC8E3),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF7EC8E3).copy(alpha = 0.1f),
    onSecondaryContainer = Color(0xFF5AA3BE),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF64B5F6).copy(alpha = 0.1f),
    onTertiaryContainer = Color(0xFF64B5F6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFA0A0A0),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFCF6679).copy(alpha = 0.1f),
    onErrorContainer = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A5BD4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A5BD4).copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF3549B0),
    secondary = Color(0xFF7EC8E3),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF7EC8E3).copy(alpha = 0.1f),
    onSecondaryContainer = Color(0xFF5AA3BE),
    tertiary = Color(0xFF64B5F6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF64B5F6).copy(alpha = 0.1f),
    onTertiaryContainer = Color(0xFF64B5F6),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFB00020).copy(alpha = 0.1f),
    onErrorContainer = Color(0xFFB00020)
)

@Composable
fun GGUFSurgeonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        shapes = Shapes,
        content = content
    )
}

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
