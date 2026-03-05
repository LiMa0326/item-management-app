package com.example.itemmanagementandroid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_photos",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"]
        )
    ],
    indices = [
        Index(value = ["item_id"], name = "idx_item_photos_item_id")
    ]
)
data class ItemPhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "local_uri")
    val localUri: String,
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String? = null,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    @ColumnInfo(name = "width")
    val width: Int? = null,
    @ColumnInfo(name = "height")
    val height: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
