package com.example.itemmanagementandroid.ui.navigation

data class AppNavigationUiState(
    val backStack: List<AppRoute> = listOf(AppRoute.Home),
    val currentRoute: AppRoute = AppRoute.Home,
    val canGoBack: Boolean = false
)
