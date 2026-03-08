package com.example.itemmanagementandroid.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onExportModeSelected: (ExportMode) -> Unit,
    onExportBackup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Settings Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = state.offlineFirstMessage,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Cloud backup enabled: ${state.cloudBackupEnabled}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Sync enabled: ${state.syncEnabled}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(text = "Backup Export Mode", style = MaterialTheme.typography.titleSmall)
        ExportMode.entries.forEach { exportMode ->
            val selectedMark = if (state.selectedExportMode == exportMode) " *" else ""
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SettingsScreenTestTags.exportModeButton(exportMode)),
                enabled = !state.isExportingBackup,
                onClick = { onExportModeSelected(exportMode) }
            ) {
                Text(text = "${exportMode.wireValue}$selectedMark")
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsScreenTestTags.EXPORT_BUTTON),
            enabled = !state.isExportingBackup,
            onClick = onExportBackup
        ) {
            Text(text = if (state.isExportingBackup) "Exporting..." else "Export Backup")
        }
        state.exportMessage?.let { exportMessage ->
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.EXPORT_MESSAGE_TEXT),
                text = exportMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.exportErrorMessage?.let { exportErrorMessage ->
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.EXPORT_ERROR_TEXT),
                text = exportErrorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        state.lastExportPath?.let { lastExportPath ->
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.LAST_EXPORT_PATH_TEXT),
                text = "Last export path: $lastExportPath",
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.lastExportAt?.let { lastExportAt ->
            Text(
                modifier = Modifier.testTag(SettingsScreenTestTags.LAST_EXPORT_AT_TEXT),
                text = "Last export at: $lastExportAt",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.Home) }
        ) {
            Text(text = "Go To Home")
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = canGoBack,
            onClick = onBack
        ) {
            Text(text = "Back")
        }
    }
}

object SettingsScreenTestTags {
    const val EXPORT_BUTTON = "settings_export_button"
    const val EXPORT_MESSAGE_TEXT = "settings_export_message_text"
    const val EXPORT_ERROR_TEXT = "settings_export_error_text"
    const val LAST_EXPORT_PATH_TEXT = "settings_last_export_path_text"
    const val LAST_EXPORT_AT_TEXT = "settings_last_export_at_text"

    fun exportModeButton(exportMode: ExportMode): String {
        return "settings_export_mode_${exportMode.name.lowercase()}"
    }
}
