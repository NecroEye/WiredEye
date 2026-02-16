package com.muratcangzm.summary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
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
import com.muratcangzm.summary.ui.components.GhostBackground
import com.muratcangzm.summary.ui.components.SummaryDaysCard
import com.muratcangzm.summary.ui.components.SummaryTodayCard
import com.muratcangzm.summary.ui.components.SummaryTokens
import com.muratcangzm.summary.ui.components.SummaryWindowChips
import com.muratcangzm.ui.components.GhostShimmer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = koinViewModel(),
    snackbarBottomPadding: Dp,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { e ->
            when (e) {
                is SummaryContract.Effect.Snackbar -> snackbarHostState.showSnackbar(e.message)
                is SummaryContract.Effect.Toast -> snackbarHostState.showSnackbar(e.message)
                is SummaryContract.Effect.NavigateDayDetail -> Unit
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        GhostBackground(
            modifier = Modifier.fillMaxSize(),
            stars = 240,
            shootingStars = true,
            shootingStarsOnlyInTopHalf = true
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Summary", color = SummaryTokens.TextBright) },
                    actions = {
                        IconButton(onClick = { viewModel.onEvent(SummaryContract.Event.Refresh) }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = SummaryTokens.Accent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SummaryTokens.TopBar,
                        titleContentColor = SummaryTokens.TextBright,
                        actionIconContentColor = SummaryTokens.Accent,
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = snackbarBottomPadding)
                )
            },
            contentWindowInsets = WindowInsets(0),
            containerColor = Color.Transparent,
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryWindowChips(
                    selected = state.selectedWindow,
                    onSelect = { viewModel.onEvent(SummaryContract.Event.SetWindow(it)) }
                )

                if (state.isLoading) {
                    SummaryShimmerToday()
                    SummaryShimmerDays()
                } else {
                    SummaryTodayCard(today = state.today)

                    SummaryDaysCard(
                        items = state.lastDays,
                        onClickDay = { viewModel.onEvent(SummaryContract.Event.OpenDay(it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryShimmerToday() {
    GhostShimmer(
        modifier = Modifier
            .fillMaxSize()
            .size(132.dp)
    )
}

@Composable
private fun SummaryShimmerDays() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GhostShimmer(
            modifier = Modifier
                .fillMaxSize()
                .size(220.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostShimmer(
                modifier = Modifier
                    .weight(1f)
                    .size(46.dp)
            )
            GhostShimmer(
                modifier = Modifier
                    .weight(1f)
                    .size(46.dp)
            )
        }
    }
}
