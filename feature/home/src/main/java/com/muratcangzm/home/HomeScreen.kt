package com.muratcangzm.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.muratcangzm.ui.composable.LoadImageFromDrawable

@Composable
fun HomeScreen(modifier: Modifier) {

    val systemUIController = rememberSystemUiController()
    systemUIController.setStatusBarColor(color = androidx.compose.ui.graphics.Color.Transparent)

    //Create Assets module :P

}