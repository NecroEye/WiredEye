package com.muratcangzm.common.nav3

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class CoreNavigator(
    private val backStack: NavBackStack<NavKey>
) : Navigator {

    override fun navigate(screen: Screens) {
        backStack.add(screen)
    }

    override fun pop() {
        backStack.removeLastOrNull()
    }
}