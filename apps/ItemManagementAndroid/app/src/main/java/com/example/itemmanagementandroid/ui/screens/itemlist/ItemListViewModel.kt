package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemListViewModel(
    private val listItemsUseCase: ListItemsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemListUiState())
    val uiState: StateFlow<ItemListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val includeDeleted = _uiState.value.includeDeleted
        load(includeDeleted = includeDeleted)
    }

    fun setIncludeDeleted(includeDeleted: Boolean) {
        if (_uiState.value.includeDeleted == includeDeleted) {
            return
        }
        load(includeDeleted = includeDeleted)
    }

    private fun load(includeDeleted: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    includeDeleted = includeDeleted,
                    errorMessage = null
                )
            }

            runCatching {
                listItemsUseCase(includeDeleted = includeDeleted)
            }.onSuccess { items ->
                _uiState.value = ItemListUiState(
                    isLoading = false,
                    includeDeleted = includeDeleted,
                    items = items
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        includeDeleted = includeDeleted,
                        errorMessage = throwable.message ?: "Failed to load items."
                    )
                }
            }
        }
    }
}
