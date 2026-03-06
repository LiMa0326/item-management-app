package com.example.itemmanagementandroid.domain.model

data class ItemPhoto(
    val id: String,
    val itemId: String,
    val localUri: String,
    val thumbnailUri: String?,
    val contentType: String,
    val width: Int?,
    val height: Int?,
    val createdAt: String
)
