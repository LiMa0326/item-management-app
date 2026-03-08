package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.CategoryDao
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.domain.model.DefaultCategories
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CategoryRepositoryImplTest {
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: CategoryRepositoryImpl
    private var nextId: Int = 0

    @Before
    fun setUp() {
        fakeCategoryDao = FakeCategoryDao()
        repository = CategoryRepositoryImpl(
            categoryDao = fakeCategoryDao,
            clock = FIXED_CLOCK,
            idGenerator = { "custom_${nextId++}" }
        )
    }

    @Test
    fun ensureDefaultCategory_insertsElectronicsOnlyOnce() = runBlocking {
        val created = repository.ensureDefaultCategory()
        val createdAgain = repository.ensureDefaultCategory()

        val categories = repository.list(includeArchived = true)
        assertTrue(created)
        assertFalse(createdAgain)
        assertEquals(1, categories.size)
        assertEquals(DefaultCategories.ELECTRONICS_ID, categories.first().id)
        assertEquals(DefaultCategories.ELECTRONICS_NAME, categories.first().name)
        assertTrue(categories.first().isSystemDefault)
    }

    @Test
    fun createUpdateArchiveAndList_workAsExpected() = runBlocking {
        repository.ensureDefaultCategory()
        val created = repository.create("  鞋子  ")
        val updated = repository.update(created.id, "运动鞋")
        val archived = repository.setArchived(updated.id, archived = true)

        val active = repository.list()
        val all = repository.list(includeArchived = true)

        assertEquals("鞋子", created.name)
        assertEquals("运动鞋", updated.name)
        assertTrue(archived.isArchived)
        assertEquals(1, active.size)
        assertEquals(DefaultCategories.ELECTRONICS_ID, active.first().id)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == archived.id && it.isArchived })
    }

    @Test
    fun reorder_updatesSortOrderByRequestedIds() = runBlocking {
        val first = repository.create("类别 A")
        val second = repository.create("类别 B")
        val third = repository.create("类别 C")

        repository.reorder(
            categoryIds = listOf(
                third.id,
                first.id,
                second.id
            )
        )

        val reordered = repository.list(includeArchived = true)
        assertEquals(
            listOf(third.id, first.id, second.id),
            reordered.map { it.id }
        )
        assertEquals(listOf(0, 1, 2), reordered.map { it.sortOrder })
    }

    private class FakeCategoryDao : CategoryDao {
        private val categories: LinkedHashMap<String, CategoryEntity> = linkedMapOf()

        override suspend fun listAllOrdered(): List<CategoryEntity> {
            return categories.values
                .sortedWith(
                    compareBy<CategoryEntity> { it.sortOrder }
                        .thenBy { it.createdAt }
                )
        }

        override suspend fun listActiveOrdered(): List<CategoryEntity> {
            return listAllOrdered().filterNot(CategoryEntity::isArchived)
        }

        override suspend fun getById(categoryId: String): CategoryEntity? {
            return categories[categoryId]
        }

        override suspend fun countSystemDefault(): Int {
            return categories.values.count(CategoryEntity::isSystemDefault)
        }

        override suspend fun nextSortOrder(): Int {
            return (categories.values.maxOfOrNull(CategoryEntity::sortOrder) ?: -1) + 1
        }

        override suspend fun insert(category: CategoryEntity): Long {
            require(!categories.containsKey(category.id)) {
                "Category already exists: ${category.id}"
            }
            categories[category.id] = category
            return 1L
        }

        override suspend fun insertOrReplace(category: CategoryEntity): Long {
            categories[category.id] = category
            return 1L
        }

        override suspend fun update(category: CategoryEntity): Int {
            require(categories.containsKey(category.id)) {
                "Category does not exist: ${category.id}"
            }
            categories[category.id] = category
            return 1
        }

        override suspend fun updateSortOrder(
            categoryId: String,
            sortOrder: Int,
            updatedAt: String
        ): Int {
            val category = requireNotNull(categories[categoryId]) {
                "Category does not exist: $categoryId"
            }
            categories[categoryId] = category.copy(
                sortOrder = sortOrder,
                updatedAt = updatedAt
            )
            return 1
        }

        override suspend fun deleteAll(): Int {
            val count = categories.size
            categories.clear()
            return count
        }
    }

    companion object {
        private val FIXED_CLOCK: Clock =
            Clock.fixed(Instant.parse("2026-03-05T10:00:00Z"), ZoneOffset.UTC)
    }
}
