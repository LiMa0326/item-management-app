package com.example.itemmanagementandroid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagementandroid.backup.export.BackupExportException
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.domain.usecase.backup.ExportLocalBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val exportLocalBackupUseCase: ExportLocalBackupUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setExportMode(exportMode: ExportMode) {
        _uiState.update { current ->
            current.copy(
                selectedExportMode = exportMode,
                exportErrorMessage = null
            )
        }
    }

    fun exportBackup() {
        val currentState = _uiState.value
        if (currentState.isExportingBackup) {
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isExportingBackup = true,
                    exportMessage = null,
                    exportErrorMessage = null
                )
            }

            try {
                val result = exportLocalBackupUseCase(
                    exportMode = _uiState.value.selectedExportMode
                )
                _uiState.update { state ->
                    state.copy(
                        isExportingBackup = false,
                        exportMessage = "Backup exported (${result.exportMode.wireValue}).",
                        exportErrorMessage = null,
                        lastExportPath = result.filePath,
                        lastExportAt = result.createdAt
                    )
                }
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
}
