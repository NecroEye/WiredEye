package com.muratcangzm.monitor

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.monitor.common.SpeedMode
import com.muratcangzm.monitor.common.UiPacket
import com.muratcangzm.monitor.common.ViewMode
import com.muratcangzm.monitor.utils.UsageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

private const val NOTI_CHANNEL_ID = "wired_eye_capture"
private const val NOTI_ID = 42
private const val ACTION_STOP_ENGINE = "com.muratcangzm.monitor.ACTION_STOP_ENGINE"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WiredEyeScreen(vm: MonitorViewModel = org.koin.androidx.compose.koinViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var startRequested by rememberSaveable { mutableStateOf(false) }
    var isStopping by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, startRequested) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && startRequested) {
                if (UsageAccess.isGranted(ctx)) vm.onEvent(MonitorUiEvent.StartEngine)
                startRequested = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(state.isEngineRunning) { if (!state.isEngineRunning) isStopping = false }

    LaunchedEffect(Unit) {
        vm.anomalyEvents.collect { msg -> snackbarHost.showSnackbar(msg) }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_STOP_ENGINE) {
                    vm.onEvent(MonitorUiEvent.StopEngine)
                    NotificationManagerCompat.from(context).cancel(NOTI_ID)
                }
            }
        }
        val filter = IntentFilter(ACTION_STOP_ENGINE)
        if (Build.VERSION.SDK_INT >= 33) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ctx.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(state.isEngineRunning, state.throughputKbs, state.pps, state.speedMode, state.totalBytes) {
        updateRunningNotification(ctx, state.isEngineRunning, state.throughputKbs, state.pps, state.speedMode, state.totalBytes)
    }

    val accent = remember { Color(0xFF7BD7FF) }
    val bg = remember { Brush.linearGradient(listOf(Color(0xFF0E141B), Color(0xFF0B1022), Color(0xFF0E141B))) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "WiredEye — Realtime Metadata",
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1200,
                                    velocity = 32.dp,
                                    spacing = MarqueeSpacing(24.dp)
                                )
                                .alignBy(FirstBaseline)
                        )
                    }
                },
                actions = {
                    val isQuiescent by rememberQuiescent(state.pps, state.throughputKbs)
                    LaunchedEffect(isStopping, state.isEngineRunning, isQuiescent) {
                        if (isStopping && !state.isEngineRunning && isQuiescent) {
                            isStopping = false
                        }
                    }
                    val topAction =
                        when {
                            state.isEngineRunning -> TopAction.Running
                            isStopping && !isQuiescent -> TopAction.Settling
                            else -> TopAction.Idle
                        }
                    AnimatedContent(
                        targetState = topAction,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "topbar-actions"
                    ) { action ->
                        when (action) {
                            TopAction.Running -> {
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isStopping = true
                                        vm.onEvent(MonitorUiEvent.StopEngine)
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LiveDotGhost(
                                            running = state.isEngineRunning,
                                            modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                                        )
                                        Text("Stop")
                                    }
                                }
                            }
                            TopAction.Settling -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp).padding(end = 10.dp),
                                    color = accent, strokeWidth = 2.dp
                                )
                            }
                            TopAction.Idle -> {
                                TextButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isStopping = false
                                    if (!UsageAccess.isGranted(ctx)) {
                                        startRequested = true
                                        UsageAccess.openSettings(ctx)
                                    } else {
                                        vm.onEvent(MonitorUiEvent.StartEngine)
                                    }
                                }) { Text("Start") }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padd ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padd)
        ) {
            Column(Modifier.fillMaxSize()) {
                TechStatsBar(
                    state = state,
                    onWindowChange = { vm.onEvent(MonitorUiEvent.SetWindow(it)) },
                    onSpeedChange = { mode -> vm.onEvent(MonitorUiEvent.SetSpeed(mode)) },
                    viewMode = state.viewMode,
                    onViewModeChange = { mode -> vm.onEvent(MonitorUiEvent.SetViewMode(mode)) },
                    onClearAll = { vm.onEvent(MonitorUiEvent.ClearNow) },
                )
                Spacer(Modifier.height(8.dp))
                FilterBar(
                    text = state.filterText,
                    minBytes = state.minBytes,
                    totalBytes = state.totalBytes,
                    onText = { vm.onEvent(MonitorUiEvent.SetFilter(it)) },
                    onClear = { vm.onEvent(MonitorUiEvent.ClearFilter) },
                    onMinBytes = { vm.onEvent(MonitorUiEvent.SetMinBytes(it)) }
                )
                Spacer(Modifier.height(8.dp))
                val adapter = rememberUiPacketAdapter()
                LaunchedEffect(state.items) { adapter.submit(state.items) }
                PacketList(
                    isRunning = state.isEngineRunning,
                    adapterItems = adapter.items,
                    rawItems = state.items,
                    pinnedUids = state.pinnedUids,
                    highlightedKeys = state.anomalyKeys,
                    onPin = { uid ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.onEvent(MonitorUiEvent.TogglePin(uid))
                    },
                    onShareWindowJson = { shareWindowJson(ctx, state.items, snackbarHost, scope) },
                    onCopied = { what ->
                        scope.launch { snackbarHost.showSnackbar("$what copied") }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LiveDotGhost(
    running: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF7BD7FF)
) {
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

@SuppressLint("DefaultLocale")
@Composable
private fun TechStatsBar(
    state: MonitorUiState,
    onWindowChange: (Long) -> Unit,
    onSpeedChange: (com.muratcangzm.monitor.common.SpeedMode) -> Unit,
    viewMode: com.muratcangzm.monitor.common.ViewMode,
    onViewModeChange: (com.muratcangzm.monitor.common.ViewMode) -> Unit,
    onClearAll: () -> Unit,
) {
    var speedExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowingStatChip("Total", humanBytes(state.totalBytes), Color(0xFF30E3A2))
            GlowingStatChip("Apps", state.uniqueApps.toString(), Color(0xFF7BD7FF))
            GlowingStatChip("PPS", String.format("%.1f", state.pps), Color(0xFFFFA6E7))
            GlowingStatChip("KB/s", String.format("%.1f", state.throughputKbs), Color(0xFFBCE784))
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { Text("Window:", color = Color(0xFF9EB2C0), modifier = Modifier.alpha(0.9f)) }
            item {
                Segmented(
                    options = listOf("5s" to 5_000L, "10s" to 10_000L, "30s" to 30_000L),
                    selected = state.windowMillis,
                    onSelect = onWindowChange
                )
            }
            item {
                GhostTonalButton(
                    text = when (state.speedMode) {
                        com.muratcangzm.monitor.common.SpeedMode.ECO -> "Eco"
                        com.muratcangzm.monitor.common.SpeedMode.FAST -> "Fast"
                        com.muratcangzm.monitor.common.SpeedMode.TURBO -> "Turbo"
                    },
                    icon = when (state.speedMode) {
                        com.muratcangzm.monitor.common.SpeedMode.ECO -> "🌿"
                        com.muratcangzm.monitor.common.SpeedMode.FAST -> "⚡"
                        com.muratcangzm.monitor.common.SpeedMode.TURBO -> "🚀"
                    },
                    active = speedExpanded,
                    mode = state.speedMode,
                    onClick = { speedExpanded = !speedExpanded }
                )
            }
            item {
                GhostViewModeAnchor(
                    mode = viewMode,
                    onChange = onViewModeChange
                )
            }
            item {
                val hasData = state.totalBytes > 0L || state.items.isNotEmpty()
                val locked = state.isEngineRunning || !hasData
                GhostDangerButton(
                    text = "Clear",
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
                onSelect = {
                    onSpeedChange(it)
                    speedExpanded = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun GhostDangerButton(
    text: String,
    icon: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.55f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "danger-alpha"
    )
    val borderBrush = if (enabled) {
        Brush.linearGradient(
            listOf(
                Color(0xFF40203A).copy(alpha = 0.95f * alpha),
                Color(0xFF9A4D76).copy(alpha = 0.95f * alpha),
                Color(0xFF40203A).copy(alpha = 0.95f * alpha)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF2A2F3A).copy(alpha = 0.7f * alpha),
                Color(0xFF3A4254).copy(alpha = 0.7f * alpha),
                Color(0xFF2A2F3A).copy(alpha = 0.7f * alpha)
            )
        )
    }
    Surface(
        shape = shape,
        color = Color(0x22101826),
        border = BorderStroke(1.dp, borderBrush),
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
            Text(text = text, color = if (enabled) Color(0xFFDBEAFE) else Color(0xFF9EB2C0).copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun GhostTonalButton(
    text: String,
    icon: String? = null,
    active: Boolean,
    mode: com.muratcangzm.monitor.common.SpeedMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF7BD7FF)
    val shape = RoundedCornerShape(10.dp)
    val glow by animateFloatAsState(
        targetValue = if (active) 1f else 0.55f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "ghost-btn-glow"
    )
    val baseBorder = Brush.linearGradient(
        listOf(
            Color(0xFF233355).copy(alpha = 0.65f * glow),
            accent.copy(alpha = 0.55f * glow),
            Color(0xFF233355).copy(alpha = 0.65f * glow)
        )
    )
    val (durationMs, highlightColor, bandFraction) = when (mode) {
        com.muratcangzm.monitor.common.SpeedMode.ECO -> Triple(3200, Color(0xFF30E3A2).copy(alpha = 0.65f), 0.18f)
        com.muratcangzm.monitor.common.SpeedMode.FAST -> Triple(1200, Color(0xFFFFC107).copy(alpha = 0.70f), 0.24f)
        com.muratcangzm.monitor.common.SpeedMode.TURBO -> Triple(700, Color(0xFFFF6B6B).copy(alpha = 0.75f), 0.30f)
    }
    val inf = rememberInfiniteTransition(label = "ghost-border")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMs, easing = androidx.compose.animation.core.LinearEasing), repeatMode = RepeatMode.Restart),
        label = "ghost-border-phase"
    )
    Surface(
        shape = shape,
        color = Color(0x22101826),
        border = BorderStroke(1.dp, baseBorder),
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
                val r = 10.dp.toPx().coerceAtMost(minOf((right - left) / 2f, (bottom - top) / 2f))
                val LedgeTop = (right - left) - 2f * r
                val LedgeRight = (bottom - top) - 2f * r
                val LedgeBottom = LedgeTop
                val LedgeLeft = LedgeRight
                val Lcorner = (PI.toFloat() / 2f) * r
                val P = LedgeTop + Lcorner + LedgeRight + Lcorner + LedgeBottom + Lcorner + LedgeLeft + Lcorner
                val bandLen = (P * bandFraction).coerceAtLeast(12.dp.toPx())
                fun drawPartial(a: Float, b: Float) {
                    var start = a
                    var end = b
                    var cursor = 0f
                    val tLen = LedgeTop
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, tLen))
                        if (e > s) {
                            val y = top
                            val x1 = left + r + s
                            val x2 = left + r + e
                            drawLine(color = highlightColor, start = Offset(x1, y), end = Offset(x2, y), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                    cursor += tLen
                    val trLen = Lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, trLen))
                        if (e > s) {
                            val rectLeft = right - 2f * r
                            val rectTop = top
                            val startDeg = 270f + (s / trLen) * 90f
                            val sweepDeg = (e - s) / trLen * 90f
                            drawArc(color = highlightColor, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false, topLeft = Offset(rectLeft, rectTop), size = androidx.compose.ui.geometry.Size(2f * r, 2f * r), style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                    cursor += trLen
                    val rLen = LedgeRight
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, rLen))
                        if (e > s) {
                            val x = right
                            val y1 = top + r + s
                            val y2 = top + r + e
                            drawLine(color = highlightColor, start = Offset(x, y1), end = Offset(x, y2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                    cursor += rLen
                    val brLen = Lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, brLen))
                        if (e > s) {
                            val rectLeft = right - 2f * r
                            val rectTop = bottom - 2f * r
                            val startDeg = 0f + (s / brLen) * 90f
                            val sweepDeg = (e - s) / brLen * 90f
                            drawArc(color = highlightColor, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false, topLeft = Offset(rectLeft, rectTop), size = androidx.compose.ui.geometry.Size(2f * r, 2f * r), style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                    cursor += brLen
                    val bLen = LedgeBottom
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, bLen))
                        if (e > s) {
                            val y = bottom
                            val x1 = right - r - s
                            val x2 = right - r - e
                            drawLine(color = highlightColor, start = Offset(x1, y), end = Offset(x2, y), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                    cursor += bLen
                    val blLen = Lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, blLen))
                        if (e > s) {
                            val rectLeft = left
                            val rectTop = bottom - 2f * r
                            val startDeg = 90f + (s / blLen) * 90f
                            val sweepDeg = (e - s) / blLen * 90f
                            drawArc(color = highlightColor, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false, topLeft = Offset(rectLeft, rectTop), size = androidx.compose.ui.geometry.Size(2f * r, 2f * r), style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                    cursor += blLen
                    val lLen = LedgeLeft
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, lLen))
                        if (e > s) {
                            val x = left
                            val y1 = bottom - r - s
                            val y2 = bottom - r - e
                            drawLine(color = highlightColor, start = Offset(x, y1), end = Offset(x, y2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                    cursor += lLen
                    val tlLen = Lcorner
                    if (end > cursor) {
                        val s = maxOf(0f, start - cursor)
                        val e = maxOf(0f, minOf(end - cursor, tlLen))
                        if (e > s) {
                            val rectLeft = left
                            val rectTop = top
                            val startDeg = 180f + (s / tlLen) * 90f
                            val sweepDeg = (e - s) / tlLen * 90f
                            drawArc(color = highlightColor, startAngle = startDeg, sweepAngle = sweepDeg, useCenter = false, topLeft = Offset(rectLeft, rectTop), size = androidx.compose.ui.geometry.Size(2f * r, 2f * r), style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                }
                fun drawSegment(s0: Float, s1: Float) {
                    if (s1 <= P) {
                        drawPartial(s0, s1)
                    } else {
                        drawPartial(s0, P)
                        drawPartial(0f, s1 - P)
                    }
                }
                val startS = phase * P
                val endS = startS + bandLen
                drawSegment(startS, endS)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (icon != null) Text(icon, color = accent.copy(alpha = 0.9f))
            Text(text, color = Color(0xFF9EB2C0))
        }
    }
}

@Composable
private fun SpeedModePanel(
    selected: com.muratcangzm.monitor.common.SpeedMode,
    onSelect: (com.muratcangzm.monitor.common.SpeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val border = Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45)))
    Surface(
        modifier = modifier,
        color = Color(0x16101826),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostPill("🌿 Eco", selected == com.muratcangzm.monitor.common.SpeedMode.ECO) { onSelect(com.muratcangzm.monitor.common.SpeedMode.ECO) }
            GhostPill("⚡ Fast", selected == com.muratcangzm.monitor.common.SpeedMode.FAST) { onSelect(com.muratcangzm.monitor.common.SpeedMode.FAST) }
            GhostPill("🚀 Turbo", selected == com.muratcangzm.monitor.common.SpeedMode.TURBO) { onSelect(com.muratcangzm.monitor.common.SpeedMode.TURBO) }
        }
    }
}

@Composable
private fun GhostPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF7BD7FF)
    val shape = RoundedCornerShape(20.dp)
    val alpha by animateFloatAsState(if (selected) 1f else 0.55f, tween(160), label = "pill-alpha")
    Surface(
        shape = shape,
        color = Color(0x22101826),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF233355).copy(alpha = 0.65f * alpha),
                    accent.copy(alpha = 0.55f * alpha),
                    Color(0xFF233355).copy(alpha = 0.65f * alpha)
                )
            )
        ),
        modifier = Modifier
            .clip(shape)
            .clickable { onClick() }
    ) {
        Text(text = text, color = if (selected) accent else Color(0xFF9EB2C0), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun GlowingStatChip(label: String, value: String, accent: Color) {
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(value) { idle = false; kotlinx.coroutines.delay(2500); idle = true }
    val inf = rememberInfiniteTransition(label = "chipGlow")
    val pulse by inf.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "glowPulse"
    )
    val shadowColor by rememberUpdatedState(accent)
    val baseSurface = remember { Color(0xFF101826) }
    val borderStroke = remember(accent) { BorderStroke(1.dp, accent.copy(alpha = 0.15f)) }
    val glowAlpha = if (idle) pulse else 0f
    val blurRadius = if (idle) 18f else 0f
    Surface(tonalElevation = 3.dp, color = baseSurface, shape = RoundedCornerShape(16.dp), border = borderStroke) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFF9EB2C0))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(shadow = Shadow(color = shadowColor.copy(alpha = glowAlpha), offset = Offset.Zero, blurRadius = blurRadius)),
                color = accent
            )
        }
    }
}

