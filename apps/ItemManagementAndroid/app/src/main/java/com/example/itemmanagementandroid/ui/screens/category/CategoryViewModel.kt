package com.example.itemmanagementandroid.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val includeArchived = _uiState.value.includeArchived
        load(includeArchived = includeArchived)
    }

    fun setIncludeArchived(includeArchived: Boolean) {
        if (_uiState.value.includeArchived == includeArchived) {
            return
        }
        load(includeArchived = includeArchived)
    }

    private fun load(includeArchived: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    includeArchived = includeArchived,
                    errorMessage = null
                )
            }

            runCatching {
                listCategoriesUseCase(includeArchived = includeArchived)
            }.onSuccess { categories ->
                _uiState.value = CategoryUiState(
                    isLoading = false,
                    includeArchived = includeArchived,
                    categories = categories
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        includeArchived = includeArchived,
                        errorMessage = throwable.message ?: "Failed to load categories."
                    )
                }
            }
        }
    }
}
