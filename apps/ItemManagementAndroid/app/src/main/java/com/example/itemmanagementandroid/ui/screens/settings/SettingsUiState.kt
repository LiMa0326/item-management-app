package com.example.itemmanagementandroid.ui.screens.settings

import com.example.itemmanagementandroid.backup.storage.BackupDocumentEntry
import com.example.itemmanagementandroid.backup.export.ExportMode

data class SettingsUiState(
    val offlineFirstMessage: String = "Offline-first mode is active in V0.",
    val cloudBackupEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    val selectedBackupTreeUri: String? = null,
    val selectedBackupTreeLabel: String? = null,
    val hasPersistedPermission: Boolean = false,
    val selectedExportMode: ExportMode = ExportMode.FULL,
    val isExportingBackup: Boolean = false,
    val isLoadingImportableBackups: Boolean = false,
    val isImportingBackup: Boolean = false,
    val importableBackups: List<BackupDocumentEntry> = emptyList(),
    val exportMessage: String? = null,
    val exportErrorMessage: String? = null,
    val importMessage: String? = null,
    val importErrorMessage: String? = null,
    val importWarnings: List<String> = emptyList(),
    val pendingImportBackupUri: String? = null,
    val pendingImportBackupName: String? = null,
    val lastExportPath: String? = null,
    val lastExportAt: String? = null
)
