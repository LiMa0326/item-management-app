package com.example.itemmanagementandroid.backup.importing

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.itemmanagementandroid.data.local.db.ItemManagementDatabase
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity
import com.example.itemmanagementandroid.data.repository.json.ItemJsonCodec
import com.example.itemmanagementandroid.data.repository.json.OrgJsonItemJsonCodec
import com.example.itemmanagementandroid.photo.AppPrivatePhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

class LocalBackupImporter(
    private val appContext: Context,
    private val database: ItemManagementDatabase,
    private val rollbackSnapshotCreator: suspend () -> String,
    private val archiveReader: BackupArchiveReader = BackupArchiveReader(),
    private val jsonParser: BackupJsonParser = BackupJsonParser(),
    private val itemJsonCodec: ItemJsonCodec = OrgJsonItemJsonCodec(),
    private val clock: Clock = Clock.systemUTC()
) : BackupImporter {
    private val photoStorage: AppPrivatePhotoStorage = AppPrivatePhotoStorage(appContext)

    override suspend fun importLocalBackup(backupFilePath: String): BackupImportResult {
        return withContext(Dispatchers.IO) {
            val normalizedPath = backupFilePath.trim()
            if (normalizedPath.isEmpty()) {
                throw BackupImportException.InvalidParameter("Backup file path must not be blank.")
            }

            val backupFile = File(normalizedPath)
            val extractionDirectory = File(
                appContext.cacheDir,
                "backup_import_${System.currentTimeMillis()}_${UUID.randomUUID()}"
            )

            try {
                val archivePayload = archiveReader.read(
                    zipFile = backupFile,
                    extractionDirectory = extractionDirectory
                )
                val parsedPackage = jsonParser.parse(archivePayload)
                val warnings = parsedPackage.warnings.toMutableList()
                collectVersionWarnings(parsedPackage, warnings)

                val rollbackSnapshotPath = runCatching {
                    rollbackSnapshotCreator()
                }.getOrElse { throwable ->
                    throw BackupImportException.RollbackSnapshotFailed(
                        message = "Failed to create rollback snapshot before import.",
                        cause = throwable as? Exception ?: Exception(throwable)
                    )
                }

                val existingPhotoFiles = collectCurrentPhotoFiles()
                val photoPlans = buildPhotoImportPlans(
                    parsedPackage = parsedPackage,
                    photoFilesByName = archivePayload.photoFilesByName,
                    warnings = warnings
                )
                val replaceResult = replaceAll(
                    backupData = parsedPackage.data,
                    photoPlans = photoPlans,
                    warnings = warnings
                )

                cleanupObsoletePhotos(
                    existingPhotoFiles = existingPhotoFiles,
                    importedPhotoFiles = replaceResult.importedPhotoFiles
                )

                BackupImportResult(
                    sourceFilePath = backupFile.absolutePath,
                    importMode = BackupImportMode.REPLACE_ALL,
                    importedAt = nowIsoString(),
                    rollbackSnapshotPath = rollbackSnapshotPath,
                    stats = BackupImportStats(
                        categories = replaceResult.categories,
                        items = replaceResult.items,
                        photos = replaceResult.photos
                    ),
                    warnings = warnings
                )
            } catch (exception: BackupImportException) {
                throw exception
            } catch (exception: IOException) {
                throw BackupImportException.IoFailure(
                    message = "Backup import failed due to I/O error.",
                    cause = exception
                )
            } finally {
                extractionDirectory.deleteRecursively()
            }
        }
    }

    private suspend fun collectCurrentPhotoFiles(): Set<File> {
        val files = linkedSetOf<File>()
        database.itemPhotoDao().listAll().forEach { photo ->
            resolveFileFromUri(photo.localUri)?.let(files::add)
            resolveFileFromUri(photo.thumbnailUri)?.let(files::add)
        }
        collectManagedPhotoFiles().forEach(files::add)
        return files
    }

    private fun collectManagedPhotoFiles(): Set<File> {
        val files = linkedSetOf<File>()
        managedPhotoDirectories().forEach { directory ->
            if (directory.exists() && directory.isDirectory) {
                directory.walkTopDown()
                    .filter(File::isFile)
                    .forEach(files::add)
            }
        }
        return files
    }

    private fun buildPhotoImportPlans(
        parsedPackage: ParsedBackupPackage,
        photoFilesByName: Map<String, File>,
        warnings: MutableList<BackupImportWarning>
    ): List<PhotoImportPlan> {
        val byPhotoId = linkedMapOf<String, MutablePhotoImportPlan>()
        parsedPackage.data.itemPhotos.forEachIndexed { index, photo ->
            val fileName = photo.fileName
            if (fileName == null) {
                warnings += BackupImportWarning(
                    code = "ITEM_PHOTO_SKIPPED",
                    message = "itemPhotos[$index] does not contain fileName and was skipped."
                )
                return@forEachIndexed
            }

            val sourceFile = photoFilesByName[fileName]
                ?: throw BackupImportException.InvalidPackage(
                    "itemPhotos[$index] references missing file photos/$fileName."
                )
            val kind = normalizePhotoKind(
                rawKind = photo.kind,
                exportMode = parsedPackage.manifest.exportMode
            )

            val accumulator = byPhotoId.getOrPut(photo.id) {
                MutablePhotoImportPlan(
                    photoId = photo.id,
                    itemId = photo.itemId,
                    contentType = photo.contentType,
                    width = photo.width,
                    height = photo.height,
                    createdAt = photo.createdAt
                )
            }
            accumulator.itemId = photo.itemId
            accumulator.contentType = photo.contentType
            accumulator.width = photo.width ?: accumulator.width
            accumulator.height = photo.height ?: accumulator.height
            accumulator.createdAt = photo.createdAt

            when (kind) {
                PHOTO_KIND_THUMBNAIL -> accumulator.thumbnailSourceFile = sourceFile
                else -> accumulator.fullSourceFile = sourceFile
            }
        }

        return byPhotoId.values.mapNotNull { plan ->
            if (plan.fullSourceFile == null && plan.thumbnailSourceFile == null) {
                null
            } else {
                PhotoImportPlan(
                    photoId = plan.photoId,
                    itemId = plan.itemId,
                    fullSourceFile = plan.fullSourceFile,
                    thumbnailSourceFile = plan.thumbnailSourceFile,
                    contentType = plan.contentType,
                    width = plan.width,
                    height = plan.height,
                    createdAt = plan.createdAt
                )
            }
        }
    }

    private suspend fun replaceAll(
        backupData: BackupImportData,
        photoPlans: List<PhotoImportPlan>,
        warnings: MutableList<BackupImportWarning>
    ): ReplaceAllResult {
        val distinctCategories = deduplicateById(
            values = backupData.categories,
            idSelector = BackupImportCategory::id,
            warningCode = "CATEGORY_DUPLICATE",
            warningMessagePrefix = "Duplicate category id skipped"
        ) { warning -> warnings += warning }

        val categoryIds = distinctCategories.mapTo(linkedSetOf(), BackupImportCategory::id)

        val distinctItems = deduplicateById(
            values = backupData.items,
            idSelector = BackupImportItem::id,
            warningCode = "ITEM_DUPLICATE",
            warningMessagePrefix = "Duplicate item id skipped"
        ) { warning -> warnings += warning }
            .filter { item ->
                val exists = categoryIds.contains(item.categoryId)
                if (!exists) {
                    warnings += BackupImportWarning(
                        code = "ITEM_SKIPPED",
                        message = "Item ${item.id} references unknown category ${item.categoryId} and was skipped."
                    )
                }
                exists
            }

        val itemIds = distinctItems.mapTo(linkedSetOf(), BackupImportItem::id)
        val distinctPhotos = deduplicateById(
            values = photoPlans,
            idSelector = PhotoImportPlan::photoId,
            warningCode = "ITEM_PHOTO_DUPLICATE",
            warningMessagePrefix = "Duplicate itemPhoto id skipped"
        ) { warning -> warnings += warning }
            .filter { photo ->
                val exists = itemIds.contains(photo.itemId)
                if (!exists) {
                    warnings += BackupImportWarning(
                        code = "ITEM_PHOTO_SKIPPED",
                        message = "ItemPhoto ${photo.photoId} references unknown item ${photo.itemId} and was skipped."
                    )
                }
                exists
            }

        val createdPhotoFiles = mutableListOf<File>()
        val importedPhotoFiles = linkedSetOf<File>()

        return try {
            database.withTransaction {
                database.itemPhotoDao().deleteAll()
                database.itemDao().deleteAll()
                database.categoryDao().deleteAll()

                distinctCategories.forEach { category ->
                    database.categoryDao().insertOrReplace(category.toEntity())
                }
                distinctItems.forEach { item ->
                    database.itemDao().insertOrReplace(item.toEntity(itemJsonCodec))
                }
                distinctPhotos.forEach { photoPlan ->
                    val restored = restorePhotoFiles(
                        photoPlan = photoPlan,
                        createdFiles = createdPhotoFiles
                    )
                    importedPhotoFiles += restored.localFile
                    restored.thumbnailFile?.let(importedPhotoFiles::add)

                    database.itemPhotoDao().insertOrReplace(
                        ItemPhotoEntity(
                            id = photoPlan.photoId,
                            itemId = photoPlan.itemId,
                            localUri = restored.localFile.toURI().toString(),
                            thumbnailUri = restored.thumbnailFile?.toURI()?.toString(),
                            contentType = photoPlan.contentType,
                            width = photoPlan.width,
                            height = photoPlan.height,
                            createdAt = photoPlan.createdAt
                        )
                    )
                }
            }

            ReplaceAllResult(
                categories = distinctCategories.size,
                items = distinctItems.size,
                photos = distinctPhotos.size,
                importedPhotoFiles = importedPhotoFiles
            )
        } catch (exception: Exception) {
            createdPhotoFiles.forEach(::deleteSilently)
            throw exception
        }
    }

    private fun restorePhotoFiles(
        photoPlan: PhotoImportPlan,
        createdFiles: MutableList<File>
    ): RestoredPhotoFiles {
        val fullFile = photoPlan.fullSourceFile?.let { sourceFile ->
            copyToManagedStorage(
                sourceFile = sourceFile,
                createTargetFile = { photoStorage.createFullImageFile() },
                createdFiles = createdFiles
            )
        }
        val thumbnailFile = photoPlan.thumbnailSourceFile?.let { sourceFile ->
            copyToManagedStorage(
                sourceFile = sourceFile,
                createTargetFile = { photoStorage.createThumbnailFile() },
                createdFiles = createdFiles
            )
        }
        val localFile = fullFile ?: thumbnailFile
            ?: throw BackupImportException.InvalidPackage(
                "itemPhotos entry ${photoPlan.photoId} does not contain importable photo files."
            )
        return RestoredPhotoFiles(
            localFile = localFile,
            thumbnailFile = thumbnailFile
        )
    }

    private fun copyToManagedStorage(
        sourceFile: File,
        createTargetFile: () -> File,
        createdFiles: MutableList<File>
    ): File {
        if (!sourceFile.exists() || !sourceFile.isFile || !sourceFile.canRead()) {
            throw BackupImportException.InvalidPackage(
                "Backup photo source file is missing: ${sourceFile.absolutePath}"
            )
        }
        val targetFile = createTargetFile()
        targetFile.parentFile?.mkdirs()
        sourceFile.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        createdFiles += targetFile
        return targetFile
    }

    private fun cleanupObsoletePhotos(
        existingPhotoFiles: Set<File>,
        importedPhotoFiles: Set<File>
    ) {
        val importedPaths = importedPhotoFiles.mapTo(hashSetOf()) { file ->
            file.absoluteFile.normalize().path
        }

        existingPhotoFiles.forEach { file ->
            val normalized = file.absoluteFile.normalize().path
            if (!importedPaths.contains(normalized)) {
                deleteSilently(file)
            }
        }

        managedPhotoDirectories().forEach { directory ->
            if (!directory.exists() || !directory.isDirectory) {
                return@forEach
            }
            directory.walkTopDown()
                .filter(File::isFile)
                .forEach { file ->
                    val normalized = file.absoluteFile.normalize().path
                    if (!importedPaths.contains(normalized)) {
                        deleteSilently(file)
                    }
                }
        }
    }

    private fun collectVersionWarnings(
        parsedPackage: ParsedBackupPackage,
        warnings: MutableList<BackupImportWarning>
    ) {
        if (isVersionHigher(parsedPackage.manifest.formatVersion, SUPPORTED_FORMAT_VERSION)) {
            warnings += BackupImportWarning(
                code = "FORMAT_VERSION_HIGHER",
                message = "Backup formatVersion ${parsedPackage.manifest.formatVersion} is newer than supported $SUPPORTED_FORMAT_VERSION. Imported known fields only."
            )
        }
        if (isVersionHigher(parsedPackage.data.schemaVersion, SUPPORTED_SCHEMA_VERSION)) {
            warnings += BackupImportWarning(
                code = "SCHEMA_VERSION_HIGHER",
                message = "Backup schemaVersion ${parsedPackage.data.schemaVersion} is newer than supported $SUPPORTED_SCHEMA_VERSION. Imported known fields only."
            )
        }
    }

    private fun isVersionHigher(candidate: String, supported: String): Boolean {
        val candidateParts = parseVersion(candidate)
        val supportedParts = parseVersion(supported)
        val maxParts = maxOf(candidateParts.size, supportedParts.size)
        for (index in 0 until maxParts) {
            val left = candidateParts.getOrElse(index) { 0 }
            val right = supportedParts.getOrElse(index) { 0 }
            if (left > right) {
                return true
            }
            if (left < right) {
                return false
            }
        }
        return false
    }

    private fun parseVersion(rawVersion: String): List<Int> {
        return rawVersion
            .trim()
            .split('.')
            .map { segment ->
                segment.trim().takeIf(String::isNotEmpty)?.toIntOrNull() ?: 0
            }
    }

    private fun normalizePhotoKind(rawKind: String?, exportMode: String?): String {
        val normalizedKind = rawKind
            ?.trim()
            ?.lowercase(Locale.ROOT)
        if (normalizedKind == PHOTO_KIND_FULL || normalizedKind == PHOTO_KIND_THUMBNAIL) {
            return normalizedKind
        }
        val normalizedMode = exportMode
            ?.trim()
            ?.lowercase(Locale.ROOT)
        return if (normalizedMode == EXPORT_MODE_THUMBNAILS) {
            PHOTO_KIND_THUMBNAIL
        } else {
            PHOTO_KIND_FULL
        }
    }

    private fun managedPhotoDirectories(): List<File> {
        return listOf(
            File(appContext.filesDir, "photos/full"),
            File(appContext.filesDir, "photos/thumbs")
        )
    }

    private fun resolveFileFromUri(uri: String?): File? {
        val normalized = uri?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return null
        }
        val parsed = Uri.parse(normalized)
        return when (parsed.scheme) {
            "file" -> parsed.path?.let(::File)
            null, "" -> File(normalized)
            else -> null
        }
    }

    private fun deleteSilently(file: File) {
        runCatching {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun nowIsoString(): String = Instant.now(clock).toString()

    private fun BackupImportCategory.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            sortOrder = sortOrder,
            isArchived = isArchived,
            isSystemDefault = isSystemDefault,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun BackupImportItem.toEntity(jsonCodec: ItemJsonCodec): ItemEntity {
        return ItemEntity(
            id = id,
            categoryId = categoryId,
            name = name,
            purchaseDate = purchaseDate,
            purchasePrice = purchasePrice,
            purchaseCurrency = purchaseCurrency,
            purchasePlace = purchasePlace,
            description = description,
            tagsJson = jsonCodec.encodeTags(tags),
            customAttributesJson = jsonCodec.encodeCustomAttributes(customAttributes),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }

    private fun <T> deduplicateById(
        values: List<T>,
        idSelector: (T) -> String,
        warningCode: String,
        warningMessagePrefix: String,
        onWarning: (BackupImportWarning) -> Unit
    ): List<T> {
        val distinct = linkedMapOf<String, T>()
        values.forEach { value ->
            val id = idSelector(value)
            if (distinct.containsKey(id)) {
                onWarning(
                    BackupImportWarning(
                        code = warningCode,
                        message = "$warningMessagePrefix: $id."
                    )
                )
            }
            distinct[id] = value
        }
        return distinct.values.toList()
    }

    private companion object {
        const val SUPPORTED_FORMAT_VERSION = "1.0"
        const val SUPPORTED_SCHEMA_VERSION = "1.0"
        const val PHOTO_KIND_FULL = "full"
        const val PHOTO_KIND_THUMBNAIL = "thumbnail"
        const val EXPORT_MODE_THUMBNAILS = "thumbnails"
    }
}

private data class MutablePhotoImportPlan(
    val photoId: String,
    var itemId: String,
    var contentType: String,
    var width: Int?,
    var height: Int?,
    var createdAt: String,
    var fullSourceFile: File? = null,
    var thumbnailSourceFile: File? = null
)

private data class PhotoImportPlan(
    val photoId: String,
    val itemId: String,
    val fullSourceFile: File?,
    val thumbnailSourceFile: File?,
    val contentType: String,
    val width: Int?,
    val height: Int?,
    val createdAt: String
)

private data class ReplaceAllResult(
    val categories: Int,
    val items: Int,
    val photos: Int,
    val importedPhotoFiles: Set<File>
)

private data class RestoredPhotoFiles(
    val localFile: File,
    val thumbnailFile: File?
)
