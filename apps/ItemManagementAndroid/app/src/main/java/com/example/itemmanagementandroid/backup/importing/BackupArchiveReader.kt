package com.example.itemmanagementandroid.backup.importing

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class BackupArchiveReader {
    fun read(
        zipFile: File,
        extractionDirectory: File
    ): BackupArchivePayload {
        if (!zipFile.exists() || !zipFile.isFile || !zipFile.canRead()) {
            throw BackupImportException.InvalidParameter(
                "Backup file does not exist or is not readable: ${zipFile.absolutePath}"
            )
        }
        if (!extractionDirectory.exists()) {
            extractionDirectory.mkdirs()
        }

        var manifestJson: ByteArray? = null
        var dataJson: ByteArray? = null
        val photoFilesByName = linkedMapOf<String, File>()

        try {
            FileInputStream(zipFile).use { fileInput ->
                ZipInputStream(fileInput).use { zipInput ->
                    while (true) {
                        val entry = zipInput.nextEntry ?: break
                        if (!entry.isDirectory) {
                            val normalizedEntryName = normalizeEntryName(entry.name)
                            when (normalizedEntryName) {
                                MANIFEST_ENTRY_PATH -> {
                                    if (manifestJson == null) {
                                        manifestJson = zipInput.readBytes()
                                    }
                                }

                                DATA_ENTRY_PATH -> {
                                    if (dataJson == null) {
                                        dataJson = zipInput.readBytes()
                                    }
                                }

                                else -> {
                                    if (normalizedEntryName.startsWith("$PHOTOS_DIRECTORY/")) {
                                        val fileName = normalizedEntryName.removePrefix("$PHOTOS_DIRECTORY/")
                                        validatePhotoFileName(fileName)
                                        val extractedFile = File(extractionDirectory, fileName)
                                        FileOutputStream(extractedFile).use { output ->
                                            zipInput.copyTo(output)
                                        }
                                        photoFilesByName[fileName] = extractedFile
                                    }
                                }
                            }
                        }
                        zipInput.closeEntry()
                    }
                }
            }
        } catch (exception: BackupImportException) {
            throw exception
        } catch (exception: IOException) {
            throw BackupImportException.IoFailure(
                message = "Failed to read backup archive.",
                cause = exception
            )
        }

        return BackupArchivePayload(
            manifestJson = requireNotNull(manifestJson) {
                throw BackupImportException.InvalidPackage(
                    "Backup package is missing manifest.json."
                )
            },
            dataJson = requireNotNull(dataJson) {
                throw BackupImportException.InvalidPackage(
                    "Backup package is missing data.json."
                )
            },
            photoFilesByName = photoFilesByName
        )
    }

    private fun normalizeEntryName(raw: String): String {
        return raw
            .replace('\\', '/')
            .trim()
            .removePrefix("/")
    }

    private fun validatePhotoFileName(fileName: String) {
        val normalized = fileName.trim()
        if (normalized.isEmpty()) {
            throw BackupImportException.InvalidPackage("Backup photo fileName is empty.")
        }
        if (normalized.contains('/') || normalized.contains('\\') || normalized.contains("..")) {
            throw BackupImportException.InvalidPackage(
                "Backup photo fileName contains invalid path traversal segment: $fileName"
            )
        }
    }

    private companion object {
        const val MANIFEST_ENTRY_PATH = "manifest.json"
        const val DATA_ENTRY_PATH = "data.json"
        const val PHOTOS_DIRECTORY = "photos"
    }
}
