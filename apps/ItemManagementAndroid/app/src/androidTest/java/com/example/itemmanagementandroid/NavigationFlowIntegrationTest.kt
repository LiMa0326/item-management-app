package com.example.itemmanagementandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.ui.components.AppPageScaffoldTestTags
import com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailScreenTestTags
import com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditScreenTestTags
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

        composeRule.onNodeWithText("Go To Item List").performClick()
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

        composeRule.onNodeWithText("Go To Item List").performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Include deleted: false").assertIsDisplayed()

        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).performClick()
        composeRule
            .onNodeWithTag(AppPageScaffoldTestTags.overflowAction("toggle_include_deleted"))
            .performClick()
        composeRule.onNodeWithText("Include deleted: true").assertIsDisplayed()
    }
}
