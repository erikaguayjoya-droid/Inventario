package com.tuapp.inventory.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Blue800,
    onPrimary          = Color.White,
    primaryContainer   = Blue100,
    onPrimaryContainer = Blue900,
    secondary            = Blue600,
    onSecondary          = Color.White,
    secondaryContainer   = Blue50,
    onSecondaryContainer = Blue800,
    tertiary            = Gray700,
    onTertiary          = Color.White,
    tertiaryContainer   = Gray100,
    onTertiaryContainer = Gray900,
    error            = RedError,
    onError          = Color.White,
    errorContainer   = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF410002),
    background       = Gray50,
    onBackground     = Gray900,
    surface          = SurfaceLight,
    onSurface        = Gray900,
    surfaceVariant   = Blue50,
    onSurfaceVariant = Gray700,
    outline          = Gray400,
    outlineVariant   = Blue100
)

private val DarkColorScheme = darkColorScheme(
    primary            = Blue400,
    onPrimary          = Blue900,
    primaryContainer   = Blue800,
    onPrimaryContainer = Blue50,
    secondary            = Blue400,
    onSecondary          = Blue900,
    secondaryContainer   = Blue700,
    onSecondaryContainer = Blue50,
    tertiary            = Gray400,
    onTertiary          = Gray900,
    tertiaryContainer   = Gray700,
    onTertiaryContainer = Gray100,
    error            = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError          = androidx.compose.ui.graphics.Color(0xFF690005),
    errorContainer   = androidx.compose.ui.graphics.Color(0xFF93000A),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    background       = androidx.compose.ui.graphics.Color(0xFF111320),
    onBackground     = OnSurfaceDark,
    surface          = SurfaceDark,
    onSurface        = OnSurfaceDark,
    surfaceVariant   = androidx.compose.ui.graphics.Color(0xFF252840),
    onSurfaceVariant = Blue100,
    outline          = Blue400,
    outlineVariant   = Blue700
)

@Composable
fun InventoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = InventoryTypography,
        shapes      = InventoryShapes,
        content     = content
    )
}
