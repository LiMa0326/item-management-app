package com.example.itemmanagementandroid.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onExportModeSelected: (ExportMode) -> Unit,
    onBackupDirectorySelected: (String?) -> Unit,
    onExportBackupToSharedDirectory: () -> Unit,
    onRefreshImportableBackups: () -> Unit,
    onRequestImport: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onImportSingleDocument: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { selectedTreeUri ->
        onBackupDirectorySelected(selectedTreeUri?.toString())
    }
    val openSingleBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedDocumentUri ->
        onImportSingleDocument(selectedDocumentUri?.toString())
    }
    var isExportModeMenuExpanded by remember { mutableStateOf(false) }
    val operationEnabled = !state.isExportingBackup && !state.isImportingBackup

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(SettingsScreenTestTags.SCROLL_CONTAINER),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "Settings Screen", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Text(
                text = state.offlineFirstMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(
                text = "Cloud backup enabled: ${state.cloudBackupEnabled}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(
                text = "Sync enabled: ${state.syncEnabled}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(text = "Backup Directory", style = MaterialTheme.typography.titleSmall)
        }
        item {
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.BACKUP_DIRECTORY_TEXT),
                text = state.selectedBackupTreeLabel
                    ?.let { "Selected: $it" }
                    ?: "No backup directory selected.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.BACKUP_DIRECTORY_PERMISSION_TEXT),
                text = "Directory permission: ${if (state.hasPersistedPermission) "granted" else "missing"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.SELECT_DIRECTORY_BUTTON),
                enabled = operationEnabled,
                onClick = {
                    val initialUri = state.selectedBackupTreeUri?.let(Uri::parse)
                    openDocumentTreeLauncher.launch(initialUri)
                }
            ) {
                Text(text = "Select Backup Directory")
            }
        }
        item {
            Text(text = "Backup Export Mode", style = MaterialTheme.typography.titleSmall)
        }
        item {
            ExposedDropdownMenuBox(
                expanded = isExportModeMenuExpanded,
                onExpandedChange = { isExpanded ->
                    if (operationEnabled) {
                        isExportModeMenuExpanded = isExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag(SettingsScreenTestTags.EXPORT_MODE_DROPDOWN),
                    value = state.selectedExportMode.wireValue,
                    onValueChange = {},
                    readOnly = true,
                    enabled = operationEnabled,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExportModeMenuExpanded)
                    }
                )
                DropdownMenu(
                    expanded = isExportModeMenuExpanded,
                    onDismissRequest = { isExportModeMenuExpanded = false }
                ) {
                    ExportMode.entries.forEach { exportMode ->
                        DropdownMenuItem(
                            modifier = Modifier.testTag(
                                SettingsScreenTestTags.exportModeMenuItem(exportMode)
                            ),
                            text = { Text(text = exportMode.wireValue) },
                            onClick = {
                                isExportModeMenuExpanded = false
                                onExportModeSelected(exportMode)
                            }
                        )
                    }
                }
            }
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.EXPORT_BUTTON),
                enabled = operationEnabled,
                onClick = onExportBackupToSharedDirectory
            ) {
                Text(text = if (state.isExportingBackup) "Exporting..." else "Export To Shared Directory")
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.IMPORT_SINGLE_DOCUMENT_BUTTON),
                enabled = operationEnabled,
                onClick = {
                    openSingleBackupDocumentLauncher.launch(
                        arrayOf("application/zip", "application/octet-stream")
                    )
                }
            ) {
                Text(text = "Import From Single File")
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.REFRESH_IMPORTABLE_BACKUPS_BUTTON),
                enabled = operationEnabled,
                onClick = onRefreshImportableBackups
            ) {
                Text(text = if (state.isLoadingImportableBackups) "Refreshing..." else "Refresh Directory Backups")
            }
        }
        item {
            Text(
                text = "Importable backups in selected directory",
                style = MaterialTheme.typography.titleSmall
            )
        }
        if (state.importableBackups.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORTABLE_BACKUP_LIST),
                    text = "No backup zip files found.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            items(state.importableBackups, key = { entry -> entry.uri }) { entry ->
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SettingsScreenTestTags.importBackupButton(entry.uri)),
                    enabled = operationEnabled,
                    onClick = { onRequestImport(entry.uri) }
                ) {
                    Text(text = "Import ${entry.displayName}")
                }
            }
        }
        state.exportMessage?.let { exportMessage ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.EXPORT_MESSAGE_TEXT),
                    text = exportMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.exportErrorMessage?.let { exportErrorMessage ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.EXPORT_ERROR_TEXT),
                    text = exportErrorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.importMessage?.let { importMessage ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_MESSAGE_TEXT),
                    text = importMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        state.importErrorMessage?.let { importErrorMessage ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_ERROR_TEXT),
                    text = importErrorMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (state.importWarnings.isNotEmpty()) {
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_WARNING_HEADER_TEXT),
                    text = "Import warnings",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            items(state.importWarnings.size) { index ->
                val warning = state.importWarnings[index]
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.importWarningText(index)),
                    text = warning,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        state.lastExportPath?.let { lastExportPath ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.LAST_EXPORT_PATH_TEXT),
                    text = "Last export path: $lastExportPath",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        state.lastExportAt?.let { lastExportAt ->
            item {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.LAST_EXPORT_AT_TEXT),
                    text = "Last export at: $lastExportAt",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.HOME_BUTTON),
                onClick = { onNavigate(AppRoute.Home) }
            ) {
                Text(text = "Go To Home")
            }
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.BACK_BUTTON),
                enabled = canGoBack,
                onClick = onBack
            ) {
                Text(text = "Back")
            }
        }
    }

    if (state.pendingImportBackupUri != null) {
        AlertDialog(
            modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_CONFIRM_DIALOG),
            onDismissRequest = onCancelImport,
            title = { Text(text = "Confirm Import") },
            text = {
                Text(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_CONFIRM_DIALOG_TEXT),
                    text = "Import ${state.pendingImportBackupName ?: state.pendingImportBackupUri}? This will replace all local data."
                )
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_CONFIRM_BUTTON),
                    onClick = onConfirmImport
                ) {
                    Text(text = "Import")
                }
            },
            dismissButton = {
                OutlinedButton(
                    modifier = Modifier.testTag(SettingsScreenTestTags.IMPORT_CANCEL_BUTTON),
                    onClick = onCancelImport
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

object SettingsScreenTestTags {
    const val SCROLL_CONTAINER = "settings_scroll_container"
    const val SELECT_DIRECTORY_BUTTON = "settings_select_directory_button"
    const val BACKUP_DIRECTORY_TEXT = "settings_backup_directory_text"
    const val BACKUP_DIRECTORY_PERMISSION_TEXT = "settings_backup_directory_permission_text"
    const val REFRESH_IMPORTABLE_BACKUPS_BUTTON = "settings_refresh_importable_backups_button"
    const val IMPORT_SINGLE_DOCUMENT_BUTTON = "settings_import_single_document_button"
    const val IMPORTABLE_BACKUP_LIST = "settings_importable_backup_list"
    const val EXPORT_MODE_DROPDOWN = "settings_export_mode_dropdown"
    const val EXPORT_BUTTON = "settings_export_button"
    const val EXPORT_MESSAGE_TEXT = "settings_export_message_text"
    const val EXPORT_ERROR_TEXT = "settings_export_error_text"
    const val IMPORT_MESSAGE_TEXT = "settings_import_message_text"
    const val IMPORT_ERROR_TEXT = "settings_import_error_text"
    const val IMPORT_WARNING_HEADER_TEXT = "settings_import_warning_header_text"
    const val IMPORT_CONFIRM_DIALOG = "settings_import_confirm_dialog"
    const val IMPORT_CONFIRM_DIALOG_TEXT = "settings_import_confirm_dialog_text"
    const val IMPORT_CONFIRM_BUTTON = "settings_import_confirm_button"
    const val IMPORT_CANCEL_BUTTON = "settings_import_cancel_button"
    const val LAST_EXPORT_PATH_TEXT = "settings_last_export_path_text"
    const val LAST_EXPORT_AT_TEXT = "settings_last_export_at_text"
    const val HOME_BUTTON = "settings_home_button"
    const val BACK_BUTTON = "settings_back_button"

    fun exportModeMenuItem(exportMode: ExportMode): String {
        return "settings_export_mode_menu_item_${exportMode.name.lowercase()}"
    }

    fun importBackupButton(documentUri: String): String {
        return "settings_import_backup_${documentUri.hashCode()}"
    }

    fun importWarningText(index: Int): String {
        return "settings_import_warning_$index"
    }
}
