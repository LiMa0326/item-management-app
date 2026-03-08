package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.export.BackupService
import com.example.itemmanagementandroid.backup.importing.BackupImportResult

class ImportLocalBackupUseCase(
    private val backupService: BackupService
) {
    suspend operator fun invoke(backupFilePath: String): BackupImportResult {
        return backupService.importLocalBackup(backupFilePath)
    }
}
