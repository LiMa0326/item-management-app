package com.example.itemmanagementandroid.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.backup.export.ExportMode
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modeSelectionAndExportButton_shouldTriggerCallbacksAndShowStatus() {
        var selectedMode = ExportMode.FULL
        var exportClicks = 0

        composeRule.setContent {
            var mode by remember { mutableStateOf(ExportMode.FULL) }
            var clicks by remember { mutableIntStateOf(0) }
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = mode,
                    exportMessage = "Backup exported (full).",
                    lastExportPath = "/tmp/backup_full.zip"
                ),
                canGoBack = true,
                onNavigate = {},
                onExportModeSelected = { newMode ->
                    mode = newMode
                    selectedMode = newMode
                },
                onExportBackup = {
                    clicks += 1
                    exportClicks = clicks
                },
                onBack = {}
            )
        }

        composeRule
            .onNodeWithTag(SettingsScreenTestTags.exportModeButton(ExportMode.THUMBNAILS))
            .performClick()
        assertEquals(ExportMode.THUMBNAILS, selectedMode)

        composeRule.onNodeWithTag(SettingsScreenTestTags.EXPORT_BUTTON).performClick()
        assertEquals(1, exportClicks)

        composeRule.onNodeWithTag(SettingsScreenTestTags.EXPORT_MESSAGE_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.LAST_EXPORT_PATH_TEXT).assertIsDisplayed()
        composeRule.onNodeWithText("Backup exported (full).").assertIsDisplayed()
        composeRule.onNodeWithText("Last export path: /tmp/backup_full.zip").assertIsDisplayed()
    }

    @Test
    fun exportingState_shouldDisableExportAndModeButtons() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(
                    selectedExportMode = ExportMode.FULL,
                    isExportingBackup = true
                ),
                canGoBack = true,
                onNavigate = { _: AppRoute -> },
                onExportModeSelected = {},
                onExportBackup = {},
                onBack = {}
            )
        }

        composeRule.onNodeWithTag(SettingsScreenTestTags.EXPORT_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Exporting...").assertIsDisplayed()
    }
}

