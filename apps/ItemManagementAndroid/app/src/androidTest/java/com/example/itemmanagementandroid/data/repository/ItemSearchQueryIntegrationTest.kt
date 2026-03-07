package com.example.itemmanagementandroid.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.itemmanagementandroid.data.local.db.ItemManagementDatabase
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemSearchQueryIntegrationTest {
    private lateinit var database: ItemManagementDatabase
    private lateinit var repository: ItemRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ItemManagementDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ItemRepositoryImpl(database.itemDao())
        seedCategories()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun search_matchesFieldsAndTagWholeWord_andSupportsCategoryFilter() = runBlocking {
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Sony Camera"
            )
        )
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Lens",
                description = "Best for SONY users"
            )
        )
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_b",
                name = "Store Entry",
                purchasePlace = "Sony Official Store"
            )
        )
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Tag Exact",
                tags = listOf("Audio")
            )
        )
        repository.create(
            draft = ItemDraft(
                categoryId = "cat_a",
                name = "Tag Substring",
                tags = listOf("Audiophile")
            )
        )

        val sonyResult = repository.list(
            query = ItemListQuery(
                includeDeleted = false,
                searchKeyword = "sony",
                sortOption = ItemListSortOption.RECENTLY_UPDATED
            )
        )
        assertEquals(3, sonyResult.size)

        val sonyInCategoryA = repository.list(
            query = ItemListQuery(
                includeDeleted = false,
                categoryId = "cat_a",
                searchKeyword = "sony",
                sortOption = ItemListSortOption.RECENTLY_UPDATED
            )
        )
        assertEquals(2, sonyInCategoryA.size)
        assertTrue(sonyInCategoryA.all { item -> item.categoryId == "cat_a" })

        val audioResult = repository.list(
            query = ItemListQuery(
                includeDeleted = false,
                categoryId = "cat_a",
                searchKeyword = "audio",
                sortOption = ItemListSortOption.RECENTLY_UPDATED
            )
        )
        assertEquals(listOf("Tag Exact"), audioResult.map { item -> item.name })
    }

    private suspend fun seedCategories() {
        val now = "2026-03-07T00:00:00Z"
        database.categoryDao().insert(
            CategoryEntity(
                id = "cat_a",
                name = "Category A",
                sortOrder = 0,
                isArchived = false,
                isSystemDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
        database.categoryDao().insert(
            CategoryEntity(
                id = "cat_b",
                name = "Category B",
                sortOrder = 1,
                isArchived = false,
                isSystemDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
