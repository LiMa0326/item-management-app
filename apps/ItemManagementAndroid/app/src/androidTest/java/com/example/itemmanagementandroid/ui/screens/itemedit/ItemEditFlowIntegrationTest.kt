package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.MainActivity
import com.example.itemmanagementandroid.ui.components.AppPageScaffoldTestTags
import com.example.itemmanagementandroid.ui.screens.category.CategoryScreenTestTags
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreenTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemEditFlowIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun newItem_reenterShouldBeBlank_andDuplicateNameShouldBeRejected() {
        val uniqueName = "FlowItem_${System.currentTimeMillis()}"

        navigateToItemList()
        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON).performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        selectDefaultCategory()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement(uniqueName)
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON)
            .performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()

        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON).performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("")
            )
        )

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement("  ${uniqueName.uppercase()}  ")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT).performScrollTo()
        composeRule.onNodeWithText("Item name already exists.").assertIsDisplayed()
    }

    @Test
    fun saveFromItemEdit_navigatesToDetail_thenBackToItemList() {
        val uniqueName = "SavedItem_${System.currentTimeMillis()}"

        navigateToItemList()
        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON).performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        selectDefaultCategory()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement(uniqueName)
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.PURCHASE_DATE_INPUT)
            .performTextReplacement("2026-03-07")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.PURCHASE_PRICE_INPUT)
            .performTextReplacement("199.99")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Item Detail").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Item Detail").assertIsDisplayed()
        composeRule.onNodeWithText("Name: $uniqueName").assertIsDisplayed()

        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON)
            .performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
    }

    @Test
    fun editFromDetail_saveShouldAutoRefreshDetail() {
        val uniqueName = "DetailRefresh_${System.currentTimeMillis()}"

        navigateToItemList()
        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON).performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        selectDefaultCategory()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement(uniqueName)
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.PURCHASE_PLACE_INPUT)
            .performTextReplacement("Old Place")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Purchase Place: Old Place").assertIsDisplayed()

        composeRule.onNodeWithText("Edit Item").performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.PURCHASE_PLACE_INPUT)
            .performTextReplacement("New Place")
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Purchase Place: New Place").assertIsDisplayed()
    }

    @Test
    fun backToCategory_shouldAutoRefreshItemCount() {
        val uniqueName = "CategoryRefresh_${System.currentTimeMillis()}"

        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()

        val initialCount = readCategoryItemCount(categoryId = "cat_electronics")

        composeRule.onNodeWithTag(CategoryScreenTestTags.ALL_ITEMS_ROW).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_EDIT_BUTTON).performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()
        selectDefaultCategory()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement(uniqueName)
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()

        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON)
            .performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON).performClick()
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()

        val refreshedCount = readCategoryItemCount(categoryId = "cat_electronics")
        assertEquals(initialCount + 1, refreshedCount)
    }

    private fun navigateToItemList() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(CategoryScreenTestTags.ALL_ITEMS_ROW).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
    }

    private fun selectDefaultCategory() {
        val categoryButtonTag = ItemEditScreenTestTags.categoryOptionButton("cat_electronics")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(categoryButtonTag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule
            .onNodeWithTag(categoryButtonTag)
            .performScrollTo()
            .performClick()
    }

    private fun readCategoryItemCount(categoryId: String): Int {
        val countTextNode = composeRule.onNode(
            hasText("Items:", substring = true) and
                hasAnyAncestor(hasTestTag(CategoryScreenTestTags.categoryRow(categoryId))),
            useUnmergedTree = true
        )
        val rawText = countTextNode
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(separator = "") { annotated -> annotated.text }
        return rawText
            .substringAfter("Items:", missingDelimiterValue = "")
            .trim()
            .toInt()
    }
}
