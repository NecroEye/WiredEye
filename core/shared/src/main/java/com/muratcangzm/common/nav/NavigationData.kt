package com.muratcangzm.common.nav

data class NavigationData(
    val destination: Screens,
    val popUpTo: Screens? = null,
    val popUpToInclusive: Boolean = false,
)