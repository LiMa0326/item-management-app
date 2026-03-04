package com.example.itemmanagementandroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

class AppNavigatorState(initialRoute: AppRoute = AppRoute.Home) {
    private val backStack = mutableStateListOf(initialRoute)

    val currentRoute: AppRoute
        get() = backStack.last()

    fun navigate(route: AppRoute) {
        if (route != currentRoute) {
            backStack.add(route)
        }
    }

    fun canGoBack(): Boolean = backStack.size > 1

    fun goBack() {
        if (canGoBack()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}

@Composable
fun rememberAppNavigatorState(initialRoute: AppRoute = AppRoute.Home): AppNavigatorState {
    return remember {
        AppNavigatorState(initialRoute = initialRoute)
    }
}
