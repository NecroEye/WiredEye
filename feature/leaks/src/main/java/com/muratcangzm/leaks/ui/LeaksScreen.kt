package com.muratcangzm.leaks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.leaks.ui.components.LeaksBackground
import com.muratcangzm.leaks.ui.components.LeaksGaugeCard
import com.muratcangzm.leaks.ui.components.LeaksPanels
import com.muratcangzm.leaks.ui.components.LeaksScopeRow
import com.muratcangzm.leaks.ui.components.LeaksTokens
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaksScreen(
    viewModel: LeaksViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

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

                LeaksGaugeCard(
                    score = state.score,
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
