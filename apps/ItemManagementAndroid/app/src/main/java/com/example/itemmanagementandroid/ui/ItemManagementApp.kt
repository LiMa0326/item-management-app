package com.example.itemmanagementandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemmanagementandroid.ui.di.rememberAppDependencies
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import com.example.itemmanagementandroid.ui.navigation.AppNavigationViewModel
import com.example.itemmanagementandroid.ui.screens.category.CategoryScreen
import com.example.itemmanagementandroid.ui.screens.category.CategoryViewModel
import com.example.itemmanagementandroid.ui.screens.home.HomeScreen
import com.example.itemmanagementandroid.ui.screens.home.HomeViewModel
import com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailScreen
import com.example.itemmanagementandroid.ui.screens.itemdetail.ItemDetailViewModel
import com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditScreen
import com.example.itemmanagementandroid.ui.screens.itemedit.ItemEditViewModel
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListScreen
import com.example.itemmanagementandroid.ui.screens.itemlist.ItemListViewModel
import com.example.itemmanagementandroid.ui.screens.settings.SettingsScreen
import com.example.itemmanagementandroid.ui.screens.settings.SettingsViewModel
import com.example.itemmanagementandroid.ui.viewmodel.singleViewModelFactory

@Composable
fun ItemManagementApp() {
    val dependencies = rememberAppDependencies()
    val navigationViewModel: AppNavigationViewModel = viewModel()
    val navigationState by navigationViewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        when (navigationState.currentRoute) {
            AppRoute.Home -> {
                val homeViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        HomeViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            listItemsUseCase = dependencies.listItemsUseCase
                        )
                    }
                }
                val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
                val homeState by homeViewModel.uiState.collectAsState()
                HomeScreen(
                    state = homeState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = homeViewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.Category -> {
                val categoryViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        CategoryViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase
                        )
                    }
                }
                val categoryViewModel: CategoryViewModel = viewModel(factory = categoryViewModelFactory)
                val categoryState by categoryViewModel.uiState.collectAsState()
                CategoryScreen(
                    state = categoryState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = categoryViewModel::refresh,
                    onToggleIncludeArchived = categoryViewModel::setIncludeArchived,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.ItemList -> {
                val itemListViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        ItemListViewModel(
                            listItemsUseCase = dependencies.listItemsUseCase
                        )
                    }
                }
                val itemListViewModel: ItemListViewModel = viewModel(factory = itemListViewModelFactory)
                val itemListState by itemListViewModel.uiState.collectAsState()
                ItemListScreen(
                    state = itemListState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemListViewModel::refresh,
                    onToggleIncludeDeleted = itemListViewModel::setIncludeDeleted,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.ItemDetail -> {
                val itemDetailViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        ItemDetailViewModel(
                            listItemsUseCase = dependencies.listItemsUseCase,
                            getItemUseCase = dependencies.getItemUseCase,
                            listItemPhotosUseCase = dependencies.listItemPhotosUseCase
                        )
                    }
                }
                val itemDetailViewModel: ItemDetailViewModel = viewModel(factory = itemDetailViewModelFactory)
                val itemDetailState by itemDetailViewModel.uiState.collectAsState()
                ItemDetailScreen(
                    state = itemDetailState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemDetailViewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.ItemEdit -> {
                val itemEditViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        ItemEditViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            listItemsUseCase = dependencies.listItemsUseCase,
                            getItemUseCase = dependencies.getItemUseCase
                        )
                    }
                }
                val itemEditViewModel: ItemEditViewModel = viewModel(factory = itemEditViewModelFactory)
                val itemEditState by itemEditViewModel.uiState.collectAsState()
                ItemEditScreen(
                    state = itemEditState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemEditViewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.Settings -> {
                val settingsViewModel: SettingsViewModel = viewModel()
                val settingsState by settingsViewModel.uiState.collectAsState()
                SettingsScreen(
                    state = settingsState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
