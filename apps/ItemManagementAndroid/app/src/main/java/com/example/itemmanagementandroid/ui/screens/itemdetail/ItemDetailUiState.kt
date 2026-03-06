package com.example.itemmanagementandroid.ui.screens.itemdetail

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val selectedItemId: String? = null,
    val selectedItemName: String? = null,
    val selectedCategoryId: String? = null,
    val photoCount: Int = 0,
    val errorMessage: String? = null
)
