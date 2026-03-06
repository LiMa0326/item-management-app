package com.example.itemmanagementandroid.ui.screens.settings

data class SettingsUiState(
    val offlineFirstMessage: String = "Offline-first mode is active in V0.",
    val cloudBackupEnabled: Boolean = false,
    val syncEnabled: Boolean = false
)
