package com.muratcangzm.wiredeye

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.muratcangzm.wiredeye.nav.NavigationRoot
import com.muratcangzm.wiredeye.ui.theme.WiredEyeTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WiredEyeTheme {
                val navController: NavHostController = rememberNavController()
                NavigationRoot(modifier = Modifier, navController = navController)
            }
        }
    }
}
