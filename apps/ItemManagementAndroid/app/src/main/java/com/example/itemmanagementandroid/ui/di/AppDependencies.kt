package com.example.itemmanagementandroid.ui.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.itemmanagementandroid.data.local.db.DatabaseProvider
import com.example.itemmanagementandroid.data.repository.CategoryRepositoryImpl
import com.example.itemmanagementandroid.data.repository.ItemRepositoryImpl
import com.example.itemmanagementandroid.data.repository.PhotoRepositoryImpl
import com.example.itemmanagementandroid.domain.usecase.category.ListCategoriesUseCase
import com.example.itemmanagementandroid.domain.usecase.item.GetItemUseCase
import com.example.itemmanagementandroid.domain.usecase.item.ListItemsUseCase
import com.example.itemmanagementandroid.domain.usecase.photo.ListItemPhotosUseCase

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

    val listCategoriesUseCase: ListCategoriesUseCase by lazy {
        ListCategoriesUseCase(categoryRepository)
    }

    val listItemsUseCase: ListItemsUseCase by lazy {
        ListItemsUseCase(itemRepository)
    }

    val getItemUseCase: GetItemUseCase by lazy {
        GetItemUseCase(itemRepository)
    }

    val listItemPhotosUseCase: ListItemPhotosUseCase by lazy {
        ListItemPhotosUseCase(photoRepository)
    }
}

@Composable
fun rememberAppDependencies(): AppDependencies {
    val applicationContext = LocalContext.current.applicationContext
    return remember(applicationContext) {
        AppDependencies(applicationContext)
    }
}
