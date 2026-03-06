package com.example.itemmanagementandroid.ui.screens.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val listItemsUseCase: ListItemsUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val listItemPhotosUseCase: ListItemPhotosUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null)
            }

            runCatching {
                val selectedItemId = listItemsUseCase(includeDeleted = false)
                    .firstOrNull()
                    ?.id

                if (selectedItemId == null) {
                    return@runCatching ItemDetailUiState(isLoading = false)
                }

                val selectedItem = getItemUseCase(itemId = selectedItemId)
                if (selectedItem == null) {
                    return@runCatching ItemDetailUiState(isLoading = false)
                }

                val photoCount = listItemPhotosUseCase(itemId = selectedItemId).size
                ItemDetailUiState(
                    isLoading = false,
                    selectedItemId = selectedItem.id,
                    selectedItemName = selectedItem.name,
                    selectedCategoryId = selectedItem.categoryId,
                    photoCount = photoCount
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Failed to load item detail."
                    )
                }
            }
        }
    }
}