@Composable
private fun GhostFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null
) {
    val accent = Color(0xFF7BD7FF)
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val glow by animateFloatAsState(targetValue = if (focused) 1f else 0.6f, animationSpec = tween(220, easing = FastOutSlowInEasing), label = "ghostGlow")
    val borderBrush = Brush.linearGradient(colors = listOf(Color(0xFF233355).copy(alpha = 0.65f * glow), accent.copy(alpha = 0.55f * glow), Color(0xFF233355).copy(alpha = 0.65f * glow)))
    val baseSurface = Color(0x22101826)
    Surface(modifier = modifier, shape = shape, tonalElevation = if (focused) 2.dp else 0.dp, color = baseSurface, border = BorderStroke(1.dp, borderBrush)) {
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
                cursorColor = accent,
                focusedTextColor = Color(0xFFDBEAFE),
                unfocusedTextColor = Color(0xFFC9D6E2),
                focusedLabelColor = accent,
                unfocusedLabelColor = Color(0xFF9EB2C0).copy(alpha = 0.72f),
                focusedPlaceholderColor = Color(0xFF9EB2C0),
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
private fun GhostLabelAnimated(
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
        } else edgeProgress.snapTo(0f)
    }
    val dimColor = baseColor.copy(alpha = 0.35f)
    val edgeColor = lerp(start = dimColor, stop = revealColor, fraction = edgeProgress.value)
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
private fun Segmented(
    options: List<Pair<String, Long>>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    val density = LocalDensity.current
    val selectedIndex by remember(options, selected) {
        derivedStateOf { options.indexOfFirst { it.second == selected }.let { if (it >= 0) it else 0 } }
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
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(3.dp)) {
        Surface(color = indicatorColor, shape = RoundedCornerShape(10.dp), shadowElevation = 0.dp, modifier = Modifier.offset(x = xDp).width(wDp).height(hDp)) {}
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val onSelectState by rememberUpdatedState(onSelect)
            options.forEachIndexed { idx, (label, value) ->
                val active = idx == selectedIndex
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
                    Text(text = label, color = if (active) Color(0xFF7BD7FF) else Color(0xFF9EB2C0))
                }
            }
        }
    }
}

