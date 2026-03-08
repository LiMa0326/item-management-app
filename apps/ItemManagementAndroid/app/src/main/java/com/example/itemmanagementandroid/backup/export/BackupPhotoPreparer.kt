package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.photo.PhotoBackupFileNameMapper
import java.io.File
import java.net.URI

class BackupPhotoPreparer {
    fun prepare(
        exportMode: ExportMode,
        itemPhotos: List<ItemPhoto>
    ): List<PreparedPhotoEntry> {
        if (exportMode == ExportMode.METADATA_ONLY) {
            return emptyList()
        }

        return itemPhotos.map { photo ->
            prepareSingle(
                exportMode = exportMode,
                photo = photo
            )
        }
    }

    private fun prepareSingle(
        exportMode: ExportMode,
        photo: ItemPhoto
    ): PreparedPhotoEntry {
        val sourceUri = when (exportMode) {
            ExportMode.METADATA_ONLY -> null
            ExportMode.THUMBNAILS -> photo.thumbnailUri
            ExportMode.FULL -> photo.localUri
        }

        val sourceFile = resolveFile(sourceUri)
            ?: throw BackupExportException.MissingPhotoFile(
                photoId = photo.id,
                sourceUri = sourceUri,
                exportMode = exportMode
            )

        if (!sourceFile.exists() || !sourceFile.isFile || !sourceFile.canRead()) {
            throw BackupExportException.MissingPhotoFile(
                photoId = photo.id,
                sourceUri = sourceUri,
                exportMode = exportMode
            )
        }

        val fileName = when (exportMode) {
            ExportMode.METADATA_ONLY -> error("metadata_only should not prepare photos")
            ExportMode.THUMBNAILS -> PhotoBackupFileNameMapper.buildThumbnailFileName(
                photoId = photo.id,
                contentType = photo.contentType,
                thumbnailUri = sourceUri
            )
            ExportMode.FULL -> PhotoBackupFileNameMapper.buildFullFileName(
                photoId = photo.id,
                contentType = photo.contentType,
                localUri = sourceUri
            )
        }

        return PreparedPhotoEntry(
            photoId = photo.id,
            itemId = photo.itemId,
            fileName = fileName,
            kind = if (exportMode == ExportMode.THUMBNAILS) "thumbnail" else "full",
            contentType = photo.contentType,
            width = photo.width,
            height = photo.height,
            createdAt = photo.createdAt,
            sourceFile = sourceFile
        )
    }

    private fun resolveFile(sourceUri: String?): File? {
        val normalized = sourceUri?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return null
        }

        val parsedUri = runCatching { URI(normalized) }.getOrNull()
        if (parsedUri == null) {
            return File(normalized)
        }

        val scheme = parsedUri.scheme.orEmpty().lowercase()
        return when (scheme) {
            "" -> File(normalized)
            "file" -> runCatching { File(parsedUri) }.getOrNull()
            else -> null
        }
    }
}

