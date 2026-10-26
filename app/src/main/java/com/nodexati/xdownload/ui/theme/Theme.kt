package com.nodexati.xdownload.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density

/** Cores de estado, as mesmas do RotaPRO. */
val DoneGreen = Color(0xFF1FA463)
val WarningAmber = Color(0xFFF59E0B)
val FailedRed = Color(0xFFE5484D)

/** Azul da marca (o mesmo #0B63F6 do RotaPRO) e a versão clara usada no modo escuro. */
private val BrandBlue = Color(0xFF0B63F6)
private val BrandBlueDark = Color(0xFF6EA2FF)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFF),
    onPrimaryContainer = Color(0xFF0A1630),
    secondary = BrandBlue,
    onSecondary = Color.White,
    background = Color(0xFFF3F5F9),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD5DAE3),
    error = FailedRed,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF0A1630),
    primaryContainer = Color(0xFF14284F),
    onPrimaryContainer = Color(0xFFE6EEFF),
    secondary = BrandBlueDark,
    onSecondary = Color(0xFF0A1630),
    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF121A29),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1A2335),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF2A3447),
    error = FailedRed,
    onError = Color.White,
)

/**
 * Tema do app, no mesmo molde do RotaPROTheme: claro/escuro escolhido nos ajustes
 * e opção de texto grande (aumenta a escala de fonte em 15%).
 */
@Composable
fun XDownloadTheme(darkTheme: Boolean, largeText: Boolean = false, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scaled = if (largeText) Density(density.density, density.fontScale * 1.15f) else density
    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
