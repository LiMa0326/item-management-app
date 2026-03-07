package com.example.itemmanagementandroid.ui.screens.itemlist

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemListSortOption

data class ItemListUiState(
    val isLoading: Boolean = true,
    val includeDeleted: Boolean = false,
    val searchKeyword: String = "",
    val sortOption: ItemListSortOption = ItemListSortOption.RECENTLY_UPDATED,
    val selectedCategoryId: String? = null,
    val categoryFilters: List<ItemListCategoryFilterUiModel> = emptyList(),
    val hasAnyItemsInCurrentMode: Boolean = false,
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null
) {
    val shouldShowEmptyState: Boolean
        get() = !isLoading && items.isEmpty() && !hasAnyItemsInCurrentMode

    val shouldShowNoResultsState: Boolean
        get() = !isLoading && items.isEmpty() && hasAnyItemsInCurrentMode
}
