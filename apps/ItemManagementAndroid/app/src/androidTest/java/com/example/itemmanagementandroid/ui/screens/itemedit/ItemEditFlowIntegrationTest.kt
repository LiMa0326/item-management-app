package com.example.itemmanagementandroid.ui.screens.itemedit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreenTestTags
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

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.NAME_INPUT)
            .performTextReplacement(uniqueName)
        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.SAVE_BUTTON)
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.BACK_BUTTON)
            .performScrollTo()
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

    private fun navigateToItemList() {
        composeRule.onNodeWithText("Home Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Go To Category").performClick()
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Go To Item List").performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()
    }
}
