package com.example.itemmanagementandroid.ui.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.itemmanagementandroid.data.local.db.DatabaseProvider
import com.example.itemmanagementandroid.data.repository.CategoryRepositoryImpl
import com.example.itemmanagementandroid.data.repository.ItemRepositoryImpl
import com.example.itemmanagementandroid.data.repository.PhotoRepositoryImpl
import com.example.itemmanagementandroid.domain.usecase.category.CreateCategoryUseCase
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.category.ReorderCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.category.SetCategoryArchivedUseCase
import com.example.itemmanagementandroid.domain.usecase.category.UpdateCategoryUseCase
import com.example.itemmanagementandroid.domain.usecase.item.CreateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.item.RestoreItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.SoftDeleteItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.UpdateItemUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.AddItemPhotoUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ImportItemPhotosUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotoCoversUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase
import com.example.itemmanagementandroid.photo.AndroidPhotoAssetProcessor

class AppDependencies(
    context: Context
) {
    private val database = DatabaseProvider.get(context.applicationContext)

    private val categoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    private val itemRepository by lazy {
        ItemRepositoryImpl(database.itemDao())
    }

    private val photoRepository by lazy {
        PhotoRepositoryImpl(database.itemPhotoDao())
    }

    private val photoAssetProcessor by lazy {
        AndroidPhotoAssetProcessor(context.applicationContext)
    }

    val listCategoriesUseCase: ListCategoriesUseCase by lazy {
        ListCategoriesUseCase(categoryRepository)
    }

    val createCategoryUseCase: CreateCategoryUseCase by lazy {
        CreateCategoryUseCase(categoryRepository)
    }

    val updateCategoryUseCase: UpdateCategoryUseCase by lazy {
        UpdateCategoryUseCase(categoryRepository)
    }

    val setCategoryArchivedUseCase: SetCategoryArchivedUseCase by lazy {
        SetCategoryArchivedUseCase(categoryRepository)
    }

    val reorderCategoriesUseCase: ReorderCategoriesUseCase by lazy {
        ReorderCategoriesUseCase(categoryRepository)
    }

    val listItemsUseCase: ListItemsUseCase by lazy {
        ListItemsUseCase(itemRepository)
    }

    val getItemUseCase: GetItemUseCase by lazy {
        GetItemUseCase(itemRepository)
    }

    val createItemUseCase: CreateItemUseCase by lazy {
        CreateItemUseCase(itemRepository)
    }

    val updateItemUseCase: UpdateItemUseCase by lazy {
        UpdateItemUseCase(itemRepository)
    }

    val softDeleteItemUseCase: SoftDeleteItemUseCase by lazy {
        SoftDeleteItemUseCase(itemRepository)
    }

    val restoreItemUseCase: RestoreItemUseCase by lazy {
        RestoreItemUseCase(itemRepository)
    }

    val listItemPhotosUseCase: ListItemPhotosUseCase by lazy {
        ListItemPhotosUseCase(photoRepository)
    }

    private val addItemPhotoUseCase: AddItemPhotoUseCase by lazy {
        AddItemPhotoUseCase(photoRepository)
    }

    val importItemPhotosUseCase: ImportItemPhotosUseCase by lazy {
        ImportItemPhotosUseCase(
            photoAssetProcessor = photoAssetProcessor,
            addItemPhotoUseCase = addItemPhotoUseCase
        )
    }

    val listItemPhotoCoversUseCase: ListItemPhotoCoversUseCase by lazy {
        ListItemPhotoCoversUseCase(photoRepository)
    }
}

@Composable
fun rememberAppDependencies(): AppDependencies {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        AppDependencies(applicationContext)
    }
}
