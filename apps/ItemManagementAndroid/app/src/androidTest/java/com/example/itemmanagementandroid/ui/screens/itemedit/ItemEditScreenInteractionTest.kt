package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemEditScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveButton_disabledWhenNameIsBlank() {
        setContent(
            initialState = baseState().copy(name = "")
        )

        composeRule.onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun saveButton_withValidName_triggersSaveCallback() {
        var saveTriggered = false

        setContent(
            onSave = { saveTriggered = true }
        )

        composeRule.onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON).performScrollTo().performClick()
        composeRule.runOnIdle {
            assertTrue(saveTriggered)
        }
    }

    @Test
    fun extendedFieldsInput_andSave_triggerCallbacks() {
        var saveTriggered = false

        setContent(
            onSave = { saveTriggered = true }
        )

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.TAGS_INPUT)
            .performScrollTo()
            .performTextReplacement("work, travel")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.customAttributeKeyInput("row_0"))
            .performScrollTo()
            .performTextReplacement("color")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.customAttributeValueInput("row_0"))
            .performScrollTo()
            .performTextReplacement("black")

        composeRule.onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON).performScrollTo().performClick()

        composeRule.runOnIdle {
            assertTrue(saveTriggered)
        }
    }

    @Test
    fun retryFailedImportsButton_isVisibleAndTriggersCallback() {
        var retryTriggered = false

        setContent(
            initialState = baseState().copy(
                photoImportFailures = listOf(
                    ItemEditPhotoImportFailureUiModel(
                        sourceUri = "content://mock/fail",
                        reason = "mock failure"
                    )
                )
            ),
            onRetryFailedPhotoImports = { retryTriggered = true }
        )

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.PHOTO_IMPORT_RETRY_BUTTON)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(retryTriggered)
        }
    }

    private fun setContent(
        initialState: ItemEditUiState = baseState(),
        onTagsInputChanged: (String) -> Unit = {},
        onCustomAttributeKeyChanged: (String, String) -> Unit = { _, _ -> },
        onCustomAttributeValueChanged: (String, String) -> Unit = { _, _ -> },
        onRetryFailedPhotoImports: () -> Unit = {},
        onSave: () -> Unit = {}
    ) {
        composeRule.setContent {
            var state by mutableStateOf(initialState)

            ItemEditScreen(
                state = state,
                canGoBack = true,
                onBack = {},
                onRefresh = {},
                onNameChanged = { value ->
                    state = state.copy(name = value)
                },
                onCategorySelected = { categoryId ->
                    state = state.copy(categoryId = categoryId)
                },
                onPurchaseDateChanged = { value ->
                    state = state.copy(purchaseDate = value)
                },
                onPurchasePriceChanged = { value ->
                    state = state.copy(purchasePriceInput = value)
                },
                onPurchaseCurrencyChanged = { value ->
                    state = state.copy(purchaseCurrency = value)
                },
                onPurchasePlaceChanged = { value ->
                    state = state.copy(purchasePlace = value)
                },
                onDescriptionChanged = { value ->
                    state = state.copy(description = value)
                },
                onTagsInputChanged = { value ->
                    state = state.copy(tagsInput = value)
                    onTagsInputChanged(value)
                },
                onCustomAttributeKeyChanged = { rowId, value ->
                    state = state.copy(
                        customAttributesRows = state.customAttributesRows.map { row ->
                            if (row.rowId == rowId) row.copy(key = value) else row
                        }
                    )
                    onCustomAttributeKeyChanged(rowId, value)
                },
                onCustomAttributeValueChanged = { rowId, value ->
                    state = state.copy(
                        customAttributesRows = state.customAttributesRows.map { row ->
                            if (row.rowId == rowId) row.copy(value = value) else row
                        }
                    )
                    onCustomAttributeValueChanged(rowId, value)
                },
                onAddCustomAttributeRow = {
                    state = state.copy(
                        customAttributesRows = state.customAttributesRows +
                            ItemEditCustomAttributeRowUiModel(rowId = "row_new")
                    )
                },
                onRemoveCustomAttributeRow = { rowId ->
                    state = state.copy(
                        customAttributesRows = state.customAttributesRows.filterNot { it.rowId == rowId }
                    )
                },
                onImportPhotoUris = {},
                onRetryFailedPhotoImports = onRetryFailedPhotoImports,
                onSave = onSave,
                onCancel = {}
            )
        }
    }

    private fun baseState(): ItemEditUiState {
        return ItemEditUiState(
            isLoading = false,
            mode = ItemEditMode.CREATE,
            availableCategories = listOf(
                ItemEditCategoryOptionUiModel(
                    id = "cat_electronics",
                    name = "Electronics",
                    isArchived = false
                )
            ),
            categoryId = "cat_electronics",
            name = "Headphone",
            customAttributesRows = listOf(
                ItemEditCustomAttributeRowUiModel(rowId = "row_0")
            )
        )
    }
}
