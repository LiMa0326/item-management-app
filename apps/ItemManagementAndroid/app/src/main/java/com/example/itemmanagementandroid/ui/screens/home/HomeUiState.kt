package com.example.itemmanagementandroid.ui.screens.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val categoryCount: Int = 0,
    val itemCount: Int = 0,
    val errorMessage: String? = null
)
