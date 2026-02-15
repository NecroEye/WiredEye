package com.muratcangzm.summary.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.muratcangzm.summary.ui.SummaryContract
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GhostBackground(
    modifier: Modifier,
    stars: Int,
    shootingStars: Boolean,
    shootingStarsOnlyInTopHalf: Boolean
) {
    val bgBrush = remember {
        Brush.linearGradient(
            colors = listOf(SummaryTokens.BgTop, SummaryTokens.BgMid, SummaryTokens.BgBottom)
        )
    }
    Box(modifier = modifier.background(bgBrush)) {
        StarField(modifier = Modifier.matchParentSize(), count = stars)
        if (shootingStars) {
            ShootingStars(
                modifier = Modifier.matchParentSize(),
                onlyTopHalf = shootingStarsOnlyInTopHalf
            )
        }
        SubtleVignette(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun StarField(
    modifier: Modifier,
    count: Int
) {
    val specs = rememberStarSpecs(count)

    val isPreview = LocalInspectionMode.current
    val inf = rememberInfiniteTransition(label = "starfield")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseAccent = SummaryTokens.Accent
        val white = Color.White

        val clock = if (isPreview) 0.35f else t

        for (s in specs) {
            val raw = ((clock * s.twinkle) + s.phase) % 1f
            val tri = 1f - abs(2f * raw - 1f)
            val eased = FastOutSlowInEasing.transform(tri.coerceIn(0f, 1f))
            val tw = (0.55f + 0.45f * eased).coerceIn(0.12f, 1f)
            val alpha = (s.a * tw).coerceIn(0f, 1f)

            val cx = s.x * w
            val cy = s.y * h
            val center = Offset(cx, cy)

            drawCircle(
                color = baseAccent.copy(alpha = alpha * 0.10f),
                radius = s.r * 2.1f,
                center = center
            )
            drawCircle(
                color = white.copy(alpha = alpha),
                radius = s.r,
                center = center
            )
        }
    }
}

@Composable
private fun rememberStarSpecs(count: Int): List<StarSpec> {
    val clamped = count.coerceIn(40, 420)
    return remember(clamped) {
        val rnd = Random(1337 + clamped)
        List(clamped) {
            StarSpec(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                r = 0.55f + rnd.nextFloat() * 1.65f,
                a = 0.10f + rnd.nextFloat() * 0.55f,
                twinkle = 0.25f + rnd.nextFloat() * 0.75f,
                phase = rnd.nextFloat()
            )
        }
    }
}

@Immutable
private data class StarSpec(
    val x: Float,
    val y: Float,
    val r: Float,
    val a: Float,
    val twinkle: Float,
    val phase: Float
)

@Composable
private fun SubtleVignette(modifier: Modifier) {
    Canvas(modifier) {
        val r = (minOf(size.width, size.height) * 0.72f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF000000).copy(alpha = 0.28f)
                ),
                center = center,
                radius = r
            )
        )
    }
}

@Stable
private data class ShootingStarSpec(
    val startX: Float,
    val startY: Float,
    val angleRad: Float,
    val lengthPx: Float,
    val durationMs: Int,
    val delayMs: Int
)

