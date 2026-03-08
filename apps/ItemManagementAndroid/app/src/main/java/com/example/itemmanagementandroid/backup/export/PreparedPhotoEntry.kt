package com.example.itemmanagementandroid.backup.export

import java.io.File

data class PreparedPhotoEntry(
    val photoId: String,
    val itemId: String,
    val fileName: String,
    val kind: String,
    val contentType: String,
    val width: Int?,
    val height: Int?,
    val createdAt: String,
    val sourceFile: File
)

