package com.example.itemmanagementandroid.data.local.dao.model

import androidx.room.ColumnInfo

data class ItemPhotoCoverRow(
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "thumbnail_uri")
    val thumbnailUri: String?,
    @ColumnInfo(name = "local_uri")
    val localUri: String?
)