@Composable
private fun ShootingStars(
    modifier: Modifier,
    onlyTopHalf: Boolean
) {
    val specs = rememberShootingStarSpecs(onlyTopHalf)

    val isPreview = LocalInspectionMode.current
    val inf = rememberInfiniteTransition(label = "shooting-stars")
    val clock by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "clock"
    )

    val totalMs = 9000f
    val tMs = (if (isPreview) 0.52f else clock) * totalMs

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val accent = SummaryTokens.Accent
        val white = Color.White

        for (s in specs) {
            val local = (tMs - s.delayMs).toFloat()
            if (local <= 0f || local >= s.durationMs.toFloat()) continue

            val raw = local / s.durationMs.toFloat()
            val p = FastOutSlowInEasing.transform(raw.coerceIn(0f, 1f))

            val fadeIn = (raw / 0.12f).coerceIn(0f, 1f)
            val fadeOut = ((1f - raw) / 0.18f).coerceIn(0f, 1f)
            val fade = (fadeIn * fadeOut).coerceIn(0f, 1f)

            val dx = cos(s.angleRad)
            val dy = sin(s.angleRad)

            val cx = s.startX * w + p * (dx * s.lengthPx)
            val cy = s.startY * h + p * (dy * s.lengthPx)

            val tail = (90f + 160f * (1f - p)) * (1f + 0.25f * p)
            val x2 = cx - dx * tail
            val y2 = cy - dy * tail

            val stroke = (1.6f + 2.4f * (1f - p)) * (1f + 0.10f * p)

            val head = Offset(cx, cy)
            val tailP = Offset(x2, y2)

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0f),
                        accent.copy(alpha = 0.35f * fade),
                        white.copy(alpha = 0.80f * fade)
                    ),
                    start = tailP,
                    end = head
                ),
                start = tailP,
                end = head,
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )

            val sparkle = (0.40f + 0.60f * fade).coerceIn(0f, 1f)
            val headLen = 10f + 10f * (1f - p)

            drawLine(
                color = white.copy(alpha = 0.85f * sparkle),
                start = Offset(cx - dx * headLen, cy - dy * headLen),
                end = Offset(cx + dx * headLen * 0.35f, cy + dy * headLen * 0.35f),
                strokeWidth = stroke * 0.95f,
                cap = StrokeCap.Round
            )

            val px = -dy
            val py = dx
            val cross = 6f + 7f * (1f - p)

            drawLine(
                color = accent.copy(alpha = 0.55f * sparkle),
                start = Offset(cx - px * cross, cy - py * cross),
                end = Offset(cx + px * cross, cy + py * cross),
                strokeWidth = stroke * 0.70f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun rememberShootingStarSpecs(onlyTopHalf: Boolean): List<ShootingStarSpec> {
    return remember(onlyTopHalf) {
        val rnd = Random(911 + if (onlyTopHalf) 1 else 0)
        List(3) {
            val startX = 0.50f + rnd.nextFloat() * 0.46f
            val startY =
                (if (onlyTopHalf) 0.06f else 0.08f) +
                        rnd.nextFloat() * (if (onlyTopHalf) 0.38f else 0.72f)
            val angle = (125f + rnd.nextFloat() * 18f) * (PI.toFloat() / 180f)
            val length = 520f + rnd.nextFloat() * 560f
            val duration = 1050 + rnd.nextInt(650)
            val delay = 1800 + rnd.nextInt(4200)
            ShootingStarSpec(
                startX = startX,
                startY = startY,
                angleRad = angle,
                lengthPx = length,
                durationMs = duration,
                delayMs = delay
            )
        }
    }
}

@Composable
fun SummaryWindowChips(
    selected: SummaryContract.Window,
    onSelect: (SummaryContract.Window) -> Unit
) {
    Surface(
        color = SummaryTokens.SurfaceWeak,
        shape = SummaryTokens.ShapeCard,
        border = BorderStroke(1.dp, SummaryTokens.BorderSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WindowChip(label = "Day", selected = selected == SummaryContract.Window.Day) { onSelect(SummaryContract.Window.Day) }
            WindowChip(label = "Week", selected = selected == SummaryContract.Window.Week) { onSelect(SummaryContract.Window.Week) }
            WindowChip(label = "Month", selected = selected == SummaryContract.Window.Month) { onSelect(SummaryContract.Window.Month) }
        }
    }
}

@Composable
private fun WindowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val border = remember(selected) {
        Brush.linearGradient(
            colors = listOf(
                SummaryTokens.Border.copy(alpha = 0.55f),
                SummaryTokens.Accent.copy(alpha = if (selected) 0.55f else 0.22f),
                SummaryTokens.Border.copy(alpha = 0.55f)
            )
        )
    }
    Surface(
        color = SummaryTokens.Surface,
        shape = SummaryTokens.ShapeChip,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .clip(SummaryTokens.ShapeChip)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Text(
            text = if (selected) "• $label" else label,
            color = if (selected) SummaryTokens.Accent else SummaryTokens.TextDim,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun SummaryTodayCard(today: SummaryContract.Today) {
    val border = remember {
        Brush.linearGradient(
            colors = listOf(
                SummaryTokens.Border.copy(alpha = 0.55f),
                SummaryTokens.Accent.copy(alpha = 0.28f),
                SummaryTokens.Border.copy(alpha = 0.55f)
            )
        )
    }
    Surface(
        color = SummaryTokens.SurfaceWeak,
        shape = SummaryTokens.ShapeCard,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Today", color = SummaryTokens.TextBright, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                GlowDot(
                    color = SummaryTokens.Accent,
                    sizeDp = 14.dp,
                    glowDp = 8.dp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryStatPill(
                    label = "Leaks",
                    value = today.leakCount.toString(),
                    accent = SummaryTokens.Accent
                )
                SummaryStatPill(
                    label = "Trackers",
                    value = today.trackerCount.toString(),
                    accent = SummaryTokens.Accent3
                )
                SummaryStatPill(
                    label = "Traffic",
                    value = "—",
                    accent = SummaryTokens.Accent2
                )
            }

            Text(
                text = "Top domain: ${today.topAppLabel}",
                color = SummaryTokens.TextDim,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryStatPill(
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        color = SummaryTokens.Surface,
        shape = SummaryTokens.ShapePill,
        border = BorderStroke(1.dp, SummaryTokens.Border.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, color = SummaryTokens.TextDim, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = value, color = accent, style = MaterialTheme.typography.titleMedium)
                GlowDot(
                    color = accent,
                    sizeDp = 10.dp,
                    glowDp = 7.dp,
                    modifier = Modifier.alpha(0.95f)
                )
            }
        }
    }
}

@Composable
fun SummaryDaysCard(
    items: List<SummaryContract.DayItem>,
    onClickDay: (String) -> Unit
) {
    Surface(
        color = SummaryTokens.SurfaceWeak,
        shape = SummaryTokens.ShapeCard,
        border = BorderStroke(1.dp, SummaryTokens.Border.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Last days", color = SummaryTokens.TextBright, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(text = items.size.toString(), color = SummaryTokens.TextDim, style = MaterialTheme.typography.bodyMedium)
            }

            if (items.isEmpty()) {
                Surface(
                    color = SummaryTokens.Surface,
                    shape = SummaryTokens.ShapePill,
                    border = BorderStroke(1.dp, SummaryTokens.Border.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No data yet",
                        color = SummaryTokens.TextDim,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.dateLabel }) { day ->
                        DayRow(day = day) { onClickDay(day.dateLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: SummaryContract.DayItem,
    onClick: () -> Unit
) {
    Surface(
        color = SummaryTokens.Surface,
        shape = SummaryTokens.ShapePill,
        border = BorderStroke(1.dp, SummaryTokens.Border.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(SummaryTokens.ShapePill)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = day.dateLabel, color = SummaryTokens.TextBright, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Text(text = "Leaks: ${day.leakCount}", color = SummaryTokens.TextDim)
        }
    }
}

@Composable
private fun GlowDot(
    color: Color,
    sizeDp: Dp,
    glowDp: Dp,
    modifier: Modifier = Modifier
) {
    val glowBrush = remember(color) {
        Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.22f),
                Color.Transparent
            )
        )
    }
    Canvas(modifier = modifier.size(sizeDp)) {
        val r = size.minDimension / 2f
        val glow = glowDp.toPx()
        drawCircle(
            brush = glowBrush,
            radius = r + glow,
            center = center
        )
        drawCircle(
            color = color.copy(alpha = 0.92f),
            radius = r * 0.88f,
            center = center
        )
    }
}
