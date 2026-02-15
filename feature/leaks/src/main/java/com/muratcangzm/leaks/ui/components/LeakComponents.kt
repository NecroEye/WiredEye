package com.muratcangzm.leaks.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muratcangzm.leaks.ui.LeaksContract
import com.muratcangzm.shared.model.leak.TopDomain
import com.muratcangzm.shared.model.leak.TopServer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun LeaksBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(LeaksTokens.BackA, LeaksTokens.BackB, LeaksTokens.BackA)
            )
        )
    ) {
        RadarSweep(modifier = Modifier.matchParentSize())
        content()
    }
}

@Composable
private fun RadarSweep(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "radar")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "t"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.78f
        val cy = h * 0.22f
        val r = min(w, h) * 0.62f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LeaksTokens.Accent.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )

        val angle = (t * 2f * PI.toFloat())
        val dx = cos(angle)
        val dy = sin(angle)
        val x2 = cx + dx * r
        val y2 = cy + dy * r

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    LeaksTokens.Accent.copy(alpha = 0.0f),
                    LeaksTokens.Accent.copy(alpha = 0.22f),
                    LeaksTokens.Accent.copy(alpha = 0.0f)
                ),
                start = Offset(cx, cy),
                end = Offset(x2, y2)
            ),
            start = Offset(cx, cy),
            end = Offset(x2, y2),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        val rings = 4
        for (i in 1..rings) {
            val rr = r * (i / rings.toFloat())
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = rr,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2f)
            )
        }
    }
}

@Composable
fun LeaksScopeRow(
    selectedSeverity: LeaksContract.Severity,
    selectedTimeRange: LeaksContract.TimeRange,
    onSeverity: (LeaksContract.Severity) -> Unit,
    onTimeRange: (LeaksContract.TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = LeaksTokens.SurfaceStrong,
        shape = RoundedCornerShape(LeaksTokens.Corner),
        border = BorderStroke(1.dp, LeaksTokens.BorderBrush),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeaksChip(
                label = "Severity",
                value = selectedSeverity.name,
                accent = when (selectedSeverity) {
                    LeaksContract.Severity.All -> LeaksTokens.Accent
                    LeaksContract.Severity.Low -> LeaksTokens.Good
                    LeaksContract.Severity.Medium -> LeaksTokens.Warn
                    LeaksContract.Severity.High -> LeaksTokens.Danger
                },
                onClick = {
                    val next = when (selectedSeverity) {
                        LeaksContract.Severity.All -> LeaksContract.Severity.Low
                        LeaksContract.Severity.Low -> LeaksContract.Severity.Medium
                        LeaksContract.Severity.Medium -> LeaksContract.Severity.High
                        LeaksContract.Severity.High -> LeaksContract.Severity.All
                    }
                    onSeverity(next)
                }
            )

            Spacer(Modifier.width(2.dp))

            LeaksChip(
                label = "Window",
                value = selectedTimeRange.name,
                accent = LeaksTokens.Accent,
                onClick = {
                    val next = when (selectedTimeRange) {
                        LeaksContract.TimeRange.Last1h -> LeaksContract.TimeRange.Last24h
                        LeaksContract.TimeRange.Last24h -> LeaksContract.TimeRange.Last7d
                        LeaksContract.TimeRange.Last7d -> LeaksContract.TimeRange.Last1h
                    }
                    onTimeRange(next)
                }
            )
        }
    }
}

