package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.backup.importing.BackupImportException
import com.example.itemmanagementandroid.backup.importing.BackupImportResult
import com.example.itemmanagementandroid.backup.importing.BackupImporter
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class LocalBackupService(
    private val snapshotCollector: BackupSnapshotCollector,
    private val outputDirectoryProvider: BackupOutputDirectoryProvider,
    private val jsonBuilder: BackupJsonBuilder,
    private val photoPreparer: BackupPhotoPreparer = BackupPhotoPreparer(),
    private val zipWriter: BackupZipWriter = BackupZipWriter(),
    private val checksumGenerator: BackupChecksumGenerator = NoOpBackupChecksumGenerator,
    private val backupImporter: BackupImporter? = null,
    private val clock: Clock = Clock.systemUTC()
) : BackupService {
    override suspend fun exportLocalBackup(exportMode: ExportMode): BackupExportResult {
        val exportedAt = nowIsoString()
        return try {
            val snapshot = snapshotCollector.collect()
            val preparedPhotos = photoPreparer.prepare(
                exportMode = exportMode,
                itemPhotos = snapshot.itemPhotos
            )

            val preparedPhotosById = preparedPhotos.associateBy { entry -> entry.photoId }
            val manifestJson = jsonBuilder.buildManifestJson(
                snapshot = snapshot,
                exportMode = exportMode,
                createdAt = exportedAt
            )
            val dataJson = jsonBuilder.buildDataJson(
                snapshot = snapshot,
                exportedAt = exportedAt,
                preparedPhotosById = preparedPhotosById
            )
            val checksumsJson = checksumGenerator.generateChecksumsJson(
                manifestJson = manifestJson,
                dataJson = dataJson,
                photoEntries = preparedPhotos
            )

            val outputDirectory = outputDirectoryProvider.getBackupDirectory()
            val outputFile = outputDirectory.resolve(
                "backup_${formatFileTimestamp(exportedAt)}_${exportMode.wireValue}.zip"
            )

            runCatching {
                zipWriter.write(
                    outputFile = outputFile,
                    manifestJson = manifestJson,
                    dataJson = dataJson,
                    photoEntries = preparedPhotos,
                    checksumsJson = checksumsJson
                )
            }.getOrElse { throwable ->
                runCatching { outputFile.delete() }
                throw throwable
            }

            BackupExportResult(
                filePath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                exportMode = exportMode,
                createdAt = exportedAt,
                stats = BackupStats(
                    categories = snapshot.categories.size,
                    items = snapshot.items.size,
                    photos = snapshot.itemPhotos.size
                )
            )
        } catch (exception: BackupExportException) {
            throw exception
        } catch (exception: IllegalArgumentException) {
            throw BackupExportException.InvalidParameter(
                message = exception.message ?: "Invalid backup export input.",
                cause = exception
            )
        } catch (exception: IOException) {
            throw BackupExportException.IoFailure(
                message = "Backup export failed due to I/O error.",
                cause = exception
            )
        }
    }

    override suspend fun importLocalBackup(backupFilePath: String): BackupImportResult {
        val importer = backupImporter
            ?: throw BackupImportException.InvalidParameter(
                "Backup importer is not configured."
            )
        return importer.importLocalBackup(backupFilePath)
    }

    private fun nowIsoString(): String = Instant.now(clock).toString()

    private fun formatFileTimestamp(isoTimestamp: String): String {
        val instant = runCatching { Instant.parse(isoTimestamp) }.getOrElse {
            Instant.now(clock)
        }
        return FILE_TIMESTAMP_FORMATTER.format(instant)
    }

    private companion object {
        val FILE_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss_SSS")
            .withZone(ZoneOffset.UTC)
    }
}

