package com.example.itemmanagementandroid.backup.storage

data class BackupDocumentEntry(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: Long
)
