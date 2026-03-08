package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemPhoto

data class BackupSnapshot(
    val categories: List<Category>,
    val items: List<Item>,
    val itemPhotos: List<ItemPhoto>
)

