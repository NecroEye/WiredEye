package com.muratcangzm.preferences.model

import kotlinx.serialization.Serializable

@Serializable
data class Preferences(
    val startHintShown: Boolean = false,
)