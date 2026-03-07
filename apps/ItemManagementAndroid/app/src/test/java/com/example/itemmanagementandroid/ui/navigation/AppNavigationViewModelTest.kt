package com.example.itemmanagementandroid.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationViewModelTest {
    @Test
    fun navigationBackStack_behavesAsExpected() {
        val viewModel = AppNavigationViewModel()

        assertEquals(AppRoute.Home, viewModel.uiState.value.currentRoute)
        assertFalse(viewModel.uiState.value.canGoBack)

        viewModel.navigate(AppRoute.Category)
        assertEquals(AppRoute.Category, viewModel.uiState.value.currentRoute)
        assertTrue(viewModel.uiState.value.canGoBack)

        viewModel.navigate(AppRoute.Category)
        assertEquals(2, viewModel.uiState.value.backStack.size)

        viewModel.navigate(AppRoute.ItemDetail())
        assertEquals(AppRoute.ItemDetail(), viewModel.uiState.value.currentRoute)
        viewModel.navigate(AppRoute.ItemDetail())
        assertEquals(3, viewModel.uiState.value.backStack.size)

        viewModel.goBack()
        assertEquals(AppRoute.Category, viewModel.uiState.value.currentRoute)
        assertTrue(viewModel.uiState.value.canGoBack)

        viewModel.goBack()
        assertEquals(AppRoute.Home, viewModel.uiState.value.currentRoute)
        assertFalse(viewModel.uiState.value.canGoBack)
    }
}
