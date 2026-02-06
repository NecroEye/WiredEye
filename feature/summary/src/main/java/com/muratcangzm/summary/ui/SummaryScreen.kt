package com.muratcangzm.summary.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    val bg = Brush.linearGradient(
        listOf(Color(0xFF0E141B), Color(0xFF0B1022), Color(0xFF0E141B))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary", color = Color(0xFFDBEAFE)) },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(SummaryContract.Event.Refresh) }) {
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
            WindowRow(
                selected = state.selectedWindow,
                onSelect = { viewModel.onEvent(SummaryContract.Event.SetWindow(it)) }
            )

            TodayCard(state.today)

            DaysList(
                items = state.lastDays,
                onClickDay = { viewModel.onEvent(SummaryContract.Event.OpenDay(it)) }
            )
        }
    }
}


@Composable
private fun HeaderRow(onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("📊 Summary", color = Color(0xFFDBEAFE), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onRefresh) { Text("Refresh", color = Color(0xFF7BD7FF)) }
    }
}

@Composable
private fun WindowRow(
    selected: SummaryContract.Window,
    onSelect: (SummaryContract.Window) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostChip(
            label = "Day",
            selected = selected == SummaryContract.Window.Day,
            onClick = { onSelect(SummaryContract.Window.Day) }
        )
        GhostChip(
            label = "Week",
            selected = selected == SummaryContract.Window.Week,
            onClick = { onSelect(SummaryContract.Window.Week) }
        )
        GhostChip(
            label = "Month",
            selected = selected == SummaryContract.Window.Month,
            onClick = { onSelect(SummaryContract.Window.Month) }
        )
    }
}

@Composable
private fun TodayCard(today: SummaryContract.Today) {
    val shape = RoundedCornerShape(16.dp)
    val border = Brush.linearGradient(
        listOf(
            Color(0xFF233355).copy(alpha = 0.55f),
            Color(0xFF7BD7FF).copy(alpha = 0.35f),
            Color(0xFF233355).copy(alpha = 0.55f)
        )
    )

    Surface(
        color = Color(0x16101826),
        shape = shape,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Today", color = Color(0xFFDBEAFE))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                StatPill(label = "Traffic", value = "—")
                StatPill(label = "Leaks", value = today.leakCount.toString())
                StatPill(label = "Trackers", value = today.trackerCount.toString())
            }

            Text(
                text = "Top app: ${today.topAppLabel}",
                color = Color(0xFF9EB2C0)
            )
        }
    }
}

@Composable
private fun DaysList(
    items: List<SummaryContract.DayItem>,
    onClickDay: (String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = Color(0x12101826),
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFF233355).copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items, key = { it.dateLabel }) { day ->
                DayRow(
                    day = day,
                    onClick = { onClickDay(day.dateLabel) }
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    day: SummaryContract.DayItem,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(day.dateLabel, color = Color(0xFFDBEAFE))
        Spacer(Modifier.weight(1f))
        Text("Traffic: —", color = Color(0xFF9EB2C0))
        Spacer(Modifier.height(0.dp))
        Text("  •  Leaks: ${day.leakCount}", color = Color(0xFF9EB2C0))
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        color = Color(0x22101826),
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFF233355).copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, color = Color(0xFF9EB2C0))
            Text(value, color = Color(0xFF7BD7FF))
        }
    }
}

@Composable
private fun GhostChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border = Brush.linearGradient(
        listOf(
            Color(0xFF233355).copy(alpha = 0.65f),
            Color(0xFF7BD7FF).copy(alpha = if (selected) 0.55f else 0.25f),
            Color(0xFF233355).copy(alpha = 0.65f)
        )
    )
    Surface(
        color = Color(0x22101826),
        shape = shape,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = if (selected) "• $label" else label,
            color = if (selected) Color(0xFF7BD7FF) else Color(0xFF9EB2C0),
            modifier = Modifier
                .clickableNoRipple(onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
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
