package com.example.itemmanagementandroid.ui.screens.category

data class CategoryListItemUiModel(
    val id: String,
    val name: String,
    val isArchived: Boolean,
    val isSystemDefault: Boolean,
    val itemCount: Int
)
