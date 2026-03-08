package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import kotlin.io.path.createTempDirectory

class LocalBackupServiceTest {
    private lateinit var testRootDir: File
    private lateinit var backupOutputDir: File
    private lateinit var fullPhotoFile: File
    private lateinit var thumbnailPhotoFile: File
    private lateinit var baseSnapshot: BackupSnapshot

    @Before
    fun setUp() {
        testRootDir = createTempDirectory(prefix = "local_backup_service_test_").toFile()
        backupOutputDir = File(testRootDir, "backups").also(File::mkdirs)
        fullPhotoFile = File(testRootDir, "photo_full.jpg").apply {
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        thumbnailPhotoFile = File(testRootDir, "photo_thumb.jpg").apply {
            writeBytes(byteArrayOf(0x11, 0x12))
        }
        baseSnapshot = BackupSnapshot(
            categories = listOf(
                Category(
                    id = "cat_1",
                    name = "Electronics",
                    sortOrder = 0,
                    isArchived = false,
                    isSystemDefault = true,
                    createdAt = "2026-03-08T08:00:00Z",
                    updatedAt = "2026-03-08T08:00:00Z"
                )
            ),
            items = listOf(
                Item(
                    id = "item_1",
                    categoryId = "cat_1",
                    name = "Noise Cancelling Headphones",
                    purchaseDate = "2025-10-12",
                    purchasePrice = 329.99,
                    purchaseCurrency = "USD",
                    purchasePlace = "Amazon",
                    description = "Commute headset",
                    tags = listOf("audio", "commute"),
                    customAttributes = mapOf(
                        "color" to "Black",
                        "active" to true
                    ),
                    createdAt = "2026-03-08T08:01:00Z",
                    updatedAt = "2026-03-08T08:02:00Z",
                    deletedAt = null
                )
            ),
            itemPhotos = listOf(
                ItemPhoto(
                    id = "photo_1",
                    itemId = "item_1",
                    localUri = fullPhotoFile.toURI().toString(),
                    thumbnailUri = thumbnailPhotoFile.toURI().toString(),
                    contentType = "image/jpeg",
                    width = 1600,
                    height = 1200,
                    createdAt = "2026-03-08T08:03:00Z"
                )
            )
        )
    }

    @Test
    fun export_metadataOnly_writesOnlyManifestAndData() = runBlocking {
        val service = buildService(snapshot = baseSnapshot)

        val result = service.exportLocalBackup(ExportMode.METADATA_ONLY)
        val zipEntries = readZipEntries(File(result.filePath))
        val manifest = zipEntries.getValue("manifest.json").toString(StandardCharsets.UTF_8)
        val data = zipEntries.getValue("data.json").toString(StandardCharsets.UTF_8)

        assertTrue(zipEntries.containsKey("manifest.json"))
        assertTrue(zipEntries.containsKey("data.json"))
        assertFalse(zipEntries.keys.any { entryName -> entryName.startsWith("photos/") })
        assertFalse(zipEntries.containsKey("checksums.json"))

        assertTrue(manifest.contains("\"formatVersion\":\"1.0\""))
        assertTrue(manifest.contains("\"exportMode\":\"metadata_only\""))
        assertTrue(manifest.contains("\"createdAt\":\"2026-03-08T12:00:00Z\""))
        assertTrue(manifest.contains("\"stats\":{\"categories\":1,\"items\":1,\"photos\":1}"))

        assertTrue(data.contains("\"schemaVersion\":\"1.0\""))
        assertTrue(data.contains("\"categories\""))
        assertTrue(data.contains("\"items\""))
        assertTrue(data.contains("\"itemPhotos\""))
        assertFalse(data.contains("\"fileName\""))
    }

    @Test
    fun export_thumbnails_writesThumbnailPhotoEntries() = runBlocking {
        val service = buildService(snapshot = baseSnapshot)

        val result = service.exportLocalBackup(ExportMode.THUMBNAILS)
        val zipEntries = readZipEntries(File(result.filePath))
        val data = zipEntries.getValue("data.json").toString(StandardCharsets.UTF_8)

        assertTrue(zipEntries.containsKey("photos/photo_1_thumb.jpg"))
        assertTrue(data.contains("\"fileName\":\"photo_1_thumb.jpg\""))
        assertTrue(data.contains("\"kind\":\"thumbnail\""))
        assertFalse(zipEntries.containsKey("checksums.json"))
    }

    @Test
    fun export_full_writesFullPhotoEntries() = runBlocking {
        val service = buildService(snapshot = baseSnapshot)

        val result = service.exportLocalBackup(ExportMode.FULL)
        val zipEntries = readZipEntries(File(result.filePath))
        val data = zipEntries.getValue("data.json").toString(StandardCharsets.UTF_8)

        assertTrue(zipEntries.containsKey("photos/photo_1.jpg"))
        assertTrue(data.contains("\"fileName\":\"photo_1.jpg\""))
        assertTrue(data.contains("\"kind\":\"full\""))
        assertFalse(zipEntries.containsKey("checksums.json"))
    }

    @Test
    fun export_missingPhotoFile_throwsMissingPhotoFileException() {
        val invalidSnapshot = baseSnapshot.copy(
            itemPhotos = listOf(
                baseSnapshot.itemPhotos.first().copy(
                    thumbnailUri = File(testRootDir, "missing_thumb.jpg").toURI().toString()
                )
            )
        )
        val service = buildService(snapshot = invalidSnapshot)

        val exception = assertThrows(BackupExportException.MissingPhotoFile::class.java) {
            runBlocking {
                service.exportLocalBackup(ExportMode.THUMBNAILS)
            }
        }

        assertEquals("photo_1", exception.photoId)
        assertNotNull(exception.sourceUri)
        assertTrue(backupOutputDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun export_withChecksumGenerator_writesChecksumsEntry() = runBlocking {
        val service = buildService(
            snapshot = baseSnapshot,
            checksumGenerator = object : BackupChecksumGenerator {
                override fun generateChecksumsJson(
                    manifestJson: ByteArray,
                    dataJson: ByteArray,
                    photoEntries: List<PreparedPhotoEntry>
                ): ByteArray {
                    return """{"checksums":{"manifest.json":"sha256:test"}}"""
                        .toByteArray(StandardCharsets.UTF_8)
                }
            }
        )

        val result = service.exportLocalBackup(ExportMode.METADATA_ONLY)
        val zipEntries = readZipEntries(File(result.filePath))

        assertTrue(zipEntries.containsKey("checksums.json"))
        val checksums = zipEntries.getValue("checksums.json").toString(StandardCharsets.UTF_8)
        assertTrue(checksums.contains("\"checksums\""))
    }

    private fun buildService(
        snapshot: BackupSnapshot,
        checksumGenerator: BackupChecksumGenerator = NoOpBackupChecksumGenerator
    ): LocalBackupService {
        val collector = BackupSnapshotCollector(
            listCategories = { snapshot.categories },
            listItems = { snapshot.items },
            listItemPhotosByItemId = { itemId ->
                snapshot.itemPhotos.filter { photo -> photo.itemId == itemId }
            }
        )
        return LocalBackupService(
            snapshotCollector = collector,
            outputDirectoryProvider = object : BackupOutputDirectoryProvider {
                override fun getBackupDirectory(): File = backupOutputDir
            },
            jsonBuilder = BackupJsonBuilder(
                appName = "ItemManagementAndroid",
                appBuild = "test",
                appPlatform = "android"
            ),
            checksumGenerator = checksumGenerator,
            clock = Clock.fixed(Instant.parse("2026-03-08T12:00:00Z"), ZoneOffset.UTC)
        )
    }

    private fun readZipEntries(zipFile: File): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        FileInputStream(zipFile).use { fileInput ->
            ZipInputStream(fileInput).use { zipInput ->
                while (true) {
                    val entry = zipInput.nextEntry ?: break
                    entries[entry.name] = zipInput.readBytes()
                    zipInput.closeEntry()
                }
            }
        }
        return entries
    }
}
