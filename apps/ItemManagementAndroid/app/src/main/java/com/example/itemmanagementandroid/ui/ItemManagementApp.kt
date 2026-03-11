package com.example.itemmanagementandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.itemmanagementandroid.ui.components.AppOverflowAction
import com.example.itemmanagementandroid.ui.components.AppPageScaffold
import com.example.itemmanagementandroid.ui.di.rememberAppDependencies
import com.example.itemmanagementandroid.ui.navigation.AppRoute
import com.example.itemmanagementandroid.ui.navigation.AppNavigationViewModel
import com.example.itemmanagementandroid.ui.screens.category.CategoryScreen
import com.example.itemmanagementandroid.ui.screens.category.CategoryViewModel
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
    val showBackButton = navigationState.canGoBack
    val settingsOverflowAction = remember {
        AppOverflowAction(
            id = "open_settings",
            label = "Settings",
            onClick = { navigationViewModel.navigate(AppRoute.Settings) }
        )
    }
    val backToCategoryOverflowAction = remember {
        AppOverflowAction(
            id = "back_to_category",
            label = "Back To Category",
            onClick = navigationViewModel::navigateToCategoryRoot
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val currentRoute = navigationState.currentRoute) {
            AppRoute.Home -> {
                LaunchedEffect(Unit) {
                    navigationViewModel.navigateToCategoryRoot()
                }
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
                LaunchedEffect(currentRoute) {
                    categoryViewModel.refresh()
                }
                val categoryState by categoryViewModel.uiState.collectAsState()
                val toggleIncludeArchivedOverflowAction = AppOverflowAction(
                    id = "toggle_include_archived",
                    label = "Toggle Include Archived",
                    onClick = {
                        categoryViewModel.setIncludeArchived(!categoryState.includeArchived)
                    }
                )
                AppPageScaffold(
                    title = currentRoute.title,
                    showBackButton = false,
                    onBack = null,
                    onRefresh = categoryViewModel::refresh,
                    overflowActions = listOf(
                        toggleIncludeArchivedOverflowAction,
                        settingsOverflowAction
                    ),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CategoryScreen(
                        state = categoryState,
                        onNavigate = navigationViewModel::navigate,
                        onCreateCategory = categoryViewModel::createCategory,
                        onRenameCategory = categoryViewModel::renameCategory,
                        onSetArchived = categoryViewModel::setCategoryArchived,
                        onMoveCategoryUp = categoryViewModel::moveCategoryUp,
                        onMoveCategoryDown = categoryViewModel::moveCategoryDown,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }

            AppRoute.ItemList -> {
                val itemListViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        ItemListViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            listItemsUseCase = dependencies.listItemsUseCase,
                            listItemPhotoCoversUseCase = dependencies.listItemPhotoCoversUseCase
                        )
                    }
                }
                val itemListViewModel: ItemListViewModel = viewModel(factory = itemListViewModelFactory)
                LaunchedEffect(currentRoute) {
                    itemListViewModel.refresh()
                }
                val itemListState by itemListViewModel.uiState.collectAsState()
                val toggleIncludeDeletedOverflowAction = AppOverflowAction(
                    id = "toggle_include_deleted",
                    label = "Toggle Include Deleted",
                    onClick = {
                        itemListViewModel.setIncludeDeleted(!itemListState.includeDeleted)
                    }
                )
                AppPageScaffold(
                    title = currentRoute.title,
                    showBackButton = showBackButton,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemListViewModel::refresh,
                    overflowActions = listOf(
                        toggleIncludeDeletedOverflowAction,
                        settingsOverflowAction
                    ),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ItemListScreen(
                        state = itemListState,
                        onNavigate = navigationViewModel::navigate,
                        onSearchKeywordChanged = itemListViewModel::setSearchKeyword,
                        onCategoryFilterChanged = itemListViewModel::setCategoryFilter,
                        onSortOptionChanged = itemListViewModel::setSortOption,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
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
                LaunchedEffect(currentRoute) {
                    itemDetailViewModel.refresh()
                }
                val itemDetailState by itemDetailViewModel.uiState.collectAsState()
                AppPageScaffold(
                    title = currentRoute.title,
                    showBackButton = showBackButton,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemDetailViewModel::refresh,
                    overflowActions = listOf(settingsOverflowAction),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ItemDetailScreen(
                        state = itemDetailState,
                        onNavigate = navigationViewModel::navigate,
                        onSoftDelete = itemDetailViewModel::softDelete,
                        onRestore = itemDetailViewModel::restore,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }

            is AppRoute.ItemEdit -> {
                val itemEditViewModelFactory = remember(dependencies, currentRoute.itemId) {
                    singleViewModelFactory {
                        ItemEditViewModel(
                            listCategoriesUseCase = dependencies.listCategoriesUseCase,
                            getItemUseCase = dependencies.getItemUseCase,
                            listItemPhotosUseCase = dependencies.listItemPhotosUseCase,
                            importItemPhotosUseCase = dependencies.importItemPhotosUseCase,
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
                LaunchedEffect(itemEditState.navigateToDetailItemId) {
                    val targetItemId = itemEditState.navigateToDetailItemId ?: return@LaunchedEffect
                    navigationViewModel.navigateToItemDetailAfterEdit(targetItemId)
                    itemEditViewModel.onNavigateToDetailHandled()
                }
                AppPageScaffold(
                    title = currentRoute.title,
                    showBackButton = showBackButton,
                    onBack = navigationViewModel::goBack,
                    onRefresh = itemEditViewModel::refresh,
                    overflowActions = listOf(settingsOverflowAction),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
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
                        onImportPhotoUris = itemEditViewModel::importPhotoUris,
                        onRetryFailedPhotoImports = itemEditViewModel::retryFailedPhotoImports,
                        onSave = itemEditViewModel::save,
                        onCancel = navigationViewModel::goBack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }

            AppRoute.Settings -> {
                val settingsViewModelFactory = remember(dependencies) {
                    singleViewModelFactory {
                        SettingsViewModel(
                            setBackupDirectoryUseCase = dependencies.setBackupDirectoryUseCase,
                            getBackupDirectoryUseCase = dependencies.getBackupDirectoryUseCase,
                            listImportableBackupsUseCase = dependencies.listImportableBackupsUseCase,
                            exportBackupToSharedDirectoryUseCase = dependencies.exportBackupToSharedDirectoryUseCase,
                            importBackupFromDocumentUseCase = dependencies.importBackupFromDocumentUseCase
                        )
                    }
                }
                val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
                LaunchedEffect(currentRoute) {
                    settingsViewModel.refresh()
                }
                val settingsState by settingsViewModel.uiState.collectAsState()
                AppPageScaffold(
                    title = currentRoute.title,
                    showBackButton = showBackButton,
                    onBack = navigationViewModel::goBack,
                    onRefresh = settingsViewModel::refresh,
                    overflowActions = listOf(backToCategoryOverflowAction),
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    SettingsScreen(
                        state = settingsState,
                        onExportModeSelected = settingsViewModel::setExportMode,
                        onBackupDirectorySelected = settingsViewModel::onBackupDirectorySelected,
                        onExportBackupToSharedDirectory = settingsViewModel::exportBackupToSharedDirectory,
                        onRefreshImportableBackups = settingsViewModel::refreshImportableBackups,
                        onRequestImport = settingsViewModel::requestImport,
                        onConfirmImport = settingsViewModel::confirmImport,
                        onCancelImport = settingsViewModel::cancelImport,
                        onImportSingleDocument = settingsViewModel::importFromSingleDocument,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
