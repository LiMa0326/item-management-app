package com.example.itemmanagementandroid.ui.screens.itemedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.itemmanagementandroid.ui.components.UriImage
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    state: ItemEditUiState,
    canGoBack: Boolean,
    onBack: () -> Unit,
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
    onImportPhotoUris: (List<String>) -> Unit,
    onRetryFailedPhotoImports: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) {
            onImportPhotoUris(listOf(uri.toString()))
        } else if (uri != null) {
            runCatching { uri.path?.let(::File)?.delete() }
        }
    }

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportPhotoUris(uris.map(Uri::toString))
        }
    }

    fun launchCameraCapture() {
        val cameraFile = createCameraTempFile(context.cacheDir)
        val cameraUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cameraFile
        )
        pendingCameraUri = cameraUri
        takePictureLauncher.launch(cameraUri)
    }

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
        if (state.isImportingPhotos) {
            Text(
                modifier = Modifier.testTag(ItemEditScreenTestTags.PHOTO_IMPORT_STATUS_TEXT),
                text = "Importing photos...",
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.saveResultMessage?.let { saveResultMessage ->
            Text(
                text = saveResultMessage,
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.photoImportMessage?.let { photoImportMessage ->
            Text(
                modifier = Modifier.testTag(ItemEditScreenTestTags.PHOTO_IMPORT_MESSAGE_TEXT),
                text = photoImportMessage,
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.errorMessage?.let { errorMessage ->
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall)
        }

        Text(text = "Photos", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PHOTO_ACTIONS_ROW),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag(ItemEditScreenTestTags.PHOTO_IMPORT_CAMERA_BUTTON),
                enabled = !state.isLoading && !state.isSaving && !state.isImportingPhotos,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                onClick = ::launchCameraCapture
            ) {
                Text(
                    text = "Take Photo",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag(ItemEditScreenTestTags.PHOTO_IMPORT_PICK_LIBRARY_BUTTON),
                enabled = !state.isLoading && !state.isSaving && !state.isImportingPhotos,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                onClick = {
                    pickMultipleLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text(
                    text = "Pick From Library",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        if (state.photoImportFailures.isNotEmpty()) {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ItemEditScreenTestTags.PHOTO_IMPORT_RETRY_BUTTON),
                enabled = !state.isImportingPhotos && !state.isSaving,
                onClick = onRetryFailedPhotoImports
            ) {
                Text(text = "Retry Failed Imports (${state.photoImportFailures.size})")
            }
            state.photoImportFailures.forEach { failure ->
                Text(
                    text = "Failed: ${failure.reason}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ItemEditScreenTestTags.PHOTO_LIST),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.photos.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .size(96.dp)
                            .testTag(ItemEditScreenTestTags.EMPTY_PHOTO_CARD)
                    ) {
                        UriImage(
                            uri = null,
                            contentDescription = "No photo yet",
                            modifier = Modifier.fillMaxSize(),
                            placeholderText = "No Photos"
                        )
                    }
                }
            }
            items(state.photos, key = { photo -> photo.id }) { photo ->
                OutlinedCard(
                    modifier = Modifier
                        .size(96.dp)
                        .testTag(ItemEditScreenTestTags.photoCard(photo.id))
                ) {
                    UriImage(
                        uri = photo.thumbnailUri ?: photo.localUri,
                        contentDescription = "Item photo thumbnail",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.small),
                        placeholderText = "No Preview"
                    )
                }
            }
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
        state.fieldErrors.purchaseDate?.let { purchaseDateError ->
            Text(text = purchaseDateError, style = MaterialTheme.typography.bodySmall)
        }
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

        ExposedDropdownMenuBox(
            expanded = isCurrencyMenuExpanded,
            onExpandedChange = { isExpanded ->
                if (!state.isLoading && !state.isSaving && !state.isImportingPhotos) {
                    isCurrencyMenuExpanded = isExpanded
                }
            }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag(ItemEditScreenTestTags.PURCHASE_CURRENCY_DROPDOWN),
                value = state.purchaseCurrency,
                onValueChange = {},
                singleLine = true,
                readOnly = true,
                label = { Text(text = "Purchase Currency") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyMenuExpanded)
                }
            )
            DropdownMenu(
                expanded = isCurrencyMenuExpanded,
                onDismissRequest = { isCurrencyMenuExpanded = false }
            ) {
                PURCHASE_CURRENCY_OPTIONS.forEach { currencyCode ->
                    DropdownMenuItem(
                        modifier = Modifier.testTag(
                            ItemEditScreenTestTags.purchaseCurrencyOption(currencyCode)
                        ),
                        text = { Text(text = currencyCode) },
                        onClick = {
                            isCurrencyMenuExpanded = false
                            onPurchaseCurrencyChanged(currencyCode)
                        }
                    )
                }
            }
        }
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
                        .width(88.dp)
                        .testTag(ItemEditScreenTestTags.removeCustomAttributeButton(row.rowId)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = { onRemoveCustomAttributeRow(row.rowId) }
                ) {
                    Text(
                        text = "Remove",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = MaterialTheme.typography.labelSmall
                    )
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
                .testTag(ItemEditScreenTestTags.SAVE_BUTTON),
            enabled = !state.isLoading &&
                !state.isSaving &&
                !state.isImportingPhotos &&
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
            enabled = !state.isSaving && !state.isImportingPhotos,
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

private fun createCameraTempFile(cacheDir: File): File {
    val cameraDir = File(cacheDir, CAMERA_CAPTURE_DIRECTORY)
    if (!cameraDir.exists()) {
        cameraDir.mkdirs()
    }
    require(cameraDir.exists() && cameraDir.isDirectory) {
        "Failed to create camera capture directory."
    }
    val timestamp = CAMERA_FILE_TIME_FORMATTER.format(Instant.now())
    return File(cameraDir, "capture_$timestamp.jpg")
}

object ItemEditScreenTestTags {
    const val ROOT = "item_edit_root"
    const val PHOTO_ACTIONS_ROW = "item_edit_photo_actions_row"
    const val PHOTO_LIST = "item_edit_photo_list"
    const val EMPTY_PHOTO_CARD = "item_edit_photo_card_empty"
    const val PHOTO_IMPORT_CAMERA_BUTTON = "item_edit_photo_import_camera_button"
    const val PHOTO_IMPORT_PICK_LIBRARY_BUTTON = "item_edit_photo_import_pick_library_button"
    const val PHOTO_IMPORT_RETRY_BUTTON = "item_edit_photo_import_retry_button"
    const val PHOTO_IMPORT_STATUS_TEXT = "item_edit_photo_import_status_text"
    const val PHOTO_IMPORT_MESSAGE_TEXT = "item_edit_photo_import_message_text"
    const val CATEGORY_SECTION = "item_edit_category_section"
    const val NAME_INPUT = "item_edit_name_input"
    const val PURCHASE_DATE_INPUT = "item_edit_purchase_date_input"
    const val PURCHASE_PRICE_INPUT = "item_edit_purchase_price_input"
    const val PURCHASE_CURRENCY_DROPDOWN = "item_edit_purchase_currency_dropdown"
    const val PURCHASE_PLACE_INPUT = "item_edit_purchase_place_input"
    const val DESCRIPTION_INPUT = "item_edit_description_input"
    const val TAGS_INPUT = "item_edit_tags_input"
    const val ADD_CUSTOM_ATTRIBUTE_BUTTON = "item_edit_add_custom_attribute_button"
    const val SAVE_BUTTON = "item_edit_save_button"
    const val CANCEL_BUTTON = "item_edit_cancel_button"
    const val BACK_BUTTON = "item_edit_back_button"

    fun categoryOptionButton(categoryId: String): String = "item_edit_category_option_$categoryId"
    fun customAttributeRow(rowId: String): String = "item_edit_custom_attribute_row_$rowId"
    fun customAttributeKeyInput(rowId: String): String = "item_edit_custom_attribute_key_$rowId"
    fun customAttributeValueInput(rowId: String): String = "item_edit_custom_attribute_value_$rowId"
    fun removeCustomAttributeButton(rowId: String): String = "item_edit_custom_attribute_remove_$rowId"
    fun photoCard(photoId: String): String = "item_edit_photo_card_$photoId"
    fun purchaseCurrencyOption(currencyCode: String): String {
        return "item_edit_purchase_currency_option_${currencyCode.lowercase()}"
    }
}

private const val CAMERA_CAPTURE_DIRECTORY = "camera-captures"
private val PURCHASE_CURRENCY_OPTIONS = listOf(
    "USD",
    "CNY",
    "JPY",
    "EUR",
    "GBP",
    "HKD",
    "CAD",
    "AUD",
    "SGD",
    "KRW"
)

private val CAMERA_FILE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
        .withZone(ZoneOffset.UTC)
