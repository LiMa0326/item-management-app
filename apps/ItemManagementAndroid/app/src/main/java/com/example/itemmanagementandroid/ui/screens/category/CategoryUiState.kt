package com.example.itemmanagementandroid.ui.screens.category

import com.example.itemmanagementandroid.domain.model.Category

data class CategoryUiState(
    val isLoading: Boolean = true,
    val includeArchived: Boolean = false,
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null
)
