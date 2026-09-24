package com.mid.varagh.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Varagh app theme.
 *
 * @param darkTheme whether to use the dark scheme.
 * @param dynamicColor use Material You wallpaper colours on Android 12+; falls back to the
 * book-themed palette otherwise.
 */
@Composable
fun VaraghTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicTheming() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VaraghDarkColors
        else -> VaraghLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VaraghTypography,
        content = content,
    )
}

fun supportsDynamicTheming(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
