package com.example.itemmanagementandroid.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationViewModelTest {
    @Test
    fun navigationBackStack_behavesAsExpected() {
        val viewModel = AppNavigationViewModel()

        assertEquals(AppRoute.Category, viewModel.uiState.value.currentRoute)
        assertFalse(viewModel.uiState.value.canGoBack)

        viewModel.navigate(AppRoute.ItemList())
        assertEquals(AppRoute.ItemList(), viewModel.uiState.value.currentRoute)
        assertTrue(viewModel.uiState.value.canGoBack)

        viewModel.navigate(AppRoute.ItemList())
        assertEquals(2, viewModel.uiState.value.backStack.size)

        viewModel.navigate(AppRoute.ItemDetail())
        assertEquals(AppRoute.ItemDetail(), viewModel.uiState.value.currentRoute)
        viewModel.navigate(AppRoute.ItemDetail())
        assertEquals(3, viewModel.uiState.value.backStack.size)

        viewModel.goBack()
        assertEquals(AppRoute.ItemList(), viewModel.uiState.value.currentRoute)
        assertTrue(viewModel.uiState.value.canGoBack)

        viewModel.goBack()
        assertEquals(AppRoute.Category, viewModel.uiState.value.currentRoute)
        assertFalse(viewModel.uiState.value.canGoBack)
    }

    @Test
    fun navigateToItemDetailAfterEdit_popsToItemListThenPushesDetail() {
        val viewModel = AppNavigationViewModel()

        viewModel.navigate(AppRoute.Category)
        viewModel.navigate(AppRoute.ItemList())
        viewModel.navigate(AppRoute.ItemDetail(itemId = "item_old"))
        viewModel.navigate(AppRoute.ItemEdit(itemId = "item_old"))

        viewModel.navigateToItemDetailAfterEdit(itemId = "item_new")

        assertEquals(
            listOf(
                AppRoute.Category,
                AppRoute.ItemList(),
                AppRoute.ItemDetail(itemId = "item_new")
            ),
            viewModel.uiState.value.backStack
        )

        viewModel.goBack()
        assertEquals(AppRoute.ItemList(), viewModel.uiState.value.currentRoute)
    }

    @Test
    fun navigateToCategoryRoot_resetsBackStackToCategoryOnly() {
        val viewModel = AppNavigationViewModel()

        viewModel.navigate(AppRoute.ItemList())
        viewModel.navigate(AppRoute.ItemDetail(itemId = "item_1"))

        viewModel.navigateToCategoryRoot()

        assertEquals(listOf(AppRoute.Category), viewModel.uiState.value.backStack)
        assertEquals(AppRoute.Category, viewModel.uiState.value.currentRoute)
        assertFalse(viewModel.uiState.value.canGoBack)
    }
}
