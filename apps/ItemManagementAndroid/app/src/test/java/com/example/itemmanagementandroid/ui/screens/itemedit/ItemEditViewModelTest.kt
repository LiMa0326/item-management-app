package com.example.itemmanagementandroid.ui.screens.itemedit

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.repository.CategoryRepository
import com.example.itemmanagementandroid.domain.repository.ItemRepository
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.CreateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.UpdateItemUseCase
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
    fun save_inCreateMode_createsItemAndKeepsCreateBlankForm() = runTest(mainDispatcherRule.dispatcher) {
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
        assertEquals(ItemEditMode.CREATE, state.mode)
        assertNull(state.editingItemId)
        assertEquals("", state.name)
        assertEquals("Item created.", state.saveResultMessage)
        assertEquals(1, itemRepository.createdCount)
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

    private fun createViewModel(
        initialItemId: String?,
        itemRepository: FakeItemRepository
    ): ItemEditViewModel {
        val categoryRepository = FakeCategoryRepository()
        return ItemEditViewModel(
            listCategoriesUseCase = ListCategoriesUseCase(categoryRepository),
            getItemUseCase = GetItemUseCase(itemRepository),
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
