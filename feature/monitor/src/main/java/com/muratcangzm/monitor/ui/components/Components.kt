package com.muratcangzm.monitor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muratcangzm.monitor.MonitorUiState
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.monitor.model.StatKind
import com.muratcangzm.monitor.ui.menus.GhostViewModeAnchor
import com.muratcangzm.monitor.utils.humanBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.PI
import com.muratcangzm.resources.R as Res
import kotlin.math.abs
import kotlin.math.min

private val GhostShapeSmall = RoundedCornerShape(10.dp)
private val GhostShapeMedium = RoundedCornerShape(12.dp)
private val GhostShapeLarge = RoundedCornerShape(16.dp)
private val GhostShapeXL = RoundedCornerShape(20.dp)
private val GhostSurface = Color(0x22101826)
private val GhostTextDim = Color(0xFF9EB2C0)
private val GhostTextBright = Color(0xFFDBEAFE)
private val GhostAccent = Color(0xFF7BD7FF)

@Composable
fun rememberKeyboardVisible(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(ime, density) {
        snapshotFlow { ime.getBottom(density) > 0 }
            .distinctUntilChanged()
            .collect { isOpen -> visible.value = isOpen }
    }
    return visible
}

@Composable
fun rememberQuiescent(
    pps: Double,
    kbs: Double,
    ppsThreshold: Double = 0.05,
    kbsThreshold: Double = 0.05,
    holdMs: Long = 1000
): State<Boolean> {
    val quiet = remember { mutableStateOf(false) }
    LaunchedEffect(pps, kbs) {
        val nearZero = (abs(pps) < ppsThreshold && abs(kbs) < kbsThreshold)
        if (!nearZero) {
            quiet.value = false
            return@LaunchedEffect
        }
        delay(holdMs)
        quiet.value = (abs(pps) < ppsThreshold && abs(kbs) < kbsThreshold)
    }
    return quiet
}

@Composable
fun GhostLabelAnimated(
    text: String,
    baseColor: Color = Color(0xFF9EB2C0).copy(alpha = 0.62f),
    revealColor: Color = baseColor.copy(alpha = 0.98f),
    tickMs: Long = 60L,
    cyclePauseMs: Long = 6000L,
    edgeAnimMs: Int = 220
) {
    var visible by remember(text) { mutableIntStateOf(0) }
    val edgeProgress = remember { Animatable(0f) }
    LaunchedEffect(text) {
        while (true) {
            for (i in 0..text.length) {
                visible = i
                delay(tickMs)
            }
            delay(cyclePauseMs)
            visible = 0
        }
    }
    LaunchedEffect(visible) {
        if (visible > 0) {
            edgeProgress.snapTo(0f)
            edgeProgress.animateTo(1f, animationSpec = tween(edgeAnimMs))
        } else {
            edgeProgress.snapTo(0f)
        }
    }
    val dimColor = remember(baseColor) { baseColor.copy(alpha = 0.35f) }
    val edgeColor = androidx.compose.ui.graphics.lerp(start = dimColor, stop = revealColor, fraction = edgeProgress.value)
    val annotated = remember(visible, text, dimColor, revealColor, edgeColor) {
        buildAnnotatedString {
            if (visible > 1) withStyle(SpanStyle(color = revealColor)) { append(text.substring(0, visible - 1)) }
            if (visible in 1..text.length) withStyle(SpanStyle(color = edgeColor)) { append(text[visible - 1].toString()) }
            if (visible < text.length) withStyle(SpanStyle(color = dimColor)) { append(text.substring(visible)) }
        }
    }
    Text(annotated)
}

@Composable
fun GlowingStatChip(
    label: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        idle = false
        delay(2500)
        idle = true
    }
    val pulse = remember { Animatable(0.65f) }
    LaunchedEffect(idle) {
        if (!idle) {
            pulse.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            pulse.animateTo(1.6f, animationSpec = tween(625, easing = FastOutSlowInEasing))
            pulse.animateTo(0.65f, animationSpec = tween(625, easing = FastOutSlowInEasing))
        }
    }
    val baseSurface = remember { Color(0xFF101826) }
    val shadowColor by rememberUpdatedState(accent)
    val borderStroke = remember(accent) { BorderStroke(1.dp, accent.copy(alpha = 0.15f)) }
    val glowAlpha = if (idle) pulse.value else 0f
    val blurRadius = if (idle) 18f * glowAlpha.coerceIn(0f, 1f) else 0f
    val onClickStable by rememberUpdatedState(onClick)
    Surface(
        tonalElevation = 3.dp,
        color = baseSurface,
        shape = GhostShapeLarge,
        border = borderStroke,
        modifier = Modifier.then(
            if (onClickStable != null)
                Modifier
                    .clip(GhostShapeLarge)
                    .clickable { onClickStable?.invoke() }
            else Modifier
        )
    ) {
        val type = MaterialTheme.typography.titleLarge.copy(
            shadow = Shadow(color = shadowColor.copy(alpha = glowAlpha), offset = Offset.Zero, blurRadius = blurRadius)
        )
        val textStyle = remember(glowAlpha, shadowColor) {
            type
        }
        val staticLabel = remember(label) { movableContentOf { Text(text = label, color = GhostTextDim) } }
        Column(Modifier.padding(12.dp)) {
            staticLabel()
            Text(text = value, style = textStyle, color = accent)
        }
    }
}

