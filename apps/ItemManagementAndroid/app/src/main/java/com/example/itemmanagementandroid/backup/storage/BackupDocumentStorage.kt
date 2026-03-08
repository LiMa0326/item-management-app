package com.example.itemmanagementandroid.backup.storage

import java.io.File

interface BackupDocumentStorage {
    suspend fun setBackupDirectory(treeUri: String): BackupDirectoryInfo

    suspend fun getBackupDirectory(): BackupDirectoryInfo

    suspend fun listBackupFiles(treeUri: String): List<BackupDocumentEntry>

    suspend fun copyLocalFileToDocument(
        localFilePath: String,
        targetTreeUri: String,
        targetName: String
    ): BackupDocumentEntry

    suspend fun copyDocumentToTempFile(documentUri: String): File
}
