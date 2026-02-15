package com.leosoft.longevity.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF7FBFA1),
    secondary = Color(0xFF7DA6D9),
    tertiary = Color(0xFFB8A6D9),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E2A28),
    onSurface = Color(0xFF4B5C59)
)

@Composable
fun LongevityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content
    )
}
