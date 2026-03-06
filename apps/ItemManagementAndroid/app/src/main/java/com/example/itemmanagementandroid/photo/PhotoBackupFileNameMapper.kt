package com.example.itemmanagementandroid.photo

object PhotoBackupFileNameMapper {
    fun buildFullFileName(
        photoId: String,
        contentType: String?,
        localUri: String?
    ): String {
        val normalizedPhotoId = normalizePhotoId(photoId)
        val extension = resolveExtension(
            contentType = contentType,
            uri = localUri
        )
        return "$normalizedPhotoId.$extension"
    }

    fun buildThumbnailFileName(
        photoId: String,
        contentType: String?,
        thumbnailUri: String?
    ): String {
        val normalizedPhotoId = normalizePhotoId(photoId)
        val extension = resolveExtension(
            contentType = contentType,
            uri = thumbnailUri
        )
        return "${normalizedPhotoId}_thumb.$extension"
    }

    private fun resolveExtension(contentType: String?, uri: String?): String {
        val fromContentType = contentType
            ?.trim()
            ?.lowercase()
            ?.substringBefore(';')
            ?.let(::mapContentTypeToExtension)
        if (fromContentType != null) {
            return fromContentType
        }

        val fromUri = parseExtensionFromUri(uri)
        if (fromUri != null) {
            return fromUri
        }

        return "jpg"
    }

    private fun mapContentTypeToExtension(contentType: String): String? {
        return when (contentType) {
            "image/jpeg",
            "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> null
        }
    }

    private fun parseExtensionFromUri(uri: String?): String? {
        val normalizedUri = uri?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?: return null

        val fileName = normalizedUri.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', "")
            .lowercase()
            .trim()
        if (extension.isEmpty()) {
            return null
        }
        return extension
    }

    private fun normalizePhotoId(photoId: String): String {
        val normalized = photoId.trim()
        require(normalized.isNotEmpty()) {
            "photoId must not be blank."
        }
        return normalized
    }
}
