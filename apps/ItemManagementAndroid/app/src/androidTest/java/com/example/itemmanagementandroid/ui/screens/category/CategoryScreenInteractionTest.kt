package com.example.itemmanagementandroid.ui.screens.category

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun createCategory_triggersCallback() {
        var createdName: String? = null

        setCategoryScreenContent(
            onCreateCategory = { name -> createdName = name }
        )

        composeRule.onNodeWithTag(CategoryScreenTestTags.CREATE_CATEGORY_BUTTON).performClick()
        composeRule.onNodeWithTag(CategoryScreenTestTags.CREATE_CATEGORY_INPUT).performTextInput("Camera")
        composeRule.onNodeWithTag(CategoryScreenTestTags.CREATE_CATEGORY_CONFIRM_BUTTON).performClick()

        assertEquals("Camera", createdName)
    }

    @Test
    fun renameCategory_triggersCallback() {
        var renamed: Pair<String, String>? = null

        setCategoryScreenContent(
            onRenameCategory = { categoryId, newName ->
                renamed = categoryId to newName
            }
        )

        composeRule.onNodeWithTag(CategoryScreenTestTags.renameButton("cat_a")).performClick()
        composeRule.onNodeWithTag(CategoryScreenTestTags.RENAME_CATEGORY_INPUT).performTextClearance()
        composeRule.onNodeWithTag(CategoryScreenTestTags.RENAME_CATEGORY_INPUT).performTextInput("Renamed A")
        composeRule.onNodeWithTag(CategoryScreenTestTags.RENAME_CATEGORY_CONFIRM_BUTTON).performClick()

        assertEquals("cat_a" to "Renamed A", renamed)
    }

    @Test
    fun archiveToggle_triggersCallback() {
        var archiveEvent: Pair<String, Boolean>? = null

        setCategoryScreenContent(
            onSetArchived = { categoryId, archived ->
                archiveEvent = categoryId to archived
            }
        )

        composeRule.onNodeWithTag(CategoryScreenTestTags.archiveButton("cat_a")).performClick()

        assertEquals("cat_a" to true, archiveEvent)
    }

    @Test
    fun moveDown_triggersCallback() {
        var moveDownId: String? = null

        setCategoryScreenContent(
            onMoveCategoryDown = { categoryId -> moveDownId = categoryId }
        )

        composeRule.onNodeWithTag(CategoryScreenTestTags.moveDownButton("cat_a")).performClick()
        composeRule.waitForIdle()

        assertEquals("cat_a", moveDownId)
    }

    @Test
    fun moveUp_triggersCallback() {
        var moveUpId: String? = null

        setCategoryScreenContent(
            onMoveCategoryUp = { categoryId -> moveUpId = categoryId }
        )

        composeRule
            .onNodeWithTag(CategoryScreenTestTags.CATEGORY_LIST)
            .performScrollToNode(hasTestTag(CategoryScreenTestTags.moveUpButton("cat_b")))
        composeRule.onNodeWithTag(CategoryScreenTestTags.moveUpButton("cat_b")).assertIsEnabled()
        composeRule.onNodeWithTag(CategoryScreenTestTags.moveUpButton("cat_b")).performClick()
        composeRule.waitForIdle()

        assertEquals("cat_b", moveUpId)
    }

    @Test
    fun categoryRowClick_navigatesToItemListWithCategoryFilter() {
        var targetRoute: AppRoute? = null

        setCategoryScreenContent(
            onNavigate = { route -> targetRoute = route }
        )

        composeRule
            .onNodeWithTag(CategoryScreenTestTags.categoryRow("cat_a"))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(AppRoute.ItemList(initialCategoryId = "cat_a"), targetRoute)
    }

    @Test
    fun allItemsRowClick_navigatesToItemListWithoutCategoryFilter() {
        var targetRoute: AppRoute? = null

        setCategoryScreenContent(
            onNavigate = { route -> targetRoute = route }
        )

        composeRule
            .onNodeWithTag(CategoryScreenTestTags.ALL_ITEMS_ROW)
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(AppRoute.ItemList(initialCategoryId = null), targetRoute)
    }

    @Test
    fun toggleIncludeArchivedButton_notDisplayedInScreenContent() {
        setCategoryScreenContent()
        composeRule.onAllNodesWithText("Toggle Include Archived").assertCountEquals(0)
    }

    private fun setCategoryScreenContent(
        onNavigate: (AppRoute) -> Unit = { _ -> },
        onCreateCategory: (String) -> Unit = {},
        onRenameCategory: (String, String) -> Unit = { _, _ -> },
        onSetArchived: (String, Boolean) -> Unit = { _, _ -> },
        onMoveCategoryUp: (String) -> Unit = {},
        onMoveCategoryDown: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            CategoryScreen(
                state = CategoryUiState(
                    isLoading = false,
                    includeArchived = false,
                    categories = listOf(
                        CategoryListItemUiModel(
                            id = "cat_a",
                            name = "Category A",
                            isArchived = false,
                            isSystemDefault = true,
                            itemCount = 2
                        ),
                        CategoryListItemUiModel(
                            id = "cat_b",
                            name = "Category B",
                            isArchived = false,
                            isSystemDefault = false,
                            itemCount = 1
                        )
                    )
                ),
                onNavigate = onNavigate,
                onCreateCategory = onCreateCategory,
                onRenameCategory = onRenameCategory,
                onSetArchived = onSetArchived,
                onMoveCategoryUp = onMoveCategoryUp,
                onMoveCategoryDown = onMoveCategoryDown
            )
        }
    }
}
