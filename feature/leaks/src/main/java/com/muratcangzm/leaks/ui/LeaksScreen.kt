package com.muratcangzm.leaks.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaksScreen(
    viewModel: LeaksViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    val bg = Brush.linearGradient(
        listOf(Color(0xFF0E141B), Color(0xFF0B1022), Color(0xFF0E141B))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaks", color = Color(0xFFDBEAFE)) },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(LeaksContract.Event.OpenHelp) }) {
                        Text("Help", color = Color(0xFF9EB2C0))
                    }
                    TextButton(onClick = { viewModel.onEvent(LeaksContract.Event.Refresh) }) {
                        Text("Refresh", color = Color(0xFF7BD7FF))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterRow(
                selectedSeverity = state.selectedSeverity,
                selectedTimeRange = state.selectedTimeRange,
                onSeverity = { viewModel.onEvent(LeaksContract.Event.SetSeverity(it)) },
                onTimeRange = { viewModel.onEvent(LeaksContract.Event.SetTimeRange(it)) }
            )

            EmptyStateCard()
        }
    }
}

@Composable
private fun HeaderRow(
    onRefresh: () -> Unit,
    onHelp: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🛡️ Leaks", color = Color(0xFFDBEAFE))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onHelp) { Text("Help", color = Color(0xFF9EB2C0)) }
        TextButton(onClick = onRefresh) { Text("Refresh", color = Color(0xFF7BD7FF)) }
    }
}

@Composable
private fun FilterRow(
    selectedSeverity: LeaksContract.Severity,
    selectedTimeRange: LeaksContract.TimeRange,
    onSeverity: (LeaksContract.Severity) -> Unit,
    onTimeRange: (LeaksContract.TimeRange) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostChip(
            label = "Severity: ${selectedSeverity.name}",
            onClick = {
                val next = when (selectedSeverity) {
                    LeaksContract.Severity.All -> LeaksContract.Severity.Low
                    LeaksContract.Severity.Low -> LeaksContract.Severity.Medium
                    LeaksContract.Severity.Medium -> LeaksContract.Severity.High
                    LeaksContract.Severity.High -> LeaksContract.Severity.All
                }
                onSeverity(next)
            }
        )
        GhostChip(
            label = "Range: ${selectedTimeRange.name}",
            onClick = {
                val next = when (selectedTimeRange) {
                    LeaksContract.TimeRange.Last1h -> LeaksContract.TimeRange.Last24h
                    LeaksContract.TimeRange.Last24h -> LeaksContract.TimeRange.Last7d
                    LeaksContract.TimeRange.Last7d -> LeaksContract.TimeRange.Last1h
                }
                onTimeRange(next)
            }
        )
    }
}

@Composable
private fun GhostChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border = Brush.linearGradient(
        listOf(
            Color(0xFF233355).copy(alpha = 0.65f),
            Color(0xFF7BD7FF).copy(alpha = 0.45f),
            Color(0xFF233355).copy(alpha = 0.65f)
        )
    )
    Surface(
        color = Color(0x22101826),
        shape = shape,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF9EB2C0),
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickableNoRipple(onClick)
        )
    }
}

@Composable
private fun EmptyStateCard() {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = Color(0x16101826),
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFF233355).copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxSize().padding(bottom = 40.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("No leaks detected yet.", color = Color(0xFFDBEAFE))
            Text(
                "When a payload matches sensitive patterns, events will appear here.",
                color = Color(0xFF9EB2C0)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Planned: app filters • data types • severity scoring • evidence redaction",
                color = Color(0xFF7BD7FF)
            )
        }
    }
}

@SuppressLint("SuspiciousModifierThen")
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )
