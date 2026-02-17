package com.muratcangzm.settings.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsUpgradeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailingText: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            scheme.primary.copy(alpha = 0.20f),
            scheme.surface.copy(alpha = 0.25f),
            scheme.primary.copy(alpha = 0.08f),
        )
    )

    val shouldShine = trailingText.equals("Soon", ignoreCase = true)

    val infinite = rememberInfiniteTransition(label = "upgrade_shine")
    val phase = infinite.animateFloat(
        initialValue = -0.30f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
        ),
        label = "upgrade_shine_phase"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = scheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            if (shouldShine) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CardDefaults.shape)
                        .background(Color.Transparent)
                        .drawWithContent {
                            drawContent()

                            val w = size.width
                            val h = size.height

                            val bandWidth = w * 0.22f
                            val xCenter = phase.value * w

                            val shine = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.45f to scheme.primary.copy(alpha = 0.00f),
                                    0.50f to scheme.primary.copy(alpha = 0.18f),
                                    0.55f to scheme.primary.copy(alpha = 0.00f),
                                    1.0f to Color.Transparent,
                                ),
                                start = Offset(xCenter - bandWidth, 0f),
                                end = Offset(xCenter + bandWidth, h)
                            )

                            drawRect(
                                brush = shine,
                                size = Size(w, h),
                                blendMode = BlendMode.SrcOver
                            )
                        }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(22.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.72f)
                    )
                }

                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
        }
    }
}

