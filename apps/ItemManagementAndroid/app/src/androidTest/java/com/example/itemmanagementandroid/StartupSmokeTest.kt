package com.example.itemmanagementandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.itemmanagementandroid.ui.components.AppPageScaffoldTestTags
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesToCategoryScreen() {
        composeRule.onNodeWithText("Category Screen").assertIsDisplayed()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.TOP_BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AppPageScaffoldTestTags.OVERFLOW_BUTTON).assertIsDisplayed()
    }
}
