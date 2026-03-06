package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemListViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val listItemsUseCase: ListItemsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            selectedCategoryId = state.selectedCategoryId,
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
            selectedCategoryId = state.selectedCategoryId,
            sortOption = state.sortOption
        )
    }

    fun setCategoryFilter(categoryId: String?) {
        if (_uiState.value.selectedCategoryId == categoryId) {
            return
        }
        val state = _uiState.value
        load(
            includeDeleted = state.includeDeleted,
            selectedCategoryId = categoryId,
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
            selectedCategoryId = state.selectedCategoryId,
            sortOption = sortOption
        )
    }

    private fun load(
        includeDeleted: Boolean,
        selectedCategoryId: String?,
        sortOption: ItemListSortOption
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    includeDeleted = includeDeleted,
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
                        sortOption = sortOption
                    )
                )
                val hasAnyItemsInCurrentMode = listItemsUseCase(
                    query = ItemListQuery(
                        includeDeleted = includeDeleted,
                        categoryId = null,
                        sortOption = sortOption
                    )
                ).isNotEmpty()

                ItemListUiState(
                    isLoading = false,
                    includeDeleted = includeDeleted,
                    sortOption = sortOption,
                    selectedCategoryId = selectedCategoryId,
                    categoryFilters = categories,
                    hasAnyItemsInCurrentMode = hasAnyItemsInCurrentMode,
                    items = items
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        includeDeleted = includeDeleted,
                        selectedCategoryId = selectedCategoryId,
                        sortOption = sortOption,
                        errorMessage = throwable.message ?: "Failed to load items."
                    )
                }
            }
        }
    }
}
