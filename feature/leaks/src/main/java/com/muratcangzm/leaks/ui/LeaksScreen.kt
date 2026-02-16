package com.muratcangzm.leaks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.leaks.ui.components.LeaksBackground
import com.muratcangzm.leaks.ui.components.LeaksGaugeCard
import com.muratcangzm.leaks.ui.components.LeaksPanels
import com.muratcangzm.leaks.ui.components.LeaksScopeRow
import com.muratcangzm.leaks.ui.components.LeaksTokens
import com.muratcangzm.ui.components.GhostShimmer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaksScreen(
    viewModel: LeaksViewModel = koinViewModel(),
    snackbarBottomPadding: Dp,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { e ->
            when (e) {
                is LeaksContract.Effect.Snackbar -> snackbarHostState.showSnackbar(e.message)
                is LeaksContract.Effect.Toast -> snackbarHostState.showSnackbar(e.message)
                is LeaksContract.Effect.NavigateLeakDetail -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaks", color = LeaksTokens.TextStrong) },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(LeaksContract.Event.Refresh) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = LeaksTokens.Accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LeaksTokens.TopBar,
                    titleContentColor = LeaksTokens.TextStrong,
                    actionIconContentColor = LeaksTokens.Accent
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = snackbarBottomPadding)) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LeaksBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LeaksScopeRow(
                    selectedSeverity = state.selectedSeverity,
                    selectedTimeRange = state.selectedTimeRange,
                    onSeverity = { viewModel.onEvent(LeaksContract.Event.SetSeverity(it)) },
                    onTimeRange = { viewModel.onEvent(LeaksContract.Event.SetTimeRange(it)) }
                )

                if (state.isLoading) {
                    LeaksShimmerGauge()
                    LeaksShimmerPanels()
                } else {
                    LeaksGaugeCard(
                        score = state.score,
                        reasons = state.reasons,
                        totalQueries = state.totalQueries,
                        uniqueDomains = state.uniqueDomains,
                        publicDnsRatio = state.publicDnsRatio,
                        suspiciousEntropyQueries = state.suspiciousEntropyQueries,
                        burstQueries = state.burstQueries
                    )

                    LeaksPanels(
                        topDomains = state.topDomains,
                        topServers = state.topServers
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaksShimmerGauge() {
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .width(1.dp)
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
            .padding(top = 0.dp)
            .padding(0.dp)
            .padding(0.dp)
            .padding(0.dp)
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
            .padding(0.dp)
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    )
}

@Composable
private fun LeaksShimmerPanels() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostShimmer(modifier = Modifier.weight(1f).size(140.dp))
        GhostShimmer(modifier = Modifier.weight(1f).size(140.dp))
    }
}