@Composable
fun GhostFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val shape = GhostShapeMedium
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val glow by animateFloatAsState(targetValue = if (focused) 1f else 0.6f, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "ghostGlow")
    val borderBrush = remember(glow) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF233355).copy(alpha = 0.65f * glow),
                GhostAccent.copy(alpha = 0.55f * glow),
                Color(0xFF233355).copy(alpha = 0.65f * glow)
            )
        )
    }
    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = if (focused) 2.dp else 0.dp,
        color = GhostSurface,
        border = remember(borderBrush) { BorderStroke(1.dp, borderBrush) }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            label = { GhostLabelAnimated(text = label) },
            trailingIcon = trailing,
            leadingIcon = leading,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                cursorColor = GhostAccent,
                focusedTextColor = GhostTextBright,
                unfocusedTextColor = Color(0xFFC9D6E2),
                focusedLabelColor = GhostAccent,
                unfocusedLabelColor = GhostTextDim.copy(alpha = 0.72f),
                focusedPlaceholderColor = GhostTextDim,
                unfocusedPlaceholderColor = Color(0xFF6B7C8F)
            ),
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 38.dp)
                .padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun Segmented(
    options: List<Pair<String, Long>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    val density = LocalDensity.current
    val selectedIndex by remember(options, selected) {
        derivedStateOf {
            val i = options.indexOfFirst { it.second == selected }
            if (i >= 0) i else 0
        }
    }
    val xs = remember(options) { mutableStateListOf<Float>().apply { repeat(options.size) { add(0f) } } }
    val ws = remember(options) { mutableStateListOf<Float>().apply { repeat(options.size) { add(0f) } } }
    val hs = remember(options) { mutableStateListOf<Float>().apply { repeat(options.size) { add(0f) } } }
    val targetX = xs.getOrNull(selectedIndex) ?: 0f
    val targetW = ws.getOrNull(selectedIndex) ?: 0f
    val targetH = hs.getOrNull(selectedIndex) ?: 0f
    val fallbackH = with(density) { 28.dp.toPx() }
    val fallbackW = with(density) { 42.dp.toPx() }
    val xDp by animateDpAsState(targetValue = with(density) { targetX.toDp() }, animationSpec = tween(280, easing = FastOutSlowInEasing), label = "segX")
    val wDp by animateDpAsState(targetValue = with(density) { (if (targetW > 0f) targetW else fallbackW).toDp() }, animationSpec = tween(280, easing = FastOutSlowInEasing), label = "segW")
    val hDp by animateDpAsState(targetValue = with(density) { (if (targetH > 0f) targetH else fallbackH).toDp() }, animationSpec = tween(280, easing = FastOutSlowInEasing), label = "segH")
    val bg = remember { Color(0xFF0E1624) }
    val indicatorColor = remember { Color(0xFF1B2B45) }
    Box(
        Modifier
            .clip(GhostShapeMedium)
            .background(bg)
            .padding(3.dp)
    ) {
        Surface(
            color = indicatorColor,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 0.dp,
            modifier = Modifier
                .offset(x = xDp)
                .width(wDp)
                .height(hDp)
        ) {}
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val onSelectState by rememberUpdatedState(onSelect)
            options.forEachIndexed { idx, (label, value) ->
                val active = idx == selectedIndex
                val labelColor = if (active) GhostAccent else GhostTextDim
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { c ->
                            val p = c.positionInParent()
                            val w = c.size.width.toFloat()
                            val h = c.size.height.toFloat()
                            if (xs[idx] != p.x) xs[idx] = p.x
                            if (ws[idx] != w) ws[idx] = w
                            if (hs[idx] != h) hs[idx] = h
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectState(value) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = label, color = labelColor)
                }
            }
        }
    }
}

