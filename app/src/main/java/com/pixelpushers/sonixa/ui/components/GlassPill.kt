package com.pixelpushers.sonixa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // High-readability frosted glass background fill with increased blur opacity
    val glassBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x60FFFFFF), // High frosted top edge
                Color(0x30FFFFFF), // Translucent mid highlight
                Color(0xF812151E)  // Deep frosted base for maximum readability
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xF5F2F5F8)
            )
        )
    }

    // Specular border gradient (bright top rim fading down)
    val glassBorderBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xDDFFFFFF),
                Color(0x55FFFFFF),
                Color(0x22FFFFFF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xAA000000),
                Color(0x44000000)
            )
        )
    }

    val glowSpotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.5f else 0.3f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.7f),
                spotColor = glowSpotColor
            )
            .clip(shape)
            .background(glassBackgroundBrush)
            .border(width = 1.25.dp, brush = glassBorderBrush, shape = shape)
    ) {
        // Top Specular Reflection Shine Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.30f else 0.50f),
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.15f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 100f
                    )
                )
        )
        content()
    }
}
