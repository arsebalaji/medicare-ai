package com.medicare.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle

private val Light = lightColorScheme(
    primary = Color(0xFF00796B), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F3EF), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF1565C0), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8FF), onSecondaryContainer = Color(0xFF001B3E),
    background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFEAF4F3),
    error = Color(0xFFC62828), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF4DB6AC), onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF00504A), onPrimaryContainer = Color(0xFFD7F3EF),
    secondary = Color(0xFF90CAF9), onSecondary = Color(0xFF00315F),
    secondaryContainer = Color(0xFF0D3C6E), onSecondaryContainer = Color(0xFFDCE8FF),
    background = Color(0xFF0F1514), surface = Color(0xFF0F1514), surfaceVariant = Color(0xFF1B2927),
    error = Color(0xFFFF8A80), errorContainer = Color(0xFF7A1A12), onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MediCareTheme(dark: Boolean, content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (dark) Dark else Light,
    shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
    typography = Typography(bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)),
    content = content
)