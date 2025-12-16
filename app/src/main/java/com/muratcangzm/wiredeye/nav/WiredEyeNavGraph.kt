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
import androidx.navigation.toRoute
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.common.nav.createGenericNavType
import com.muratcangzm.details.DetailsScreen
import com.muratcangzm.monitor.WiredEyeScreen
import com.muratcangzm.shared.model.UiPacket
import kotlin.reflect.typeOf
import com.muratcangzm.common.nav.Screens as Screen

@Suppress("ParamsComparedByRef")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WiredEyeNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    homeViewModel: HomeViewModel,
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
            WiredEyeScreen(homeViewModel = homeViewModel)
        }

        composable<Screen.DetailsScreen>(
            typeMap = mapOf(typeOf<UiPacket>() to createGenericNavType<UiPacket>())
        ) {
            val arguments = it.toRoute<Screen.DetailsScreen>()
            DetailsScreen(homeViewModel = homeViewModel, arguments = arguments)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun defaultEnterTransition(): EnterTransition {
    return slideInHorizontally(initialOffsetX = { it })
}

@OptIn(ExperimentalAnimationApi::class)
private fun defaultExitTransition(): ExitTransition {
    return slideOutHorizontally(targetOffsetX = { -it })
}

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopEnterTransition(): EnterTransition {
    return slideInHorizontally(initialOffsetX = { -it })
}

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopExitTransition(): ExitTransition {
    return slideOutHorizontally(targetOffsetX = { it })
}