@Composable
fun GhostDangerButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = GhostShapeSmall
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.55f, animationSpec = tween(180, easing = FastOutSlowInEasing), label = "danger-alpha")
    val enabledBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF40203A).copy(alpha = 0.95f), Color(0xFF9A4D76).copy(alpha = 0.95f), Color(0xFF40203A).copy(alpha = 0.95f)))
    }
    val disabledBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF2A2F3A).copy(alpha = 0.7f), Color(0xFF3A4254).copy(alpha = 0.7f), Color(0xFF2A2F3A).copy(alpha = 0.7f)))
    }
    Surface(
        shape = shape,
        color = GhostSurface,
        border = remember(enabled) { BorderStroke(1.dp, if (enabled) enabledBrush else disabledBrush) },
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .alpha(alpha)
        ) {
            Text(text = icon, color = Color.Unspecified, fontSize = 16.sp)
            Text(text = text, color = if (enabled) GhostTextBright else GhostTextDim.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun GhostTonalButton(
    text: String,
    icon: String? = null,
    active: Boolean,
    mode: SpeedMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = GhostShapeSmall
    val glow by animateFloatAsState(targetValue = if (active) 1f else 0.55f, animationSpec = tween(180, easing = FastOutSlowInEasing), label = "ghost-btn-glow")
    val baseBorder = remember(glow) {
        Brush.linearGradient(
            listOf(
                Color(0xFF233355).copy(alpha = 0.65f * glow),
                GhostAccent.copy(alpha = 0.55f * glow),
                Color(0xFF233355).copy(alpha = 0.65f * glow)
            )
        )
    }
    val triple = remember(mode) {
        when (mode) {
            SpeedMode.ECO -> Triple(3200, Color(0xFF30E3A2).copy(alpha = 0.65f), 0.18f)
            SpeedMode.FAST -> Triple(1200, Color(0xFFFFC107).copy(alpha = 0.70f), 0.24f)
            SpeedMode.TURBO -> Triple(700, Color(0xFFFF6B6B).copy(alpha = 0.75f), 0.30f)
        }
    }
    val durationMs = triple.first
    val highlightColor = triple.second
    val bandFraction = triple.third
    val inf = rememberInfiniteTransition(label = "ghost-border")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMs, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "ghost-border-phase"
    )
    Surface(
        shape = shape,
        color = GhostSurface,
        border = remember(baseBorder) { BorderStroke(1.dp, baseBorder) },
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
            .drawBehind {
                val stroke = 1.4.dp.toPx()
                val inset = stroke / 2f
                val left = inset
                val top = inset
                val right = size.width - inset
                val bottom = size.height - inset
                val r = 10.dp.toPx().coerceAtMost(min((right - left) / 2f, (bottom - top) / 2f))
                val ledgeTop = (right - left) - 2f * r
                val ledgeRight = (bottom - top) - 2f * r
                val ledgeBottom = ledgeTop
                val ledgeLeft = ledgeRight
                val lcorner = (PI.toFloat() / 2f) * r
                val p = ledgeTop + lcorner + ledgeRight + lcorner + ledgeBottom + lcorner + ledgeLeft + lcorner
                val bandLen = (p * bandFraction).coerceAtLeast(12.dp.toPx())
                fun drawPartial(a: Float, b: Float) {
                    var start = a
                    var end = b
                    var cursor = 0f
                    val tLen = ledgeTop
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, tLen))
                        if (e > s) {
                            val y = top
                            val x1 = left + r + s
                            val x2 = left + r + e
                            drawLine(color = highlightColor, start = Offset(x1, y), end = Offset(x2, y), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                    cursor += tLen
                    val trLen = lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, trLen))
                        if (e > s) {
                            val rectLeft = right - 2f * r
                            val rectTop = top
                            val startDeg = 270f + (s / trLen) * 90f
                            val sweepDeg = (e - s) / trLen * 90f
                            drawArc(
                                color = highlightColor,
                                startAngle = startDeg,
                                sweepAngle = sweepDeg,
                                useCenter = false,
                                topLeft = Offset(rectLeft, rectTop),
                                size = androidx.compose.ui.geometry.Size(2f * r, 2f * r),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    cursor += trLen
                    val rLen = ledgeRight
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, rLen))
                        if (e > s) {
                            val x = right
                            val y1 = top + r + s
                            val y2 = top + r + e
                            drawLine(color = highlightColor, start = Offset(x, y1), end = Offset(x, y2), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                    cursor += rLen
                    val brLen = lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, brLen))
                        if (e > s) {
                            val rectLeft = right - 2f * r
                            val rectTop = bottom - 2f * r
                            val startDeg = 0f + (s / brLen) * 90f
                            val sweepDeg = (e - s) / brLen * 90f
                            drawArc(
                                color = highlightColor,
                                startAngle = startDeg,
                                sweepAngle = sweepDeg,
                                useCenter = false,
                                topLeft = Offset(rectLeft, rectTop),
                                size = androidx.compose.ui.geometry.Size(2f * r, 2f * r),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    cursor += brLen
                    val bLen = ledgeBottom
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, bLen))
                        if (e > s) {
                            val y = bottom
                            val x1 = right - r - s
                            val x2 = right - r - e
                            drawLine(color = highlightColor, start = Offset(x1, y), end = Offset(x2, y), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                    cursor += bLen
                    val blLen = lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, blLen))
                        if (e > s) {
                            val rectLeft = left
                            val rectTop = bottom - 2f * r
                            val startDeg = 90f + (s / blLen) * 90f
                            val sweepDeg = (e - s) / blLen * 90f
                            drawArc(
                                color = highlightColor,
                                startAngle = startDeg,
                                sweepAngle = sweepDeg,
                                useCenter = false,
                                topLeft = Offset(rectLeft, rectTop),
                                size = androidx.compose.ui.geometry.Size(2f * r, 2f * r),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    cursor += blLen
                    val lLen = ledgeLeft
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, lLen))
                        if (e > s) {
                            val x = left
                            val y1 = bottom - r - s
                            val y2 = bottom - r - e
                            drawLine(color = highlightColor, start = Offset(x, y1), end = Offset(x, y2), strokeWidth = stroke, cap = StrokeCap.Round)
                        }
                    }
                    cursor += lLen
                    val tlLen = lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, tlLen))
                        if (e > s) {
                            val rectLeft = left
                            val rectTop = top
                            val startDeg = 180f + (s / tlLen) * 90f
                            val sweepDeg = (e - s) / tlLen * 90f
                            drawArc(
                                color = highlightColor,
                                startAngle = startDeg,
                                sweepAngle = sweepDeg,
                                useCenter = false,
                                topLeft = Offset(rectLeft, rectTop),
                                size = androidx.compose.ui.geometry.Size(2f * r, 2f * r),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                fun drawSegment(s0: Float, s1: Float) {
                    val p2 = ledgeTop + lcorner + ledgeRight + lcorner + ledgeBottom + lcorner + ledgeLeft + lcorner
                    if (s1 <= p2) drawPartial(s0, s1) else {
                        drawPartial(s0, p2)
                        drawPartial(0f, s1 - p2)
                    }
                }

                val startS = phase * p
                val endS = startS + bandLen
                drawSegment(startS, endS)
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            if (icon != null) Text(icon, color = GhostAccent.copy(alpha = 0.9f))
            Text(text, color = GhostTextDim)
        }
    }
}

