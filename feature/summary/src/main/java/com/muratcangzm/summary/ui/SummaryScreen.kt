// feature/summary/src/main/java/com/muratcangzm/summary/ui/SummaryScreen.kt
package com.muratcangzm.summary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.summary.ui.components.GhostBackground
import com.muratcangzm.summary.ui.components.SummaryDaysCard
import com.muratcangzm.summary.ui.components.SummaryTodayCard
import com.muratcangzm.summary.ui.components.SummaryTokens
import com.muratcangzm.summary.ui.components.SummaryWindowChips
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

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
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            contentWindowInsets = WindowInsets(0),
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 12f.dp, vertical = 10f.dp),
                verticalArrangement = Arrangement.spacedBy(12f.dp)
            ) {
                SummaryWindowChips(
                    selected = state.selectedWindow,
                    onSelect = { viewModel.onEvent(SummaryContract.Event.SetWindow(it)) }
                )

                SummaryTodayCard(today = state.today)

                SummaryDaysCard(
                    items = state.lastDays,
                    onClickDay = { viewModel.onEvent(SummaryContract.Event.OpenDay(it)) }
                )
            }
        }
    }
}

private val Float.dp get() = androidx.compose.ui.unit.Dp(this)
