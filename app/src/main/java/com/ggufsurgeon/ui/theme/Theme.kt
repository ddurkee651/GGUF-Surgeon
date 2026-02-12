package com.ggufsurgeon.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryLight,
    
    secondary = Secondary,
    onSecondary = Color.Black,
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = SecondaryLight,
    
    tertiary = Info,
    onTertiary = Color.Black,
    tertiaryContainer = Info.copy(alpha = 0.1f),
    onTertiaryContainer = Info,
    
    background = Background,
    onBackground = OnBackground,
    
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    
    error = Error,
    onError = Color.Black,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Primary.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryDark,
    
    secondary = Secondary,
    onSecondary = Color.Black,
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = SecondaryDark,
    
    tertiary = Info,
    onTertiary = Color.Black,
    tertiaryContainer = Info.copy(alpha = 0.1f),
    onTertiaryContainer = Info,
    
    background = Color.White,
    onBackground = Color.Black,
    
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    
    error = Error,
    onError = Color.White,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = ErrorDark
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
)
