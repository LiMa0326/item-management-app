package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotoCoversUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemListViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val listItemsUseCase: ListItemsUseCase,
    private val listItemPhotoCoversUseCase: ListItemPhotoCoversUseCase
) : ViewModel() {
    private var routeInitialCategoryId: String? = null
    private var hasUserOverriddenCategoryFilter: Boolean = false

    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            searchKeyword = state.searchKeyword,
            selectedCategoryId = state.selectedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun onRouteEntered(initialCategoryId: String?) {
        val normalizedRouteCategoryId = normalizeCategoryId(initialCategoryId)
        val state = _uiState.value
        if (routeInitialCategoryId != normalizedRouteCategoryId) {
            routeInitialCategoryId = normalizedRouteCategoryId
            hasUserOverriddenCategoryFilter = false
            load(
                includeDeleted = state.includeDeleted,
                searchKeyword = state.searchKeyword,
                selectedCategoryId = normalizedRouteCategoryId,
                sortOption = state.sortOption
            )
            return
        }

        val selectedCategoryId = if (hasUserOverriddenCategoryFilter) {
            state.selectedCategoryId
        } else {
            normalizedRouteCategoryId
        }
        load(
            includeDeleted = state.includeDeleted,
            searchKeyword = state.searchKeyword,
            selectedCategoryId = selectedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun setIncludeDeleted(includeDeleted: Boolean) {
        if (_uiState.value.includeDeleted == includeDeleted) {
            return
        }
        val state = _uiState.value
        load(
            includeDeleted = includeDeleted,
            searchKeyword = state.searchKeyword,
            selectedCategoryId = state.selectedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun setSearchKeyword(searchKeyword: String) {
        if (_uiState.value.searchKeyword == searchKeyword) {
            return
        }
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            searchKeyword = searchKeyword,
            selectedCategoryId = state.selectedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun setCategoryFilter(categoryId: String?) {
        val normalizedCategoryId = normalizeCategoryId(categoryId)
        if (_uiState.value.selectedCategoryId == normalizedCategoryId) {
            return
        }
        hasUserOverriddenCategoryFilter = true
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            searchKeyword = state.searchKeyword,
            selectedCategoryId = normalizedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun setSortOption(sortOption: ItemListSortOption) {
        if (_uiState.value.sortOption == sortOption) {
            return
        }
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            searchKeyword = state.searchKeyword,
            selectedCategoryId = state.selectedCategoryId,
            sortOption = sortOption
        )
    }

    private fun load(
        includeDeleted: Boolean,
        searchKeyword: String,
        selectedCategoryId: String?,
        sortOption: ItemListSortOption
    ) {
        val normalizedSearchKeyword = searchKeyword.trim()
            .takeIf(String::isNotEmpty)
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    includeDeleted = includeDeleted,
                    searchKeyword = searchKeyword,
                    selectedCategoryId = selectedCategoryId,
                    sortOption = sortOption,
                    errorMessage = null
                )
            }

            runCatching {
                val categories = listCategoriesUseCase(includeArchived = true)
                    .map { category ->
                        ItemListCategoryFilterUiModel(
                            id = category.id,
                            name = category.name,
                            isArchived = category.isArchived
                        )
                    }
                val items = listItemsUseCase(
                    query = ItemListQuery(
                        includeDeleted = includeDeleted,
                        categoryId = selectedCategoryId,
                        searchKeyword = normalizedSearchKeyword,
                        sortOption = sortOption
                    )
                )
                val hasAnyItemsInCurrentMode = listItemsUseCase(
                    query = ItemListQuery(
                        includeDeleted = includeDeleted,
                        categoryId = selectedCategoryId,
                        searchKeyword = null,
                        sortOption = sortOption
                    )
                ).isNotEmpty()
                val coverUriByItemId = listItemPhotoCoversUseCase(
                    itemIds = items.map { item -> item.id }
                ).associate { cover ->
                    cover.itemId to (cover.thumbnailUri ?: cover.localUri).orEmpty()
                }.filterValues { uri -> uri.isNotBlank() }

                ItemListUiState(
                    isLoading = false,
                    includeDeleted = includeDeleted,
                    searchKeyword = searchKeyword,
                    sortOption = sortOption,
                    selectedCategoryId = selectedCategoryId,
                    categoryFilters = categories,
                    hasAnyItemsInCurrentMode = hasAnyItemsInCurrentMode,
                    items = items,
                    coverUriByItemId = coverUriByItemId
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        includeDeleted = includeDeleted,
                        searchKeyword = searchKeyword,
                        selectedCategoryId = selectedCategoryId,
                        sortOption = sortOption,
                        errorMessage = throwable.message ?: "Failed to load items."
                    )
                }
            }
        }
    }

    private fun normalizeCategoryId(categoryId: String?): String? {
        return categoryId?.trim()?.takeIf(String::isNotEmpty)
    }
}