private data class UiPacketItem(val id: String, val model: UiPacket, val contentHash: Int)

private class UiPacketAdapter {
    val items = mutableStateListOf<UiPacketItem>()
    fun submit(models: List<UiPacket>) {
        val seen = HashMap<String, Int>(max(16, models.size))
        val newProjected = ArrayList<UiPacketItem>(models.size)
        val oldById = HashMap<String, UiPacketItem>(items.size)
        items.forEach { oldById[it.id] = it }
        for (m in models) {
            val n = (seen[m.key] ?: 0) + 1
            seen[m.key] = n
            val id = "${m.key}#$n"
            val ch = fastContentHash(m)
            val reused = oldById[id]?.takeIf { it.contentHash == ch }
            if (reused != null) newProjected.add(reused) else newProjected.add(UiPacketItem(id, m, ch))
        }
        val newIds = newProjected.mapTo(HashSet(newProjected.size)) { it.id }
        for (i in items.lastIndex downTo 0) if (!newIds.contains(items[i].id)) items.removeAt(i)
        var i = 0
        while (i < newProjected.size) {
            val desired = newProjected[i]
            if (i >= items.size) items.add(desired) else {
                val current = items[i]
                if (current.id == desired.id) {
                    if (current.contentHash != desired.contentHash || current.model !== desired.model) items[i] = desired
                } else {
                    val existingIndex = items.indexOfFirst { it.id == desired.id }
                    if (existingIndex >= 0) {
                        val moved = items.removeAt(existingIndex)
                        items.add(i, moved)
                        if (moved.contentHash != desired.contentHash || moved.model !== desired.model) items[i] = desired
                    } else items.add(i, desired)
                }
            }
            i++
        }
        while (items.size > newProjected.size) items.removeAt(items.lastIndex)
    }
    private fun fastContentHash(m: UiPacket): Int {
        var h = 17
        h = 31 * h + (m.raw.timestamp xor (m.raw.timestamp ushr 32)).toInt()
        h = 31 * h + (m.raw.bytes xor (m.raw.bytes ushr 32)).toInt()
        h = 31 * h + m.proto.hashCode()
        h = 31 * h + m.from.hashCode()
        h = 31 * h + m.to.hashCode()
        h = 31 * h + m.app.hashCode()
        return h
    }
}

