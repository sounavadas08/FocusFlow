package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FocusFlowDarkColorScheme = darkColorScheme(
    primary = CalmTeal,
    onPrimary = OledBlack,
    primaryContainer = Color(0xFF00382E),
    onPrimaryContainer = TealGlow,
    secondary = ElectricCyan,
    onSecondary = OledBlack,
    secondaryContainer = Color(0xFF003544),
    onSecondaryContainer = Color(0xFFA3EEFF),
    tertiary = DeepIndigo,
    onTertiary = Color.White,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = AlertCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Preserve intentional OLED black and calm teal aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocusFlowDarkColorScheme,
        typography = Typography,
        content = content
    )
}
