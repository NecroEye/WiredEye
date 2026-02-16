package com.muratcangzm.wiredeye.nav

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.leaks.ui.LeaksScreen
import com.muratcangzm.monitor.WiredEyeScreen
import com.muratcangzm.summary.ui.SummaryScreen

private object RootRoutes {
    const val Monitor = "tab/monitor"
    const val Leaks = "tab/leaks"
    const val Summary = "tab/summary"
}

@Immutable
data class TabSpec(
    val route: String,
    val label: String,
    val iconText: String,
)

private val tabs = listOf(
    TabSpec(RootRoutes.Monitor, "Monitor", "🛰️"),
    TabSpec(RootRoutes.Leaks, "Leaks", "🛡️"),
    TabSpec(RootRoutes.Summary, "Summary", "📊"),
)

val LocalBottomBarPadding = compositionLocalOf { 0.dp }

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WiredEyeRootScreen(
    homeViewModel: HomeViewModel,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val selectedRoute = backStackEntry?.destination?.route ?: RootRoutes.Monitor

    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeightDp = with(LocalDensity.current) { bottomBarHeightPx.toDp() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            WiredEyeBottomBar(
                tabs = tabs,
                selectedRoute = selectedRoute,
                onTabClick = { route ->
                    tabNavController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(tabNavController.graph.startDestinationId) { saveState = true }
                    }
                },
                modifier = Modifier.onSizeChanged { bottomBarHeightPx = it.height }
            )
        }
    ) {
                NavHost(
                    navController = tabNavController,
                    startDestination = RootRoutes.Monitor,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(RootRoutes.Monitor) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = bottomBarHeightDp)
                        ) {
                            WiredEyeScreen(homeViewModel = homeViewModel)
                        }
                    }

                    composable(RootRoutes.Leaks) {
                        CompositionLocalProvider(
                            LocalBottomBarPadding provides (bottomBarHeightDp + 8.dp)
                        ) {
                            LeaksScreen(snackbarBottomPadding = bottomBarHeightDp)
                        }
                    }

                    composable(RootRoutes.Summary) {
                        CompositionLocalProvider(
                            LocalBottomBarPadding provides (bottomBarHeightDp + 8.dp)
                        ) {
                            SummaryScreen(snackbarBottomPadding = bottomBarHeightDp)
                        }
                    }
                }
            }
        }
