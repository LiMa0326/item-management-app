package com.example.itemmanagementandroid.domain.usecase.backup

import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage
import com.example.itemmanagementandroid.backup.storage.BackupSharedExportResult
import java.io.File

class ExportBackupToSharedDirectoryUseCase(
    private val exportLocalBackupUseCase: ExportLocalBackupUseCase,
    private val backupDocumentStorage: BackupDocumentStorage
) {
    suspend operator fun invoke(
        exportMode: ExportMode,
        targetTreeUri: String
    ): BackupSharedExportResult {
        val localResult = exportLocalBackupUseCase(exportMode)
        val localFile = File(localResult.filePath)
        return try {
            val documentEntry = backupDocumentStorage.copyLocalFileToDocument(
                localFilePath = localResult.filePath,
                targetTreeUri = targetTreeUri,
                targetName = localFile.name
            )
            BackupSharedExportResult(
                document = documentEntry,
                exportMode = localResult.exportMode,
                createdAt = localResult.createdAt,
                stats = localResult.stats
            )
        } finally {
            runCatching {
                if (localFile.exists()) {
                    localFile.delete()
                }
            }
        }
    }
}
