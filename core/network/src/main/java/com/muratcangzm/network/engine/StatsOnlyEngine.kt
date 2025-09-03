package com.muratcangzm.network.engine

import android.app.Application
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.core.content.ContextCompat
import com.muratcangzm.data.model.meta.PacketMeta
import com.muratcangzm.network.vpn.DnsSnifferVpnService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatsOnlyEngine(
    private val app: Application,
    @Suppress("UNUSED_PARAMETER") private val cm: android.net.ConnectivityManager,
    @Suppress("UNUSED_PARAMETER") private val nsm: android.app.usage.NetworkStatsManager,
    dispatcher: CoroutineDispatcher,
    private val bus: PacketEventBus,
) : PacketCaptureEngine {

    override val info: EngineInfo = EngineInfo(
        id = "vpn-meta",
        mode = EngineMode.StatsOnly,
        capabilities = setOf()
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow<EngineState>(EngineState.Stopped)
    override val state: StateFlow<EngineState> = _state

    // Artık broadcast dinlemiyoruz; servis bus’a basıyor, biz direkt flow’u expose ediyoruz
    override val events: SharedFlow<PacketMeta> = bus.events

    override fun updateConfig(config: PacketCaptureConfig) { /* no-op */ }

    override suspend fun start() {
        if (_state.value is EngineState.Running) return

        val consentNeeded = VpnService.prepare(app) != null
        if (consentNeeded) {
            Log.w(TAG, "start(): VPN consent required")
            _state.value = EngineState.Failed(IllegalStateException("VPN consent required"))
            return
        }

        _state.value = EngineState.Starting
        scope.launch {
            Log.d(TAG, "Starting DnsSnifferVpnService …")
            ContextCompat.startForegroundService(
                app,
                Intent(app, DnsSnifferVpnService::class.java)
                    .setAction(DnsSnifferVpnService.ACTION_START)
            )
            _state.value = EngineState.Running
            Log.d(TAG, "Engine state → Running")
        }
    }

    override suspend fun stop() {
        Log.d(TAG, "stop() called → stopping service")
        app.startService(
            Intent(app, DnsSnifferVpnService::class.java)
                .setAction(DnsSnifferVpnService.ACTION_STOP)
        )
        _state.value = EngineState.Stopped
    }

    companion object { private const val TAG = "WireLogEngine" }
}