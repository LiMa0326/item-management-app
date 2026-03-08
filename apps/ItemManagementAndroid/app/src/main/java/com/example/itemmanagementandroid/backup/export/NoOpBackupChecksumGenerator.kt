package com.example.itemmanagementandroid.backup.export

object NoOpBackupChecksumGenerator : BackupChecksumGenerator {
    override fun generateChecksumsJson(
        manifestJson: ByteArray,
        dataJson: ByteArray,
        photoEntries: List<PreparedPhotoEntry>
    ): ByteArray? = null
}

