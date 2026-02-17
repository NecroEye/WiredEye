package com.muratcangzm.settings.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.max
import kotlin.math.min

@Composable
fun SettingsSurface(
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val transition = rememberInfiniteTransition(label = "settings_surface_drift")

        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 52000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "settings_surface_drift_progress"
        )

        val safeWidth = max(1f, widthPx)
        val safeHeight = max(1f, heightPx)

        val driftY = (progress - 0.5f) * 2f * (safeHeight * 0.045f)
        val driftX = (progress - 0.5f) * 2f * (safeWidth * 0.035f)

        val baseTop = Color(0xFF0A1624)
        val baseMid = Color(0xFF08172B)
        val baseBottom = Color(0xFF070F1B)

        val glowA = Color(0xFF1A4B73)
        val glowB = Color(0xFF103A5E)

        val baseBrush = remember(safeWidth, safeHeight, driftY) {
            Brush.verticalGradient(
                colors = listOf(
                    baseTop,
                    baseMid,
                    baseBottom
                ),
                startY = min(0f, driftY),
                endY = safeHeight + max(0f, driftY)
            )
        }

        val glowBrush = remember(safeWidth, safeHeight, driftX, driftY) {
            Brush.radialGradient(
                colors = listOf(
                    glowA.copy(alpha = 0.28f),
                    glowB.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(
                    x = (safeWidth * 0.20f) + driftX,
                    y = (safeHeight * 0.18f) + driftY
                ),
                radius = safeWidth * 1.15f
            )
        }

        val glowBrushSecondary = remember(safeWidth, safeHeight, driftX, driftY) {
            Brush.radialGradient(
                colors = listOf(
                    glowB.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(
                    x = (safeWidth * 0.85f) - driftX,
                    y = (safeHeight * 0.70f) - (driftY * 0.7f)
                ),
                radius = safeWidth * 1.35f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(baseBrush)
                .background(glowBrush)
                .background(glowBrushSecondary)
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun SettingsSurfacePreview() {
    SettingsSurface {}
}
