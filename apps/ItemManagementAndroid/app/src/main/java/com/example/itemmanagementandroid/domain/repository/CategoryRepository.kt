package com.example.itemmanagementandroid.domain.repository

import com.example.itemmanagementandroid.domain.model.Category

interface CategoryRepository {
    suspend fun list(includeArchived: Boolean = false): List<Category>
    suspend fun create(name: String): Category
    suspend fun update(categoryId: String, name: String): Category
    suspend fun setArchived(categoryId: String, archived: Boolean): Category
    suspend fun reorder(categoryIds: List<String>)
    suspend fun ensureDefaultCategory(): Boolean
}
