package com.example.itemmanagementandroid.backup.storage

import com.example.itemmanagementandroid.backup.export.BackupStats
import com.example.itemmanagementandroid.backup.export.ExportMode

data class BackupSharedExportResult(
    val document: BackupDocumentEntry,
    val exportMode: ExportMode,
    val createdAt: String,
    val stats: BackupStats
)
