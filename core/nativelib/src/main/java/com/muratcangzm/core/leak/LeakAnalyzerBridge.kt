package com.muratcangzm.core.leak

import com.muratcangzm.shared.model.leak.LeakSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

interface LeakAnalyzerBridge : Closeable {
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
    private val nativeMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val mutableSnapshot = MutableStateFlow(LeakSnapshot(windowMs = initialWindowMs))
    override val snapshot: StateFlow<LeakSnapshot> = mutableSnapshot.asStateFlow()

    private val lastEmitMillis = AtomicLong(0L)

    private val snapshotRequests = MutableSharedFlow<Boolean>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            nativeMutex.withLock {
                analyzer.nativeInit(initialWindowMs)
            }
            snapshotRequests.tryEmit(true)
            snapshotRequests.collectLatest { force ->
                if (!force) {
                    val now = System.currentTimeMillis()
                    val prev = lastEmitMillis.get()
                    val wait = emitMinIntervalMs - (now - prev)
                    if (wait > 0L) delay(wait)
                }
                lastEmitMillis.set(System.currentTimeMillis())

                val raw = nativeMutex.withLock { analyzer.nativeSnapshotJson(topN) }
                val parsed = runCatching { json.decodeFromString(LeakSnapshot.serializer(), raw) }.getOrNull()
                if (parsed != null) {
                    mutableSnapshot.value = parsed
                }
            }
        }
    }

    override fun setWindowMillis(windowMillis: Long) {
        val w = windowMillis.coerceAtLeast(1_000L)
        scope.launch {
            nativeMutex.withLock {
                analyzer.nativeSetWindowMs(w)
            }
            mutableSnapshot.update { it.copy(windowMs = w) }
            snapshotRequests.tryEmit(true)
        }
    }

    override fun reset() {
        scope.launch {
            nativeMutex.withLock {
                analyzer.nativeReset()
            }
            val w = mutableSnapshot.value.windowMs
            mutableSnapshot.value = LeakSnapshot(windowMs = w)
            snapshotRequests.tryEmit(true)
        }
    }

    override fun onDns(timestampMillis: Long, userIdentifier: Int, queryName: String, queryType: Int, serverIp: String) {
        scope.launch {
            nativeMutex.withLock {
                analyzer.nativeOnDns(timestampMillis, userIdentifier, queryName, queryType, serverIp)
            }
            snapshotRequests.tryEmit(false)
        }
    }

    override fun emitSnapshot(force: Boolean) {
        snapshotRequests.tryEmit(force)
    }

    override fun close() {
        scope.launch {
            nativeMutex.withLock {
                analyzer.nativeReset()
            }
        }
    }
}
