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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity): Int
}
