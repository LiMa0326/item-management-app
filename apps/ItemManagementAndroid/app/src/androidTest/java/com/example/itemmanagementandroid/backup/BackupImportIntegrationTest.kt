package com.example.itemmanagementandroid.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.data.local.db.DatabaseProvider
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity
import com.example.itemmanagementandroid.domain.model.DefaultCategories
import com.example.itemmanagementandroid.ui.di.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupImportIntegrationTest {
    private lateinit var appContext: android.content.Context
    private lateinit var dependencies: AppDependencies

    @Before
    fun setUp() {
        runBlocking {
            appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
            dependencies = AppDependencies(appContext)
            resetLocalState()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            resetLocalState()
        }
    }

    @Test
    fun importLocalBackup_replaceAll_shouldReplaceDataAndCleanupOldFiles() {
        runBlocking {
            val sourceBackup = withContext(Dispatchers.IO) {
                seedDatasetA()
                dependencies.exportLocalBackupUseCase(ExportMode.FULL)
            }

            val obsoleteFiles = withContext(Dispatchers.IO) {
                seedDatasetB()
            }

            val result = withContext(Dispatchers.IO) {
                dependencies.importLocalBackupUseCase(sourceBackup.filePath)
            }

            withContext(Dispatchers.IO) {
                val database = DatabaseProvider.get(appContext)
                val categories = database.categoryDao().listAllOrdered()
                val items = database.itemDao().listAllOrdered()
                val photos = database.itemPhotoDao().listAll()

                assertTrue(categories.any { it.id == "cat_source" })
                assertFalse(categories.any { it.id == "cat_target" })

                assertTrue(items.any { it.id == "item_source" })
                assertFalse(items.any { it.id == "item_target" })

                assertEquals(1, photos.size)
                val restoredPhoto = photos.first()
                assertEquals("photo_source", restoredPhoto.id)
                assertTrue(File(java.net.URI(restoredPhoto.localUri)).exists())

                obsoleteFiles.forEach { obsoleteFile ->
                    assertFalse(obsoleteFile.exists())
                }
            }

            assertEquals(1, result.stats.categories)
            assertEquals(1, result.stats.items)
            assertEquals(1, result.stats.photos)
            assertTrue(File(result.rollbackSnapshotPath).exists())
        }
    }

    @Test
    fun importLocalBackup_unknownFieldsAndHigherVersion_shouldImportWithWarnings() {
        runBlocking {
            withContext(Dispatchers.IO) {
                seedDatasetB()
            }

            val backupWithUnknownFields = withContext(Dispatchers.IO) {
                createBackupZipWithUnknownFields()
            }

            val result = withContext(Dispatchers.IO) {
                dependencies.importLocalBackupUseCase(backupWithUnknownFields.absolutePath)
            }

            withContext(Dispatchers.IO) {
                val database = DatabaseProvider.get(appContext)
                val categories = database.categoryDao().listAllOrdered()
                val items = database.itemDao().listAllOrdered()

                assertTrue(categories.any { it.id == "cat_unknown" })
                assertTrue(items.any { it.id == "item_unknown" })
                assertFalse(categories.any { it.id == "cat_target" })
            }

            assertTrue(result.warnings.any { it.code == "FORMAT_VERSION_HIGHER" })
            assertTrue(result.warnings.any { it.code == "SCHEMA_VERSION_HIGHER" })
            assertEquals(1, result.stats.categories)
            assertEquals(1, result.stats.items)
            assertEquals(0, result.stats.photos)
        }
    }

    private suspend fun seedDatasetA() {
        val database = DatabaseProvider.get(appContext)
        database.clearAllTables()

        database.categoryDao().insert(
            CategoryEntity(
                id = "cat_source",
                name = "Source Category",
                sortOrder = 0,
                isArchived = false,
                isSystemDefault = false,
                createdAt = "2026-03-08T10:00:00Z",
                updatedAt = "2026-03-08T10:00:00Z"
            )
        )
        database.itemDao().insert(
            ItemEntity(
                id = "item_source",
                categoryId = "cat_source",
                name = "Source Item",
                purchaseDate = "2026-03-08",
                purchasePrice = 99.0,
                purchaseCurrency = "USD",
                purchasePlace = "Store A",
                description = "Source description",
                tagsJson = """["source"]""",
                customAttributesJson = """{"enabled":true}""",
                createdAt = "2026-03-08T10:01:00Z",
                updatedAt = "2026-03-08T10:01:00Z",
                deletedAt = null
            )
        )

        val fullFile = File(appContext.filesDir, "seed_source_full.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        }
        val thumbFile = File(appContext.filesDir, "seed_source_thumb.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x11, 0x12, 0x13))
        }
        database.itemPhotoDao().insert(
            ItemPhotoEntity(
                id = "photo_source",
                itemId = "item_source",
                localUri = fullFile.toURI().toString(),
                thumbnailUri = thumbFile.toURI().toString(),
                contentType = "image/jpeg",
                width = 100,
                height = 100,
                createdAt = "2026-03-08T10:02:00Z"
            )
        )
    }

    private suspend fun seedDatasetB(): List<File> {
        val database = DatabaseProvider.get(appContext)
        database.clearAllTables()

        database.categoryDao().insert(
            CategoryEntity(
                id = "cat_target",
                name = "Target Category",
                sortOrder = 0,
                isArchived = false,
                isSystemDefault = false,
                createdAt = "2026-03-08T11:00:00Z",
                updatedAt = "2026-03-08T11:00:00Z"
            )
        )
        database.itemDao().insert(
            ItemEntity(
                id = "item_target",
                categoryId = "cat_target",
                name = "Target Item",
                purchaseDate = "2026-03-08",
                purchasePrice = 199.0,
                purchaseCurrency = "USD",
                purchasePlace = "Store B",
                description = "Target description",
                tagsJson = """["target"]""",
                customAttributesJson = """{"enabled":false}""",
                createdAt = "2026-03-08T11:01:00Z",
                updatedAt = "2026-03-08T11:01:00Z",
                deletedAt = null
            )
        )

        val targetFull = File(appContext.filesDir, "seed_target_full.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x21, 0x22, 0x23))
        }
        val targetThumb = File(appContext.filesDir, "seed_target_thumb.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x31, 0x32, 0x33))
        }
        database.itemPhotoDao().insert(
            ItemPhotoEntity(
                id = "photo_target",
                itemId = "item_target",
                localUri = targetFull.toURI().toString(),
                thumbnailUri = targetThumb.toURI().toString(),
                contentType = "image/jpeg",
                width = 120,
                height = 120,
                createdAt = "2026-03-08T11:02:00Z"
            )
        )

        val orphanFile = File(appContext.filesDir, "photos/full/orphan_${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0x55, 0x56))
        }

        return listOf(targetFull, targetThumb, orphanFile)
    }

    private fun createBackupZipWithUnknownFields(): File {
        val backupFile = File(
            appContext.cacheDir,
            "backup_unknown_${System.currentTimeMillis()}.zip"
        )
        val manifest = """
            {
              "formatVersion": "9.0",
              "createdAt": "2026-03-08T12:00:00Z",
              "exportMode": "metadata_only",
              "unknownManifestField": "ignored",
              "app": {"name":"ItemManagementAndroid","build":"test","platform":"android"},
              "stats": {"categories":1,"items":1,"photos":0}
            }
        """.trimIndent()
        val data = """
            {
              "schemaVersion": "9.0",
              "exportedAt": "2026-03-08T12:00:00Z",
              "categories": [
                {
                  "id": "cat_unknown",
                  "name": "Unknown Category",
                  "sortOrder": 0,
                  "isArchived": false,
                  "createdAt": "2026-03-08T12:00:00Z",
                  "updatedAt": "2026-03-08T12:00:00Z",
                  "futureField": "ignored"
                }
              ],
              "items": [
                {
                  "id": "item_unknown",
                  "categoryId": "cat_unknown",
                  "name": "Unknown Item",
                  "customAttributes": {
                    "enabled": true,
                    "nested": {"k":"v"}
                  },
                  "createdAt": "2026-03-08T12:00:00Z",
                  "updatedAt": "2026-03-08T12:00:00Z",
                  "futureField": 1
                }
              ],
              "itemPhotos": [],
              "futureTopLevel": true
            }
        """.trimIndent()

        ZipOutputStream(backupFile.outputStream()).use { zipOutput ->
            zipOutput.putNextEntry(ZipEntry("manifest.json"))
            zipOutput.write(manifest.toByteArray())
            zipOutput.closeEntry()

            zipOutput.putNextEntry(ZipEntry("data.json"))
            zipOutput.write(data.toByteArray())
            zipOutput.closeEntry()
        }
        return backupFile
    }

    private suspend fun resetLocalState() {
        withContext(Dispatchers.IO) {
            val database = DatabaseProvider.get(appContext)
            database.clearAllTables()
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
            appContext.getExternalFilesDir("backups")?.deleteRecursively()
            File(appContext.filesDir, "backups").deleteRecursively()
            File(appContext.filesDir, "photos").deleteRecursively()
            appContext.cacheDir.listFiles()
                ?.filter { file -> file.name.startsWith("backup_unknown_") || file.name.startsWith("backup_import_") }
                ?.forEach(File::deleteRecursively)
        }
    }
}