@Composable
fun GhostPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val alpha by animateFloatAsState(if (selected) 1f else 0.55f, tween(160), label = "pill-alpha")
    val border = remember(alpha) {
        Brush.linearGradient(
            listOf(
                Color(0xFF233355).copy(alpha = 0.65f * alpha),
                GhostAccent.copy(alpha = 0.55f * alpha),
                Color(0xFF233355).copy(alpha = 0.65f * alpha)
            )
        )
    }
    Surface(
        shape = shape,
        color = GhostSurface,
        border = remember(border) { BorderStroke(1.dp, border) },
        modifier = Modifier
            .clip(shape)
            .clickable { onClick() }
    ) {
        Text(text = text, color = if (selected) GhostAccent else GhostTextDim, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
fun LiveDotGhost(running: Boolean, modifier: Modifier = Modifier, color: Color = GhostAccent) {
    val inf = rememberInfiniteTransition(label = "live-dot")
    val pulse by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "pulse"
    )
    val p = if (running) pulse else 0f
    Canvas(modifier.size(10.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = color.copy(alpha = 0.22f * (1f - p) + 0.04f), radius = r * (1.6f + p * 1.0f))
        drawCircle(color = color.copy(alpha = 0.14f * (1f - p)), radius = r * (2.1f + p * 0.7f))
        drawCircle(color = color.copy(alpha = 0.18f * (1f - p)), radius = r * (1.3f + p * 1.2f), style = Stroke(width = r * 0.28f))
        drawCircle(color = color.copy(alpha = 0.95f), radius = r * (0.78f + p * 0.06f))
    }
}

@Composable
fun FilterBar(
    text: String,
    minBytes: Long,
    totalBytes: Long,
    onText: (String) -> Unit,
    onClear: () -> Unit,
    onMinBytes: (Long) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardVisible by rememberKeyboardVisible()
    LaunchedEffect(keyboardVisible) { if (!keyboardVisible) focusManager.clearFocus(force = false) }
    Column(Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)) {
        GhostFilterField(
            value = text,
            onValueChange = onText,
            label = stringResource(Res.string.filter_label),
            modifier = Modifier.fillMaxWidth(),
            trailing = {
                if (text.isNotBlank()) {
                    TextButton(onClick = onClear) { Text(stringResource(Res.string.action_clear_text)) }
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.min_bytes), color = GhostTextDim, modifier = Modifier
                .alpha(0.9f)
                .padding(end = 8.dp))
            val maxForSlider = totalBytes.coerceAtLeast(0L)
            val clampedValue = minBytes.coerceIn(0L, maxForSlider)
            Slider(
                value = clampedValue.toFloat(),
                onValueChange = { v -> if (totalBytes > 0L) onMinBytes(v.toLong()) },
                valueRange = 0f..(if (totalBytes > 0L) maxForSlider.toFloat() else 0f),
                enabled = totalBytes > 0L,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(humanBytes(clampedValue), color = GhostAccent)
        }
    }
}

@Suppress("DefaultLocale")
@Composable
fun TechStatsBar(
    state: MonitorUiState,
    onWindowChange: (Long) -> Unit,
    onSpeedChange: (SpeedMode) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onClearAll: () -> Unit,
    onChipClick: (StatKind) -> Unit
) {
    var speedExpanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowingStatChip(stringResource(Res.string.chip_total), humanBytes(state.totalBytes), Color(0xFF30E3A2)) { onChipClick(StatKind.Total) }
            GlowingStatChip(stringResource(Res.string.chip_apps), state.uniqueApps.toString(), GhostAccent) { onChipClick(StatKind.Apps) }
            GlowingStatChip(stringResource(Res.string.chip_pps), String.format("%.1f", state.pps), Color(0xFFFFA6E7)) { onChipClick(StatKind.Pps) }
            GlowingStatChip(stringResource(Res.string.chip_kbs), String.format("%.1f", state.throughputKbs), Color(0xFFBCE784)) { onChipClick(StatKind.Kbs) }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { Text(stringResource(Res.string.label_window), color = GhostTextDim, modifier = Modifier.alpha(0.9f)) }
            item {
                Segmented(
                    options = listOf(
                        stringResource(Res.string.window_5s) to 5_000L,
                        stringResource(Res.string.window_10s) to 10_000L,
                        stringResource(Res.string.window_30s) to 30_000L
                    ),
                    selected = state.windowMillis,
                    onSelect = onWindowChange
                )
            }
            item {
                GhostTonalButton(
                    text = when (state.speedMode) {
                        SpeedMode.ECO -> stringResource(Res.string.speed_eco)
                        SpeedMode.FAST -> stringResource(Res.string.speed_fast)
                        SpeedMode.TURBO -> stringResource(Res.string.speed_turbo)
                    },
                    icon = when (state.speedMode) {
                        SpeedMode.ECO -> "🌿"; SpeedMode.FAST -> "⚡"; SpeedMode.TURBO -> "🚀"
                    },
                    active = speedExpanded,
                    mode = state.speedMode,
                    onClick = { speedExpanded = !speedExpanded }
                )
            }
            item { GhostViewModeAnchor(mode = viewMode, onChange = onViewModeChange) }
            item {
                val hasData = state.totalBytes > 0L || state.items.isNotEmpty()
                val locked = state.isEngineRunning || !hasData
                GhostDangerButton(
                    text = stringResource(Res.string.action_clear),
                    icon = if (!locked) "🗑️" else "🔒",
                    enabled = !locked,
                    onClick = onClearAll
                )
            }
        }
        AnimatedVisibility(
            visible = speedExpanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
        ) {
            SpeedModePanel(
                selected = state.speedMode,
                onSelect = { onSpeedChange(it); speedExpanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun SpeedModePanel(
    selected: SpeedMode,
    onSelect: (SpeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val border = remember { Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45))) }
    Surface(
        modifier = modifier,
        color = Color(0x16101826),
        tonalElevation = 2.dp,
        shape = GhostShapeMedium,
        border = remember(border) { BorderStroke(1.dp, border) }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostPill("🌿 " + stringResource(Res.string.speed_eco), selected == SpeedMode.ECO) { onSelect(SpeedMode.ECO) }
            GhostPill("⚡ " + stringResource(Res.string.speed_fast), selected == SpeedMode.FAST) { onSelect(SpeedMode.FAST) }
            GhostPill("🚀 " + stringResource(Res.string.speed_turbo), selected == SpeedMode.TURBO) { onSelect(SpeedMode.TURBO) }
        }
    }
}