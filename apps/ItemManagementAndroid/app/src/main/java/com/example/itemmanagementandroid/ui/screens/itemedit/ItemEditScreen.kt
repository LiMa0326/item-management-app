package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ItemEditScreen(
    state: ItemEditUiState,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNameChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPurchaseDateChanged: (String) -> Unit,
    onPurchasePriceChanged: (String) -> Unit,
    onPurchaseCurrencyChanged: (String) -> Unit,
    onPurchasePlaceChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onTagsInputChanged: (String) -> Unit,
    onCustomAttributeKeyChanged: (String, String) -> Unit,
    onCustomAttributeValueChanged: (String, String) -> Unit,
    onAddCustomAttributeRow: () -> Unit,
    onRemoveCustomAttributeRow: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
            .testTag(ItemEditScreenTestTags.ROOT),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Item Edit Screen", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Mode: ${state.mode.name}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Editing item id: ${state.editingItemId ?: "New item"}",
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.isLoading) {
            Text(text = "Loading edit context...", style = MaterialTheme.typography.bodySmall)
        }
        if (state.isSaving) {
            Text(text = "Saving item...", style = MaterialTheme.typography.bodySmall)
        }
        state.saveResultMessage?.let { saveResultMessage ->
            Text(
                text = saveResultMessage,
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }

        Text(text = "Category", style = MaterialTheme.typography.titleSmall)
        if (state.availableCategories.isEmpty()) {
            Text(text = "No categories available.", style = MaterialTheme.typography.bodySmall)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemEditScreenTestTags.CATEGORY_SECTION),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableCategories.forEach { category ->
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(ItemEditScreenTestTags.categoryOptionButton(category.id)),
                        onClick = { onCategorySelected(category.id) }
                    ) {
                        val selectedMark = if (state.categoryId == category.id) " *" else ""
                        val archivedMark = if (category.isArchived) " (Archived)" else ""
                        Text(text = "${category.name}$archivedMark$selectedMark")
                    }
                }
            }
        }
        state.fieldErrors.categoryId?.let { categoryError ->
            Text(text = categoryError, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.NAME_INPUT),
            value = state.name,
            onValueChange = onNameChanged,
            singleLine = true,
            label = { Text(text = "Name *") }
        )
        state.fieldErrors.name?.let { nameError ->
            Text(text = nameError, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PURCHASE_DATE_INPUT),
            value = state.purchaseDate,
            onValueChange = onPurchaseDateChanged,
            singleLine = true,
            label = { Text(text = "Purchase Date (YYYY-MM-DD)") }
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PURCHASE_PRICE_INPUT),
            value = state.purchasePriceInput,
            onValueChange = onPurchasePriceChanged,
            singleLine = true,
            label = { Text(text = "Purchase Price") }
        )
        state.fieldErrors.purchasePrice?.let { purchasePriceError ->
            Text(text = purchasePriceError, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PURCHASE_CURRENCY_INPUT),
            value = state.purchaseCurrency,
            onValueChange = onPurchaseCurrencyChanged,
            singleLine = true,
            label = { Text(text = "Purchase Currency") }
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PURCHASE_PLACE_INPUT),
            value = state.purchasePlace,
            onValueChange = onPurchasePlaceChanged,
            singleLine = true,
            label = { Text(text = "Purchase Place") }
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.DESCRIPTION_INPUT),
            value = state.description,
            onValueChange = onDescriptionChanged,
            label = { Text(text = "Description") }
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.TAGS_INPUT),
            value = state.tagsInput,
            onValueChange = onTagsInputChanged,
            singleLine = true,
            label = { Text(text = "Tags (comma separated)") }
        )

        Text(text = "Custom Attributes", style = MaterialTheme.typography.titleSmall)
        state.customAttributesRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemEditScreenTestTags.customAttributeRow(row.rowId)),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ItemEditScreenTestTags.customAttributeKeyInput(row.rowId)),
                    value = row.key,
                    onValueChange = { value ->
                        onCustomAttributeKeyChanged(row.rowId, value)
                    },
                    singleLine = true,
                    label = { Text(text = "Key") }
                )
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(ItemEditScreenTestTags.customAttributeValueInput(row.rowId)),
                    value = row.value,
                    onValueChange = { value ->
                        onCustomAttributeValueChanged(row.rowId, value)
                    },
                    singleLine = true,
                    label = { Text(text = "Value") }
                )
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(96.dp)
                        .testTag(ItemEditScreenTestTags.removeCustomAttributeButton(row.rowId)),
                    onClick = { onRemoveCustomAttributeRow(row.rowId) }
                ) {
                    Text(text = "Remove")
                }
            }
        }
        state.fieldErrors.customAttributes?.let { customAttributeError ->
            Text(text = customAttributeError, style = MaterialTheme.typography.bodySmall)
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.ADD_CUSTOM_ATTRIBUTE_BUTTON),
            onClick = onAddCustomAttributeRow
        ) {
            Text(text = "Add Custom Attribute")
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.REFRESH_BUTTON),
            onClick = onRefresh
        ) {
            Text(text = "Refresh")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.SAVE_BUTTON),
            enabled = !state.isLoading &&
                !state.isSaving &&
                state.name.trim().isNotEmpty() &&
                state.categoryId.trim().isNotEmpty(),
            onClick = onSave
        ) {
            Text(text = "Save")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.CANCEL_BUTTON),
            enabled = !state.isSaving,
            onClick = onCancel
        ) {
            Text(text = "Cancel")
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.BACK_BUTTON),
            enabled = canGoBack,
            onClick = onBack
        ) {
            Text(text = "Back")
        }
    }
}

object ItemEditScreenTestTags {
    const val ROOT = "item_edit_root"
    const val CATEGORY_SECTION = "item_edit_category_section"
    const val NAME_INPUT = "item_edit_name_input"
    const val PURCHASE_DATE_INPUT = "item_edit_purchase_date_input"
    const val PURCHASE_PRICE_INPUT = "item_edit_purchase_price_input"
    const val PURCHASE_CURRENCY_INPUT = "item_edit_purchase_currency_input"
    const val PURCHASE_PLACE_INPUT = "item_edit_purchase_place_input"
    const val DESCRIPTION_INPUT = "item_edit_description_input"
    const val TAGS_INPUT = "item_edit_tags_input"
    const val ADD_CUSTOM_ATTRIBUTE_BUTTON = "item_edit_add_custom_attribute_button"
    const val REFRESH_BUTTON = "item_edit_refresh_button"
    const val SAVE_BUTTON = "item_edit_save_button"
    const val CANCEL_BUTTON = "item_edit_cancel_button"
    const val BACK_BUTTON = "item_edit_back_button"

    fun categoryOptionButton(categoryId: String): String = "item_edit_category_option_$categoryId"
    fun customAttributeRow(rowId: String): String = "item_edit_custom_attribute_row_$rowId"
    fun customAttributeKeyInput(rowId: String): String = "item_edit_custom_attribute_key_$rowId"
    fun customAttributeValueInput(rowId: String): String = "item_edit_custom_attribute_value_$rowId"
    fun removeCustomAttributeButton(rowId: String): String = "item_edit_custom_attribute_remove_$rowId"
}
