package com.example.itemmanagementandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.itemmanagementandroid.data.local.dao.model.DeferredPhotoCleanupRow
import com.example.itemmanagementandroid.data.local.dao.model.ItemPhotoCoverRow
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity

@Dao
interface ItemPhotoDao {
    @Query("SELECT * FROM item_photos WHERE item_id = :itemId ORDER BY created_at ASC")
    suspend fun listByItemOrdered(itemId: String): List<ItemPhotoEntity>

    @Query("SELECT * FROM item_photos WHERE id = :photoId LIMIT 1")
    suspend fun getById(photoId: String): ItemPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(photo: ItemPhotoEntity): Long

    @Query("DELETE FROM item_photos WHERE id = :photoId")
    suspend fun deleteById(photoId: String): Int

    @Query(
        """
        SELECT p.item_id AS item_id, p.thumbnail_uri AS thumbnail_uri, p.local_uri AS local_uri
        FROM item_photos AS p
        INNER JOIN (
            SELECT item_id, MIN(created_at) AS min_created_at
            FROM item_photos
            WHERE item_id IN (:itemIds)
            GROUP BY item_id
        ) AS first_photo
        ON p.item_id = first_photo.item_id
           AND p.created_at = first_photo.min_created_at
        WHERE p.item_id IN (:itemIds)
        """
    )
    suspend fun listCoversByItemIds(itemIds: List<String>): List<ItemPhotoCoverRow>

    @Query(
        """
        SELECT
            p.id AS photo_id,
            p.item_id AS item_id,
            p.local_uri AS local_uri,
            p.thumbnail_uri AS thumbnail_uri,
            p.content_type AS content_type,
            i.deleted_at AS item_deleted_at
        FROM item_photos AS p
        INNER JOIN items AS i ON i.id = p.item_id
        WHERE i.deleted_at IS NOT NULL
        ORDER BY i.deleted_at ASC, p.created_at ASC
        """
    )
    suspend fun listDeferredCleanupRows(): List<DeferredPhotoCleanupRow>
}
