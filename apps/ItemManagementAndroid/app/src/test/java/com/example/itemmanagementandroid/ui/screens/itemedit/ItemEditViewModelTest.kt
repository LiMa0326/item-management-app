package com.example.itemmanagementandroid.ui.screens.itemedit

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoCover
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset
import com.example.itemmanagementandroid.domain.repository.CategoryRepository
import com.example.itemmanagementandroid.domain.repository.ItemRepository
import com.example.itemmanagementandroid.domain.repository.PhotoAssetProcessor
import com.example.itemmanagementandroid.domain.repository.PhotoRepository
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.CreateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.UpdateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.AddItemPhotoUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ImportItemPhotosUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase
import com.example.itemmanagementandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onRouteEntered_withNull_alwaysResetsToCreateBlankForm() = runTest(mainDispatcherRule.dispatcher) {
        val existingItem = Item(
            id = "item_existing",
            categoryId = DEFAULT_CATEGORY.id,
            name = "Existing Item",
            purchaseDate = null,
            purchasePrice = null,
            purchaseCurrency = null,
            purchasePlace = null,
            description = null,
            tags = emptyList(),
            customAttributes = emptyMap(),
            createdAt = "2026-03-07T10:00:00Z",
            updatedAt = "2026-03-07T10:00:00Z",
            deletedAt = null
        )
        val itemRepository = FakeItemRepository(initialItems = listOf(existingItem))
        val viewModel = createViewModel(
            initialItemId = existingItem.id,
            itemRepository = itemRepository
        )

        advanceUntilIdle()
        assertEquals(ItemEditMode.EDIT, viewModel.uiState.value.mode)
        assertEquals("Existing Item", viewModel.uiState.value.name)

        viewModel.setName("Temp Input")
        viewModel.onRouteEntered(itemId = null)
        advanceUntilIdle()
        assertCreateBlankState(viewModel.uiState.value)

        viewModel.setName("Temp Input Again")
        viewModel.onRouteEntered(itemId = null)
        advanceUntilIdle()
        assertCreateBlankState(viewModel.uiState.value)
    }

    @Test
    fun save_inCreateMode_createsItemAndRequestsNavigateToDetail() = runTest(mainDispatcherRule.dispatcher) {
        val itemRepository = FakeItemRepository()
        val viewModel = createViewModel(
            initialItemId = null,
            itemRepository = itemRepository
        )

        advanceUntilIdle()
        viewModel.setName("Sony WH-1000XM6")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ItemEditMode.EDIT, state.mode)
        assertTrue(state.editingItemId != null)
        assertEquals(state.editingItemId, state.navigateToDetailItemId)
        assertEquals("Item created.", state.saveResultMessage)
        assertEquals(1, itemRepository.createdCount)

        viewModel.onNavigateToDetailHandled()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.navigateToDetailItemId)
    }

    @Test
    fun save_duplicateName_mapsErrorToNameField() = runTest(mainDispatcherRule.dispatcher) {
        val existingItem = Item(
            id = "item_existing",
            categoryId = DEFAULT_CATEGORY.id,
            name = "Duplicate Name",
            purchaseDate = null,
            purchasePrice = null,
            purchaseCurrency = null,
            purchasePlace = null,
            description = null,
            tags = emptyList(),
            customAttributes = emptyMap(),
            createdAt = "2026-03-07T10:00:00Z",
            updatedAt = "2026-03-07T10:00:00Z",
            deletedAt = null
        )
        val itemRepository = FakeItemRepository(initialItems = listOf(existingItem))
        val viewModel = createViewModel(
            initialItemId = null,
            itemRepository = itemRepository
        )

        advanceUntilIdle()
        viewModel.setName("  duplicate name  ")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Item name already exists.", state.fieldErrors.name)
        assertNull(state.errorMessage)
        assertEquals(ItemEditMode.CREATE, state.mode)
        assertTrue(!state.isSaving)
    }

    @Test
    fun importPhotos_inCreateMode_autoCreatesItemAndSupportsRetry() = runTest(mainDispatcherRule.dispatcher) {
        val itemRepository = FakeItemRepository()
        val photoRepository = FakePhotoRepository()
        val photoProcessor = FakePhotoAssetProcessor(
            failedUris = mutableSetOf("content://mock/fail")
        )
        val viewModel = createViewModel(
            initialItemId = null,
            itemRepository = itemRepository,
            photoRepository = photoRepository,
            photoProcessor = photoProcessor
        )

        advanceUntilIdle()
        viewModel.importPhotoUris(
            sourceUris = listOf("content://mock/ok", "content://mock/fail")
        )
        advanceUntilIdle()

        val firstState = viewModel.uiState.value
        assertEquals(ItemEditMode.EDIT, firstState.mode)
        assertTrue(firstState.editingItemId != null)
        assertEquals("", firstState.name)
        assertEquals(1, firstState.photos.size)
        assertEquals(1, firstState.photoImportFailures.size)
        assertTrue(firstState.photoImportMessage!!.contains("failed 1"))
        assertTrue(itemRepository.createdCount >= 1)

        photoProcessor.failedUris.clear()
        viewModel.retryFailedPhotoImports()
        advanceUntilIdle()

        val secondState = viewModel.uiState.value
        assertEquals(2, secondState.photos.size)
        assertTrue(secondState.photoImportFailures.isEmpty())
        assertTrue(secondState.photoImportMessage!!.contains("Imported 1 photo"))
    }

    @Test
    fun importPhotos_preservesAlreadyTypedFields() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(
            initialItemId = null,
            itemRepository = FakeItemRepository()
        )

        advanceUntilIdle()
        val rowId = viewModel.uiState.value.customAttributesRows.first().rowId

        viewModel.setName("Camera Lens")
        viewModel.setPurchaseDate("2026-03-07")
        viewModel.setPurchasePriceInput("149.99")
        viewModel.setPurchaseCurrency("USD")
        viewModel.setPurchasePlace("B&H")
        viewModel.setDescription("Prime lens")
        viewModel.setTagsInput("photo, gear")
        viewModel.setCustomAttributeKey(rowId, "mount")
        viewModel.setCustomAttributeValue(rowId, "E-mount")

        viewModel.importPhotoUris(sourceUris = listOf("content://mock/photo_1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ItemEditMode.EDIT, state.mode)
        assertTrue(state.editingItemId != null)
        assertEquals("Camera Lens", state.name)
        assertEquals("2026-03-07", state.purchaseDate)
        assertEquals("149.99", state.purchasePriceInput)
        assertEquals("USD", state.purchaseCurrency)
        assertEquals("B&H", state.purchasePlace)
        assertEquals("Prime lens", state.description)
        assertEquals("photo, gear", state.tagsInput)
        assertEquals("mount", state.customAttributesRows.first().key)
        assertEquals("E-mount", state.customAttributesRows.first().value)
        assertEquals(1, state.photos.size)
    }

    private fun createViewModel(
        initialItemId: String?,
        itemRepository: FakeItemRepository,
        photoRepository: FakePhotoRepository = FakePhotoRepository(),
        photoProcessor: FakePhotoAssetProcessor = FakePhotoAssetProcessor()
    ): ItemEditViewModel {
        val categoryRepository = FakeCategoryRepository()
        return ItemEditViewModel(
            listCategoriesUseCase = ListCategoriesUseCase(categoryRepository),
            getItemUseCase = GetItemUseCase(itemRepository),
            listItemPhotosUseCase = ListItemPhotosUseCase(photoRepository),
            importItemPhotosUseCase = ImportItemPhotosUseCase(
                photoAssetProcessor = photoProcessor,
                addItemPhotoUseCase = AddItemPhotoUseCase(photoRepository)
            ),
            createItemUseCase = CreateItemUseCase(itemRepository),
            updateItemUseCase = UpdateItemUseCase(itemRepository),
            initialItemId = initialItemId
        )
    }

    private fun assertCreateBlankState(state: ItemEditUiState) {
        assertEquals(ItemEditMode.CREATE, state.mode)
        assertNull(state.editingItemId)
        assertEquals("", state.name)
        assertEquals("", state.purchaseDate)
        assertEquals("", state.purchasePriceInput)
        assertEquals("", state.purchaseCurrency)
        assertEquals("", state.purchasePlace)
        assertEquals("", state.description)
        assertEquals("", state.tagsInput)
        assertTrue(state.customAttributesRows.isNotEmpty())
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun list(includeArchived: Boolean): List<Category> {
            return listOf(DEFAULT_CATEGORY)
        }

        override suspend fun create(name: String): Category {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun update(categoryId: String, name: String): Category {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun setArchived(categoryId: String, archived: Boolean): Category {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun reorder(categoryIds: List<String>) {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun ensureDefaultCategory(): Boolean {
            return false
        }
    }

    private class FakeItemRepository(
        initialItems: List<Item> = emptyList()
    ) : ItemRepository {
        private val items: LinkedHashMap<String, Item> = linkedMapOf()
        private var nextId: Int = 0

        var createdCount: Int = 0
            private set

        init {
            initialItems.forEach { item ->
                items[item.id] = item.copy(name = item.name.trim())
                val suffix = item.id.removePrefix("item_").toIntOrNull() ?: 0
                if (suffix >= nextId) {
                    nextId = suffix + 1
                }
            }
        }

        override suspend fun list(query: ItemListQuery): List<Item> {
            return items.values
                .filter { item -> query.includeDeleted || item.deletedAt == null }
                .filter { item -> query.categoryId == null || item.categoryId == query.categoryId }
        }

        override suspend fun get(itemId: String): Item? {
            return items[itemId]
        }

        override suspend fun create(draft: ItemDraft): Item {
            val normalizedName = draft.name.trim()
            requireUniqueNameForCreate(normalizedName)

            val now = "2026-03-07T10:00:00Z"
            val created = Item(
                id = "item_${nextId++}",
                categoryId = draft.categoryId.trim(),
                name = normalizedName,
                purchaseDate = draft.purchaseDate?.trim()?.ifEmpty { null },
                purchasePrice = draft.purchasePrice,
                purchaseCurrency = draft.purchaseCurrency?.trim()?.ifEmpty { null },
                purchasePlace = draft.purchasePlace?.trim()?.ifEmpty { null },
                description = draft.description?.trim()?.ifEmpty { null },
                tags = draft.tags,
                customAttributes = draft.customAttributes,
                createdAt = now,
                updatedAt = now,
                deletedAt = null
            )
            items[created.id] = created
            createdCount += 1
            return created
        }

        override suspend fun update(itemId: String, draft: ItemDraft): Item {
            val existing = requireNotNull(items[itemId]) { "Item not found: $itemId" }
            val normalizedName = draft.name.trim()
            requireUniqueNameForUpdate(
                itemId = itemId,
                normalizedName = normalizedName
            )
            val updated = existing.copy(
                categoryId = draft.categoryId.trim(),
                name = normalizedName,
                purchaseDate = draft.purchaseDate?.trim()?.ifEmpty { null },
                purchasePrice = draft.purchasePrice,
                purchaseCurrency = draft.purchaseCurrency?.trim()?.ifEmpty { null },
                purchasePlace = draft.purchasePlace?.trim()?.ifEmpty { null },
                description = draft.description?.trim()?.ifEmpty { null },
                tags = draft.tags,
                customAttributes = draft.customAttributes,
                updatedAt = "2026-03-07T10:00:00Z"
            )
            items[itemId] = updated
            return updated
        }

        override suspend fun softDelete(itemId: String): Item {
            val existing = requireNotNull(items[itemId]) { "Item not found: $itemId" }
            val deleted = existing.copy(
                updatedAt = "2026-03-07T10:00:00Z",
                deletedAt = "2026-03-07T10:00:00Z"
            )
            items[itemId] = deleted
            return deleted
        }

        override suspend fun restore(itemId: String): Item {
            val existing = requireNotNull(items[itemId]) { "Item not found: $itemId" }
            val restored = existing.copy(
                updatedAt = "2026-03-07T10:00:00Z",
                deletedAt = null
            )
            items[itemId] = restored
            return restored
        }

        private fun requireUniqueNameForCreate(normalizedName: String) {
            val duplicated = items.values.any { item ->
                item.deletedAt == null && item.name.trim().equals(normalizedName, ignoreCase = true)
            }
            if (duplicated) {
                throw DuplicateItemNameException()
            }
        }

        private fun requireUniqueNameForUpdate(itemId: String, normalizedName: String) {
            val duplicated = items.values.any { item ->
                item.id != itemId &&
                    item.deletedAt == null &&
                    item.name.trim().equals(normalizedName, ignoreCase = true)
            }
            if (duplicated) {
                throw DuplicateItemNameException()
            }
        }
    }

    private class FakePhotoRepository : PhotoRepository {
        private val photosById: LinkedHashMap<String, ItemPhoto> = linkedMapOf()
        private var nextPhotoId: Int = 0

        override suspend fun listByItem(itemId: String): List<ItemPhoto> {
            return photosById.values.filter { it.itemId == itemId }
        }

        override suspend fun get(photoId: String): ItemPhoto? {
            return photosById[photoId]
        }

        override suspend fun add(draft: ItemPhotoDraft): ItemPhoto {
            val photo = ItemPhoto(
                id = "photo_${nextPhotoId++}",
                itemId = draft.itemId,
                localUri = draft.localUri,
                thumbnailUri = draft.thumbnailUri,
                contentType = draft.contentType,
                width = draft.width,
                height = draft.height,
                createdAt = "2026-03-07T10:00:00Z"
            )
            photosById[photo.id] = photo
            return photo
        }

        override suspend fun remove(photoId: String): Boolean {
            return photosById.remove(photoId) != null
        }

        override suspend fun listDeferredCleanupCandidates(): List<DeferredPhotoCleanupCandidate> {
            return emptyList()
        }

        override suspend fun listCoversByItemIds(itemIds: List<String>): List<ItemPhotoCover> {
            return itemIds.mapNotNull { itemId ->
                val first = photosById.values.firstOrNull { photo -> photo.itemId == itemId }
                if (first == null) {
                    null
                } else {
                    ItemPhotoCover(
                        itemId = itemId,
                        thumbnailUri = first.thumbnailUri,
                        localUri = first.localUri
                    )
                }
            }
        }
    }

    private class FakePhotoAssetProcessor(
        val failedUris: MutableSet<String> = mutableSetOf()
    ) : PhotoAssetProcessor {
        override suspend fun process(sourceUri: String): ProcessedPhotoAsset {
            if (failedUris.contains(sourceUri)) {
                throw IllegalStateException("Import failed for $sourceUri")
            }
            val normalized = sourceUri.replace(Regex("[^A-Za-z0-9]"), "_")
            return ProcessedPhotoAsset(
                sourceUri = sourceUri,
                localUri = "file:///processed/full_$normalized.jpg",
                thumbnailUri = "file:///processed/thumb_$normalized.jpg",
                contentType = "image/jpeg",
                width = 1200,
                height = 800
            )
        }

        override suspend fun delete(asset: ProcessedPhotoAsset) = Unit
    }

    private companion object {
        val DEFAULT_CATEGORY = Category(
            id = "cat_electronics",
            name = "Electronics",
            sortOrder = 0,
            isArchived = false,
            isSystemDefault = true,
            createdAt = "2026-03-07T10:00:00Z",
            updatedAt = "2026-03-07T10:00:00Z"
        )
    }
}
