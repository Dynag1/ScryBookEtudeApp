package co.dynag.scrybook.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = InkBlue80,
    onPrimary = InkBlue20,
    primaryContainer = InkBlue40,
    onPrimaryContainer = InkBlue80,
    secondary = ParchmentGold80,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = ParchmentGold40,
    onSecondaryContainer = ParchmentGold80,
    tertiary = LeatherBrown80,
    onTertiary = Color(0xFF241200),
    tertiaryContainer = LeatherBrown40,
    onTertiaryContainer = LeatherBrown80,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    error = ErrorRed,
    onError = Color(0xFF1A0008)
)

private val LightColorScheme = lightColorScheme(
    primary = InkBlue40,
    onPrimary = Color.White,
    primaryContainer = InkBlue80,
    onPrimaryContainer = InkBlue20,
    secondary = ParchmentGold40,
    onSecondary = Color.White,
    secondaryContainer = ParchmentGold80,
    onSecondaryContainer = Color(0xFF1A1200),
    tertiary = LeatherBrown40,
    onTertiary = Color.White,
    tertiaryContainer = LeatherBrown80,
    onTertiaryContainer = Color(0xFF1F0800),
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ScryBookTheme(
    appTheme: String = "system",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        "dark" -> true
        "light" -> false
        else -> darkTheme
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScryBookTypography,
        content = content
    )
}
