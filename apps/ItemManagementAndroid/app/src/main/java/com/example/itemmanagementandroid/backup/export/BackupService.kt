package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.backup.importing.BackupImportResult

interface BackupService {
    suspend fun exportLocalBackup(exportMode: ExportMode): BackupExportResult
    suspend fun importLocalBackup(backupFilePath: String): BackupImportResult
}

