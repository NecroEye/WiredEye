package com.muratcangzm.wiredeye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.wiredeye.nav2.WiredEyeNavGraph
import com.muratcangzm.wiredeye.ui.theme.WiredEyeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_WiredEye)
        enableEdgeToEdge()
        setContent {
            WiredEyeTheme {
                val navController = rememberNavController()
                val homeViewModel = homeViewModel

                LaunchedEffect(Unit) {
                    homeViewModel.navigationEvents.collect { data ->
                        navController.navigate(data.destination) {
                            data.popUpTo?.let { popTarget ->
                                popUpTo(popTarget) {
                                    inclusive = data.popUpToInclusive
                                }
                            }
                        }
                    }
                }

                WiredEyeNavGraph(
                    modifier = Modifier,
                    navController = navController,
                    homeViewModel = homeViewModel
                )
            }
        }
    }
}
