package com.example.itemmanagementandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.ui.components.AppPageScaffoldTestTags
import com.example.itemmanagementandroid.ui.screens.category.CategoryScreenTestTags
import com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailScreenTestTags
import com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditScreenTestTags
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigateMainFlowAndBack() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).assertIsDisplayed()

        composeRule.onNodeWithTag(CategoryScreenTestTags.ALL_ITEMS_ROW).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()

        composeRule.onNodeWithText("Go To Item Detail").performClick()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()

        composeRule
            .onNodeWithTag(ItemDetailScreenTestTags.EDIT_BUTTON)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(ItemEditScreenTestTags.ROOT).assertIsDisplayed()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.CANCEL_BUTTON)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_refreshAndSettingsNavigation_workAsExpected() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).performClick()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_REFRESH_ACTION).assertIsDisplayed()
        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.overflowAction("open_settings"))
            .performClick()

        composeRule.onNodeWithText("Settings Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).performClick()
        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.overflowAction("back_to_category"))
            .performClick()

        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_toggleActions_updateCategoryAndItemListState() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Include archived: false").assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).performClick()
        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.overflowAction("toggle_include_archived"))
            .performClick()
        composeRule.onNodeWithText("Include archived: true").assertIsDisplayed()

        composeRule.onNodeWithTag(CategoryScreenTestTags.ALL_ITEMS_ROW).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Include deleted: false | Loaded items:", substring = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).performClick()
        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.overflowAction("toggle_include_deleted"))
            .performClick()
        composeRule.onNodeWithText("Include deleted: true | Loaded items:", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun categoryRowNavigation_appliesPrefilter_thenManualFilterPersistsOnBack() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()

        composeRule
            .onNodeWithTag(CategoryScreenTestTags.categoryRow("cat_electronics"))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithTag(ItemListScreenTestTags.categoryFilterButton("cat_electronics"))
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertIsSelected()
                true
            } catch (_: Throwable) {
                false
            }
        }

        composeRule.onNodeWithTag(ItemListScreenTestTags.CATEGORY_FILTER_ALL_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithTag(ItemListScreenTestTags.CATEGORY_FILTER_ALL_BUTTON)
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertIsSelected()
                true
            } catch (_: Throwable) {
                false
            }
        }
        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_DETAIL_BUTTON).performClick()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.BACK_BUTTON).performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(ItemListScreenTestTags.CATEGORY_FILTER_ALL_BUTTON)
            .assertIsDisplayed()
            .assertIsSelected()
    }
}
