package com.example.itemmanagementandroid.ui.screens.itemdetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemDetailScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fullFieldsAndPhotoWall_areDisplayed() {
        composeRule.setContent {
            ItemDetailScreen(
                state = baseState(),
                canGoBack = true,
                onNavigate = {},
                onBack = {},
                onRefresh = {},
                onSoftDelete = {},
                onRestore = {}
            )
        }

        composeRule.onNodeWithTag(ItemDetailScreenTestTags.FIELD_SECTION).assertIsDisplayed()
        composeRule.onNodeWithTag(ItemDetailScreenTestTags.CREATED_AT_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(ItemDetailScreenTestTags.UPDATED_AT_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(ItemDetailScreenTestTags.PHOTO_WALL).assertIsDisplayed()
        composeRule
            .onNodeWithTag(ItemDetailScreenTestTags.photoCard("photo_1"))
            .assertIsDisplayed()
    }

    @Test
    fun deleteAction_activeItem_showsDeleteAndTriggersCallback() {
        var deleteTriggered = false

        composeRule.setContent {
            ItemDetailScreen(
                state = baseState().copy(deletedAt = null),
                canGoBack = true,
                onNavigate = {},
                onBack = {},
                onRefresh = {},
                onSoftDelete = { deleteTriggered = true },
                onRestore = {}
            )
        }

        composeRule.onNodeWithTag(ItemDetailScreenTestTags.DELETE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(ItemDetailScreenTestTags.DELETE_BUTTON).performClick()

        composeRule.runOnIdle {
            assertTrue(deleteTriggered)
        }
    }

    @Test
    fun restoreAction_deletedItem_showsRestoreAndTriggersCallback() {
        var restoreTriggered = false

        composeRule.setContent {
            ItemDetailScreen(
                state = baseState().copy(deletedAt = "2026-03-07T10:00:00Z"),
                canGoBack = true,
                onNavigate = {},
                onBack = {},
                onRefresh = {},
                onSoftDelete = {},
                onRestore = { restoreTriggered = true }
            )
        }

        composeRule.onNodeWithTag(ItemDetailScreenTestTags.RESTORE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(ItemDetailScreenTestTags.RESTORE_BUTTON).performClick()

        composeRule.runOnIdle {
            assertTrue(restoreTriggered)
        }
    }

    private fun baseState(): ItemDetailUiState {
        return ItemDetailUiState(
            isLoading = false,
            selectedItemId = "item_1",
            categoryId = "cat_electronics",
            name = "Sony WH-1000XM5",
            purchaseDate = "2025-10-12",
            purchasePrice = 329.99,
            purchaseCurrency = "USD",
            purchasePlace = "Amazon",
            description = "Noise cancelling headphone for commute",
            tags = listOf("audio", "commute"),
            customAttributes = mapOf(
                "color" to "Black",
                "warrantyYears" to 2
            ),
            createdAt = "2026-03-01T10:00:00Z",
            updatedAt = "2026-03-07T10:00:00Z",
            deletedAt = null,
            photos = listOf(
                ItemDetailPhotoUiModel(
                    id = "photo_1",
                    contentType = "image/jpeg",
                    localUri = "file:///photos/photo_1.jpg",
                    thumbnailUri = "file:///photos/photo_1_thumb.jpg",
                    width = 1200,
                    height = 1600
                )
            )
        )
    }
}
