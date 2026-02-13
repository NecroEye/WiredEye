package com.muratcangzm.monitor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.monitor.model.ACTION_STOP_ENGINE
import com.muratcangzm.monitor.model.NOTIFICATION_ID
import com.muratcangzm.monitor.model.StatKind
import com.muratcangzm.monitor.model.TopAction
import com.muratcangzm.monitor.ui.adapters.rememberUiPacketAdapter
import com.muratcangzm.monitor.ui.components.FilterBar
import com.muratcangzm.monitor.ui.components.LiveDotGhost
import com.muratcangzm.monitor.ui.components.TechStatsBar
import com.muratcangzm.monitor.ui.components.rememberQuiescent
import com.muratcangzm.monitor.ui.dialogs.MetricInfoDialog
import com.muratcangzm.monitor.ui.list.PacketList
import com.muratcangzm.monitor.ui.notification.updateRunningNotification
import com.muratcangzm.monitor.utils.humanBytes
import com.muratcangzm.monitor.utils.shareWindowJson
import com.muratcangzm.network.vpn.DnsSnifferVpnService
import com.muratcangzm.resources.ui.theme.GhostColors
import com.muratcangzm.ui.components.StatusBarStyle
import com.muratcangzm.utils.StringUtils
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import com.muratcangzm.resources.R as Res

