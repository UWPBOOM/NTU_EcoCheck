package com.ntu.electricity.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun NTUElectricityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    colorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val colors = m3ColorSchemes.getOrElse(colorIndex) { m3ColorSchemes[0] }

    val lightScheme = lightColorScheme(
        primary = colors.lightPrimary,
        onPrimary = Surface,
        primaryContainer = colors.lightContainer,
        onPrimaryContainer = colors.lightOnContainer,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
        outlineVariant = OutlineVariant,
        error = Error,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
    )

    val darkScheme = darkColorScheme(
        primary = colors.darkPrimary,
        onPrimary = SurfaceDark,
        primaryContainer = colors.darkContainer,
        onPrimaryContainer = colors.darkOnContainer,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        error = ErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
