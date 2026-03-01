package com.example.myradio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = StereoAmber,
    secondary = StereoOrange,
    tertiary = StereoRed,
    background = StereoBlack,
    surface = StereoPanel,
    surfaceVariant = StereoPanelRaised,
    onPrimary = StereoBlack,
    onSecondary = StereoBlack,
    onTertiary = StereoBlack,
    onBackground = StereoText,
    onSurface = StereoText,
    onSurfaceVariant = StereoSubtext,
    outline = StereoOutline
)

@Composable
fun MyRadioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
