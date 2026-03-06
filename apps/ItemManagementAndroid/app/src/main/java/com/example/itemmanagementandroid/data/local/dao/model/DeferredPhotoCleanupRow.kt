package com.example.itemmanagementandroid.data.local.dao.model

import androidx.room.ColumnInfo

data class DeferredPhotoCleanupRow(
    @ColumnInfo(name = "photo_id")
    val photoId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "local_uri")
    val localUri: String,
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String?,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    @ColumnInfo(name = "item_deleted_at")
    val itemDeletedAt: String
)
