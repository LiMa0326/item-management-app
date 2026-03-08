package com.example.itemmanagementandroid.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.data.local.db.DatabaseProvider
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity
import com.example.itemmanagementandroid.domain.model.DefaultCategories
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.photo.PhotoBackupFileNameMapper
import com.example.itemmanagementandroid.ui.di.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipInputStream

@RunWith(AndroidJUnit4::class)
class BackupExportIntegrationTest {
    private lateinit var appContext: android.content.Context
    private lateinit var dependencies: AppDependencies

    @Before
    fun setUp() {
        runBlocking {
            appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            dependencies = AppDependencies(appContext)
            withContext(Dispatchers.IO) {
                val database = DatabaseProvider.get(appContext)
                database.clearAllTables()
                insertDefaultCategory(database)
                appContext.getExternalFilesDir("backups")?.deleteRecursively()
                File(appContext.filesDir, "backups").deleteRecursively()
            }
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            withContext(Dispatchers.IO) {
                val database = DatabaseProvider.get(appContext)
                database.clearAllTables()
                insertDefaultCategory(database)
                appContext.getExternalFilesDir("backups")?.deleteRecursively()
                File(appContext.filesDir, "backups").deleteRecursively()
            }
        }
    }

    @Test
    fun exportLocalBackup_allModes_generateExpectedZipStructure() {
        runBlocking {
            val seed = seedTestData(appContext)

            val metadataResult = withContext(Dispatchers.IO) {
                dependencies.exportLocalBackupUseCase(ExportMode.METADATA_ONLY)
            }
            val metadataEntries = readZipEntries(File(metadataResult.filePath))
            assertCommonJsonFields(
                entries = metadataEntries,
                expectedMode = "metadata_only"
            )
            assertFalse(metadataEntries.keys.any { it.startsWith("photos/") })
            assertFalse(metadataEntries.containsKey("checksums.json"))
            run {
                val data = JSONObject(metadataEntries.getValue("data.json").toString(StandardCharsets.UTF_8))
                val itemPhoto = data.getJSONArray("itemPhotos").getJSONObject(0)
                assertFalse(itemPhoto.has("fileName"))
                assertFalse(itemPhoto.has("kind"))
            }

            val thumbnailsResult = withContext(Dispatchers.IO) {
                dependencies.exportLocalBackupUseCase(ExportMode.THUMBNAILS)
            }
            val thumbnailsEntries = readZipEntries(File(thumbnailsResult.filePath))
            assertCommonJsonFields(
                entries = thumbnailsEntries,
                expectedMode = "thumbnails"
            )
            assertTrue(thumbnailsEntries.containsKey("photos/${seed.thumbnailFileName}"))
            assertFalse(thumbnailsEntries.containsKey("checksums.json"))
            run {
                val data = JSONObject(thumbnailsEntries.getValue("data.json").toString(StandardCharsets.UTF_8))
                val itemPhoto = data.getJSONArray("itemPhotos").getJSONObject(0)
                assertEquals(seed.thumbnailFileName, itemPhoto.getString("fileName"))
                assertEquals("thumbnail", itemPhoto.getString("kind"))
            }

            val fullResult = withContext(Dispatchers.IO) {
                dependencies.exportLocalBackupUseCase(ExportMode.FULL)
            }
            val fullEntries = readZipEntries(File(fullResult.filePath))
            assertCommonJsonFields(
                entries = fullEntries,
                expectedMode = "full"
            )
            assertTrue(fullEntries.containsKey("photos/${seed.fullFileName}"))
            assertFalse(fullEntries.containsKey("checksums.json"))
            run {
                val data = JSONObject(fullEntries.getValue("data.json").toString(StandardCharsets.UTF_8))
                val itemPhoto = data.getJSONArray("itemPhotos").getJSONObject(0)
                assertEquals(seed.fullFileName, itemPhoto.getString("fileName"))
                assertEquals("full", itemPhoto.getString("kind"))
            }
        }
    }

    private suspend fun seedTestData(context: android.content.Context): SeedData {
        val timestamp = System.currentTimeMillis()
        val category = dependencies.createCategoryUseCase("Backup Category $timestamp")
        val item = dependencies.createItemUseCase(
            ItemDraft(
                categoryId = category.id,
                name = "Backup Item $timestamp",
                purchaseDate = "2026-03-08",
                purchasePrice = 199.99,
                purchaseCurrency = "USD",
                purchasePlace = "Local Store",
                description = "Backup export integration test",
                tags = listOf("backup", "export"),
                customAttributes = mapOf("enabled" to true)
            )
        )

        val fullFile = File(context.filesDir, "backup_integration_full_$timestamp.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        }
        val thumbnailFile = File(context.filesDir, "backup_integration_thumb_$timestamp.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x11, 0x12, 0x13))
        }

        val photoId = "backup_photo_$timestamp"
        DatabaseProvider.get(context).itemPhotoDao().insert(
            ItemPhotoEntity(
                id = photoId,
                itemId = item.id,
                localUri = fullFile.toURI().toString(),
                thumbnailUri = thumbnailFile.toURI().toString(),
                contentType = "image/jpeg",
                width = 100,
                height = 100,
                createdAt = Instant.now().toString()
            )
        )

        return SeedData(
            fullFileName = PhotoBackupFileNameMapper.buildFullFileName(
                photoId = photoId,
                contentType = "image/jpeg",
                localUri = fullFile.toURI().toString()
            ),
            thumbnailFileName = PhotoBackupFileNameMapper.buildThumbnailFileName(
                photoId = photoId,
                contentType = "image/jpeg",
                thumbnailUri = thumbnailFile.toURI().toString()
            )
        )
    }

    private suspend fun insertDefaultCategory(
        database: com.example.itemmanagementandroid.data.local.db.ItemManagementDatabase
    ) {
        val now = Instant.now().toString()
        database.categoryDao().insert(
            CategoryEntity(
                id = DefaultCategories.ELECTRONICS_ID,
                name = DefaultCategories.ELECTRONICS_NAME,
                sortOrder = 0,
                isArchived = false,
                isSystemDefault = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private fun assertCommonJsonFields(
        entries: Map<String, ByteArray>,
        expectedMode: String
    ) {
        assertTrue(entries.containsKey("manifest.json"))
        assertTrue(entries.containsKey("data.json"))

        val manifest = JSONObject(entries.getValue("manifest.json").toString(StandardCharsets.UTF_8))
        assertEquals("1.0", manifest.getString("formatVersion"))
        assertEquals(expectedMode, manifest.getString("exportMode"))
        assertTrue(manifest.has("createdAt"))
        assertTrue(manifest.has("app"))
        assertTrue(manifest.has("stats"))

        val data = JSONObject(entries.getValue("data.json").toString(StandardCharsets.UTF_8))
        assertEquals("1.0", data.getString("schemaVersion"))
        assertTrue(data.has("exportedAt"))
        assertTrue(data.has("categories"))
        assertTrue(data.has("items"))
        assertTrue(data.has("itemPhotos"))
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

    private data class SeedData(
        val fullFileName: String,
        val thumbnailFileName: String
    )
}
