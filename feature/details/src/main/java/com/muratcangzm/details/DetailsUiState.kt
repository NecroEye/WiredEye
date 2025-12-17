package com.muratcangzm.details

import androidx.compose.runtime.Immutable
import com.muratcangzm.shared.model.UiPacket

@Immutable
data class DetailsUiState(
    val uiPacket: UiPacket = UiPacket.EMPTY,
    val isRawExpanded: Boolean = false,
)