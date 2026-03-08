package com.example.itemmanagementandroid.ui.screens.settings

import com.example.itemmanagementandroid.backup.export.BackupExportResult
import com.example.itemmanagementandroid.backup.export.BackupService
import com.example.itemmanagementandroid.backup.export.BackupStats
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.backup.importing.BackupImportException
import com.example.itemmanagementandroid.backup.importing.BackupImportMode
import com.example.itemmanagementandroid.backup.importing.BackupImportResult
import com.example.itemmanagementandroid.backup.importing.BackupImportStats
import com.example.itemmanagementandroid.backup.importing.BackupImportWarning
import com.example.itemmanagementandroid.backup.storage.BackupDirectoryInfo
import com.example.itemmanagementandroid.backup.storage.BackupDocumentEntry
import com.example.itemmanagementandroid.backup.storage.BackupDocumentStorage
import com.example.itemmanagementandroid.domain.usecase.backup.ExportBackupToSharedDirectoryUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ExportLocalBackupUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.GetBackupDirectoryUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ImportBackupFromDocumentUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ImportLocalBackupUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ListImportableBackupsUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.SetBackupDirectoryUseCase
import com.example.itemmanagementandroid.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exportAndListWithoutDirectory_shouldShowError() = runTest(mainDispatcherRule.dispatcher) {
        val storage = FakeBackupDocumentStorage()
        val viewModel = createViewModel(storage = storage)
        advanceUntilIdle()

        viewModel.exportBackupToSharedDirectory()
        viewModel.refreshImportableBackups()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Please select a backup directory first.", state.exportErrorMessage)
        assertEquals("Please select a backup directory first.", state.importErrorMessage)
    }

    @Test
    fun onBackupDirectorySelected_shouldPersistAndLoadBackups() = runTest(mainDispatcherRule.dispatcher) {
        val storage = FakeBackupDocumentStorage(
            backups = listOf(
                BackupDocumentEntry(
                    uri = "content://backup/tree/backup_1.zip",
                    displayName = "backup_1.zip",
                    sizeBytes = 100,
                    lastModified = 20
                ),
                BackupDocumentEntry(
                    uri = "content://backup/tree/backup_2.zip",
                    displayName = "backup_2.zip",
                    sizeBytes = 200,
                    lastModified = 10
                )
            )
        )
        val viewModel = createViewModel(storage = storage)
        advanceUntilIdle()

        viewModel.onBackupDirectorySelected("content://backup/tree")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("content://backup/tree", state.selectedBackupTreeUri)
        assertTrue(state.hasPersistedPermission)
        assertEquals(2, state.importableBackups.size)
    }

    @Test
    fun exportBackupToSharedDirectory_shouldUpdateExportState() = runTest(mainDispatcherRule.dispatcher) {
        val storage = FakeBackupDocumentStorage()
        val backupService = FakeBackupService()
        val viewModel = createViewModel(
            storage = storage,
            backupService = backupService
        )
        advanceUntilIdle()

        viewModel.onBackupDirectorySelected("content://backup/tree")
        advanceUntilIdle()
        viewModel.exportBackupToSharedDirectory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exportMessage)
        assertNotNull(state.lastExportPath)
        assertNull(state.exportErrorMessage)
    }

    @Test
    fun importFlow_shouldHandleConfirmAndWarnings() = runTest(mainDispatcherRule.dispatcher) {
        val storage = FakeBackupDocumentStorage(
            backups = listOf(
                BackupDocumentEntry(
                    uri = "content://backup/tree/backup_1.zip",
                    displayName = "backup_1.zip",
                    sizeBytes = 100,
                    lastModified = 20
                )
            )
        )
        val backupService = FakeBackupService(
            importResult = BackupImportResult(
                sourceFilePath = "C:/temp/source.zip",
                importMode = BackupImportMode.REPLACE_ALL,
                importedAt = "2026-03-08T12:00:00Z",
                rollbackSnapshotPath = "C:/temp/rollback.zip",
                stats = BackupImportStats(categories = 1, items = 2, photos = 3),
                warnings = listOf(
                    BackupImportWarning(
                        code = "FORMAT_VERSION_HIGHER",
                        message = "newer version"
                    )
                )
            )
        )
        val viewModel = createViewModel(
            storage = storage,
            backupService = backupService
        )
        advanceUntilIdle()
        viewModel.onBackupDirectorySelected("content://backup/tree")
        advanceUntilIdle()

        viewModel.requestImport("content://backup/tree/backup_1.zip")
        assertEquals("content://backup/tree/backup_1.zip", viewModel.uiState.value.pendingImportBackupUri)

        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pendingImportBackupUri)
        assertNull(state.pendingImportBackupName)
        assertNotNull(state.importMessage)
        assertEquals(1, state.importWarnings.size)
        assertNull(state.importErrorMessage)
    }

    @Test
    fun importFlow_failure_shouldMapErrorMessage() = runTest(mainDispatcherRule.dispatcher) {
        val storage = FakeBackupDocumentStorage(
            backups = listOf(
                BackupDocumentEntry(
                    uri = "content://backup/tree/backup_1.zip",
                    displayName = "backup_1.zip",
                    sizeBytes = 100,
                    lastModified = 20
                )
            )
        )
        val backupService = FakeBackupService(
            importThrowable = BackupImportException.InvalidPackage("invalid")
        )
        val viewModel = createViewModel(
            storage = storage,
            backupService = backupService
        )
        advanceUntilIdle()
        viewModel.onBackupDirectorySelected("content://backup/tree")
        advanceUntilIdle()

        viewModel.requestImport("content://backup/tree/backup_1.zip")
        viewModel.confirmImport()
        advanceUntilIdle()

        assertEquals("Import failed: backup package is invalid.", viewModel.uiState.value.importErrorMessage)
    }

    private fun createViewModel(
        storage: FakeBackupDocumentStorage,
        backupService: FakeBackupService = FakeBackupService()
    ): SettingsViewModel {
        val exportLocalBackupUseCase = ExportLocalBackupUseCase(backupService)
        val importLocalBackupUseCase = ImportLocalBackupUseCase(backupService)
        return SettingsViewModel(
            setBackupDirectoryUseCase = SetBackupDirectoryUseCase(storage),
            getBackupDirectoryUseCase = GetBackupDirectoryUseCase(storage),
            listImportableBackupsUseCase = ListImportableBackupsUseCase(storage),
            exportBackupToSharedDirectoryUseCase = ExportBackupToSharedDirectoryUseCase(
                exportLocalBackupUseCase = exportLocalBackupUseCase,
                backupDocumentStorage = storage
            ),
            importBackupFromDocumentUseCase = ImportBackupFromDocumentUseCase(
                importLocalBackupUseCase = importLocalBackupUseCase,
                backupDocumentStorage = storage
            )
        )
    }
}

