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
import com.muratcangzm.shared.model.network.UiPacket
import kotlin.reflect.typeOf
import com.muratcangzm.common.nav.Screens as Screen

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
        startDestination = Screen.Root,
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() },
    ) {

        composable<Screen.Root> {
            WiredEyeRootScreen(homeViewModel = homeViewModel)
        }

        composable<Screen.DetailsScreen>(
            typeMap = mapOf(typeOf<UiPacket>() to createGenericNavType<UiPacket>())
        ) {
            val arguments = it.toRoute<Screen.DetailsScreen>()
            DetailsScreen(homeViewModel = homeViewModel, arguments = arguments)
        }

        composable<Screen.SettingsScreen> {
            com.muratcangzm.settings.ui.SettingsScreen(
                onNavigateAccount = {},
                onNavigatePremium = {},
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun defaultEnterTransition(): EnterTransition = slideInHorizontally(initialOffsetX = { it })

@OptIn(ExperimentalAnimationApi::class)
private fun defaultExitTransition(): ExitTransition = slideOutHorizontally(targetOffsetX = { -it })

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it })

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it })
