package com.muratcangzm.common.nav2

import com.muratcangzm.common.nav3.Screens

data class NavigationData(
    val destination: Screens,
    val popUpTo: Screens? = null,
    val popUpToInclusive: Boolean = false,
)