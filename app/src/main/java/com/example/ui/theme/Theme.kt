package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberPurple,
    secondary = NeonCyan,
    tertiary = CinemaCoral,
    background = PitchBlack,
    surface = DeepSlate,
    onBackground = PureWhite,
    onSurface = TextSoft,
    surfaceVariant = ActiveGrey
)

@Composable
fun VeloEditTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