@Suppress("ParamsComparedByRef")
@SuppressLint("MissingPermission")
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun WiredEyeScreen(
    homeViewModel: HomeViewModel,
    monitorViewModel: MonitorViewModel = koinViewModel(),
) {
    val state by monitorViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val snackBarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isStopping by rememberSaveable { mutableStateOf(false) }

    val stringVpnDenied = stringResource(Res.string.vpn_denied)
    val stringWifi = stringResource(Res.string.wifi)
    val stringCellular = stringResource(Res.string.cellular)
    val stringOther = stringResource(Res.string.other)

    val vpnLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                monitorViewModel.onEvent(MonitorContract.Event.StartEngine)
            } else {
                scope.launch { snackBarHost.showSnackbar(stringVpnDenied) }
            }
        }

    StatusBarStyle(
        color = GhostColors.Surface,
        darkIcons = false
    )

    fun startWithVpnConsent() {
        val i = VpnService.prepare(context)
        if (i != null) vpnLauncher.launch(i) else monitorViewModel.onEvent(MonitorContract.Event.StartEngine)
    }

    val lastNetworkType = rememberSaveable { mutableStateOf<String?>(null) }
    val hasBaseline = rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            @SuppressLint("LocalContextGetResourceValueCall")
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val t = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> stringWifi
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> stringCellular
                    else -> stringOther
                }

                if (!hasBaseline.value) {
                    hasBaseline.value = true
                    lastNetworkType.value = t
                    return
                }

                if (t != lastNetworkType.value) {
                    lastNetworkType.value = t
                    scope.launch {
                        snackBarHost.showSnackbar(
                            context.getString(Res.string.network_changed_snackbar, t)
                        )
                    }
                    monitorViewModel.onEvent(MonitorContract.Event.ClearNow)
                }
            }
        }

        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
        onDispose { runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) } }
    }

    LaunchedEffect(state.isEngineRunning) {
        if (!state.isEngineRunning) isStopping = false
    }

    LaunchedEffect(Unit) {
        monitorViewModel.effects.collect { eff ->
            when (eff) {
                is MonitorContract.Effect.Snackbar -> snackBarHost.showSnackbar(eff.message)
            }
        }
    }

    var statDialog by rememberSaveable { mutableStateOf<StatKind?>(null) }
    val blurRadius = if (statDialog != null) 16.dp else 0.dp
    val accent = remember { Color(0xFF7BD7FF) }
    val bg = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF0E141B),
                Color(0xFF0B1022),
                Color(0xFF0E141B),
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.title_realtime_metadata),
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
                    val topAction = when {
                        state.isEngineRunning -> TopAction.Running
                        isStopping && !isQuiescent -> TopAction.Settling
                        else -> TopAction.Idle
                    }

                    AnimatedContent(
                        targetState = topAction,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "topbar-actions",
                    ) { action ->
                        when (action) {
                            TopAction.Running -> {
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isStopping = true
                                        monitorViewModel.onEvent(MonitorContract.Event.StopEngine)
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LiveDotGhost(
                                            running = state.isEngineRunning,
                                            modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                                        )
                                        Text(stringResource(Res.string.action_stop))
                                    }
                                }
                            }

                            TopAction.Settling -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 10.dp),
                                    color = accent,
                                    strokeWidth = 2.dp,
                                )
                            }

                            TopAction.Idle -> {
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isStopping = false
                                        startWithVpnConsent()
                                    }
                                ) {
                                    Text(stringResource(Res.string.action_start))
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHost) }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .background(bg)
                .padding(paddingValues)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            ) {
                TechStatsBar(
                    state = state,
                    onWindowChange = { monitorViewModel.onEvent(MonitorContract.Event.SetWindow(it)) },
                    onSpeedChange = { mode ->
                        monitorViewModel.onEvent(
                            MonitorContract.Event.SetSpeed(
                                mode
                            )
                        )
                    },
                    viewMode = state.viewMode,
                    onViewModeChange = { mode ->
                        monitorViewModel.onEvent(
                            MonitorContract.Event.SetViewMode(
                                mode
                            )
                        )
                    },
                    onClearAll = { monitorViewModel.onEvent(MonitorContract.Event.ClearNow) },
                    onChipClick = { kind -> statDialog = kind }
                )

                Spacer(Modifier.height(8.dp))

                FilterBar(
                    text = state.filterText,
                    minBytes = state.minBytes,
                    totalBytes = state.totalBytes,
                    onText = { monitorViewModel.onEvent(MonitorContract.Event.SetFilter(it)) },
                    onClear = { monitorViewModel.onEvent(MonitorContract.Event.ClearFilter) },
                    onMinBytes = { monitorViewModel.onEvent(MonitorContract.Event.SetMinBytes(it)) }
                )

                Spacer(Modifier.height(8.dp))

                val adapter = rememberUiPacketAdapter()
                LaunchedEffect(state.items) { adapter.submit(state.items) }

                PacketList(
                    modifier = Modifier.weight(1f),
                    isRunning = state.isEngineRunning,
                    adapterItems = adapter.items,
                    rawItems = state.items,
                    pinnedUids = state.pinnedUids,
                    highlightedKeys = state.anomalyKeys,
                    onPin = { uid ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        monitorViewModel.onEvent(MonitorContract.Event.TogglePin(uid))
                    },
                    onShareWindowJson = {
                        shareWindowJson(
                            context,
                            state.items,
                            snackBarHost,
                            scope
                        )
                    },
                    onCopied = { what ->
                        scope.launch { snackBarHost.showSnackbar("$what copied") }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    monitorViewModel = monitorViewModel,
                    onNavigateDetails = { uiPacket ->
                        homeViewModel.openDetails(uiPacket = uiPacket)
                    }
                )
            }

            if (statDialog != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { statDialog = null }
                )

                Box(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    MetricInfoDialog(
                        kind = statDialog!!,
                        valueProvider = {
                            when (statDialog) {
                                StatKind.Total -> humanBytes(state.totalBytes)
                                StatKind.Apps -> state.uniqueApps.toString()
                                StatKind.Pps -> String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    state.pps
                                )

                                StatKind.Kbs -> String.format(
                                    Locale.getDefault(),
                                    "%.1f",
                                    state.throughputKbs
                                )

                                null -> StringUtils.EMPTY
                            }
                        },
                        onDismiss = { statDialog = null }
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_STOP_ENGINE) {
                    monitorViewModel.onEvent(MonitorContract.Event.StopEngine)
                    context.startService(
                        Intent(context, DnsSnifferVpnService::class.java)
                            .setAction(DnsSnifferVpnService.ACTION_STOP)
                    )
                    context.stopService(Intent(context, DnsSnifferVpnService::class.java))
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                }
            }
        }

        val filter = IntentFilter(ACTION_STOP_ENGINE)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }

        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
}
