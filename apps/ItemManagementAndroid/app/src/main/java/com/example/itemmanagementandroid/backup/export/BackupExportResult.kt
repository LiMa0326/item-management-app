package com.example.itemmanagementandroid.backup.export

data class BackupExportResult(
    val filePath: String,
    val fileSizeBytes: Long,
    val exportMode: ExportMode,
    val createdAt: String,
    val stats: BackupStats
)

