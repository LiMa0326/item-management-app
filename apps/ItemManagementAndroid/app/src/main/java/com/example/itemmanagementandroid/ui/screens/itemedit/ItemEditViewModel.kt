package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemEditViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val listItemsUseCase: ListItemsUseCase,
    private val getItemUseCase: GetItemUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemEditUiState())
    val uiState: StateFlow<ItemEditUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            runCatching {
                val categoryCount = listCategoriesUseCase(includeArchived = false).size
                val selectedItemId = listItemsUseCase(includeDeleted = false).firstOrNull()?.id
                val selectedItem = selectedItemId?.let { itemId ->
                    getItemUseCase(itemId = itemId)
                }

                val mode = if (selectedItem == null) {
                    ItemEditMode.CREATE
                } else {
                    ItemEditMode.EDIT
                }

                ItemEditUiState(
                    isLoading = false,
                    mode = mode,
                    targetItemName = selectedItem?.name ?: "New item",
                    availableCategoryCount = categoryCount
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Failed to load edit context."
                    )
                }
            }
        }
    }
}
