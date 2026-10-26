package io.github.delgado74.wavelength.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BitcoinOrange = Color(0xFFF7931A)
val PositiveGreen = Color(0xFF5BD68B)
val NegativeRed = Color(0xFFFF7A7A)
val WarningAmber = Color(0xFFFFC857)

private val DarkColors = darkColorScheme(
    primary = BitcoinOrange,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF3A2A08),
    onPrimaryContainer = Color(0xFFFFDFA6),
    secondary = Color(0xFF9AD0FF),
    onSecondary = Color(0xFF00243D),
    background = Color(0xFF0E0F12),
    onBackground = Color(0xFFE8E8EA),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFE8E8EA),
    surfaceVariant = Color(0xFF23262E),
    onSurfaceVariant = Color(0xFFB9BCC6),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000),
    errorContainer = Color(0xFF3A0B0B),
    onErrorContainer = Color(0xFFFFD7D7),
)

@Composable
fun WavelengthTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
