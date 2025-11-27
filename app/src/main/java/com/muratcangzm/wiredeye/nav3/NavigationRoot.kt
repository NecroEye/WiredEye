package com.muratcangzm.wiredeye.nav3

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.muratcangzm.common.nav3.CoreNavigator
import com.muratcangzm.common.nav3.LocalNavigator
import com.muratcangzm.common.nav3.Screens
import com.muratcangzm.details.DetailsScreen as DetailsScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Screens.WiredEyeScreen)
    val navigator = remember { CoreNavigator(backStack) }

    //TODO: Use it when it's working well or delete it later

    BackHandler(enabled = backStack.size > 1) {
        navigator.pop()
    }

    val provider = entryProvider<NavKey> {

        entry<Screens.WiredEyeScreen> {
            //WiredEyeContent()
        }

        entry<Screens.DetailsScreen> {
            DetailsScreenContent()
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { navigator.pop() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = provider,
        )
    }
}
