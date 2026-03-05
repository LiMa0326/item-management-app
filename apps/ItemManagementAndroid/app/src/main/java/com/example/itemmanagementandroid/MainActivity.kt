package com.example.itemmanagementandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.itemmanagementandroid.data.local.db.DatabaseProvider
import com.example.itemmanagementandroid.data.repository.CategoryRepositoryImpl
import com.example.itemmanagementandroid.domain.usecase.category.EnsureDefaultCategoryUseCase
import com.example.itemmanagementandroid.ui.ItemManagementApp
import com.example.itemmanagementandroid.ui.theme.ItemManagementAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val ensureDefaultCategoryUseCase: EnsureDefaultCategoryUseCase by lazy {
        val database = DatabaseProvider.get(applicationContext)
        val repository = CategoryRepositoryImpl(database.categoryDao())
        EnsureDefaultCategoryUseCase(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            ensureDefaultCategoryUseCase()
        }

        enableEdgeToEdge()
        setContent {
            ItemManagementAndroidTheme {
                ItemManagementApp()
            }
        }
    }
}
