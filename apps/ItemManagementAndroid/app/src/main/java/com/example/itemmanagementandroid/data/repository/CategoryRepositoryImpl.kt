package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.CategoryDao
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.DefaultCategories
import com.example.itemmanagementandroid.domain.repository.CategoryRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) : CategoryRepository {
    override suspend fun list(includeArchived: Boolean): List<Category> {
        val entities = if (includeArchived) {
            categoryDao.listAllOrdered()
        } else {
            categoryDao.listActiveOrdered()
        }
        return entities.map(CategoryEntity::toDomain)
    }

    override suspend fun create(name: String): Category {
        val normalizedName = normalizeName(name)
        val now = nowIsoString()
        val category = CategoryEntity(
            id = idGenerator(),
            name = normalizedName,
            sortOrder = categoryDao.nextSortOrder(),
            isArchived = false,
            isSystemDefault = false,
            createdAt = now,
            updatedAt = now
        )
        categoryDao.insert(category)
        return category.toDomain()
    }

    override suspend fun update(categoryId: String, name: String): Category {
        val normalizedName = normalizeName(name)
        val existing = requireCategory(categoryId)
        val updated = existing.copy(
            name = normalizedName,
            updatedAt = nowIsoString()
        )
        categoryDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun setArchived(categoryId: String, archived: Boolean): Category {
        val existing = requireCategory(categoryId)
        val updated = existing.copy(
            isArchived = archived,
            updatedAt = nowIsoString()
        )
        categoryDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun reorder(categoryIds: List<String>) {
        if (categoryIds.isEmpty()) {
            return
        }

        val current = categoryDao.listAllOrdered()
        if (current.isEmpty()) {
            return
        }

        val byId = current.associateBy { it.id }
        val requestedOrder = categoryIds.distinct()
        val missingIds = requestedOrder.filterNot(byId::containsKey)
        require(missingIds.isEmpty()) {
            "Unknown category ids: ${missingIds.joinToString(separator = ", ")}"
        }

        val finalOrder = buildList {
            addAll(requestedOrder)
            addAll(current.map { it.id }.filterNot(requestedOrder::contains))
        }

        val now = nowIsoString()
        finalOrder.forEachIndexed { index, categoryId ->
            val category = byId.getValue(categoryId)
            if (category.sortOrder != index) {
                categoryDao.updateSortOrder(
                    categoryId = categoryId,
                    sortOrder = index,
                    updatedAt = now
                )
            }
        }
    }

    override suspend fun ensureDefaultCategory(): Boolean {
        if (categoryDao.countSystemDefault() > 0) {
            return false
        }

        val now = nowIsoString()
        val category = CategoryEntity(
            id = DefaultCategories.ELECTRONICS_ID,
            name = DefaultCategories.ELECTRONICS_NAME,
            sortOrder = categoryDao.nextSortOrder(),
            isArchived = false,
            isSystemDefault = true,
            createdAt = now,
            updatedAt = now
        )
        categoryDao.insert(category)
        return true
    }

    private suspend fun requireCategory(categoryId: String): CategoryEntity {
        return requireNotNull(categoryDao.getById(categoryId)) {
            "Category not found: $categoryId"
        }
    }

    private fun nowIsoString(): String = Instant.now(clock).toString()

    private fun normalizeName(name: String): String {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) {
            "Category name must not be blank."
        }
        return normalized
    }
}

private fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        sortOrder = sortOrder,
        isArchived = isArchived,
        isSystemDefault = isSystemDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
