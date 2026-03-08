package com.example.itemmanagementandroid.photo

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class AppPrivatePhotoStorage(
    private val appContext: Context
) {
    fun createFullImageFile(): File {
        return createFile(
            directory = File(appContext.filesDir, FULL_DIRECTORY),
            prefix = "full"
        )
    }

    fun createThumbnailFile(): File {
        return createFile(
            directory = File(appContext.filesDir, THUMB_DIRECTORY),
            prefix = "thumb"
        )
    }

    fun deleteByUri(uri: String?) {
        val normalized = uri?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return
        }
        val parsed = Uri.parse(normalized)
        val file = when (parsed.scheme) {
            "file" -> parsed.path?.let(::File)
            null, "" -> File(normalized)
            else -> null
        } ?: return
        runCatching {
            file.delete()
        }
    }

    private fun createFile(directory: File, prefix: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        require(directory.exists() && directory.isDirectory) {
            "Failed to create storage directory: ${directory.absolutePath}"
        }
        val fileName = "${prefix}_${UUID.randomUUID()}.jpg"
        return File(directory, fileName)
    }

    private companion object {
        const val FULL_DIRECTORY = "photos/full"
        const val THUMB_DIRECTORY = "photos/thumbs"
    }
}
