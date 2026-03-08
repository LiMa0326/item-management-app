package com.example.itemmanagementandroid.backup.storage

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

class SharedBackupDocumentStorage(
    private val appContext: Context,
    private val directoryPreferenceStore: BackupDirectoryPreferenceStore
) : BackupDocumentStorage {
    private val contentResolver: ContentResolver = appContext.contentResolver

    override suspend fun setBackupDirectory(treeUri: String): BackupDirectoryInfo {
        return withContext(Dispatchers.IO) {
            val normalizedTreeUri = normalizeUri(treeUri)
            val uri = parseUri(normalizedTreeUri)
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)

            val rootDirectory = requireTreeDirectory(uri)
            directoryPreferenceStore.setBackupTreeUri(normalizedTreeUri)

            BackupDirectoryInfo(
                treeUri = normalizedTreeUri,
                displayName = rootDirectory.name ?: uri.lastPathSegment.orEmpty(),
                hasPersistedPermission = hasPersistedPermission(uri)
            )
        }
    }

    override suspend fun getBackupDirectory(): BackupDirectoryInfo {
        return withContext(Dispatchers.IO) {
            val treeUri = directoryPreferenceStore.getBackupTreeUri()
            if (treeUri == null) {
                return@withContext BackupDirectoryInfo(
                    treeUri = null,
                    displayName = null,
                    hasPersistedPermission = false
                )
            }

            val uri = runCatching { parseUri(treeUri) }.getOrNull()
            if (uri == null) {
                return@withContext BackupDirectoryInfo(
                    treeUri = treeUri,
                    displayName = null,
                    hasPersistedPermission = false
                )
            }

            val hasPermission = hasPersistedPermission(uri)
            val displayName = if (hasPermission) {
                runCatching {
                    requireTreeDirectory(uri).name ?: uri.lastPathSegment.orEmpty()
                }.getOrNull()
            } else {
                null
            }
            BackupDirectoryInfo(
                treeUri = treeUri,
                displayName = displayName,
                hasPersistedPermission = hasPermission
            )
        }
    }

    override suspend fun listBackupFiles(treeUri: String): List<BackupDocumentEntry> {
        return withContext(Dispatchers.IO) {
            val uri = parseUri(normalizeUri(treeUri))
            requirePersistedPermission(uri)
            val rootDirectory = requireTreeDirectory(uri)

            rootDirectory.listFiles()
                .asSequence()
                .filter(DocumentFile::isFile)
                .filter { file ->
                    val name = file.name.orEmpty()
                    name.lowercase(Locale.ROOT).endsWith(ZIP_EXTENSION)
                }
                .map { file ->
                    BackupDocumentEntry(
                        uri = file.uri.toString(),
                        displayName = file.name ?: file.uri.toString(),
                        sizeBytes = file.length(),
                        lastModified = file.lastModified()
                    )
                }
                .sortedByDescending { entry -> entry.lastModified }
                .toList()
        }
    }

    override suspend fun copyLocalFileToDocument(
        localFilePath: String,
        targetTreeUri: String,
        targetName: String
    ): BackupDocumentEntry {
        return withContext(Dispatchers.IO) {
            val normalizedTreeUri = normalizeUri(targetTreeUri)
            val normalizedTargetName = normalizeTargetName(targetName)
            val sourceFile = File(localFilePath)
            require(sourceFile.exists() && sourceFile.isFile && sourceFile.canRead()) {
                "Source file does not exist or is not readable: $localFilePath"
            }

            val treeUri = parseUri(normalizedTreeUri)
            requirePersistedPermission(treeUri)
            val rootDirectory = requireTreeDirectory(treeUri)

            rootDirectory.findFile(normalizedTargetName)?.delete()
            val targetDocument = requireNotNull(
                rootDirectory.createFile(MIME_TYPE_ZIP, normalizedTargetName)
            ) {
                "Failed to create target backup document: $normalizedTargetName"
            }

            contentResolver.openOutputStream(targetDocument.uri, "w").use { output ->
                requireNotNull(output) {
                    "Failed to open output stream for ${targetDocument.uri}"
                }
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            BackupDocumentEntry(
                uri = targetDocument.uri.toString(),
                displayName = targetDocument.name ?: normalizedTargetName,
                sizeBytes = targetDocument.length(),
                lastModified = targetDocument.lastModified()
            )
        }
    }

    override suspend fun copyDocumentToTempFile(documentUri: String): File {
        return withContext(Dispatchers.IO) {
            val normalizedDocumentUri = normalizeUri(documentUri)
            val uri = parseUri(normalizedDocumentUri)
            val targetFile = File(
                appContext.cacheDir,
                "backup_import_${System.currentTimeMillis()}_${UUID.randomUUID()}$ZIP_EXTENSION"
            )
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) {
                    "Failed to open input stream for $normalizedDocumentUri"
                }
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        }
    }

    private fun requireTreeDirectory(uri: Uri): DocumentFile {
        val directory = DocumentFile.fromTreeUri(appContext, uri)
        require(directory != null && directory.exists() && directory.isDirectory) {
            "Backup directory is not accessible: $uri"
        }
        return directory
    }

    private fun requirePersistedPermission(uri: Uri) {
        require(hasPersistedPermission(uri)) {
            "Backup directory permission is missing: $uri"
        }
    }

    private fun hasPersistedPermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission && permission.isWritePermission
        }
    }

    private fun normalizeUri(rawUri: String): String {
        val normalized = rawUri.trim()
        require(normalized.isNotEmpty()) {
            "Uri must not be blank."
        }
        return normalized
    }

    private fun parseUri(rawUri: String): Uri {
        return runCatching { Uri.parse(rawUri) }.getOrElse {
            throw IllegalArgumentException("Invalid uri: $rawUri")
        }
    }

    private fun normalizeTargetName(targetName: String): String {
        val normalized = targetName.trim()
        require(normalized.isNotEmpty()) {
            "Target backup file name must not be blank."
        }
        return if (normalized.lowercase(Locale.ROOT).endsWith(ZIP_EXTENSION)) {
            normalized
        } else {
            "$normalized$ZIP_EXTENSION"
        }
    }

    private companion object {
        const val MIME_TYPE_ZIP = "application/zip"
        const val ZIP_EXTENSION = ".zip"
    }
}
