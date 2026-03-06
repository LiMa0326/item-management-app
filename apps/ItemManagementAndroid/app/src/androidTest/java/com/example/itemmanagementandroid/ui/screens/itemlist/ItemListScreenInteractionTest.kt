package com.example.itemmanagementandroid.ui.screens.itemlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        var navigatedToItemDetail = false
        var backInvoked = false

        setItemListContent(
            onNavigate = { route ->
                if (route == AppRoute.ItemDetail) {
                    navigatedToItemDetail = true
                }
            },
            onBack = { backInvoked = true }
        )

        composeRule.onNodeWithTag(ItemListScreenTestTags.GO_TO_ITEM_DETAIL_BUTTON).performClick()
        composeRule.onNodeWithTag(ItemListScreenTestTags.BACK_BUTTON).performClick()

        assertTrue(navigatedToItemDetail)
        assertTrue(backInvoked)
    }

    private fun setItemListContent(
        state: ItemListUiState = baseState(),
        onNavigate: (AppRoute) -> Unit = {},
        onBack: () -> Unit = {},
        onCategoryFilterChanged: (String?) -> Unit = {},
        onSortOptionChanged: (ItemListSortOption) -> Unit = {}
    ) {
        composeRule.setContent {
            ItemListScreen(
                state = state,
                canGoBack = true,
                onNavigate = onNavigate,
                onBack = onBack,
                onRefresh = {},
                onToggleIncludeDeleted = {},
                onCategoryFilterChanged = onCategoryFilterChanged,
                onSortOptionChanged = onSortOptionChanged
            )
        }
    }

    private fun baseState(): ItemListUiState {
        return ItemListUiState(
            isLoading = false,
            includeDeleted = false,
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
