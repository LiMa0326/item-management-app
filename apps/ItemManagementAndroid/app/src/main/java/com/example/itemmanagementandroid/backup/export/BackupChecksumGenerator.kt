package com.example.itemmanagementandroid.backup.export

interface BackupChecksumGenerator {
    fun generateChecksumsJson(
        manifestJson: ByteArray,
        dataJson: ByteArray,
        photoEntries: List<PreparedPhotoEntry>
    ): ByteArray?
}

