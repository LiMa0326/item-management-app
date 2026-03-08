package com.example.itemmanagementandroid.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppNavigationViewModel : ViewModel() {
    private val backStack = mutableListOf<AppRoute>(AppRoute.Home)
    private val _uiState = MutableStateFlow(
        AppNavigationUiState(
            backStack = backStack.toList(),
            currentRoute = AppRoute.Home,
            canGoBack = false
        )
    )

    val uiState: StateFlow<AppNavigationUiState> = _uiState.asStateFlow()

    fun navigate(route: AppRoute) {
        if (route == backStack.last()) {
            return
        }
        backStack.add(route)
        publishState()
    }

    fun goBack() {
        if (backStack.size <= 1) {
            return
        }
        backStack.removeAt(backStack.lastIndex)
        publishState()
    }

    fun navigateToItemDetailAfterEdit(itemId: String) {
        val normalizedItemId = itemId.trim()
        if (normalizedItemId.isEmpty()) {
            return
        }

        val itemListIndex = backStack.indexOfLast { route -> route is AppRoute.ItemList }
        if (itemListIndex >= 0) {
            while (backStack.size > itemListIndex + 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        } else {
            while (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
            if (backStack.lastOrNull() != AppRoute.ItemList) {
                backStack.add(AppRoute.ItemList)
            }
        }

        val targetRoute = AppRoute.ItemDetail(itemId = normalizedItemId)
        if (backStack.lastOrNull() != targetRoute) {
            backStack.add(targetRoute)
        }
        publishState()
    }

    private fun publishState() {
        val stackSnapshot = backStack.toList()
        _uiState.value = AppNavigationUiState(
            backStack = stackSnapshot,
            currentRoute = stackSnapshot.last(),
            canGoBack = stackSnapshot.size > 1
        )
    }
}
