package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.export.BackupExportResult
import com.example.itemmanagementandroid.backup.export.BackupService
import com.example.itemmanagementandroid.backup.export.ExportMode

class ExportLocalBackupUseCase(
    private val backupService: BackupService
) {
    suspend operator fun invoke(exportMode: ExportMode): BackupExportResult {
        return backupService.exportLocalBackup(exportMode)
    }
}

