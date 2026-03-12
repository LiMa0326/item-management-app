package com.example.itemmanagementandroid.ui.screens.itemlist

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoCover
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotoCoversUseCase
import com.example.itemmanagementandroid.domain.repository.CategoryRepository
import com.example.itemmanagementandroid.domain.repository.ItemRepository
import com.example.itemmanagementandroid.domain.repository.PhotoRepository
import com.example.itemmanagementandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onRouteEntered_appliesInitialCategoryFilter() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.onRouteEntered(initialCategoryId = "cat_camera")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("cat_camera", state.selectedCategoryId)
        assertEquals(listOf("item_camera"), state.items.map(Item::id))
    }

    @Test
    fun onRouteEntered_sameRouteAfterUserOverride_keepsUserSelection() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.onRouteEntered(initialCategoryId = "cat_camera")
        advanceUntilIdle()
        viewModel.setCategoryFilter(categoryId = null)
        advanceUntilIdle()

        viewModel.onRouteEntered(initialCategoryId = "cat_camera")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedCategoryId)
        assertEquals(setOf("item_camera", "item_audio"), state.items.map(Item::id).toSet())
    }

    @Test
    fun onRouteEntered_newRouteResetsPrefilterAfterUserOverride() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()

        viewModel.onRouteEntered(initialCategoryId = "cat_camera")
        advanceUntilIdle()
        viewModel.setCategoryFilter(categoryId = null)
        advanceUntilIdle()

        viewModel.onRouteEntered(initialCategoryId = "cat_audio")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("cat_audio", state.selectedCategoryId)
        assertEquals(listOf("item_audio"), state.items.map(Item::id))
    }

    private fun createViewModel(): ItemListViewModel {
        return ItemListViewModel(
            listCategoriesUseCase = ListCategoriesUseCase(FakeCategoryRepository()),
            listItemsUseCase = ListItemsUseCase(FakeItemRepository()),
            listItemPhotoCoversUseCase = ListItemPhotoCoversUseCase(FakePhotoRepository())
        )
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun list(includeArchived: Boolean): List<Category> {
            return listOf(
                category(id = "cat_camera", name = "Camera", sortOrder = 0),
                category(id = "cat_audio", name = "Audio", sortOrder = 1)
            )
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

    private class FakeItemRepository : ItemRepository {
        private val items = listOf(
            item(id = "item_camera", categoryId = "cat_camera", name = "Sony A"),
            item(id = "item_audio", categoryId = "cat_audio", name = "Bose QC")
        )

        override suspend fun list(query: ItemListQuery): List<Item> {
            return items.filter { item ->
                (query.categoryId == null || item.categoryId == query.categoryId) &&
                    (query.includeDeleted || item.deletedAt == null)
            }
        }

        override suspend fun get(itemId: String): Item? {
            return items.firstOrNull { item -> item.id == itemId }
        }

        override suspend fun create(draft: ItemDraft): Item {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun update(itemId: String, draft: ItemDraft): Item {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun softDelete(itemId: String): Item {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun restore(itemId: String): Item {
            throw UnsupportedOperationException("Not needed in this test.")
        }
    }

    private class FakePhotoRepository : PhotoRepository {
        override suspend fun listByItem(itemId: String): List<ItemPhoto> {
            return emptyList()
        }

        override suspend fun get(photoId: String): ItemPhoto? {
            return null
        }

        override suspend fun add(draft: ItemPhotoDraft): ItemPhoto {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun remove(photoId: String): Boolean {
            throw UnsupportedOperationException("Not needed in this test.")
        }

        override suspend fun listDeferredCleanupCandidates(): List<DeferredPhotoCleanupCandidate> {
            return emptyList()
        }

        override suspend fun listCoversByItemIds(itemIds: List<String>): List<ItemPhotoCover> {
            return emptyList()
        }
    }

    private companion object {
        fun category(id: String, name: String, sortOrder: Int): Category {
            return Category(
                id = id,
                name = name,
                sortOrder = sortOrder,
                isArchived = false,
                isSystemDefault = false,
                createdAt = "2026-03-11T00:00:00Z",
                updatedAt = "2026-03-11T00:00:00Z"
            )
        }

        fun item(id: String, categoryId: String, name: String): Item {
            return Item(
                id = id,
                categoryId = categoryId,
                name = name,
                purchaseDate = null,
                purchasePrice = null,
                purchaseCurrency = null,
                purchasePlace = null,
                description = null,
                tags = emptyList(),
                customAttributes = emptyMap(),
                createdAt = "2026-03-11T00:00:00Z",
                updatedAt = "2026-03-11T00:00:00Z",
                deletedAt = null
            )
        }
    }
}
