package org.zeroshare.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zeroshare.app.presentation.theme.GlassBackground
import org.zeroshare.app.presentation.theme.GlassBorderLight
import org.zeroshare.app.presentation.theme.GlassBorderUltraLight
import org.zeroshare.app.presentation.theme.IosBlue
import org.zeroshare.app.presentation.theme.IosIndigo
import org.zeroshare.app.presentation.theme.IosMint

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackground)
    ) {
        // Ambient Liquid Glow (Simulating Apple Mesh Orbs behind Frosted Glass)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Top-Right Electric Blue / Cyan Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IosBlue.copy(alpha = 0.28f),
                        IosMint.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.85f, canvasHeight * 0.15f),
                    radius = canvasWidth * 0.85f
                )
            )

            // Bottom-Left Purple / Indigo Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IosIndigo.copy(alpha = 0.25f),
                        Color(0xFF8E8E93).copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.15f, canvasHeight * 0.85f),
                    radius = canvasWidth * 0.9f
                )
            )

            // Center subtle ambient aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x14007AFF),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                    radius = canvasWidth * 0.6f
                )
            )
        }

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color(0x18FFFFFF),
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val glassBorderBrush = Brush.verticalGradient(
        listOf(
            GlassBorderLight,
            GlassBorderUltraLight
        )
    )

    val glassBackgroundBrush = Brush.verticalGradient(
        listOf(
            backgroundColor,
            backgroundColor.copy(alpha = (backgroundColor.alpha * 0.6f).coerceIn(0f, 1f))
        )
    )

    val baseModifier = modifier
        .clip(shape)
        .background(glassBackgroundBrush)
        .border(width = borderWidth, brush = glassBorderBrush, shape = shape)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        content()
    }
}
