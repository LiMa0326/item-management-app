package com.example.itemmanagementandroid.backup.export

interface BackupService {
    suspend fun exportLocalBackup(exportMode: ExportMode): BackupExportResult
}

