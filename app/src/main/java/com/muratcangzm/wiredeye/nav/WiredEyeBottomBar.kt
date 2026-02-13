package com.muratcangzm.wiredeye.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WiredEyeBottomBar(
    tabs: List<TabSpec>,
    selectedRoute: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ghostSurface = Color(0x22101826)
    val ghostDim = Color(0xFF9EB2C0)
    val ghostBright = Color(0xFFDBEAFE)
    val ghostAccent = Color(0xFF7BD7FF)

    val shape = RoundedCornerShape(18.dp)
    val borderBrush = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF233355).copy(alpha = 0.55f),
                ghostAccent.copy(alpha = 0.35f),
                Color(0xFF233355).copy(alpha = 0.55f)
            )
        )
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Surface(
            color = ghostSurface,
            shape = shape,
            border = BorderStroke(1.dp, borderBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    BottomTabItem(
                        label = tab.label,
                        iconText = tab.iconText,
                        selected = tab.route == selectedRoute,
                        dim = ghostDim,
                        bright = ghostBright,
                        accent = ghostAccent,
                        onClick = { onTabClick(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    iconText: String,
    selected: Boolean,
    dim: Color,
    bright: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val onClickState by rememberUpdatedState(onClick)

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tab-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.72f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tab-alpha"
    )

    val inf = rememberInfiniteTransition(label = "tab-pulse")
    val pulse by inf.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tab-pulse-value"
    )

    val glow = if (selected) {
        val x = ((pulse - 0.35f) / 0.65f).coerceIn(0f, 1f)
        x * x * (3f - 2f * x)
    } else 0f

    val pillShape = RoundedCornerShape(16.dp)
    val pillBrush = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF1B2B45).copy(alpha = 0.35f),
                accent.copy(alpha = 0.18f),
                Color(0xFF1B2B45).copy(alpha = 0.35f)
            )
        )
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .clip(pillShape)
            .clickable(interactionSource = interaction, indication = null) { onClickState() }
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .alpha(alpha)
            .scale(scale)
    ) {
        Row(
            modifier = Modifier
                .clip(pillShape)
                .background(
                    brush = pillBrush,
                    alpha = if (selected) 1f else 0f
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(160)) togetherWith fadeOut(tween(120)))
                        .using(SizeTransform(clip = false))
                },
                label = "icon-anim"
            ) { isSel ->
                val iconScale = if (isSel) 1f + (0.06f * glow) else 1f
                val iconColor = if (isSel) accent else dim
                Text(
                    text = iconText,
                    fontSize = 16.sp,
                    color = iconColor,
                    modifier = Modifier.scale(iconScale),
                    style = TextStyle(
                        shadow = if (isSel) Shadow(
                            color = accent.copy(alpha = 0.55f),
                            blurRadius = 14f * glow
                        ) else Shadow(Color.Transparent, blurRadius = 0f)
                    )
                )
            }

            Text(
                text = label,
                color = if (selected) bright else dim,
                style = TextStyle(
                    shadow = if (selected) Shadow(
                        color = accent.copy(alpha = 0.28f),
                        blurRadius = 10f
                    ) else Shadow(Color.Transparent, blurRadius = 0f)
                )
            )
        }
    }
}
