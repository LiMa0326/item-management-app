package com.example.itemmanagementandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeRule.onNodeWithText("Home Screen").assertIsDisplayed()

        composeRule.onNodeWithText("Go To Category").performClick()
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()

        composeRule.onNodeWithText("Go To Item List").performClick()
        composeRule.onNodeWithText("Item List Screen").assertIsDisplayed()

        composeRule.onNodeWithText("Go To Item Detail").performClick()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()

        composeRule.onNodeWithText("Edit Item").performClick()
        composeRule.onNodeWithText("Item Edit Screen").assertIsDisplayed()

        composeRule
            .onNodeWithTag(ItemEditScreenTestTags.CANCEL_BUTTON)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Item Detail Screen").assertIsDisplayed()
    }
}
