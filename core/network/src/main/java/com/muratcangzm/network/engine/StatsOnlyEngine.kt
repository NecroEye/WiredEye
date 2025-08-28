package com.muratcangzm.network.engine

import android.app.Application
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import com.muratcangzm.data.model.meta.PacketMeta
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class StatsOnlyEngine(
    private val app: Application,
    private val cm: ConnectivityManager,
    private val nsm: NetworkStatsManager,
    dispatcher: CoroutineDispatcher,
) : PacketCaptureEngine {

    override val info: EngineInfo = EngineInfo(
        id = "stats-only",
        mode = EngineMode.StatsOnly,
        capabilities = emptySet()
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow<EngineState>(EngineState.Stopped)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PacketMeta>(extraBufferCapacity = 256)
    override val events = _events.asSharedFlow()

    private var job: Job? = null

    override suspend fun start() {
        if (job != null) return
        _state.value = EngineState.Starting
        job = scope.launch {
            val bucket = NetworkStats.Bucket()
            try {
                _state.value = EngineState.Running
                while (isActive) {
                    val now = System.currentTimeMillis()
                    listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE).forEach { type ->
                        runCatching {
                            val summary = nsm.querySummary(type, null, now - 2_000, now)
                            while (summary.hasNextBucket()) {
                                summary.getNextBucket(bucket)
                                val bytes = bucket.rxBytes + bucket.txBytes
                                if (bucket.uid > 0 && bytes > 0) {
                                    val pkg = app.packageManager.getPackagesForUid(bucket.uid)?.firstOrNull()
                                    _events.tryEmit(
                                        PacketMeta(
                                            timestamp = now,
                                            uid = bucket.uid,
                                            packageName = pkg,
                                            protocol = "AGG",
                                            localAddress = "-",
                                            localPort = 0,
                                            remoteAddress = "-",
                                            remotePort = 0,
                                            bytes = bytes
                                        )
                                    )
                                }
                            }
                            summary.close()
                        }
                    }
                    delay(1_000)
                }
            } catch (t: Throwable) {
                _state.value = EngineState.Failed(t)
                throw t
            } finally {
                if (!isActive) _state.value = EngineState.Stopped
            }
        }
    }

    override suspend fun stop() {
        job?.cancelAndJoin()
        job = null
        _state.value = EngineState.Stopped
    }
}
