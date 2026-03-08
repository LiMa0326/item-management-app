package com.example.itemmanagementandroid.backup.export

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupZipWriter {
    fun write(
        outputFile: File,
        manifestJson: ByteArray,
        dataJson: ByteArray,
        photoEntries: List<PreparedPhotoEntry>,
        checksumsJson: ByteArray?
    ) {
        outputFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        FileOutputStream(outputFile).use { fileOutput ->
            ZipOutputStream(fileOutput).use { zipOutput ->
                writeBytesEntry(
                    zipOutput = zipOutput,
                    entryPath = MANIFEST_ENTRY_PATH,
                    payload = manifestJson
                )
                writeBytesEntry(
                    zipOutput = zipOutput,
                    entryPath = DATA_ENTRY_PATH,
                    payload = dataJson
                )

                photoEntries
                    .sortedBy { entry -> entry.fileName }
                    .forEach { photoEntry ->
                        val entryPath = "$PHOTOS_DIRECTORY/${photoEntry.fileName}"
                        writeFileEntry(
                            zipOutput = zipOutput,
                            entryPath = entryPath,
                            sourceFile = photoEntry.sourceFile
                        )
                    }

                if (checksumsJson != null) {
                    writeBytesEntry(
                        zipOutput = zipOutput,
                        entryPath = CHECKSUMS_ENTRY_PATH,
                        payload = checksumsJson
                    )
                }
            }
        }
    }

    private fun writeBytesEntry(
        zipOutput: ZipOutputStream,
        entryPath: String,
        payload: ByteArray
    ) {
        val zipEntry = ZipEntry(entryPath)
        zipOutput.putNextEntry(zipEntry)
        zipOutput.write(payload)
        zipOutput.closeEntry()
    }

    private fun writeFileEntry(
        zipOutput: ZipOutputStream,
        entryPath: String,
        sourceFile: File
    ) {
        val zipEntry = ZipEntry(entryPath)
        zipOutput.putNextEntry(zipEntry)
        FileInputStream(sourceFile).use { input ->
            input.copyTo(zipOutput)
        }
        zipOutput.closeEntry()
    }

    private companion object {
        const val MANIFEST_ENTRY_PATH = "manifest.json"
        const val DATA_ENTRY_PATH = "data.json"
        const val CHECKSUMS_ENTRY_PATH = "checksums.json"
        const val PHOTOS_DIRECTORY = "photos"
    }
}

