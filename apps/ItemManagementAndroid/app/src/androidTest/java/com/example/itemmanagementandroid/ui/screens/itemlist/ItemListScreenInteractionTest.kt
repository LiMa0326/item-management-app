package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemListScreenInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun categoryFilterButtons_triggerCallbacks() {
        var selectedCategory: String? = "cat_a"

        setItemListContent(
            onCategoryFilterChanged = { selectedCategory = it }
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.CATEGORY_FILTER_ALL_BUTTON).performClick()
        assertEquals(null, selectedCategory)

        composeRule.onNodeWithTag(ItemListScreenTestTags.categoryFilterButton("cat_b")).performClick()
        assertEquals("cat_b", selectedCategory)
    }

    @Test
    fun searchInput_changesTriggerCallback() {
        var keyword = ""

        setItemListContent(
            onSearchKeywordChanged = { keyword = it }
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.SEARCH_INPUT).performTextInput("sony")
        assertEquals("sony", keyword)
    }

    @Test
    fun searchInput_clearRestoresEmptyKeyword() {
        var keyword = "initial"

        setItemListContent(
            state = baseState().copy(searchKeyword = "sony"),
            onSearchKeywordChanged = { keyword = it }
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.SEARCH_INPUT).performTextClearance()
        assertEquals("", keyword)
    }

    @Test
    fun sortOptionButtons_triggerCallbacks() {
        var selectedSort: ItemListSortOption? = null

        setItemListContent(
            onSortOptionChanged = { selectedSort = it }
        )

        composeRule
            .onNodeWithTag(ItemListScreenTestTags.SORT_ROW)
            .performScrollToNode(
                hasTestTag(
                    ItemListScreenTestTags.sortOptionButton(ItemListSortOption.PURCHASE_PRICE)
                )
            )
        composeRule
            .onNodeWithTag(ItemListScreenTestTags.sortOptionButton(ItemListSortOption.PURCHASE_PRICE))
            .performClick()

        assertEquals(ItemListSortOption.PURCHASE_PRICE, selectedSort)
    }

    @Test
    fun emptyState_isDisplayed() {
        setItemListContent(
            state = baseState().copy(
                items = emptyList(),
                hasAnyItemsInCurrentMode = false
            )
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.EMPTY_STATE_TEXT).assertIsDisplayed()
    }

    @Test
    fun noResultsState_isDisplayed() {
        setItemListContent(
            state = baseState().copy(
                items = emptyList(),
                hasAnyItemsInCurrentMode = true
            )
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.NO_RESULTS_TEXT).assertIsDisplayed()
    }

    @Test
    fun navigationButtons_triggerCallbacks() {
        var navigatedToItemDetail: AppRoute.ItemDetail? = null

        setItemListContent(
            onNavigate = { route ->
                if (route is AppRoute.ItemDetail) {
                    navigatedToItemDetail = route
                }
            }
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_DETAIL_BUTTON).performClick()

        assertEquals(null, navigatedToItemDetail?.itemId)
    }

    @Test
    fun toggleIncludeDeletedButton_notDisplayedInScreenContent() {
        setItemListContent()
        composeRule.onAllNodesWithText("Toggle Include Deleted").assertCountEquals(0)
    }

    @Test
    fun itemRowClick_navigatesWithItemId() {
        var navigatedToItemDetail: AppRoute.ItemDetail? = null

        setItemListContent(
            state = baseState().copy(
                categoryFilters = emptyList()
            ),
            onNavigate = { route ->
                if (route is AppRoute.ItemDetail) {
                    navigatedToItemDetail = route
                }
            }
        )

        composeRule
            .onNodeWithTag(ItemListScreenTestTags.itemRow("item_1"))
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals("item_1", navigatedToItemDetail?.itemId)
    }

    @Test
    fun itemRowCover_isDisplayedWhenCoverUriExists() {
        setItemListContent(
            state = baseState().copy(
                coverUriByItemId = mapOf(
                    "item_1" to "file:///tmp/item_1_thumb.jpg"
                )
            )
        )

        composeRule
            .onNodeWithTag(ItemListScreenTestTags.itemRow("item_1"))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(ItemListScreenTestTags.itemCover("item_1"), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun setItemListContent(
        state: ItemListUiState = baseState(),
        onNavigate: (AppRoute) -> Unit = {},
        onSearchKeywordChanged: (String) -> Unit = {},
        onCategoryFilterChanged: (String?) -> Unit = {},
        onSortOptionChanged: (ItemListSortOption) -> Unit = {}
    ) {
        composeRule.setContent {
            ItemListScreen(
                state = state,
                onNavigate = onNavigate,
                onSearchKeywordChanged = onSearchKeywordChanged,
                onCategoryFilterChanged = onCategoryFilterChanged,
                onSortOptionChanged = onSortOptionChanged,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun baseState(): ItemListUiState {
        return ItemListUiState(
            isLoading = false,
            includeDeleted = false,
            searchKeyword = "",
            sortOption = ItemListSortOption.RECENTLY_UPDATED,
            selectedCategoryId = null,
            categoryFilters = listOf(
                ItemListCategoryFilterUiModel(
                    id = "cat_a",
                    name = "Category A",
                    isArchived = false
                ),
                ItemListCategoryFilterUiModel(
                    id = "cat_b",
                    name = "Category B",
                    isArchived = true
                )
            ),
            hasAnyItemsInCurrentMode = true,
            items = listOf(
                Item(
                    id = "item_1",
                    categoryId = "cat_a",
                    name = "Item 1",
                    purchaseDate = "2025-01-01",
                    purchasePrice = 100.0,
                    purchaseCurrency = "USD",
                    purchasePlace = "Store",
                    description = null,
                    tags = emptyList(),
                    customAttributes = emptyMap(),
                    createdAt = "2026-03-06T00:00:00Z",
                    updatedAt = "2026-03-06T00:00:00Z",
                    deletedAt = null
                )
            )
        )
    }
}
