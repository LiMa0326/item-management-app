package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import java.time.Instant

class BackupSnapshotCollector(
    private val listCategories: suspend () -> List<Category>,
    private val listItems: suspend () -> List<Item>,
    private val listItemPhotosByItemId: suspend (String) -> List<ItemPhoto>
) {
    suspend fun collect(): BackupSnapshot {
        val categories = listCategories()
            .sortedWith(
                compareBy<Category> { category -> category.sortOrder }
                    .thenBy { category -> parseInstant(category.createdAt) }
                    .thenBy { category -> category.id }
            )

        val items = listItems()
            .sortedWith(
                compareBy<Item> { item -> parseInstant(item.createdAt) }
                    .thenBy { item -> item.id }
            )

        val photosById = linkedMapOf<String, ItemPhoto>()
        items.forEach { item ->
            val itemPhotos = listItemPhotosByItemId(item.id)
                .sortedWith(
                    compareBy<ItemPhoto> { photo -> parseInstant(photo.createdAt) }
                        .thenBy { photo -> photo.id }
                )
            itemPhotos.forEach { photo ->
                photosById.putIfAbsent(photo.id, photo)
            }
        }

        return BackupSnapshot(
            categories = categories,
            items = items,
            itemPhotos = photosById.values.toList()
        )
    }

    private fun parseInstant(value: String): Instant {
        return runCatching {
            Instant.parse(value)
        }.getOrElse {
            Instant.EPOCH
        }
    }
}

