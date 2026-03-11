package com.example.itemmanagementandroid.ui.navigation

data class AppNavigationUiState(
    val backStack: List<AppRoute> = listOf(AppRoute.Category),
    val currentRoute: AppRoute = AppRoute.Category,
    val canGoBack: Boolean = false
)
