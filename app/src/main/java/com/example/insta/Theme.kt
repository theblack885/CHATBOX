package com.example.insta

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BrandPink = Color(0xFFFF2D87)
val BrandBrush = Brush.linearGradient(
    listOf(Color(0xFF7C4DFF), Color(0xFFFF2D87), Color(0xFFFF9F43))
)

private val DarkColors = darkColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    background = Color(0xFF0A0A0F),
    onBackground = Color.White,
    surface = Color(0xFF14141C),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1F1F2B),
    onSurfaceVariant = Color(0xFFA3A3B8)
)

private val LightColors = lightColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    background = Color(0xFFF4F4FA),
    onBackground = Color(0xFF111118),
    surface = Color.White,
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFEBEBF4),
    onSurfaceVariant = Color(0xFF6B6B7D)
)

@Composable
fun InstaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
