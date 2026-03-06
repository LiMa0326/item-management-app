package com.example.itemmanagementandroid.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.usecase.category.CreateCategoryUseCase
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.category.ReorderCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.category.SetCategoryArchivedUseCase
import com.example.itemmanagementandroid.domain.usecase.category.UpdateCategoryUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val listItemsUseCase: ListItemsUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val setCategoryArchivedUseCase: SetCategoryArchivedUseCase,
    private val reorderCategoriesUseCase: ReorderCategoriesUseCase
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

    fun createCategory(name: String) {
        performMutation {
            createCategoryUseCase(name = name)
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        performMutation {
            updateCategoryUseCase(
                categoryId = categoryId,
                name = newName
            )
        }
    }

    fun setCategoryArchived(categoryId: String, archived: Boolean) {
        performMutation {
            setCategoryArchivedUseCase(
                categoryId = categoryId,
                archived = archived
            )
        }
    }

    fun moveCategoryUp(categoryId: String) {
        performReorder(
            categoryId = categoryId,
            direction = -1
        )
    }

    fun moveCategoryDown(categoryId: String) {
        performReorder(
            categoryId = categoryId,
            direction = 1
        )
    }

    private fun performMutation(action: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching {
                action()
            }.onSuccess {
                refresh()
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: "Failed to update category."
                    )
                }
            }
        }
    }

    private fun performReorder(
        categoryId: String,
        direction: Int
    ) {
        viewModelScope.launch {
            runCatching {
                val allCategories = listCategoriesUseCase(includeArchived = true)
                val visibleCategories = if (_uiState.value.includeArchived) {
                    allCategories
                } else {
                    allCategories.filterNot(Category::isArchived)
                }
                val visibleOrder = visibleCategories.map(Category::id).toMutableList()
                val currentIndex = visibleOrder.indexOf(categoryId)
                if (currentIndex == -1) {
                    return@runCatching
                }

                val targetIndex = currentIndex + direction
                if (targetIndex !in visibleOrder.indices) {
                    return@runCatching
                }

                val movedId = visibleOrder[currentIndex]
                visibleOrder[currentIndex] = visibleOrder[targetIndex]
                visibleOrder[targetIndex] = movedId

                val visibleIds = visibleOrder.toSet()
                var visibleCursor = 0
                val mergedOrder = allCategories.map { category ->
                    if (visibleIds.contains(category.id)) {
                        visibleOrder[visibleCursor++]
                    } else {
                        category.id
                    }
                }

                reorderCategoriesUseCase(categoryIds = mergedOrder)
            }.onSuccess {
                refresh()
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        errorMessage = throwable.message ?: "Failed to reorder categories."
                    )
                }
            }
        }
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
                val categories = listCategoriesUseCase(includeArchived = includeArchived)
                val activeItems = listItemsUseCase(includeDeleted = false)
                val itemCountByCategory = activeItems
                    .groupingBy { item -> item.categoryId }
                    .eachCount()

                categories.map { category ->
                    category.toListItemUiModel(
                        itemCount = itemCountByCategory[category.id] ?: 0
                    )
                }
            }.onSuccess { categoryItems ->
                _uiState.value = CategoryUiState(
                    isLoading = false,
                    includeArchived = includeArchived,
                    categories = categoryItems
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

private fun Category.toListItemUiModel(itemCount: Int): CategoryListItemUiModel {
    return CategoryListItemUiModel(
        id = id,
        name = name,
        isArchived = isArchived,
        isSystemDefault = isSystemDefault,
        itemCount = itemCount
    )
}
