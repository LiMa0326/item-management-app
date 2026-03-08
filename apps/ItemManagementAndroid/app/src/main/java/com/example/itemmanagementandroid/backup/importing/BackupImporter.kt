package com.example.itemmanagementandroid.backup.importing

interface BackupImporter {
    suspend fun importLocalBackup(backupFilePath: String): BackupImportResult
}
