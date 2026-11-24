package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MidnightStudioColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = ElectricVioletBright,
    secondary = NeonCyan,
    onSecondary = Color(0xFF002026),
    secondaryContainer = DeepCyanContainer,
    onSecondaryContainer = NeonCyan,
    tertiary = CoralRose,
    onTertiary = Color.White,
    tertiaryContainer = DeepCoralContainer,
    onTertiaryContainer = Color(0xFFFFB2CA),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    outline = ObsidianBorder,
    error = ErrorCrimson,
    onError = Color.White
)

private val DaylightStudioColorScheme = lightColorScheme(
    primary = VioletPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = CyanSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F7FA),
    onSecondaryContainer = Color(0xFF00363D),
    tertiary = RoseTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4EE),
    onTertiaryContainer = Color(0xFF590024),
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = DaylightSurfaceElevated,
    surfaceContainerHigh = DaylightSurfaceVariant,
    outline = DaylightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VidGenAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MidnightStudioColorScheme else DaylightStudioColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VidGenAITypography,
        shapes = StudioShapes,
        content = content
    )
}
