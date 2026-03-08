package com.example.itemmanagementandroid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.backup.importing.BackupImportException
import com.example.itemmanagementandroid.backup.export.BackupExportException
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.backup.storage.BackupDocumentEntry
import com.example.itemmanagementandroid.backup.storage.BackupDirectoryInfo
import com.example.itemmanagementandroid.domain.usecase.backup.ExportBackupToSharedDirectoryUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.GetBackupDirectoryUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ImportBackupFromDocumentUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.ListImportableBackupsUseCase
import com.example.itemmanagementandroid.domain.usecase.backup.SetBackupDirectoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val setBackupDirectoryUseCase: SetBackupDirectoryUseCase,
    private val getBackupDirectoryUseCase: GetBackupDirectoryUseCase,
    private val listImportableBackupsUseCase: ListImportableBackupsUseCase,
    private val exportBackupToSharedDirectoryUseCase: ExportBackupToSharedDirectoryUseCase,
    private val importBackupFromDocumentUseCase: ImportBackupFromDocumentUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshDirectoryStateAndBackups()
        }
    }

    fun setExportMode(exportMode: ExportMode) {
        _uiState.update { current ->
            current.copy(
                selectedExportMode = exportMode,
                exportErrorMessage = null
            )
        }
    }

    fun onBackupDirectorySelected(treeUri: String?) {
        val normalizedTreeUri = treeUri?.trim().orEmpty()
        if (normalizedTreeUri.isEmpty()) {
            return
        }
        viewModelScope.launch {
            runCatching {
                setBackupDirectoryUseCase(normalizedTreeUri)
            }.onSuccess { directoryInfo ->
                _uiState.update { state ->
                    state.copy(
                        selectedBackupTreeUri = directoryInfo.treeUri,
                        selectedBackupTreeLabel = directoryInfo.displayName,
                        hasPersistedPermission = directoryInfo.hasPersistedPermission,
                        exportErrorMessage = null,
                        importErrorMessage = null
                    )
                }
                refreshImportableBackupsInternal()
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        exportErrorMessage = "Failed to save backup directory: ${throwable.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun refreshImportableBackups() {
        viewModelScope.launch {
            refreshImportableBackupsInternal()
        }
    }

    fun exportBackupToSharedDirectory() {
        val currentState = _uiState.value
        if (currentState.isExportingBackup || currentState.isImportingBackup) {
            return
        }

        val selectedTreeUri = currentState.selectedBackupTreeUri
        if (selectedTreeUri.isNullOrBlank() || !currentState.hasPersistedPermission) {
            _uiState.update { state ->
                state.copy(
                    exportErrorMessage = "Please select a backup directory first."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isExportingBackup = true,
                    exportMessage = null,
                    exportErrorMessage = null,
                    importErrorMessage = null
                )
            }

            try {
                val result = exportBackupToSharedDirectoryUseCase(
                    exportMode = _uiState.value.selectedExportMode,
                    targetTreeUri = selectedTreeUri
                )
                _uiState.update { state ->
                    state.copy(
                        isExportingBackup = false,
                        exportMessage = "Backup exported to shared directory (${result.exportMode.wireValue}).",
                        exportErrorMessage = null,
                        lastExportPath = result.document.uri,
                        lastExportAt = result.createdAt
                    )
                }
                refreshImportableBackupsInternal()
            } catch (throwable: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        isExportingBackup = false,
                        exportErrorMessage = toExportErrorMessage(throwable)
                    )
                }
            }
        }
    }

    fun requestImport(documentUri: String) {
        val normalizedDocumentUri = documentUri.trim()
        if (normalizedDocumentUri.isEmpty()) {
            return
        }
        val displayName = _uiState.value.importableBackups
            .firstOrNull { entry -> entry.uri == normalizedDocumentUri }
            ?.displayName ?: normalizedDocumentUri
        _uiState.update { state ->
            state.copy(
                pendingImportBackupUri = normalizedDocumentUri,
                pendingImportBackupName = displayName,
                importErrorMessage = null
            )
        }
    }

    fun importFromSingleDocument(documentUri: String?) {
        val normalizedDocumentUri = documentUri?.trim().orEmpty()
        if (normalizedDocumentUri.isEmpty()) {
            return
        }
        requestImport(normalizedDocumentUri)
    }

    fun confirmImport() {
        val state = _uiState.value
        val pendingUri = state.pendingImportBackupUri ?: return
        val pendingName = state.pendingImportBackupName ?: pendingUri
        performImport(
            documentUri = pendingUri,
            documentName = pendingName
        )
    }

    fun cancelImport() {
        _uiState.update { state ->
            state.copy(
                pendingImportBackupUri = null,
                pendingImportBackupName = null
            )
        }
    }

    private fun performImport(
        documentUri: String,
        documentName: String
    ) {
        val currentState = _uiState.value
        if (currentState.isImportingBackup || currentState.isExportingBackup) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isImportingBackup = true,
                    importMessage = null,
                    importWarnings = emptyList(),
                    importErrorMessage = null,
                    pendingImportBackupUri = null,
                    pendingImportBackupName = null
                )
            }

            try {
                val result = importBackupFromDocumentUseCase(documentUri)
                _uiState.update { state ->
                    state.copy(
                        isImportingBackup = false,
                        importMessage = "Backup imported: $documentName (categories=${result.stats.categories}, items=${result.stats.items}, photos=${result.stats.photos}).",
                        importWarnings = result.warnings.map { warning -> warning.message },
                        importErrorMessage = null
                    )
                }
                refreshImportableBackupsInternal()
            } catch (throwable: Throwable) {
                _uiState.update { state ->
                    state.copy(
                        isImportingBackup = false,
                        importErrorMessage = toImportErrorMessage(throwable)
                    )
                }
            }
        }
    }

    private suspend fun refreshDirectoryStateAndBackups() {
        val directoryInfo = runCatching {
            getBackupDirectoryUseCase()
        }.getOrElse { throwable ->
            _uiState.update { state ->
                state.copy(
                    exportErrorMessage = "Failed to load backup directory: ${throwable.message ?: "unknown error"}"
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                selectedBackupTreeUri = directoryInfo.treeUri,
                selectedBackupTreeLabel = directoryInfo.displayName,
                hasPersistedPermission = directoryInfo.hasPersistedPermission
            )
        }

        if (directoryInfo.treeUri != null && directoryInfo.hasPersistedPermission) {
            refreshImportableBackupsInternal()
        }
    }

    private suspend fun refreshImportableBackupsInternal() {
        val state = _uiState.value
        val treeUri = state.selectedBackupTreeUri
        if (treeUri.isNullOrBlank()) {
            _uiState.update { current ->
                current.copy(
                    importableBackups = emptyList(),
                    importErrorMessage = "Please select a backup directory first."
                )
            }
            return
        }
        if (!state.hasPersistedPermission) {
            _uiState.update { current ->
                current.copy(
                    importableBackups = emptyList(),
                    importErrorMessage = "Backup directory permission expired. Please re-select directory."
                )
            }
            return
        }

        _uiState.update { current ->
            current.copy(
                isLoadingImportableBackups = true,
                importErrorMessage = null
            )
        }

        runCatching {
            listImportableBackupsUseCase(treeUri)
        }.onSuccess { backups ->
            _uiState.update { current ->
                current.copy(
                    isLoadingImportableBackups = false,
                    importableBackups = backups
                )
            }
        }.onFailure { throwable ->
            _uiState.update { current ->
                current.copy(
                    isLoadingImportableBackups = false,
                    importableBackups = emptyList(),
                    importErrorMessage = "Failed to load backup files: ${throwable.message ?: "unknown error"}"
                )
            }
        }
    }

    private fun toExportErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is BackupExportException.MissingPhotoFile -> {
                "Backup failed: required photo file is missing."
            }
            is BackupExportException.InvalidParameter -> {
                "Backup failed: invalid export input."
            }
            is BackupExportException.IoFailure -> {
                "Backup failed: file I/O error."
            }
            else -> {
                throwable.message ?: "Backup failed due to unknown error."
            }
        }
    }

    private fun toImportErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is BackupImportException.InvalidParameter -> {
                "Import failed: invalid backup file."
            }
            is BackupImportException.InvalidPackage -> {
                "Import failed: backup package is invalid."
            }
            is BackupImportException.RollbackSnapshotFailed -> {
                "Import failed: cannot create rollback snapshot."
            }
            is BackupImportException.IoFailure -> {
                "Import failed: file I/O error."
            }
            else -> {
                throwable.message ?: "Import failed due to unknown error."
            }
        }
    }
}