@Composable
private fun rememberUiPacketAdapter(): UiPacketAdapter = remember { UiPacketAdapter() }

@Composable
private fun FilterBar(
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        GhostFilterField(
            value = text,
            onValueChange = onText,
            label = "Filter (app/ip/port/protocol)",
            modifier = Modifier.fillMaxWidth(),
            trailing = { if (text.isNotBlank()) TextButton(onClick = onClear) { Text("Clear") } }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Min bytes:", color = Color(0xFF9EB2C0), modifier = Modifier.padding(end = 8.dp))
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
            Text(humanBytes(clampedValue), color = Color(0xFF7BD7FF))
        }
    }
}

@Composable
private fun PacketList(
    isRunning: Boolean,
    adapterItems: List<UiPacketItem>,
    rawItems: List<UiPacket>,
    pinnedUids: Set<Int>,
    highlightedKeys: Set<String>,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isEmpty = adapterItems.isEmpty() && rawItems.isEmpty()
    if (isEmpty) {
        EmptyState(isRunning = isRunning)
        return
    }
    val pinnedLatest = remember(rawItems, pinnedUids) {
        val map = HashMap<Int, UiPacket>()
        for (it in rawItems) {
            val uid = it.raw.uid ?: -1
            if (uid in pinnedUids && uid >= 0) {
                val curr = map[uid]
                if (curr == null || it.raw.timestamp > curr.raw.timestamp) map[uid] = it
            }
        }
        map.values.sortedBy { it.app.lowercase(Locale.getDefault()) }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (pinnedLatest.isNotEmpty()) {
            stickyHeader {
                Surface(color = Color(0xFF0E141B), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
                        Text("Pinned", color = Color(0xFF9EB2C0), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                        pinnedLatest.forEach { r ->
                            PinnedRowCompact(row = r, onUnpin = { uid -> onPin(uid) }, onShareWindowJson = onShareWindowJson)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
        items(items = adapterItems, key = { it.id }) { item ->
            PacketRow(
                row = item.model,
                highlighted = highlightedKeys.contains(item.model.key),
                onPin = onPin,
                onShareWindowJson = onShareWindowJson,
                onCopied = onCopied,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun EmptyState(isRunning: Boolean) {
    val hint = if (isRunning) "Listening for packets…" else "Tap Start to begin capture"
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("No packets yet", color = Color(0xFF9EB2C0), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, color = Color(0xFF6E8296), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PinnedRowCompact(
    row: UiPacket,
    onUnpin: (Int) -> Unit,
    onShareWindowJson: () -> Unit
) {
    val uid = row.raw.uid ?: -1
    val border = remember { Brush.linearGradient(listOf(Color(0xFF314260), Color(0xFF243656), Color(0xFF314260))) }
    Surface(color = Color(0xFF0F1726), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.app, color = Color(0xFF7BD7FF), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${row.proto} • ${row.bytesLabel}", color = Color(0xFF30E3A2), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onShareWindowJson) { Text("Share") }
                    TextButton(onClick = { if (uid >= 0) onUnpin(uid) }) { Text("Unpin") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PacketRow(
    row: UiPacket,
    highlighted: Boolean,
    onPin: (Int) -> Unit,
    onShareWindowJson: () -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val border = remember { Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45))) }
    val uid = row.raw.uid ?: -1
    val dir = inferDirection(row)
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val flash by animateFloatAsState(targetValue = if (highlighted) 1f else 0f, animationSpec = tween(350, easing = FastOutSlowInEasing), label = "anomaly-flash")
    Surface(
        color = Color(0xFF0F1726),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (flash > 0f) {
                    val stroke = 2.dp.toPx()
                    drawRoundRect(color = Color(0xFF30E3A2).copy(alpha = 0.55f * flash), cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()), style = Stroke(width = stroke))
                }
            }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(row.app, style = MaterialTheme.typography.titleMedium, color = Color(0xFF7BD7FF), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    val uidText = "uid:${row.raw.uid ?: -1}"
                    val pkgText = row.raw.packageName ?: "—"
                    Text("$pkgText  •  $uidText", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8EA0B5), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(row.time, color = Color(0xFF9EB2C0), style = MaterialTheme.typography.labelLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val dirLabel = when (dir) {
                    Direction.TX -> "TX"; Direction.RX -> "RX"; Direction.MIX -> "MIX"; null -> "—"
                }
                val dirColor = when (dir) {
                    Direction.TX -> Color(0xFFFFB86C); Direction.RX -> Color(0xFF7BD7FF); Direction.MIX -> Color(0xFFBCE784); null -> Color(0xFF9EB2C0)
                }
                Text(row.proto, color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodyMedium)
                Text("•", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodyMedium)
                Text(dirLabel, color = dirColor, style = MaterialTheme.typography.bodyMedium)
                Text("•", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodyMedium)
                Text(row.bytesLabel, color = Color(0xFF30E3A2), style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "from  ${row.from}",
                    color = Color(0xFFB8C8D8),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboard.setText(AnnotatedString(row.from))
                            onCopied("From")
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                )
                Text("→", color = Color(0xFF8EA0B5), style = MaterialTheme.typography.bodySmall)
                Text(
                    "to  ${row.to}",
                    color = Color(0xFFB8C8D8),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboard.setText(AnnotatedString(row.to))
                            onCopied("To")
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (uid >= 0) onPin(uid) }) { Text("Pin") }
                TextButton(onClick = onShareWindowJson) { Text("Share JSON") }
            }
        }
    }
}

private enum class Direction { TX, RX, MIX }
private enum class TopAction { Running, Settling, Idle }

private fun inferDirection(row: UiPacket): Direction? {
    val m = row.raw
    if (m.protocol.equals("AGG", true)) return Direction.MIX
    val lp = m.localPort
    val rp = m.remotePort
    val localKnown = lp in 1..1024
    val remoteKnown = rp in 1..1024
    return when {
        !localKnown && remoteKnown -> Direction.TX
        localKnown && !remoteKnown -> Direction.RX
        else -> null
    }
}

private fun shareWindowJson(
    ctx: Context,
    items: List<UiPacket>,
    snackbarHost: SnackbarHostState,
    scope: CoroutineScope
) {
    runCatching {
        val json = buildJson(items)
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "wiredeye_${System.currentTimeMillis()}.json")
        file.writeText(json)
        val authority = ctx.applicationContext.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(ctx, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(ctx.contentResolver, file.name, uri)
        }
        val resInfo = ctx.packageManager.queryIntentActivities(send, PackageManager.MATCH_DEFAULT_ONLY)
        for (ri in resInfo) ctx.grantUriPermission(ri.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, "Share snapshot"))
    }.onFailure {
        scope.launch { snackbarHost.showSnackbar("Sharing failed") }
        runCatching {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, buildJson(items))
            }
            ctx.startActivity(Intent.createChooser(fallback, "Share snapshot"))
        }
    }
}

private fun buildJson(items: List<UiPacket>): String {
    val sb = StringBuilder(items.size * 128)
    sb.append('[')
    items.forEachIndexed { i, r ->
        val m = r.raw
        sb.append("{")
            .append("\"timestamp\":").append(m.timestamp).append(',')
            .append("\"uid\":").append(m.uid ?: -1).append(',')
            .append("\"package\":\"").append((m.packageName ?: "").escapeJson()).append("\",")
            .append("\"protocol\":\"").append(m.protocol.escapeJson()).append("\",")
            .append("\"from\":\"").append(((m.localAddress ?: "") + ":" + m.localPort).escapeJson()).append("\",")
            .append("\"to\":\"").append(((m.remoteAddress ?: "") + ":" + m.remotePort).escapeJson()).append("\",")
            .append("\"bytes\":").append(m.bytes)
            .append("}")
        if (i != items.lastIndex) sb.append(',')
    }
    sb.append(']')
    return sb.toString()
}

private fun String.escapeJson(): String = buildString {
    for (c in this@escapeJson) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

private fun humanBytes(b: Long): String {
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = b.toDouble()
    var i = 0
    while (v >= 1024 && i < u.lastIndex) {
        v /= 1024; i++
    }
    return String.format(Locale.getDefault(), "%.1f %s", v, u[i])
}

@Composable
private fun rememberQuiescent(
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
            quiet.value = false; return@LaunchedEffect
        }
        delay(holdMs)
        quiet.value = (abs(pps) < ppsThreshold && abs(kbs) < kbsThreshold)
    }
    return quiet
}

@Composable
private fun rememberKeyboardVisible(): State<Boolean> {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(ime, density) {
        snapshotFlow { ime.getBottom(density) > 0 }.distinctUntilChanged().collect { isOpen -> visible.value = isOpen }
    }
    return visible
}

@Composable
private fun GhostViewModeAnchor(
    mode: ViewMode,
    onChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val accent = Color(0xFF7BD7FF)
    Box(modifier) {
        Surface(
            shape = shape,
            color = Color(0x22101826),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF233355).copy(alpha = 0.55f), accent.copy(alpha = 0.45f), Color(0xFF233355).copy(alpha = 0.55f)))),
            modifier = Modifier
                .clip(shape)
                .clickable { expanded = !expanded }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("▦", color = accent.copy(alpha = 0.9f))
                Text(if (mode == ViewMode.RAW) "Raw" else "Agg", color = Color(0xFF9EB2C0))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Surface(color = Color(0xFF0F1726), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0xFF1B2B45), Color(0xFF20304F), Color(0xFF1B2B45))))) {
                Column(Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                    Text("View mode", color = Color(0xFF9EB2C0), modifier = Modifier.padding(6.dp))
                    GhostMenuRow(icon = "≋", title = "Raw", subtitle = "Show every packet", selected = mode == ViewMode.RAW) { onChange(ViewMode.RAW); expanded = false }
                    GhostMenuRow(icon = "⧉", title = "Aggregated", subtitle = "Group by app/host", selected = mode == ViewMode.AGGREGATED) { onChange(ViewMode.AGGREGATED); expanded = false }
                }
            }
        }
    }
}

