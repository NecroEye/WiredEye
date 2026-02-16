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
import kotlin.math.roundToInt

class LeaksViewModel(
    private val leakAnalyzerBridge: LeakAnalyzerBridge
) : ViewModel(), LeaksContract.Presenter {

    private val _state = MutableStateFlow(LeaksContract.State())
    override val state: StateFlow<LeaksContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LeaksContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val effects: SharedFlow<LeaksContract.Effect> = _effects.asSharedFlow()

    private val minShimmerMs = 320L
    private var refreshToken: Long = 0L

    private val asnTable: List<Cidr> = listOf(
        Cidr("8.8.8.0", 24, AsnInfo(15169, "US", "Google")),
        Cidr("1.1.1.0", 24, AsnInfo(13335, "US", "Cloudflare")),
        Cidr("52.0.0.0", 8, AsnInfo(16509, "US", "AWS")),
        Cidr("151.101.0.0", 16, AsnInfo(54113, "US", "Fastly")),
        Cidr("23.246.0.0", 16, AsnInfo(15133, "US", "Akamai"))
    )

    init {
        viewModelScope.launch {
            leakAnalyzerBridge.snapshot.collectLatest { snap ->
                _state.update { current ->
                    val q = current.query.trim()
                    val domains = snap.topDomains.filterQuery(q) { it.domain }
                    val servers = snap.topServers
                        .filterQuery(q) { it.ip }
                        .map { s ->
                            val info = asnCountry(s.ip)
                            LeaksContract.TopServerUi(
                                ip = s.ip,
                                count = s.count,
                                asn = info?.asn,
                                country = info?.country,
                                org = info?.org
                            )
                        }

                    val reasons = buildList {
                        if (snap.publicDnsRatio >= 0.35) add(LeaksContract.Reason.PublicDns)
                        if (snap.suspiciousEntropyQueries > 0) add(LeaksContract.Reason.Entropy)
                        if (snap.burstQueries > 0) add(LeaksContract.Reason.Burst)
                    }

                    current.copy(
                        score = snap.score,
                        reasons = reasons,
                        totalQueries = snap.totalQueries,
                        uniqueDomains = snap.uniqueDomains,
                        publicDnsRatio = snap.publicDnsRatio,
                        suspiciousEntropyQueries = snap.suspiciousEntropyQueries,
                        burstQueries = snap.burstQueries,
                        topDomains = domains,
                        topServers = servers
                    )
                }
            }
        }

        viewModelScope.launch {
            leakAnalyzerBridge.emitSnapshot(force = true)
        }
    }

    override fun onEvent(event: LeaksContract.Event) {
        when (event) {
            is LeaksContract.Event.SetQuery -> {
                _state.update { it.copy(query = event.value) }
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            is LeaksContract.Event.SetSeverity -> {
                _state.update { it.copy(selectedSeverity = event.value) }
                leakAnalyzerBridge.emitSnapshot(force = true)
            }

            is LeaksContract.Event.SetTimeRange -> {
                _state.update { it.copy(selectedTimeRange = event.value) }
                leakAnalyzerBridge.setWindowMillis(event.value.windowMillis)
            }

            LeaksContract.Event.Refresh -> {
                val token = System.nanoTime()
                refreshToken = token

                _state.update { it.copy(isLoading = true) }
                _effects.tryEmit(LeaksContract.Effect.Snackbar("Re-analyzing recent traffic…"))

                leakAnalyzerBridge.emitSnapshot(force = true)

                viewModelScope.launch {
                    delay(minShimmerMs)
                    if (refreshToken == token) {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }

            LeaksContract.Event.OpenHelp -> {
                val ratio = (_state.value.publicDnsRatio * 100.0).roundToInt()
                _effects.tryEmit(
                    LeaksContract.Effect.Toast(
                        "Score=${_state.value.score} | Total=${_state.value.totalQueries} | PublicDNS=$ratio%"
                    )
                )
            }
        }
    }

    private inline fun <T> List<T>.filterQuery(query: String, crossinline key: (T) -> String): List<T> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return this
        return filter { key(it).lowercase().contains(q) }
    }

    private fun asnCountry(ip: String?): AsnInfo? {
        val safeIp = ip?.trim().orEmpty()
        if (safeIp.isEmpty()) return null
        if (!ipv4Pattern.matches(safeIp)) return null

        val x = ipv4ToInt(safeIp)
        return asnTable.firstOrNull { ipv4InCidr(x, it) }?.info
    }

    private fun ipv4ToInt(ip: String): Int {
        val parts = ip.split('.')
        if (parts.size != 4) return 0

        return try {
            (parts[0].toInt() shl 24) or
                    (parts[1].toInt() shl 16) or
                    (parts[2].toInt() shl 8) or
                    parts[3].toInt()
        } catch (_: Exception) {
            0
        }
    }

    private fun ipv4InCidr(addr: Int, c: Cidr): Boolean {
        val base = ipv4ToInt(c.base)
        val mask = if (c.prefix == 0) 0 else -1 shl (32 - c.prefix)
        return (addr and mask) == (base and mask)
    }

    private val ipv4Pattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

    private data class AsnInfo(val asn: Int, val country: String, val org: String)
    private data class Cidr(val base: String, val prefix: Int, val info: AsnInfo)
}
