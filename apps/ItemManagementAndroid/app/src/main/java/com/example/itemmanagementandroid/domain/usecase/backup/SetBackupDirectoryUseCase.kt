package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.storage.BackupDirectoryInfo
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage

class SetBackupDirectoryUseCase(
    private val backupDocumentStorage: BackupDocumentStorage
) {
    suspend operator fun invoke(treeUri: String): BackupDirectoryInfo {
        return backupDocumentStorage.setBackupDirectory(treeUri)
    }
}
