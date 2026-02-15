package com.muratcangzm.core.leak

import com.muratcangzm.shared.model.leak.LeakSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

interface LeakAnalyzerBridge {
    val snapshot: StateFlow<LeakSnapshot>
    fun setWindowMillis(windowMillis: Long)
    fun reset()
    fun onDns(timestampMillis: Long, userIdentifier: Int, queryName: String, queryType: Int, serverIp: String)
    fun emitSnapshot(force: Boolean = false)
}

class LeakAnalyzerBridgeImpl(
    dispatcher: CoroutineDispatcher,
    private val topN: Int = 12,
    private val emitMinIntervalMs: Long = 500L,
    initialWindowMs: Long = 600_000L
) : LeakAnalyzerBridge {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val analyzer = NativeLeakAnalyzer()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mutableSnapshot = MutableStateFlow(LeakSnapshot(windowMs = initialWindowMs))
    override val snapshot: StateFlow<LeakSnapshot> = mutableSnapshot.asStateFlow()

    private val lastEmitMillis = AtomicLong(0L)

    init {
        analyzer.nativeInit(initialWindowMs)
        emitSnapshot(force = true)
    }

    override fun setWindowMillis(windowMillis: Long) {
        val w = windowMillis.coerceAtLeast(1_000L)
        analyzer.nativeSetWindowMs(w)
        emitSnapshot(force = true)
    }

    override fun reset() {
        analyzer.nativeReset()
        mutableSnapshot.value = LeakSnapshot(windowMs = mutableSnapshot.value.windowMs)
        emitSnapshot(force = true)
    }

    override fun onDns(timestampMillis: Long, userIdentifier: Int, queryName: String, queryType: Int, serverIp: String) {
        analyzer.nativeOnDns(timestampMillis, userIdentifier, queryName, queryType, serverIp)
        emitSnapshot(force = false)
    }

    override fun emitSnapshot(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force) {
            val prev = lastEmitMillis.get()
            if (now - prev < emitMinIntervalMs) return
            if (!lastEmitMillis.compareAndSet(prev, now)) return
        } else {
            lastEmitMillis.set(now)
        }

        scope.launch {
            val raw = analyzer.nativeSnapshotJson(topN)
            val parsed = runCatching { json.decodeFromString(LeakSnapshot.serializer(), raw) }.getOrNull()
            if (parsed != null) {
                mutableSnapshot.value = parsed
            }
        }
    }
}
