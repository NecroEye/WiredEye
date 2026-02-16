package com.muratcangzm.leaks.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun LeaksShimmerStrip(
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "shimmer")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    val shape = RoundedCornerShape(LeaksTokens.CornerSmall)
    val brush = remember(t) {
        val base = LeaksTokens.Border.copy(alpha = 0.25f)
        val hi = LeaksTokens.Accent.copy(alpha = 0.22f)
        val x = -0.6f + 1.8f * t
        Brush.linearGradient(
            colors = listOf(base, hi, base),
            start = androidx.compose.ui.geometry.Offset(x * 1000f, 0f),
            end = androidx.compose.ui.geometry.Offset((x + 0.6f) * 1000f, 0f)
        )
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(brush)
    )
}
