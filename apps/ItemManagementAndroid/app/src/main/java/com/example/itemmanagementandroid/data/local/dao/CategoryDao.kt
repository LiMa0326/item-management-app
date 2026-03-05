package com.example.itemmanagementandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort_order ASC, created_at ASC")
    suspend fun listAllOrdered(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY sort_order ASC, created_at ASC")
    suspend fun listActiveOrdered(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getById(categoryId: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE is_system_default = 1")
    suspend fun countSystemDefault(): Int

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories")
    suspend fun nextSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity): Int

    @Query("UPDATE categories SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :categoryId")
    suspend fun updateSortOrder(
        categoryId: String,
        sortOrder: Int,
        updatedAt: String
    ): Int
}
