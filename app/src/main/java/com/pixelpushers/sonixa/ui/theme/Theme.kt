package com.pixelpushers.sonixa.ui.theme

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
import com.pixelpushers.sonixa.data.preferences.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = AccentMintDark,
    onPrimary = Color.Black,
    background = BackgroundDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = LineDark,
    onSurfaceVariant = MutedTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = AccentMintLight,
    onPrimary = Color.White,
    background = BackgroundLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = LineLight,
    onSurfaceVariant = MutedTextLight
)

private val BlackColorScheme = darkColorScheme(
    primary = AccentMintDark,
    onPrimary = Color.Black,
    background = BackgroundBlack,
    onBackground = TextBlack,
    surface = SurfaceBlack,
    onSurface = TextBlack,
    surfaceVariant = LineBlack,
    onSurfaceVariant = MutedTextBlack
)

@Composable
fun SonixaTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    materialDesignEnabled: Boolean = true,
    materialYouEnabled: Boolean = true,
    dynamicAccentColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val baseColorScheme = when (appTheme) {
        AppTheme.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.BLACK -> BlackColorScheme
    }

    val finalColorScheme = when {
        materialDesignEnabled && materialYouEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dynamicAccentColor != null -> {
            baseColorScheme.copy(primary = dynamicAccentColor)
        }
        else -> {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