@Composable
private fun GhostMenuRow(
    icon: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(0xFF7BD7FF)
    val textCol = if (selected) accent else Color(0xFFDBEAFE)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, color = accent)
        Column(Modifier.weight(1f)) {
            Text(title, color = textCol)
            Text(subtitle, color = Color(0xFF8EA0B5), style = MaterialTheme.typography.labelSmall)
        }
        if (selected) Text("✓", color = accent)
    }
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
private fun updateRunningNotification(
    ctx: Context,
    running: Boolean,
    kbs: Double,
    pps: Double,
    mode: SpeedMode,
    total: Long
) {
    val nm = NotificationManagerCompat.from(ctx)
    if (!running) {
        nm.cancel(NOTI_ID)
        return
    }
    val ch = NotificationChannel(NOTI_CHANNEL_ID, "WiredEye Capture", NotificationManager.IMPORTANCE_LOW)
    ch.enableVibration(false)
    ch.enableLights(false)
    ch.setShowBadge(false)
    val sys = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    sys.createNotificationChannel(ch)
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    val pi = PendingIntent.getActivity(
        ctx,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
    )
    val stopIntent = Intent(ACTION_STOP_ENGINE).setPackage(ctx.packageName)
    val stopPi = PendingIntent.getBroadcast(
        ctx,
        1,
        stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
    )
    val modeText = when (mode) {
        SpeedMode.ECO -> "Eco"
        SpeedMode.FAST -> "Fast"
        SpeedMode.TURBO -> "Turbo"
    }
    val content = "Total ${humanBytes(total)} • KB/s %.1f • PPS %.1f • %s".format(Locale.getDefault(), kbs, pps, modeText)
    val notif = NotificationCompat.Builder(ctx, NOTI_CHANNEL_ID)
        .setSmallIcon(R.drawable.stat_notify_sync)
        .setContentTitle("WiredEye capture running")
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setColor(0xFF7BD7FF.toInt())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(pi)
        .addAction(R.drawable.ic_media_pause, "Stop", stopPi)
        .build()
    nm.notify(NOTI_ID, notif)
}