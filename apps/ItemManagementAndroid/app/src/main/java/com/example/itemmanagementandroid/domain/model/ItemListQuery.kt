package com.example.itemmanagementandroid.domain.model

data class ItemListQuery(
    val includeDeleted: Boolean = false,
    val categoryId: String? = null,
    val searchKeyword: String? = null,
    val sortOption: ItemListSortOption = ItemListSortOption.RECENTLY_UPDATED
)