private class FakeBackupDocumentStorage(
    private val rootDir: File = createTempDirectory(prefix = "settings_vm_storage_").toFile(),
    backups: List<BackupDocumentEntry> = emptyList()
) : BackupDocumentStorage {
    private var directoryInfo = BackupDirectoryInfo(
        treeUri = null,
        displayName = null,
        hasPersistedPermission = false
    )
    private val backupEntries = backups.toMutableList()

    override suspend fun setBackupDirectory(treeUri: String): BackupDirectoryInfo {
        directoryInfo = BackupDirectoryInfo(
            treeUri = treeUri,
            displayName = "SharedBackups",
            hasPersistedPermission = true
        )
        return directoryInfo
    }

    override suspend fun getBackupDirectory(): BackupDirectoryInfo {
        return directoryInfo
    }

    override suspend fun listBackupFiles(treeUri: String): List<BackupDocumentEntry> {
        require(treeUri == directoryInfo.treeUri) { "Unknown tree uri: $treeUri" }
        return backupEntries.sortedByDescending { entry -> entry.lastModified }
    }

    override suspend fun copyLocalFileToDocument(
        localFilePath: String,
        targetTreeUri: String,
        targetName: String
    ): BackupDocumentEntry {
        require(targetTreeUri == directoryInfo.treeUri) { "Unknown tree uri: $targetTreeUri" }
        val source = File(localFilePath)
        require(source.exists()) { "Missing source: $localFilePath" }
        val entry = BackupDocumentEntry(
            uri = "content://backup/tree/$targetName",
            displayName = targetName,
            sizeBytes = source.length(),
            lastModified = System.currentTimeMillis()
        )
        backupEntries.add(entry)
        return entry
    }

    override suspend fun copyDocumentToTempFile(documentUri: String): File {
        val target = File(rootDir, "import_${documentUri.hashCode()}.zip")
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        return target
    }
}

private class FakeBackupService(
    private val rootDir: File = createTempDirectory(prefix = "settings_vm_service_").toFile(),
    private val importResult: BackupImportResult = BackupImportResult(
        sourceFilePath = "C:/temp/source.zip",
        importMode = BackupImportMode.REPLACE_ALL,
        importedAt = "2026-03-08T12:00:00Z",
        rollbackSnapshotPath = "C:/temp/rollback.zip",
        stats = BackupImportStats(categories = 1, items = 1, photos = 1),
        warnings = emptyList()
    ),
    private val importThrowable: Throwable? = null
) : BackupService {
    override suspend fun exportLocalBackup(exportMode: ExportMode): BackupExportResult {
        val localFile = File(
            rootDir,
            "backup_${System.currentTimeMillis()}_${exportMode.wireValue}.zip"
        )
        localFile.parentFile?.mkdirs()
        localFile.writeBytes(byteArrayOf(0x11, 0x12, 0x13))
        return BackupExportResult(
            filePath = localFile.absolutePath,
            fileSizeBytes = localFile.length(),
            exportMode = exportMode,
            createdAt = "2026-03-08T12:00:00Z",
            stats = BackupStats(categories = 1, items = 1, photos = 1)
        )
    }

    override suspend fun importLocalBackup(backupFilePath: String): BackupImportResult {
        if (importThrowable != null) {
            throw importThrowable
        }
        return importResult
    }
}
