package com.example.itemmanagementandroid.backup.importing

import java.io.File

data class BackupImportManifest(
    val formatVersion: String,
    val exportMode: String?
)

data class BackupImportData(
    val schemaVersion: String,
    val categories: List<BackupImportCategory>,
    val items: List<BackupImportItem>,
    val itemPhotos: List<BackupImportItemPhoto>
)

data class BackupImportCategory(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val isArchived: Boolean,
    val isSystemDefault: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class BackupImportItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val purchaseCurrency: String?,
    val purchasePlace: String?,
    val description: String?,
    val tags: List<String>,
    val customAttributes: Map<String, Any>,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)

data class BackupImportItemPhoto(
    val id: String,
    val itemId: String,
    val contentType: String,
    val createdAt: String,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val kind: String?
)

data class BackupArchivePayload(
    val manifestJson: ByteArray,
    val dataJson: ByteArray,
    val photoFilesByName: Map<String, File>
)

data class ParsedBackupPackage(
    val manifest: BackupImportManifest,
    val data: BackupImportData,
    val warnings: List<BackupImportWarning>
)
