package com.example.itemmanagementandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
        when (val currentRoute = navigationState.currentRoute) {
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
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            listItemsUseCase = dependencies.listItemsUseCase,
                            createCategoryUseCase = dependencies.createCategoryUseCase,
                            updateCategoryUseCase = dependencies.updateCategoryUseCase,
                            setCategoryArchivedUseCase = dependencies.setCategoryArchivedUseCase,
                            reorderCategoriesUseCase = dependencies.reorderCategoriesUseCase
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
                    onCreateCategory = categoryViewModel::createCategory,
                    onRenameCategory = categoryViewModel::renameCategory,
                    onSetArchived = categoryViewModel::setCategoryArchived,
                    onMoveCategoryUp = categoryViewModel::moveCategoryUp,
                    onMoveCategoryDown = categoryViewModel::moveCategoryDown,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppRoute.ItemList -> {
                val itemListViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        ItemListViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            listItemsUseCase = dependencies.listItemsUseCase
                        )
                    }
                }
                val itemListViewModel: ItemListViewModel = viewModel(factory = itemListViewModelFactory)
                LaunchedEffect(Unit) {
                    itemListViewModel.refresh()
                }
                val itemListState by itemListViewModel.uiState.collectAsState()
                ItemListScreen(
                    state = itemListState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemListViewModel::refresh,
                    onToggleIncludeDeleted = itemListViewModel::setIncludeDeleted,
                    onSearchKeywordChanged = itemListViewModel::setSearchKeyword,
                    onCategoryFilterChanged = itemListViewModel::setCategoryFilter,
                    onSortOptionChanged = itemListViewModel::setSortOption,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AppRoute.ItemDetail -> {
                val itemDetailViewModelFactory = remember(dependencies, currentRoute.itemId) {
                    singleViewModelFactory {
                        ItemDetailViewModel(
                            listItemsUseCase = dependencies.listItemsUseCase,
                            getItemUseCase = dependencies.getItemUseCase,
                            listItemPhotosUseCase = dependencies.listItemPhotosUseCase,
                            softDeleteItemUseCase = dependencies.softDeleteItemUseCase,
                            restoreItemUseCase = dependencies.restoreItemUseCase,
                            initialItemId = currentRoute.itemId
                        )
                    }
                }
                val itemDetailViewModel: ItemDetailViewModel = viewModel(
                    key = "item_detail_${currentRoute.itemId ?: "auto"}",
                    factory = itemDetailViewModelFactory
                )
                val itemDetailState by itemDetailViewModel.uiState.collectAsState()
                ItemDetailScreen(
                    state = itemDetailState,
                    canGoBack = navigationState.canGoBack,
                    onNavigate = navigationViewModel::navigate,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemDetailViewModel::refresh,
                    onSoftDelete = itemDetailViewModel::softDelete,
                    onRestore = itemDetailViewModel::restore,
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AppRoute.ItemEdit -> {
                val itemEditViewModelFactory = remember(dependencies, currentRoute.itemId) {
                    singleViewModelFactory {
                        ItemEditViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            getItemUseCase = dependencies.getItemUseCase,
                            createItemUseCase = dependencies.createItemUseCase,
                            updateItemUseCase = dependencies.updateItemUseCase,
                            initialItemId = currentRoute.itemId
                        )
                    }
                }
                val itemEditViewModel: ItemEditViewModel = viewModel(
                    key = "item_edit_${currentRoute.itemId ?: "new"}",
                    factory = itemEditViewModelFactory
                )
                LaunchedEffect(currentRoute.itemId) {
                    itemEditViewModel.onRouteEntered(currentRoute.itemId)
                }
                val itemEditState by itemEditViewModel.uiState.collectAsState()
                ItemEditScreen(
                    state = itemEditState,
                    canGoBack = navigationState.canGoBack,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemEditViewModel::refresh,
                    onNameChanged = itemEditViewModel::setName,
                    onCategorySelected = itemEditViewModel::setCategoryId,
                    onPurchaseDateChanged = itemEditViewModel::setPurchaseDate,
                    onPurchasePriceChanged = itemEditViewModel::setPurchasePriceInput,
                    onPurchaseCurrencyChanged = itemEditViewModel::setPurchaseCurrency,
                    onPurchasePlaceChanged = itemEditViewModel::setPurchasePlace,
                    onDescriptionChanged = itemEditViewModel::setDescription,
                    onTagsInputChanged = itemEditViewModel::setTagsInput,
                    onCustomAttributeKeyChanged = itemEditViewModel::setCustomAttributeKey,
                    onCustomAttributeValueChanged = itemEditViewModel::setCustomAttributeValue,
                    onAddCustomAttributeRow = itemEditViewModel::addCustomAttributeRow,
                    onRemoveCustomAttributeRow = itemEditViewModel::removeCustomAttributeRow,
                    onSave = itemEditViewModel::save,
                    onCancel = navigationViewModel::goBack,
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
