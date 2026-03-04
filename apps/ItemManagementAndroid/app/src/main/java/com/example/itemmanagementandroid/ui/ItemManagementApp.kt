package com.example.itemmanagementandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import com.example.itemmanagementandroid.ui.navigation.rememberAppNavigatorState
import com.example.itemmanagementandroid.ui.screens.category.CategoryScreen
import com.example.itemmanagementandroid.ui.screens.home.HomeScreen
import com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailScreen
import com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditScreen
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreen
import com.example.itemmanagementandroid.ui.screens.settings.SettingsScreen

@Composable
fun ItemManagementApp() {
    val navigatorState = rememberAppNavigatorState()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        when (navigatorState.currentRoute) {
            AppRoute.Home -> HomeScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )

            AppRoute.Category -> CategoryScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )

            AppRoute.ItemList -> ItemListScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )

            AppRoute.ItemDetail -> ItemDetailScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )

            AppRoute.ItemEdit -> ItemEditScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )

            AppRoute.Settings -> SettingsScreen(
                onNavigate = { route -> navigatorState.navigate(route) },
                onBack = { navigatorState.goBack() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
