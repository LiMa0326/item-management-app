package com.example.itemmanagementandroid.domain.model

data class ItemPhotoDraft(
    val itemId: String,
    val localUri: String,
    val thumbnailUri: String? = null,
    val contentType: String,
    val width: Int? = null,
    val height: Int? = null
)
