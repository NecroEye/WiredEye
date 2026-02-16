package com.muratcangzm.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GhostShimmer(
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    baseAlpha: Float = 0.10f,
    highlightAlpha: Float = 0.22f
) {
    val inf = rememberInfiniteTransition(label = "ghost_shimmer")
    val p by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p"
    )

    val base = Color.White.copy(alpha = baseAlpha)
    val highlight = Color.White.copy(alpha = highlightAlpha)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = Offset(x = -800f + 1600f * p, y = 0f),
                    end = Offset(x = 0f + 1600f * p, y = 0f)
                )
            )
    )
}

@Composable
fun GhostShimmerLine(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 999.dp
) {
    GhostShimmer(
        modifier = modifier
            .width(width)
            .height(height),
        corner = corner
    )
}

@Composable
fun GhostShimmerSpacer(height: Dp) {
    Spacer(Modifier.height(height))
}
