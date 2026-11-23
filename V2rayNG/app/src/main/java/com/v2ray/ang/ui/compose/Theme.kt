package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// fatbee: 胖蜜蜂配色 —— 蜂蜜黄 #F5B800 + 墨黑 #14130F + 暖白底（跟面板 / 官网一致）
private val LightColor = lightColorScheme(
    primary = Color(0xFFF5B800), // Honey yellow
    onPrimary = Color(0xFF14130F), // Ink
    primaryContainer = Color(0xFFFFF4CC), // Pale honey
    onPrimaryContainer = Color(0xFF3D2E00), // Dark honey
    secondary = Color(0xFF14130F), // Ink
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFFFF4CC), // Pale honey
    onSecondaryContainer = Color(0xFF3D2E00), // Dark honey
    tertiary = Color(0xFF16A34A), // Green
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFDCFCE7), // Light green
    onTertiaryContainer = Color(0xFF052E16), // Dark green
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFF6F5F1), // Warm white
    onBackground = Color(0xFF14130F), // Ink
    surface = Color(0xFFF6F5F1), // Warm white
    onSurface = Color(0xFF14130F), // Ink
    surfaceVariant = Color(0xFFECEAE3), // Warm gray
    onSurfaceVariant = Color(0xFF5F5B52), // Muted ink
    outline = Color(0xFF8A867B), // Warm medium gray
    outlineVariant = Color(0xFFDDD9CF), // Warm light gray
    inverseSurface = Color(0xFF14130F), // Ink
    inverseOnSurface = Color(0xFFF6F5F1), // Warm white
    inversePrimary = Color(0xFFF5B800), // Honey yellow
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFF5B800), // Honey yellow
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFFFFFFF), // White (cards)
    surfaceContainer = Color(0xFFF1EFE9), // Warm light gray
    surfaceContainerHigh = Color(0xFFECEAE3), // Warm gray
    surfaceContainerHighest = Color(0xFFE6E3DB), // Warm gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFF5B800), // Honey yellow
    onPrimary = Color(0xFF14130F), // Ink
    primaryContainer = Color(0xFF4A3800), // Deep honey
    onPrimaryContainer = Color(0xFFFFF4CC), // Pale honey
    secondary = Color(0xFFF5B800), // Honey yellow
    onSecondary = Color(0xFF14130F), // Ink
    secondaryContainer = Color(0xFF3A3220), // Dark honey brown
    onSecondaryContainer = Color(0xFFFFF4CC), // Pale honey
    tertiary = Color(0xFF4ADE80), // Green
    onTertiary = Color(0xFF052E16), // Dark green
    tertiaryContainer = Color(0xFF14532D), // Deep green
    onTertiaryContainer = Color(0xFFDCFCE7), // Light green
    error = Color(0xFFFFB4AB), // Light Red
    errorContainer = Color(0xFF93000A), // Dark Red
    onError = Color(0xFF690005), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF14130F), // Ink
    onBackground = Color(0xFFECEAE3), // Warm light gray
    surface = Color(0xFF14130F), // Ink
    onSurface = Color(0xFFECEAE3), // Warm light gray
    surfaceVariant = Color(0xFF34312A), // Warm dark gray
    onSurfaceVariant = Color(0xFFC9C5BA), // Warm light gray
    outline = Color(0xFF948F83), // Warm gray
    outlineVariant = Color(0xFF34312A), // Warm dark gray
    inverseSurface = Color(0xFFECEAE3), // Warm light gray
    inverseOnSurface = Color(0xFF14130F), // Ink
    inversePrimary = Color(0xFF8A6500), // Dark honey
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFFF5B800), // Honey yellow
    surfaceContainerLowest = Color(0xFF0E0D0A), // Near black
    surfaceContainerLow = Color(0xFF1C1B17), // Dark card
    surfaceContainer = Color(0xFF211F1A), // Dark gray
    surfaceContainerHigh = Color(0xFF2A2822), // Dark gray
    surfaceContainerHighest = Color(0xFF34312A), // Dark gray
)

// Semantic Colors
val colorPing = Color(0xFF16A34A) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFFB38600) // Dark honey (readable on white)
val colorFabActive = Color(0xFFF5B800) // Honey yellow
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD93F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
