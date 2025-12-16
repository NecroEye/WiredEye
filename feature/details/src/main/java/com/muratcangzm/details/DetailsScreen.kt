package com.muratcangzm.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muratcangzm.common.HomeViewModel
import com.muratcangzm.common.nav.Screens
import org.koin.androidx.compose.koinViewModel

@Composable
fun DetailsScreen(
    homeViewModel: HomeViewModel,
    detailsViewModel: DetailsViewModel = koinViewModel(),
    arguments: Screens.DetailsScreen,
) {

    val uiState by detailsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        detailsViewModel.setArguments(arguments = arguments)

    }

    DetailsScreenContent(
        uiState = uiState
    )
}

@Composable
private fun DetailsScreenContent(
    uiState: DetailsUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = uiState.uiPacket.proto.ifEmpty { "Details Screen" }, color = Color.Black)
    }
}


@Preview(showSystemUi = true)
@Composable
private fun DetailsScreenPreview() {
    DetailsScreenContent(
        uiState = DetailsUiState()
    )
}