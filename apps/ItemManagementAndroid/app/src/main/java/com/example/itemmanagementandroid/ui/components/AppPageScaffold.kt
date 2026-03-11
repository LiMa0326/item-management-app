package com.example.itemmanagementandroid.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

data class AppOverflowAction(
    val id: String,
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPageScaffold(
    title: String,
    showBackButton: Boolean,
    onBack: (() -> Unit)?,
    onRefresh: () -> Unit,
    overflowActions: List<AppOverflowAction>,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    var isOverflowMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag(AppPageScaffoldTestTags.TOP_BAR),
                title = { Text(text = title) },
                navigationIcon = {
                    if (showBackButton && onBack != null) {
                        IconButton(
                            modifier = Modifier.testTag(AppPageScaffoldTestTags.BACK_BUTTON),
                            onClick = onBack
                        ) {
                            Text(text = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON),
                        onClick = { isOverflowMenuExpanded = true }
                    ) {
                        Text(text = "More")
                    }
                    DropdownMenu(
                        modifier = Modifier.testTag(AppPageScaffoldTestTags.OVERFLOW_MENU),
                        expanded = isOverflowMenuExpanded,
                        onDismissRequest = { isOverflowMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.testTag(AppPageScaffoldTestTags.OVERFLOW_REFRESH_ACTION),
                            text = { Text(text = "Refresh") },
                            onClick = {
                                isOverflowMenuExpanded = false
                                onRefresh()
                            }
                        )
                        overflowActions.forEach { action ->
                            DropdownMenuItem(
                                modifier = Modifier.testTag(
                                    AppPageScaffoldTestTags.overflowAction(action.id)
                                ),
                                text = { Text(text = action.label) },
                                onClick = {
                                    isOverflowMenuExpanded = false
                                    action.onClick()
                                }
                            )
                        }
                    }
                }
            )
        },
        content = content
    )
}

object AppPageScaffoldTestTags {
    const val TOP_BAR = "app_page_top_bar"
    const val BACK_BUTTON = "app_page_back_button"
    const val OVERFLOW_BUTTON = "app_page_overflow_button"
    const val OVERFLOW_MENU = "app_page_overflow_menu"
    const val OVERFLOW_REFRESH_ACTION = "app_page_overflow_refresh_action"

    fun overflowAction(actionId: String): String {
        return "app_page_overflow_action_$actionId"
    }
}
