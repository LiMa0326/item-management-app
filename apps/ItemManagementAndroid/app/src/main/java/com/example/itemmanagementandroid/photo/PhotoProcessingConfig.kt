package com.example.itemmanagementandroid.photo

data class PhotoProcessingConfig(
    val thumbnailMaxLongSidePx: Int = 1280,
    val thumbnailJpegQuality: Int = 85,
    val fullImageJpegQuality: Int = 92
)
