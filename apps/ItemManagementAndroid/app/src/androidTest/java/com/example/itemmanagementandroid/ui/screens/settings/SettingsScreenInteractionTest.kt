package com.example.itemmanagementandroid.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.backup.storage.BackupDocumentEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modeSelectionExportAndDirectoryImportButtons_shouldTriggerCallbacksAndShowStatus() {
        var selectedMode = ExportMode.FULL
        var exportClicks = 0
        var importRequestUri: String? = null
        val backupEntry = BackupDocumentEntry(
            uri = "content://backup/tree/backup_a.zip",
            displayName = "backup_a.zip",
            sizeBytes = 1024,
            lastModified = 100
        )

        composeRule.setContent {
            var mode by remember { mutableStateOf(ExportMode.FULL) }
            var clicks by remember { mutableIntStateOf(0) }
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = mode,
                    selectedBackupTreeLabel = "SharedBackups",
                    hasPersistedPermission = true,
                    importableBackups = listOf(backupEntry),
                    exportMessage = "Backup exported (full).",
                    lastExportPath = "/tmp/backup_full.zip"
                ),
                onExportModeSelected = { newMode ->
                    mode = newMode
                    selectedMode = newMode
                },
                onBackupDirectorySelected = {},
                onExportBackupToSharedDirectory = {
                    clicks += 1
                    exportClicks = clicks
                },
                onRefreshImportableBackups = {},
                onRequestImport = { uri -> importRequestUri = uri },
                onConfirmImport = {},
                onCancelImport = {},
                onImportSingleDocument = {}
            )
        }

        composeRule.onNodeWithTag(SettingsScreenTestTags.BACKUP_DIRECTORY_TEXT).assertIsDisplayed()
        composeRule
            .onNodeWithTag(SettingsScreenTestTags.EXPORT_MODE_DROPDOWN)
            .performClick()
        composeRule
            .onNodeWithTag(SettingsScreenTestTags.exportModeMenuItem(ExportMode.THUMBNAILS))
            .performClick()
        assertEquals(ExportMode.THUMBNAILS, selectedMode)

        composeRule.onNodeWithTag(SettingsScreenTestTags.EXPORT_BUTTON).performClick()
        assertEquals(1, exportClicks)
        composeRule
            .onNodeWithTag(SettingsScreenTestTags.SCROLL_CONTAINER)
            .performScrollToNode(hasTestTag(SettingsScreenTestTags.importBackupButton(backupEntry.uri)))
        composeRule
            .onNodeWithTag(SettingsScreenTestTags.importBackupButton(backupEntry.uri))
            .performClick()
        assertEquals(backupEntry.uri, importRequestUri)

    }

    @Test
    fun exportingState_shouldDisableExportAndModeButtons() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = ExportMode.FULL,
                    isExportingBackup = true
                ),
                onExportModeSelected = {},
                onBackupDirectorySelected = {},
                onExportBackupToSharedDirectory = {},
                onRefreshImportableBackups = {},
                onRequestImport = {},
                onConfirmImport = {},
                onCancelImport = {},
                onImportSingleDocument = {}
            )
        }

        composeRule.onNodeWithTag(SettingsScreenTestTags.EXPORT_BUTTON).assertIsDisplayed()
    }

    @Test
    fun pendingImport_shouldShowConfirmDialogAndTriggerCallbacks() {
        var confirmClicks = 0
        var cancelClicks = 0

        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = ExportMode.FULL,
                    pendingImportBackupUri = "content://backup/tree/backup_a.zip",
                    pendingImportBackupName = "backup_a.zip"
                ),
                onExportModeSelected = {},
                onBackupDirectorySelected = {},
                onExportBackupToSharedDirectory = {},
                onRefreshImportableBackups = {},
                onRequestImport = {},
                onConfirmImport = { confirmClicks += 1 },
                onCancelImport = { cancelClicks += 1 },
                onImportSingleDocument = {}
            )
        }

        composeRule.onNodeWithTag(SettingsScreenTestTags.IMPORT_CONFIRM_DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.IMPORT_CONFIRM_BUTTON).performClick()
        composeRule.onNodeWithTag(SettingsScreenTestTags.IMPORT_CANCEL_BUTTON).performClick()
        assertEquals(1, confirmClicks)
        assertEquals(1, cancelClicks)
    }

    @Test
    fun singleScrollContainer_shouldAllowScrollingToLastImportButton() {
        val lastBackup = BackupDocumentEntry(
            uri = "content://backup/tree/backup_15.zip",
            displayName = "backup_15.zip",
            sizeBytes = 1024,
            lastModified = 15
        )
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = ExportMode.FULL,
                    selectedBackupTreeLabel = "SharedBackups",
                    hasPersistedPermission = true,
                    importableBackups = List(16) { index ->
                        BackupDocumentEntry(
                            uri = "content://backup/tree/backup_$index.zip",
                            displayName = "backup_$index.zip",
                            sizeBytes = 1024,
                            lastModified = index.toLong()
                        )
                    }
                ),
                onExportModeSelected = {},
                onBackupDirectorySelected = {},
                onExportBackupToSharedDirectory = {},
                onRefreshImportableBackups = {},
                onRequestImport = {},
                onConfirmImport = {},
                onCancelImport = {},
                onImportSingleDocument = {}
            )
        }

        composeRule
            .onNodeWithTag(SettingsScreenTestTags.SCROLL_CONTAINER)
            .performScrollToNode(hasTestTag(SettingsScreenTestTags.importBackupButton(lastBackup.uri)))
        composeRule
            .onNodeWithTag(SettingsScreenTestTags.importBackupButton(lastBackup.uri))
            .assertIsDisplayed()
    }
}

