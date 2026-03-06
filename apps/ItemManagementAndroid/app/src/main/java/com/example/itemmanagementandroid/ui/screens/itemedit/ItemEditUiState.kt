package com.example.itemmanagementandroid.ui.screens.itemedit

enum class ItemEditMode {
    CREATE,
    EDIT
}

data class ItemEditUiState(
    val isLoading: Boolean = true,
    val mode: ItemEditMode = ItemEditMode.CREATE,
    val targetItemName: String = "New item",
    val availableCategoryCount: Int = 0,
    val errorMessage: String? = null
)
