package com.aim.earny.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EarnyColors = darkColorScheme(
    primary = Gold,
    secondary = Orange,
    background = EarnyBlack,
    surface = EarnySurface,
    onPrimary = EarnyBlack,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun EarnyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = EarnyBlack.toArgb()
            window.navigationBarColor = EarnyBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = EarnyColors, typography = EarnyTypography, content = content)
}
