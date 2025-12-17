package com.muratcangzm.details

sealed interface DetailsUiEvent {
    data object ToggleRaw : DetailsUiEvent
}