package com.example.itemmanagementandroid.ui.screens.itemdetail

data class ItemDetailPhotoUiModel(
    val id: String,
    val contentType: String,
    val localUri: String,
    val thumbnailUri: String?,
    val width: Int?,
    val height: Int?
)

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val isApplyingAction: Boolean = false,
    val selectedItemId: String? = null,
    val categoryId: String? = null,
    val name: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val purchaseCurrency: String? = null,
    val purchasePlace: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val customAttributes: Map<String, Any> = emptyMap(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val photos: List<ItemDetailPhotoUiModel> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null
) {
    val hasItem: Boolean
        get() = selectedItemId != null

    val isDeleted: Boolean
        get() = deletedAt != null
}
