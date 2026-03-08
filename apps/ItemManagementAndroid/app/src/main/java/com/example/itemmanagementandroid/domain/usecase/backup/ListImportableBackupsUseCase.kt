package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.storage.BackupDocumentEntry
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage

class ListImportableBackupsUseCase(
    private val backupDocumentStorage: BackupDocumentStorage
) {
    suspend operator fun invoke(treeUri: String): List<BackupDocumentEntry> {
        return backupDocumentStorage.listBackupFiles(treeUri)
    }
}
