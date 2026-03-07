package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.CreateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.UpdateItemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemEditViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val createItemUseCase: CreateItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val initialItemId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(ItemEditUiState())
    val uiState: StateFlow<ItemEditUiState> = _uiState.asStateFlow()
    private var nextCustomAttributeRowId: Long = 0
    private var requestedItemId: String? = initialItemId

    init {
        load(
            requestedItemId = requestedItemId,
            saveResultMessage = null
        )
    }

    fun refresh() {
        load(
            requestedItemId = this.requestedItemId,
            saveResultMessage = null
        )
    }

    fun onRouteEntered(itemId: String?) {
        load(
            requestedItemId = itemId,
            saveResultMessage = null
        )
    }

    fun setName(value: String) {
        _uiState.update { state ->
            state.copy(
                name = value,
                fieldErrors = state.fieldErrors.copy(name = null),
                saveResultMessage = null
            )
        }
    }

    fun setCategoryId(value: String) {
        _uiState.update { state ->
            state.copy(
                categoryId = value,
                fieldErrors = state.fieldErrors.copy(categoryId = null),
                saveResultMessage = null
            )
        }
    }

    fun setPurchaseDate(value: String) {
        _uiState.update { state ->
            state.copy(
                purchaseDate = value,
                saveResultMessage = null
            )
        }
    }

    fun setPurchasePriceInput(value: String) {
        _uiState.update { state ->
            state.copy(
                purchasePriceInput = value,
                fieldErrors = state.fieldErrors.copy(purchasePrice = null),
                saveResultMessage = null
            )
        }
    }

    fun setPurchaseCurrency(value: String) {
        _uiState.update { state ->
            state.copy(
                purchaseCurrency = value,
                saveResultMessage = null
            )
        }
    }

    fun setPurchasePlace(value: String) {
        _uiState.update { state ->
            state.copy(
                purchasePlace = value,
                saveResultMessage = null
            )
        }
    }

    fun setDescription(value: String) {
        _uiState.update { state ->
            state.copy(
                description = value,
                saveResultMessage = null
            )
        }
    }

    fun setTagsInput(value: String) {
        _uiState.update { state ->
            state.copy(
                tagsInput = value,
                saveResultMessage = null
            )
        }
    }

    fun addCustomAttributeRow() {
        _uiState.update { state ->
            state.copy(
                customAttributesRows = state.customAttributesRows + newCustomAttributeRow(),
                fieldErrors = state.fieldErrors.copy(customAttributes = null),
                saveResultMessage = null
            )
        }
    }

    fun removeCustomAttributeRow(rowId: String) {
        _uiState.update { state ->
            val remainingRows = state.customAttributesRows
                .filterNot { row -> row.rowId == rowId }
                .ifEmpty { listOf(newCustomAttributeRow()) }

            state.copy(
                customAttributesRows = remainingRows,
                fieldErrors = state.fieldErrors.copy(customAttributes = null),
                saveResultMessage = null
            )
        }
    }

    fun setCustomAttributeKey(rowId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                customAttributesRows = state.customAttributesRows.map { row ->
                    if (row.rowId == rowId) {
                        row.copy(key = value)
                    } else {
                        row
                    }
                },
                fieldErrors = state.fieldErrors.copy(customAttributes = null),
                saveResultMessage = null
            )
        }
    }

    fun setCustomAttributeValue(rowId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                customAttributesRows = state.customAttributesRows.map { row ->
                    if (row.rowId == rowId) {
                        row.copy(value = value)
                    } else {
                        row
                    }
                },
                fieldErrors = state.fieldErrors.copy(customAttributes = null),
                saveResultMessage = null
            )
        }
    }

    fun save() {
        val stateSnapshot = _uiState.value
        if (stateSnapshot.isLoading || stateSnapshot.isSaving) {
            return
        }

        val fieldErrors = validate(stateSnapshot)
        if (fieldErrors.hasAny) {
            _uiState.update { state ->
                state.copy(
                    fieldErrors = fieldErrors,
                    saveResultMessage = null
                )
            }
            return
        }

        val itemDraft = runCatching { buildDraft(stateSnapshot) }
            .getOrElse { throwable ->
                _uiState.update { state ->
                    state.copy(
                        fieldErrors = state.fieldErrors.copy(
                            customAttributes = throwable.message ?: "Invalid custom attributes."
                        ),
                        saveResultMessage = null
                    )
                }
                return
            }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isSaving = true,
                    fieldErrors = ItemEditFieldErrors(),
                    errorMessage = null,
                    saveResultMessage = null
                )
            }

            runCatching {
                val savedItem = if (stateSnapshot.editingItemId == null) {
                    createItemUseCase(draft = itemDraft)
                } else {
                    updateItemUseCase(
                        itemId = stateSnapshot.editingItemId,
                        draft = itemDraft
                    )
                }

                val saveResultMessage = if (stateSnapshot.editingItemId == null) {
                    "Item created."
                } else {
                    "Item updated."
                }
                buildUiState(
                    requestedItemId = if (stateSnapshot.editingItemId == null) null else savedItem.id,
                    saveResultMessage = saveResultMessage
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                val duplicateNameError = if (throwable is DuplicateItemNameException) {
                    throwable.message ?: "Item name already exists."
                } else {
                    null
                }
                _uiState.update { state ->
                    state.copy(
                        isSaving = false,
                        fieldErrors = if (duplicateNameError == null) {
                            state.fieldErrors
                        } else {
                            state.fieldErrors.copy(name = duplicateNameError)
                        },
                        errorMessage = if (duplicateNameError == null) {
                            throwable.message ?: "Failed to save item."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun load(
        requestedItemId: String?,
        saveResultMessage: String?
    ) {
        this.requestedItemId = requestedItemId
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = true,
                    isSaving = false,
                    errorMessage = null,
                    saveResultMessage = saveResultMessage ?: state.saveResultMessage
                )
            }

            runCatching {
                buildUiState(
                    requestedItemId = requestedItemId,
                    saveResultMessage = saveResultMessage
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isSaving = false,
                        errorMessage = throwable.message ?: "Failed to load edit context."
                    )
                }
            }
        }
    }

    private suspend fun buildUiState(
        requestedItemId: String?,
        saveResultMessage: String?
    ): ItemEditUiState {
        val categories = listCategoriesUseCase(includeArchived = true)
            .map { category ->
                ItemEditCategoryOptionUiModel(
                    id = category.id,
                    name = category.name,
                    isArchived = category.isArchived
                )
            }

        require(categories.isNotEmpty()) {
            "At least one category is required before editing items."
        }

        val defaultCategoryId = categories.firstOrNull { category -> !category.isArchived }?.id
            ?: categories.first().id

        val selectedItem = requestedItemId?.let { itemId ->
            getItemUseCase(itemId = itemId)
        }

        val switchedToCreateMessage = if (requestedItemId != null && selectedItem == null) {
            "Item not found. Switched to create mode."
        } else {
            null
        }

        val rows = if (selectedItem == null) {
            listOf(newCustomAttributeRow())
        } else {
            toCustomAttributeRows(selectedItem.customAttributes)
        }

        return ItemEditUiState(
            isLoading = false,
            isSaving = false,
            mode = if (selectedItem == null) ItemEditMode.CREATE else ItemEditMode.EDIT,
            editingItemId = selectedItem?.id,
            availableCategories = categories,
            categoryId = selectedItem?.categoryId ?: defaultCategoryId,
            name = selectedItem?.name.orEmpty(),
            purchaseDate = selectedItem?.purchaseDate.orEmpty(),
            purchasePriceInput = selectedItem?.purchasePrice?.toString().orEmpty(),
            purchaseCurrency = selectedItem?.purchaseCurrency.orEmpty(),
            purchasePlace = selectedItem?.purchasePlace.orEmpty(),
            description = selectedItem?.description.orEmpty(),
            tagsInput = selectedItem?.tags?.joinToString(", ").orEmpty(),
            customAttributesRows = rows,
            fieldErrors = ItemEditFieldErrors(),
            errorMessage = switchedToCreateMessage,
            saveResultMessage = saveResultMessage
        )
    }

    private fun validate(state: ItemEditUiState): ItemEditFieldErrors {
        val nameError = if (state.name.trim().isEmpty()) {
            "Name is required."
        } else {
            null
        }

        val categoryError = if (state.categoryId.trim().isEmpty()) {
            "Category is required."
        } else {
            null
        }

        val purchasePriceError = runCatching {
            ItemEditFormMapper.parsePurchasePrice(state.purchasePriceInput)
        }.exceptionOrNull()?.message

        val customAttributesError = runCatching {
            ItemEditFormMapper.parseCustomAttributes(state.customAttributesRows)
        }.exceptionOrNull()?.message

        return ItemEditFieldErrors(
            name = nameError,
            categoryId = categoryError,
            purchasePrice = purchasePriceError,
            customAttributes = customAttributesError
        )
    }

    private fun buildDraft(state: ItemEditUiState): ItemDraft {
        return ItemDraft(
            categoryId = state.categoryId.trim(),
            name = state.name.trim(),
            purchaseDate = state.purchaseDate.trim().ifEmpty { null },
            purchasePrice = ItemEditFormMapper.parsePurchasePrice(state.purchasePriceInput),
            purchaseCurrency = state.purchaseCurrency.trim().ifEmpty { null },
            purchasePlace = state.purchasePlace.trim().ifEmpty { null },
            description = state.description.trim().ifEmpty { null },
            tags = ItemEditFormMapper.parseTags(state.tagsInput),
            customAttributes = ItemEditFormMapper.parseCustomAttributes(state.customAttributesRows)
        )
    }

    private fun toCustomAttributeRows(
        customAttributes: Map<String, Any>
    ): List<ItemEditCustomAttributeRowUiModel> {
        if (customAttributes.isEmpty()) {
            return listOf(newCustomAttributeRow())
        }
        return customAttributes.entries.map { (key, value) ->
            newCustomAttributeRow(
                key = key,
                value = value.toString()
            )
        }
    }

    private fun newCustomAttributeRow(
        key: String = "",
        value: String = ""
    ): ItemEditCustomAttributeRowUiModel {
        val rowId = "attr_${nextCustomAttributeRowId++}"
        return ItemEditCustomAttributeRowUiModel(
            rowId = rowId,
            key = key,
            value = value
        )
    }
}
