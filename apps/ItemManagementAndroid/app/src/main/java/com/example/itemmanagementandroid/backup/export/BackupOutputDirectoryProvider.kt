package com.example.itemmanagementandroid.backup.export

import java.io.File

interface BackupOutputDirectoryProvider {
    fun getBackupDirectory(): File
}

