package com.example.itemmanagementandroid.ui.screens.itemdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    canGoBack: Boolean,
    onNavigate: (AppRoute) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSoftDelete: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Item Detail Screen", style = MaterialTheme.typography.headlineSmall)
        if (state.isLoading) {
            Text(text = "Loading item detail...", style = MaterialTheme.typography.bodySmall)
        }
        if (state.isApplyingAction) {
            Text(text = "Applying action...", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }
        state.actionMessage?.let { actionMessage ->
            Text(text = actionMessage, style = MaterialTheme.typography.bodySmall)
        }

        if (!state.hasItem) {
            Text(
                modifier = Modifier.testTag(ItemDetailScreenTestTags.EMPTY_STATE_TEXT),
                text = "No item available.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.FIELD_SECTION),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "Name: ${state.name.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Category ID: ${state.categoryId.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Purchase Date: ${state.purchaseDate ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Purchase Price: ${state.purchasePrice?.toString() ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Purchase Currency: ${state.purchaseCurrency ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Purchase Place: ${state.purchasePlace ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Description: ${state.description ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Tags: ${if (state.tags.isEmpty()) "-" else state.tags.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(text = "Custom Attributes", style = MaterialTheme.typography.titleSmall)
                if (state.customAttributes.isEmpty()) {
                    Text(text = "-", style = MaterialTheme.typography.bodySmall)
                } else {
                    state.customAttributes.forEach { (key, value) ->
                        Text(text = "$key = $value", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    modifier = Modifier.testTag(ItemDetailScreenTestTags.CREATED_AT_TEXT),
                    text = "Created At: ${state.createdAt ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    modifier = Modifier.testTag(ItemDetailScreenTestTags.UPDATED_AT_TEXT),
                    text = "Updated At: ${state.updatedAt ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Deleted At: ${state.deletedAt ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Photo Wall (${state.photos.size})",
                style = MaterialTheme.typography.titleSmall
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.PHOTO_WALL),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.photos, key = { photo -> photo.id }) { photo ->
                    OutlinedCard(
                        modifier = Modifier.testTag(ItemDetailScreenTestTags.photoCard(photo.id))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "ID: ${photo.id}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Type: ${photo.contentType}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Local: ${photo.localUri}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Thumb: ${photo.thumbnailUri ?: "-"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "W: ${photo.width ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "H: ${photo.height ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemDetailScreenTestTags.REFRESH_BUTTON),
            onClick = onRefresh
        ) {
            Text(text = "Refresh")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemDetailScreenTestTags.EDIT_BUTTON),
            onClick = { onNavigate(AppRoute.ItemEdit(itemId = state.selectedItemId)) }
        ) {
            Text(text = "Edit Item")
        }
        if (state.isDeleted) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.RESTORE_BUTTON),
                enabled = state.hasItem && !state.isApplyingAction,
                onClick = onRestore
            ) {
                Text(text = "Restore Item")
            }
        } else {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.DELETE_BUTTON),
                enabled = state.hasItem && !state.isApplyingAction,
                onClick = onSoftDelete
            ) {
                Text(text = "Delete Item")
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemDetailScreenTestTags.BACK_BUTTON),
            enabled = canGoBack,
            onClick = onBack
        ) {
            Text(text = "Back")
        }
    }
}

object ItemDetailScreenTestTags {
    const val EMPTY_STATE_TEXT = "item_detail_empty_state_text"
    const val FIELD_SECTION = "item_detail_field_section"
    const val PHOTO_WALL = "item_detail_photo_wall"
    const val CREATED_AT_TEXT = "item_detail_created_at_text"
    const val UPDATED_AT_TEXT = "item_detail_updated_at_text"
    const val REFRESH_BUTTON = "item_detail_refresh_button"
    const val EDIT_BUTTON = "item_detail_edit_button"
    const val DELETE_BUTTON = "item_detail_delete_button"
    const val RESTORE_BUTTON = "item_detail_restore_button"
    const val BACK_BUTTON = "item_detail_back_button"

    fun photoCard(photoId: String): String = "item_detail_photo_card_$photoId"
}
