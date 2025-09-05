package com.muratcangzm.wiredeye.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.muratcangzm.common.nav.Screen
import com.muratcangzm.monitor.WiredEyeScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.WiredEyeScreen,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() },
    ) {

        composable<Screen.WiredEyeScreen> {
            WiredEyeScreen()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun defaultEnterTransition(): EnterTransition {
    return slideInHorizontally(initialOffsetX = { it })
}

@OptIn(ExperimentalAnimationApi::class)
fun defaultExitTransition(): ExitTransition {
    return slideOutHorizontally(targetOffsetX = { -it })
}

@OptIn(ExperimentalAnimationApi::class)
fun defaultPopEnterTransition(): EnterTransition {
    return slideInHorizontally(initialOffsetX = { -it })
}

@OptIn(ExperimentalAnimationApi::class)
fun defaultPopExitTransition(): ExitTransition {
    return slideOutHorizontally(targetOffsetX = { it })
}