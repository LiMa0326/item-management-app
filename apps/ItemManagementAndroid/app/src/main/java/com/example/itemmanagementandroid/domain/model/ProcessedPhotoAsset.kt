package com.example.itemmanagementandroid.domain.model

data class ProcessedPhotoAsset(
    val sourceUri: String,
    val localUri: String,
    val thumbnailUri: String,
    val contentType: String,
    val width: Int,
    val height: Int
)
