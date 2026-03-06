package com.example.itemmanagementandroid.ui.screens.itemlist

import com.example.itemmanagementandroid.domain.model.Item

data class ItemListUiState(
    val isLoading: Boolean = true,
    val includeDeleted: Boolean = false,
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null
)
