package com.example.itemmanagementandroid.ui.screens.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onNavigate: (AppRoute) -> Unit,
    onCreateCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onSetArchived: (String, Boolean) -> Unit,
    onMoveCategoryUp: (String) -> Unit,
    onMoveCategoryDown: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createCategoryName by rememberSaveable { mutableStateOf("") }
    var renameTargetCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameCategoryName by rememberSaveable { mutableStateOf("") }

    val renameTarget = state.categories.firstOrNull { category ->
        category.id == renameTargetCategoryId
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Category Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Loaded categories: ${state.categories.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Include archived: ${state.includeArchived}",
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.isLoading) {
            Text(text = "Loading categories...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CategoryScreenTestTags.CREATE_CATEGORY_BUTTON),
            onClick = { showCreateDialog = true }
        ) {
            Text(text = "Create Category")
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag(CategoryScreenTestTags.CATEGORY_LIST),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = state.categories,
                key = { _, item -> item.id }
            ) { index, category ->
                CategoryRow(
                    category = category,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.categories.lastIndex,
                    onRename = {
                        renameTargetCategoryId = category.id
                        renameCategoryName = category.name
                    },
                    onToggleArchived = {
                        onSetArchived(
                            category.id,
                            !category.isArchived
                        )
                    },
                    onMoveUp = { onMoveCategoryUp(category.id) },
                    onMoveDown = { onMoveCategoryDown(category.id) }
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigate(AppRoute.ItemList) }
        ) {
            Text(text = "Go To Item List")
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = "Create Category") },
            text = {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CategoryScreenTestTags.CREATE_CATEGORY_INPUT),
                    value = createCategoryName,
                    onValueChange = { value ->
                        createCategoryName = value
                    },
                    label = { Text(text = "Name") }
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(CategoryScreenTestTags.CREATE_CATEGORY_CONFIRM_BUTTON),
                    enabled = createCategoryName.trim().isNotEmpty(),
                    onClick = {
                        onCreateCategory(createCategoryName)
                        createCategoryName = ""
                        showCreateDialog = false
                    }
                ) {
                    Text(text = "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTargetCategoryId = null },
            title = { Text(text = "Rename Category") },
            text = {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CategoryScreenTestTags.RENAME_CATEGORY_INPUT),
                    value = renameCategoryName,
                    onValueChange = { value ->
                        renameCategoryName = value
                    },
                    label = { Text(text = "Name") }
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag(CategoryScreenTestTags.RENAME_CATEGORY_CONFIRM_BUTTON),
                    enabled = renameCategoryName.trim().isNotEmpty(),
                    onClick = {
                        onRenameCategory(
                            renameTarget.id,
                            renameCategoryName
                        )
                        renameTargetCategoryId = null
                    }
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetCategoryId = null }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryListItemUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onToggleArchived: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CategoryScreenTestTags.categoryRow(category.id))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (category.isSystemDefault) {
                Text(
                    text = "System Default",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (category.isArchived) {
                Text(
                    text = "Archived",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Text(
            text = "Items: ${category.itemCount}",
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.testTag(CategoryScreenTestTags.renameButton(category.id)),
                onClick = onRename
            ) {
                Text(text = "Rename")
            }
            OutlinedButton(
                modifier = Modifier.testTag(CategoryScreenTestTags.archiveButton(category.id)),
                onClick = onToggleArchived
            ) {
                Text(text = if (category.isArchived) "Unarchive" else "Archive")
            }
            OutlinedButton(
                modifier = Modifier.testTag(CategoryScreenTestTags.moveUpButton(category.id)),
                enabled = canMoveUp,
                onClick = onMoveUp
            ) {
                Text(text = "Up")
            }
            OutlinedButton(
                modifier = Modifier.testTag(CategoryScreenTestTags.moveDownButton(category.id)),
                enabled = canMoveDown,
                onClick = onMoveDown
            ) {
                Text(text = "Down")
            }
        }
    }
}

object CategoryScreenTestTags {
    const val CATEGORY_LIST = "category_list"
    const val CREATE_CATEGORY_BUTTON = "create_category_button"
    const val CREATE_CATEGORY_INPUT = "create_category_input"
    const val CREATE_CATEGORY_CONFIRM_BUTTON = "create_category_confirm_button"
    const val RENAME_CATEGORY_INPUT = "rename_category_input"
    const val RENAME_CATEGORY_CONFIRM_BUTTON = "rename_category_confirm_button"

    fun categoryRow(categoryId: String): String = "category_row_$categoryId"
    fun renameButton(categoryId: String): String = "rename_button_$categoryId"
    fun archiveButton(categoryId: String): String = "archive_button_$categoryId"
    fun moveUpButton(categoryId: String): String = "move_up_button_$categoryId"
    fun moveDownButton(categoryId: String): String = "move_down_button_$categoryId"
}
