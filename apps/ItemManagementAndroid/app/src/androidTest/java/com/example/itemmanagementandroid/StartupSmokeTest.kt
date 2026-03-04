package com.example.itemmanagementandroid

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesToHomeScreen() {
        composeRule.onNodeWithText("Home Screen").assertIsDisplayed()
        composeRule.onNodeWithText("Step 01 placeholder for home dashboard.").assertIsDisplayed()
    }
}
