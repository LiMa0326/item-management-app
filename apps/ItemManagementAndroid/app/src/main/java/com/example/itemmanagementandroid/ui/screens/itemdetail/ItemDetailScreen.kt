package com.example.itemmanagementandroid.ui.screens.itemdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.itemmanagementandroid.ui.components.UriImage
import com.example.itemmanagementandroid.ui.navigation.AppRoute

@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onNavigate: (AppRoute) -> Unit,
    onSoftDelete: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Item Detail Screen", style = MaterialTheme.typography.headlineSmall)
        buildStatusLine(state)?.let { statusLine ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.STATUS_LINE),
                text = statusLine,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!state.hasItem) {
            Text(
                modifier = Modifier.testTag(ItemDetailScreenTestTags.EMPTY_STATE_TEXT),
                text = "No item available.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            PhotoSection(photos = state.photos)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.FIELD_SECTION),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionCard(
                    title = "Basic Info",
                    tag = ItemDetailScreenTestTags.BASIC_INFO_CARD
                ) {
                    DetailRow(label = "Name", value = state.name.orEmpty())
                    DetailRow(label = "Category ID", value = state.categoryId.orEmpty())
                    DetailRow(label = "Description", value = state.description ?: "-")
                }

                SectionCard(
                    title = "Purchase Info",
                    tag = ItemDetailScreenTestTags.PURCHASE_INFO_CARD
                ) {
                    DetailRow(label = "Purchase Date", value = state.purchaseDate ?: "-")
                    DetailRow(label = "Purchase Price", value = state.purchasePrice?.toString() ?: "-")
                    DetailRow(label = "Purchase Currency", value = state.purchaseCurrency ?: "-")
                    DetailRow(label = "Purchase Place", value = state.purchasePlace ?: "-")
                }

                SectionCard(
                    title = "Extended Info",
                    tag = ItemDetailScreenTestTags.EXTENDED_INFO_CARD
                ) {
                    DetailRow(
                        label = "Tags",
                        value = if (state.tags.isEmpty()) "-" else state.tags.joinToString()
                    )
                    if (state.customAttributes.isEmpty()) {
                        DetailRow(label = "Custom Attributes", value = "-")
                    } else {
                        state.customAttributes.forEach { (key, value) ->
                            DetailRow(label = key, value = value.toString())
                        }
                    }
                }

                SectionCard(
                    title = "System Info",
                    tag = ItemDetailScreenTestTags.SYSTEM_INFO_CARD
                ) {
                    DetailRow(
                        modifier = Modifier.testTag(ItemDetailScreenTestTags.CREATED_AT_TEXT),
                        label = "Created At",
                        value = state.createdAt ?: "-"
                    )
                    DetailRow(
                        modifier = Modifier.testTag(ItemDetailScreenTestTags.UPDATED_AT_TEXT),
                        label = "Updated At",
                        value = state.updatedAt ?: "-"
                    )
                    DetailRow(label = "Deleted At", value = state.deletedAt ?: "-")
                }
            }
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
    }
}

@Composable
private fun PhotoSection(photos: List<ItemDetailPhotoUiModel>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ItemDetailScreenTestTags.PHOTO_SECTION),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Photos (${photos.size})",
            style = MaterialTheme.typography.titleSmall
        )
        if (photos.isEmpty()) {
            Text(text = "No photos.", style = MaterialTheme.typography.bodySmall)
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemDetailScreenTestTags.PHOTO_WALL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemDetailScreenTestTags.PHOTO_STACK),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.forEach { photo ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ItemDetailScreenTestTags.photoCard(photo.id))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UriImage(
                                uri = photo.displayUri,
                                contentDescription = "Item detail image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(MaterialTheme.shapes.small),
                                placeholderText = "No Image"
                            )
                            DetailRow(label = "Photo ID", value = photo.id)
                            DetailRow(label = "Type", value = photo.contentType)
                            DetailRow(
                                label = "Resolution",
                                value = "${photo.width ?: "-"} x ${photo.height ?: "-"}"
                            )
                            DetailRow(label = "Local", value = photo.localUri)
                            DetailRow(label = "Thumb", value = photo.thumbnailUri ?: "-")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    tag: String,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun buildStatusLine(state: ItemDetailUiState): String? {
    val segments = mutableListOf<String>()
    if (state.isLoading) {
        segments += "Loading item detail"
    }
    if (state.isApplyingAction) {
        segments += "Applying action"
    }
    state.errorMessage?.let { error ->
        segments += "Error: $error"
    }
    state.actionMessage?.let { message ->
        segments += message
    }

    if (segments.isEmpty()) {
        return null
    }
    return segments.joinToString(separator = " | ")
}

object ItemDetailScreenTestTags {
    const val EMPTY_STATE_TEXT = "item_detail_empty_state_text"
    const val STATUS_LINE = "item_detail_status_line"
    const val PHOTO_SECTION = "item_detail_photo_section"
    const val PHOTO_STACK = "item_detail_photo_stack"
    const val FIELD_SECTION = "item_detail_field_section"
    const val PHOTO_WALL = "item_detail_photo_wall"
    const val BASIC_INFO_CARD = "item_detail_basic_info_card"
    const val PURCHASE_INFO_CARD = "item_detail_purchase_info_card"
    const val EXTENDED_INFO_CARD = "item_detail_extended_info_card"
    const val SYSTEM_INFO_CARD = "item_detail_system_info_card"
    const val CREATED_AT_TEXT = "item_detail_created_at_text"
    const val UPDATED_AT_TEXT = "item_detail_updated_at_text"
    const val EDIT_BUTTON = "item_detail_edit_button"
    const val DELETE_BUTTON = "item_detail_delete_button"
    const val RESTORE_BUTTON = "item_detail_restore_button"

    fun photoCard(photoId: String): String = "item_detail_photo_card_$photoId"
}
