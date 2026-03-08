package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.storage.BackupDirectoryInfo
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage

class GetBackupDirectoryUseCase(
    private val backupDocumentStorage: BackupDocumentStorage
) {
    suspend operator fun invoke(): BackupDirectoryInfo {
        return backupDocumentStorage.getBackupDirectory()
    }
}
