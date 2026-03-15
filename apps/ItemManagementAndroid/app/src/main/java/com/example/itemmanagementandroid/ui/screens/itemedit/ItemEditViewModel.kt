package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.PhotoImportSummary
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.CreateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.UpdateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ImportItemPhotosUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ItemEditViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val getItemUseCase: GetItemUseCase,
    private val listItemPhotosUseCase: ListItemPhotosUseCase,
    private val importItemPhotosUseCase: ImportItemPhotosUseCase,
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
            requestedItemId = requestedItemId,
            saveResultMessage = null
        )
    }

    fun onRouteEntered(itemId: String?) {
        load(
            requestedItemId = itemId,
            saveResultMessage = null
        )
    }

    fun onNavigateToDetailHandled() {
        _uiState.update { state ->
            if (state.navigateToDetailItemId == null) {
                state
            } else {
                state.copy(navigateToDetailItemId = null)
            }
        }
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
                fieldErrors = state.fieldErrors.copy(purchaseDate = null),
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

    fun importPhotoUris(sourceUris: List<String>) {
        val normalizedUris = sourceUris
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (normalizedUris.isEmpty()) {
            return
        }
        val snapshot = _uiState.value
        if (snapshot.isLoading || snapshot.isSaving || snapshot.isImportingPhotos) {
            return
        }
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isImportingPhotos = true,
                    photoImportMessage = null
                )
            }

            runCatching {
                val target = ensureItemIdForPhotoImport(snapshot)
                val importSummary = importItemPhotosUseCase(
                    itemId = target.itemId,
                    sourceUris = normalizedUris
                )
                val latestPhotos = listItemPhotosUseCase(itemId = target.itemId)
                    .map(::toPhotoUiModel)
                Triple(target, importSummary, latestPhotos)
            }.onSuccess { (target, importSummary, latestPhotos) ->
                _uiState.update { state ->
                    state.copy(
                        mode = ItemEditMode.EDIT,
                        editingItemId = target.itemId,
                        categoryId = if (state.categoryId.trim().isEmpty()) {
                            target.resolvedCategoryId
                        } else {
                            state.categoryId
                        },
                        photos = latestPhotos,
                        isImportingPhotos = false,
                        photoImportFailures = importSummary.failures.map { failure ->
                            ItemEditPhotoImportFailureUiModel(
                                sourceUri = failure.sourceUri,
                                reason = failure.reason
                            )
                        },
                        photoImportMessage = buildPhotoImportMessage(importSummary),
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isImportingPhotos = false,
                        errorMessage = throwable.message ?: "Failed to import photos."
                    )
                }
            }
        }
    }

    fun retryFailedPhotoImports() {
        val failedUris = _uiState.value.photoImportFailures
            .map(ItemEditPhotoImportFailureUiModel::sourceUri)
        if (failedUris.isEmpty()) {
            return
        }
        importPhotoUris(failedUris)
    }

    fun save() {
        val stateSnapshot = _uiState.value
        if (stateSnapshot.isLoading || stateSnapshot.isSaving || stateSnapshot.isImportingPhotos) {
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
                    requestedItemId = savedItem.id,
                    saveResultMessage = saveResultMessage
                ).copy(
                    navigateToDetailItemId = savedItem.id
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
        val photos = selectedItem?.let { item ->
            listItemPhotosUseCase(itemId = item.id).map(::toPhotoUiModel)
        } ?: emptyList()

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
            purchasePriceInput = selectedItem?.purchasePrice
                ?.let(ItemEditFormMapper::formatPurchasePrice)
                .orEmpty(),
            purchaseCurrency = selectedItem?.purchaseCurrency
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: DEFAULT_PURCHASE_CURRENCY,
            purchasePlace = selectedItem?.purchasePlace.orEmpty(),
            description = selectedItem?.description.orEmpty(),
            tagsInput = selectedItem?.tags?.joinToString(", ").orEmpty(),
            customAttributesRows = rows,
            photos = photos,
            isImportingPhotos = false,
            photoImportFailures = emptyList(),
            photoImportMessage = null,
            fieldErrors = ItemEditFieldErrors(),
            errorMessage = switchedToCreateMessage,
            saveResultMessage = saveResultMessage,
            navigateToDetailItemId = null
        )
    }

    private fun toPhotoUiModel(
        photo: com.example.itemmanagementandroid.domain.model.ItemPhoto
    ): ItemEditPhotoUiModel {
        return ItemEditPhotoUiModel(
            id = photo.id,
            localUri = photo.localUri,
            thumbnailUri = photo.thumbnailUri,
            contentType = photo.contentType,
            width = photo.width,
            height = photo.height
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

        val purchaseDateError = runCatching {
            ItemEditFormMapper.normalizePurchaseDate(state.purchaseDate)
        }.exceptionOrNull()?.message

        val customAttributesError = runCatching {
            ItemEditFormMapper.parseCustomAttributes(state.customAttributesRows)
        }.exceptionOrNull()?.message

        return ItemEditFieldErrors(
            name = nameError,
            categoryId = categoryError,
            purchaseDate = purchaseDateError,
            purchasePrice = purchasePriceError,
            customAttributes = customAttributesError
        )
    }

    private fun buildDraft(state: ItemEditUiState): ItemDraft {
        return ItemDraft(
            categoryId = state.categoryId.trim(),
            name = state.name.trim(),
            purchaseDate = ItemEditFormMapper.normalizePurchaseDate(state.purchaseDate),
            purchasePrice = ItemEditFormMapper.parsePurchasePrice(state.purchasePriceInput),
            purchaseCurrency = state.purchaseCurrency
                .trim()
                .ifEmpty { DEFAULT_PURCHASE_CURRENCY },
            purchasePlace = state.purchasePlace.trim().ifEmpty { null },
            description = state.description.trim().ifEmpty { null },
            tags = ItemEditFormMapper.parseTags(state.tagsInput),
            customAttributes = ItemEditFormMapper.parseCustomAttributes(state.customAttributesRows)
        )
    }

    private suspend fun ensureItemIdForPhotoImport(
        stateSnapshot: ItemEditUiState
    ): PhotoImportTarget {
        stateSnapshot.editingItemId?.let {
            return PhotoImportTarget(
                itemId = it,
                resolvedCategoryId = stateSnapshot.categoryId
            )
        }

        val normalizedCategoryId = stateSnapshot.categoryId.trim()
            .ifEmpty {
                listCategoriesUseCase(includeArchived = true).firstOrNull()?.id.orEmpty()
            }
        require(normalizedCategoryId.isNotEmpty()) {
            "Category is required before importing photos."
        }

        val nameForCreation = stateSnapshot.name.trim().ifEmpty { defaultAutoItemName() }
        val draft = ItemDraft(
            categoryId = normalizedCategoryId,
            name = nameForCreation
        )
        val createdItem = createItemUseCase(draft = draft)
        requestedItemId = createdItem.id
        return PhotoImportTarget(
            itemId = createdItem.id,
            resolvedCategoryId = normalizedCategoryId
        )
    }

    private fun defaultAutoItemName(): String {
        return "New Item_${
            AUTO_NAME_FORMATTER.format(Instant.now())
        }"
    }

    private fun buildPhotoImportMessage(summary: PhotoImportSummary): String {
        return if (summary.failureCount == 0) {
            "Imported ${summary.successCount} photo(s)."
        } else {
            "Imported ${summary.successCount} photo(s), failed ${summary.failureCount}."
        }
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

    private data class PhotoImportTarget(
        val itemId: String,
        val resolvedCategoryId: String
    )

    private companion object {
        const val DEFAULT_PURCHASE_CURRENCY = "USD"

        val AUTO_NAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
    }
}
