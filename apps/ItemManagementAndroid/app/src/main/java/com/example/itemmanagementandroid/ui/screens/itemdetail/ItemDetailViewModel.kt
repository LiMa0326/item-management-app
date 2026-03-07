package com.example.itemmanagementandroid.ui.screens.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.item.RestoreItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.SoftDeleteItemUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val listItemsUseCase: ListItemsUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val listItemPhotosUseCase: ListItemPhotosUseCase,
    private val softDeleteItemUseCase: SoftDeleteItemUseCase,
    private val restoreItemUseCase: RestoreItemUseCase,
    initialItemId: String? = null
) : ViewModel() {
    private var preferredItemId: String? = initialItemId

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            runCatching { loadUiState() }.onSuccess { state ->
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

    fun softDelete() {
        val itemId = _uiState.value.selectedItemId ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingAction = true,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                softDeleteItemUseCase(itemId)
                preferredItemId = itemId
                loadUiState(actionMessage = "Item deleted. You can restore it from this page.")
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingAction = false,
                        errorMessage = throwable.message ?: "Failed to delete item."
                    )
                }
            }
        }
    }

    fun restore() {
        val itemId = _uiState.value.selectedItemId ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isApplyingAction = true,
                    errorMessage = null,
                    actionMessage = null
                )
            }

            runCatching {
                restoreItemUseCase(itemId)
                preferredItemId = itemId
                loadUiState(actionMessage = "Item restored.")
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isApplyingAction = false,
                        errorMessage = throwable.message ?: "Failed to restore item."
                    )
                }
            }
        }
    }

    private suspend fun loadUiState(actionMessage: String? = null): ItemDetailUiState {
        val selectedItemId = resolveSelectedItemId() ?: return ItemDetailUiState(
            isLoading = false,
            actionMessage = actionMessage
        )
        val selectedItem = getItemUseCase(itemId = selectedItemId) ?: return ItemDetailUiState(
            isLoading = false,
            actionMessage = actionMessage
        )

        preferredItemId = selectedItem.id
        val photos = listItemPhotosUseCase(itemId = selectedItem.id)

        return ItemDetailUiState(
            isLoading = false,
            isApplyingAction = false,
            selectedItemId = selectedItem.id,
            categoryId = selectedItem.categoryId,
            name = selectedItem.name,
            purchaseDate = selectedItem.purchaseDate,
            purchasePrice = selectedItem.purchasePrice,
            purchaseCurrency = selectedItem.purchaseCurrency,
            purchasePlace = selectedItem.purchasePlace,
            description = selectedItem.description,
            tags = selectedItem.tags,
            customAttributes = selectedItem.customAttributes,
            createdAt = selectedItem.createdAt,
            updatedAt = selectedItem.updatedAt,
            deletedAt = selectedItem.deletedAt,
            photos = photos.map { photo ->
                ItemDetailPhotoUiModel(
                    id = photo.id,
                    contentType = photo.contentType,
                    localUri = photo.localUri,
                    thumbnailUri = photo.thumbnailUri,
                    width = photo.width,
                    height = photo.height
                )
            },
            actionMessage = actionMessage
        )
    }

    private suspend fun resolveSelectedItemId(): String? {
        val itemFromPreferredId = preferredItemId?.let { getItemUseCase(itemId = it) }
        if (itemFromPreferredId != null) {
            return itemFromPreferredId.id
        }

        preferredItemId = null
        return listItemsUseCase(includeDeleted = false).firstOrNull()?.id
    }
}
