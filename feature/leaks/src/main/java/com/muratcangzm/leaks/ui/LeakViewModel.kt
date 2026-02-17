package com.muratcangzm.leaks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muratcangzm.core.leak.LeakAnalyzerBridge
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.math.roundToInt

class LeaksViewModel(
    private val leakAnalyzerBridge: LeakAnalyzerBridge
) : ViewModel(), LeaksContract.Presenter {

    private val mutableState = MutableStateFlow(LeaksContract.State())
    override val state: StateFlow<LeaksContract.State> = mutableState.asStateFlow()

    private val mutableEffects = MutableSharedFlow<LeaksContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<LeaksContract.Effect> = mutableEffects.asSharedFlow()

    private val minimumShimmerMilliseconds = 320L
    private var refreshToken: Long = 0L

    private val asnEntries: List<CidrEntry> = buildAsnEntries()

    init {
        viewModelScope.launch {
            leakAnalyzerBridge.snapshot.collectLatest { snapshot ->
                runCatching {
                    mutableState.update { current ->
                        val query = current.query.trim()
                        val domains = snapshot.topDomains.filterQuery(query) { it.domain }

                        val servers = snapshot.topServers
                            .filterQuery(query) { it.ip }
                            .map { server ->
                                val asnInfo = resolveAsnInfo(server.ip)
                                LeaksContract.TopServerUi(
                                    ip = server.ip,
                                    count = server.count,
                                    asn = asnInfo?.asn,
                                    country = asnInfo?.country,
                                    org = asnInfo?.org
                                )
                            }

                        val reasons = buildList {
                            if (snapshot.publicDnsRatio >= 0.35) add(LeaksContract.Reason.PublicDns)
                            if (snapshot.suspiciousEntropyQueries > 0) add(LeaksContract.Reason.Entropy)
                            if (snapshot.burstQueries > 0) add(LeaksContract.Reason.Burst)
                        }

                        current.copy(
                            score = snapshot.score,
                            reasons = reasons,
                            totalQueries = snapshot.totalQueries,
                            uniqueDomains = snapshot.uniqueDomains,
                            publicDnsRatio = snapshot.publicDnsRatio,
                            suspiciousEntropyQueries = snapshot.suspiciousEntropyQueries,
                            burstQueries = snapshot.burstQueries,
                            topDomains = domains,
                            topServers = servers
                        )
                    }
                }.onFailure {
                    mutableEffects.tryEmit(LeaksContract.Effect.Snackbar("Leak analysis failed. Showing last known results."))
                }
            }
        }

        viewModelScope.launch {
            runCatching { leakAnalyzerBridge.emitSnapshot(force = true) }
        }
    }

    override fun onEvent(event: LeaksContract.Event) {
        when (event) {
            is LeaksContract.Event.SetQuery -> {
                mutableState.update { it.copy(query = event.value) }
                runCatching { leakAnalyzerBridge.emitSnapshot(force = true) }
            }

            is LeaksContract.Event.SetSeverity -> {
                mutableState.update { it.copy(selectedSeverity = event.value) }
                runCatching { leakAnalyzerBridge.emitSnapshot(force = true) }
            }

            is LeaksContract.Event.SetTimeRange -> {
                mutableState.update { it.copy(selectedTimeRange = event.value) }
                runCatching { leakAnalyzerBridge.setWindowMillis(event.value.windowMillis) }
            }

            LeaksContract.Event.Refresh -> {
                val token = System.nanoTime()
                refreshToken = token

                mutableState.update { it.copy(isLoading = true) }
                mutableEffects.tryEmit(LeaksContract.Effect.Snackbar("Re-analyzing recent traffic…"))

                runCatching { leakAnalyzerBridge.emitSnapshot(force = true) }

                viewModelScope.launch {
                    delay(minimumShimmerMilliseconds)
                    if (refreshToken == token) {
                        mutableState.update { it.copy(isLoading = false) }
                    }
                }
            }

            LeaksContract.Event.OpenHelp -> {
                val ratioPercent = (mutableState.value.publicDnsRatio * 100.0).roundToInt()
                mutableEffects.tryEmit(
                    LeaksContract.Effect.Toast(
                        "Score=${mutableState.value.score} | Total=${mutableState.value.totalQueries} | PublicDNS=$ratioPercent%"
                    )
                )
            }
        }
    }

    private inline fun <T> List<T>.filterQuery(query: String, crossinline key: (T) -> String): List<T> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return this
        return filter { item -> key(item).lowercase().contains(normalizedQuery) }
    }

    private fun resolveAsnInfo(ipString: String?): AsnInfo? {
        val normalizedIpString = ipString?.trim().orEmpty()
        if (normalizedIpString.isEmpty()) return null

        val inetAddress = runCatching { InetAddress.getByName(normalizedIpString) }.getOrNull() ?: return null
        val addressBytes = inetAddress.address ?: return null

        val isSupportedAddress = inetAddress is Inet4Address || inetAddress is Inet6Address
        if (!isSupportedAddress) return null

        return asnEntries.firstOrNull { entry ->
            cidrContains(addressBytes, entry.baseBytes, entry.prefixBits)
        }?.info
    }

    private fun cidrContains(addressBytes: ByteArray, baseBytes: ByteArray, prefixBits: Int): Boolean {
        if (prefixBits < 0) return false
        if (addressBytes.size != baseBytes.size) return false

        if (prefixBits == 0) return true

        val fullBytesCount = prefixBits / 8
        val remainingBitsCount = prefixBits % 8

        for (index in 0 until fullBytesCount) {
            if (addressBytes[index] != baseBytes[index]) return false
        }

        if (remainingBitsCount == 0) return true

        val mask = (0xFF shl (8 - remainingBitsCount)) and 0xFF
        val addressPart = addressBytes[fullBytesCount].toInt() and 0xFF
        val basePart = baseBytes[fullBytesCount].toInt() and 0xFF

        return (addressPart and mask) == (basePart and mask)
    }

    private fun buildAsnEntries(): List<CidrEntry> {
        val rawEntries = listOf(
            CidrText("8.8.8.0", 24, AsnInfo(15169, "US", "Google")),
            CidrText("1.1.1.0", 24, AsnInfo(13335, "US", "Cloudflare")),
            CidrText("52.0.0.0", 8, AsnInfo(16509, "US", "AWS")),
            CidrText("151.101.0.0", 16, AsnInfo(54113, "US", "Fastly")),
            CidrText("23.246.0.0", 16, AsnInfo(15133, "US", "Akamai")),

            CidrText("2001:4860:4860::", 48, AsnInfo(15169, "US", "Google")),
            CidrText("2606:4700:4700::", 48, AsnInfo(13335, "US", "Cloudflare"))
        )

        return rawEntries.mapNotNull { entry ->
            val inetAddress = runCatching { InetAddress.getByName(entry.base) }.getOrNull() ?: return@mapNotNull null
            val bytes = inetAddress.address ?: return@mapNotNull null

            val isSupportedAddress = inetAddress is Inet4Address || inetAddress is Inet6Address
            if (!isSupportedAddress) return@mapNotNull null

            val maximumPrefixBits = bytes.size * 8
            val safePrefixBits = entry.prefix.coerceIn(0, maximumPrefixBits)

            CidrEntry(
                baseBytes = bytes,
                prefixBits = safePrefixBits,
                info = entry.info
            )
        }
    }

    private data class AsnInfo(val asn: Int, val country: String, val org: String)
    private data class CidrText(val base: String, val prefix: Int, val info: AsnInfo)
    private data class CidrEntry(val baseBytes: ByteArray, val prefixBits: Int, val info: AsnInfo)
}
