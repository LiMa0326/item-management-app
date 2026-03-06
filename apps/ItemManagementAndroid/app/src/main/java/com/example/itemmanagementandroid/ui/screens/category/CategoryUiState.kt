package com.example.itemmanagementandroid.ui.screens.category

data class CategoryUiState(
    val isLoading: Boolean = true,
    val includeArchived: Boolean = false,
    val categories: List<CategoryListItemUiModel> = emptyList(),
    val errorMessage: String? = null
)
