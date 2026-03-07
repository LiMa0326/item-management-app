package com.example.itemmanagementandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.itemmanagementandroid.data.local.entity.ItemEntity

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY updated_at DESC, created_at DESC")
    suspend fun listAllOrdered(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE deleted_at IS NULL ORDER BY updated_at DESC, created_at DESC")
    suspend fun listActiveOrdered(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getById(itemId: String): ItemEntity?

    @Query(
        """
        SELECT COUNT(1) FROM items
        WHERE deleted_at IS NULL
          AND lower(trim(name)) = lower(trim(:name))
        """
    )
    suspend fun countActiveByNormalizedName(name: String): Int

    @Query(
        """
        SELECT COUNT(1) FROM items
        WHERE deleted_at IS NULL
          AND id != :excludeItemId
          AND lower(trim(name)) = lower(trim(:name))
        """
    )
    suspend fun countActiveByNormalizedNameExcludingId(name: String, excludeItemId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity): Int
}
