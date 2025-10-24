package com.muratcangzm.common.extensions

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.muratcangzm.common.components.SpotlightRegistry

@Composable
fun Modifier.spotlightTarget(
    id: String,
    registry: SpotlightRegistry,
    extraPadding: Dp = 12.dp
): Modifier {
    val density = LocalDensity.current
    return this.then(
        Modifier.onGloballyPositioned { coords ->
            val extraPx = with(density) { extraPadding.toPx() }
            val b = coords.boundsInWindow()
            val rect = Rect(
                offset = b.topLeft - Offset(extraPx, extraPx),
                size = Size(b.width + 2 * extraPx, b.height + 2 * extraPx)
            )
            registry.update(id, rect)
        }
    )
}

fun Modifier.spotlightTapThrough(
    registry: SpotlightRegistry,
    onRectPicked: (Rect) -> Unit
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .pointerInput(registry) {
        detectTapGestures { posLocal -> /* remains empty */ }
    }