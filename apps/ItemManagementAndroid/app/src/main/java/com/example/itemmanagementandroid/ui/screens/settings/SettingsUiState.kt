package com.example.itemmanagementandroid.ui.screens.settings

import com.example.itemmanagementandroid.backup.export.ExportMode

data class SettingsUiState(
    val offlineFirstMessage: String = "Offline-first mode is active in V0.",
    val cloudBackupEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    val selectedExportMode: ExportMode = ExportMode.FULL,
    val isExportingBackup: Boolean = false,
    val exportMessage: String? = null,
    val exportErrorMessage: String? = null,
    val lastExportPath: String? = null,
    val lastExportAt: String? = null
)
