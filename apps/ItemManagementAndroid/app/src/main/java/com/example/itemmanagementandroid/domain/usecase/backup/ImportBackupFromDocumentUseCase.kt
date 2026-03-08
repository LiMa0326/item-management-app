package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.importing.BackupImportResult
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage
import java.io.File

class ImportBackupFromDocumentUseCase(
    private val importLocalBackupUseCase: ImportLocalBackupUseCase,
    private val backupDocumentStorage: BackupDocumentStorage
) {
    suspend operator fun invoke(documentUri: String): BackupImportResult {
        val tempFile = backupDocumentStorage.copyDocumentToTempFile(documentUri)
        return try {
            importLocalBackupUseCase(tempFile.absolutePath)
        } finally {
            deleteTempFileSafely(tempFile)
        }
    }

    private fun deleteTempFileSafely(file: File) {
        runCatching {
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