@Composable
private fun LeaksChip(
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(LeaksTokens.CornerSmall)
    val interaction = remember { MutableInteractionSource() }
    val inf = rememberInfiniteTransition(label = "chipPulse")
    val p by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "p"
    )
    val border = remember(accent, p) {
        Brush.linearGradient(
            listOf(
                LeaksTokens.Border.copy(alpha = 0.70f),
                accent.copy(alpha = 0.18f + 0.14f * p),
                LeaksTokens.Border.copy(alpha = 0.70f)
            )
        )
    }
    Surface(
        color = LeaksTokens.Surface,
        shape = shape,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = LeaksTokens.TextDim)
            Text(value, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LeaksGaugeCard(
    score: Int,
    totalQueries: Long,
    uniqueDomains: Int,
    publicDnsRatio: Double,
    suspiciousEntropyQueries: Long,
    burstQueries: Long,
    modifier: Modifier = Modifier
) {
    val clamped = score.coerceIn(0, 100)
    val severityColor by remember(clamped) {
        derivedStateOf {
            when {
                clamped >= 80 -> LeaksTokens.Danger
                clamped >= 55 -> LeaksTokens.Warn
                clamped >= 25 -> LeaksTokens.Accent
                else -> LeaksTokens.Good
            }
        }
    }
    val inf = rememberInfiniteTransition(label = "gauge")
    val sweep by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "sweep"
    )

    Surface(
        color = LeaksTokens.SurfaceStrong,
        shape = RoundedCornerShape(LeaksTokens.Corner),
        border = BorderStroke(1.dp, LeaksTokens.BorderBrush),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val r = size.minDimension / 2f
                    val stroke = 10f
                    val startAngle = 210f
                    val sweepAngleTotal = 300f
                    val sweepAngleValue = sweepAngleTotal * (clamped / 100f)

                    drawArc(
                        color = Color.White.copy(alpha = 0.06f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngleTotal,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                severityColor.copy(alpha = 0.15f),
                                severityColor.copy(alpha = 0.75f),
                                Color.White.copy(alpha = 0.60f),
                                severityColor.copy(alpha = 0.15f)
                            ),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngleValue,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    val ringPulse = 0.08f + 0.06f * (0.5f + 0.5f * sin(2f * PI.toFloat() * sweep))
                    drawCircle(
                        color = severityColor.copy(alpha = ringPulse),
                        radius = r * 1.08f,
                        style = Stroke(width = 2.2f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Risk", color = LeaksTokens.TextDim)
                    AnimatedContent(
                        targetState = clamped,
                        transitionSpec = {
                            (fadeIn(tween(140)) togetherWith fadeOut(tween(120))).using(SizeTransform(clip = false))
                        },
                        label = "score"
                    ) { v ->
                        Text(v.toString(), color = severityColor)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                LeaksMetricRow(label = "Total queries", value = totalQueries.toString(), accent = LeaksTokens.Accent)
                LeaksMetricRow(label = "Unique domains", value = uniqueDomains.toString(), accent = LeaksTokens.Accent)
                LeaksMetricRow(label = "Public DNS ratio", value = "${(publicDnsRatio * 100.0).toInt()}%", accent = LeaksTokens.Good)
                LeaksMetricRow(label = "Entropy suspicious", value = suspiciousEntropyQueries.toString(), accent = LeaksTokens.Warn)
                LeaksMetricRow(label = "Burst queries", value = burstQueries.toString(), accent = LeaksTokens.Danger)
            }
        }
    }
}

@Composable
private fun LeaksMetricRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = LeaksTokens.TextDim,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(value, color = accent)
        Spacer(Modifier.width(8.dp))
        LeaksAccentDot(accent = accent, modifier = Modifier.size(10.dp))
    }
}

@Composable
private fun LeaksAccentDot(
    accent: Color,
    modifier: Modifier = Modifier
) {
    val outerBrush = remember(accent) {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.28f),
                accent.copy(alpha = 0.12f),
                Color.Transparent
            )
        )
    }
    val midBrush = remember(accent) {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.22f),
                Color.Transparent
            )
        )
    }
    val coreBrush = remember(accent) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),
                accent.copy(alpha = 0.95f)
            )
        )
    }

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        drawCircle(brush = outerBrush, radius = r * 2.15f)
        drawCircle(brush = midBrush, radius = r * 1.55f)
        drawCircle(brush = coreBrush, radius = r * 0.90f)
        drawCircle(color = accent.copy(alpha = 0.92f), radius = r * 0.78f)
    }
}

@Composable
fun LeaksPanels(
    topDomains: List<TopDomain>,
    topServers: List<TopServer>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        LeaksListPanel(
            title = "Top domains",
            items = topDomains.map { "${it.domain} • ${it.count}" },
            modifier = Modifier.weight(1f)
        )
        LeaksListPanel(
            title = "Top servers",
            items = topServers.map { "${it.ip} • ${it.count}" },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LeaksListPanel(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "panelEdge")
    val p by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "p"
    )
    val edge = 0.10f + 0.10f * (0.5f + 0.5f * sin(2f * PI.toFloat() * p))
    val border = remember(edge) {
        Brush.linearGradient(
            listOf(
                LeaksTokens.Border.copy(alpha = 0.70f),
                LeaksTokens.Accent.copy(alpha = 0.22f + edge),
                LeaksTokens.Border.copy(alpha = 0.70f)
            )
        )
    }

    Surface(
        color = LeaksTokens.Surface,
        shape = RoundedCornerShape(LeaksTokens.Corner),
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = LeaksTokens.TextStrong)
            if (items.isEmpty()) {
                Text("No data yet.", color = LeaksTokens.TextDim)
            } else {
                items.take(8).forEach { line ->
                    Text(line, color = LeaksTokens.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
