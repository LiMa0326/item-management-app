package com.example.itemmanagementandroid.backup.export

import android.content.Context
import java.io.File

class AndroidBackupOutputDirectoryProvider(
    private val appContext: Context
) : BackupOutputDirectoryProvider {
    override fun getBackupDirectory(): File {
        val externalBackupDir = appContext.getExternalFilesDir(BACKUP_DIRECTORY_NAME)
        val backupDir = externalBackupDir ?: File(appContext.filesDir, BACKUP_DIRECTORY_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        require(backupDir.exists() && backupDir.isDirectory) {
            "Failed to create backup directory: ${backupDir.absolutePath}"
        }
        return backupDir
    }

    private companion object {
        const val BACKUP_DIRECTORY_NAME = "backups"
    }
}

