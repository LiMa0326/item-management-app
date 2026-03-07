package com.example.itemmanagementandroid.ui.screens.itemedit

enum class ItemEditMode {
    CREATE,
    EDIT
}

data class ItemEditCategoryOptionUiModel(
    val id: String,
    val name: String,
    val isArchived: Boolean
)

data class ItemEditCustomAttributeRowUiModel(
    val rowId: String,
    val key: String = "",
    val value: String = ""
)

data class ItemEditFieldErrors(
    val name: String? = null,
    val categoryId: String? = null,
    val purchasePrice: String? = null,
    val customAttributes: String? = null
) {
    val hasAny: Boolean
        get() = name != null || categoryId != null || purchasePrice != null || customAttributes != null
}

data class ItemEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val mode: ItemEditMode = ItemEditMode.CREATE,
    val editingItemId: String? = null,
    val availableCategories: List<ItemEditCategoryOptionUiModel> = emptyList(),
    val categoryId: String = "",
    val name: String = "",
    val purchaseDate: String = "",
    val purchasePriceInput: String = "",
    val purchaseCurrency: String = "",
    val purchasePlace: String = "",
    val description: String = "",
    val tagsInput: String = "",
    val customAttributesRows: List<ItemEditCustomAttributeRowUiModel> = emptyList(),
    val fieldErrors: ItemEditFieldErrors = ItemEditFieldErrors(),
    val errorMessage: String? = null,
    val saveResultMessage: String? = null
)
