package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SpaceBlueAccent,
    onPrimary = Color.White,
    secondary = SpaceLightAccent,
    onSecondary = Color.Black,
    tertiary = SpaceBluePrimary,
    background = DeepSpaceBlue,
    onBackground = Color(0xFFDDEEFF),
    surface = DeepSpaceBlue,
    onSurface = Color(0xFFDDEEFF)
)

private val LightColorScheme = lightColorScheme(
    primary = DarkNavyLight,
    onPrimary = Color.White,
    secondary = DarkNavySecondary,
    onSecondary = Color.White,
    tertiary = SpaceBluePrimary,
    background = LightIceBlue,
    onBackground = DarkNavyLight,
    surface = LightIceBlue,
    onSurface = DarkNavyLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
