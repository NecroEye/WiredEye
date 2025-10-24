package com.muratcangzm.common.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SpotlightOverlay(
    controller: SpotlightController,
    registry: SpotlightRegistry,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    dimColor: Color = Color.Black.copy(alpha = 0.65f),
    cornerRadius: Dp = 16.dp
) {
    if (!visible) return

    val cornerPx = with(LocalDensity.current) { cornerRadius.toPx() }

    var overlayCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    val targetWindow = controller.current
    val targetLocal: Rect? = remember(targetWindow, overlayCoords) {
        targetWindow?.let { r ->
            overlayCoords?.let { coords ->
                val tl = coords.windowToLocal(r.topLeft)
                Rect(tl, r.size)
            }
        }
    }

    val animated by animateRectAsState(targetValue = targetLocal ?: Rect.Zero, label = "spotRect")
    val alpha by animateFloatAsState(targetValue = if (targetLocal != null) 1f else 1f, label = "overlayAlpha")

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayCoords = it }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, alpha = alpha)
            .drawWithContent {
                drawContent()
                drawRect(dimColor)

                if (targetLocal != null && animated.width > 1f && animated.height > 1f) {
                    val glowRadius = animated.maxDimension * 0.75f
                    if (glowRadius > 1f) {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                                center = animated.center,
                                radius = glowRadius
                            ),
                            topLeft = animated.topLeft,
                            size = animated.size,
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    }
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = animated.topLeft,
                        size = animated.size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        blendMode = BlendMode.Clear
                    )
                }
            }
    )
}

@Composable
fun SpotlightAutoFocus(
    visible: Boolean,
    registry: SpotlightRegistry,
    controller: SpotlightController,
    targetId: String,
    retries: Int = 12,
    delayMs: Long = 50
) {
    LaunchedEffect(visible, targetId) {
        if (!visible) return@LaunchedEffect
        repeat(retries) {
            registry.get(targetId)?.let { r ->
                controller.highlight(r)
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(delayMs)
        }
    }
}