package com.muratcangzm.details

import androidx.lifecycle.ViewModel
import com.muratcangzm.common.nav.Screens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DetailsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun setArguments(arguments: Screens.DetailsScreen) {
        _uiState.update { it.copy(uiPacket = arguments.uiPacket) }
    }

    fun toggleRawExpanded() {
        _uiState.update { it.copy(isRawExpanded = !it.isRawExpanded) }
    }
}
