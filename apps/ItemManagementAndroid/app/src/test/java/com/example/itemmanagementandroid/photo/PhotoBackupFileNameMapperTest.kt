package com.example.itemmanagementandroid.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoBackupFileNameMapperTest {
    @Test
    fun buildFullFileName_usesContentTypeMapping() {
        val fileName = PhotoBackupFileNameMapper.buildFullFileName(
            photoId = "photo_001",
            contentType = "image/png",
            localUri = null
        )

        assertEquals("photo_001.png", fileName)
    }

    @Test
    fun buildThumbnailFileName_usesContentTypeMapping() {
        val fileName = PhotoBackupFileNameMapper.buildThumbnailFileName(
            photoId = "photo_002",
            contentType = "image/jpeg",
            thumbnailUri = null
        )

        assertEquals("photo_002_thumb.jpg", fileName)
    }

    @Test
    fun resolveExtension_fallsBackToUriExtensionWhenContentTypeUnknown() {
        val fileName = PhotoBackupFileNameMapper.buildFullFileName(
            photoId = "photo_003",
            contentType = "application/octet-stream",
            localUri = "content://local/path/source_image.WeBp?query=1"
        )

        assertEquals("photo_003.webp", fileName)
    }

    @Test
    fun resolveExtension_fallsBackToJpgWhenBothUnavailable() {
        val fileName = PhotoBackupFileNameMapper.buildThumbnailFileName(
            photoId = "photo_004",
            contentType = null,
            thumbnailUri = "content://local/path/no_extension"
        )

        assertEquals("photo_004_thumb.jpg", fileName)
    }
}
