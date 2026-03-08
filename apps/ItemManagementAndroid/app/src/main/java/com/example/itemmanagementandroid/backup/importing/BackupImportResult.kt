package com.example.itemmanagementandroid.backup.importing

data class BackupImportResult(
    val sourceFilePath: String,
    val importMode: BackupImportMode,
    val importedAt: String,
    val rollbackSnapshotPath: String,
    val stats: BackupImportStats,
    val warnings: List<BackupImportWarning>
)
