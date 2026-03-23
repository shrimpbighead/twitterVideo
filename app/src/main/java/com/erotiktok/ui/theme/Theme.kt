package com.erotiktok.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// TikTok 风格配色
val PrimaryRed = Color(0xFFFE2C55)
val PrimaryCyan = Color(0xFF25F4EE)
val BackgroundBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF161823)
val OnSurfaceDark = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryRed,
    secondary = PrimaryCyan,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
)

/**
 * 辅助函数：安全地从 Context 中寻找 Activity
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun EroTikTokTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
