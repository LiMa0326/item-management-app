package com.example.itemmanagementandroid.backup.export

import com.example.itemmanagementandroid.domain.model.Category
import com.example.itemmanagementandroid.domain.model.Item
import java.nio.charset.StandardCharsets

class BackupJsonBuilder(
    private val appName: String,
    private val appBuild: String,
    private val appPlatform: String = "android"
) {
    fun buildManifestJson(
        snapshot: BackupSnapshot,
        exportMode: ExportMode,
        createdAt: String
    ): ByteArray {
        val payload = linkedMapOf<String, Any?>(
            "formatVersion" to FORMAT_VERSION,
            "createdAt" to createdAt,
            "exportMode" to exportMode.wireValue,
            "app" to linkedMapOf(
                "name" to appName,
                "build" to appBuild,
                "platform" to appPlatform
            ),
            "stats" to linkedMapOf(
                "categories" to snapshot.categories.size,
                "items" to snapshot.items.size,
                "photos" to snapshot.itemPhotos.size
            )
        )
        return BackupJsonEncoder
            .encode(payload)
            .toByteArray(StandardCharsets.UTF_8)
    }

    fun buildDataJson(
        snapshot: BackupSnapshot,
        exportedAt: String,
        preparedPhotosById: Map<String, PreparedPhotoEntry>
    ): ByteArray {
        val categoryPayloads = snapshot.categories.map(::toCategoryPayload)
        val itemPayloads = snapshot.items.map(::toItemPayload)
        val itemPhotoPayloads = snapshot.itemPhotos.map { photo ->
            val preparedPhoto = preparedPhotosById[photo.id]
            linkedMapOf<String, Any?>(
                "id" to photo.id,
                "itemId" to photo.itemId,
                "contentType" to photo.contentType,
                "createdAt" to photo.createdAt
            ).apply {
                photo.width?.let { width -> put("width", width) }
                photo.height?.let { height -> put("height", height) }
                if (preparedPhoto != null) {
                    put("fileName", preparedPhoto.fileName)
                    put("kind", preparedPhoto.kind)
                }
            }
        }

        val payload = linkedMapOf<String, Any?>(
            "schemaVersion" to SCHEMA_VERSION,
            "exportedAt" to exportedAt,
            "categories" to categoryPayloads,
            "items" to itemPayloads,
            "itemPhotos" to itemPhotoPayloads
        )
        return BackupJsonEncoder
            .encode(payload)
            .toByteArray(StandardCharsets.UTF_8)
    }

    private fun toCategoryPayload(category: Category): Map<String, Any?> {
        return linkedMapOf(
            "id" to category.id,
            "name" to category.name,
            "sortOrder" to category.sortOrder,
            "isArchived" to category.isArchived,
            "isSystemDefault" to category.isSystemDefault,
            "createdAt" to category.createdAt,
            "updatedAt" to category.updatedAt
        )
    }

    private fun toItemPayload(item: Item): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "id" to item.id,
            "categoryId" to item.categoryId,
            "name" to item.name,
            "tags" to item.tags,
            "customAttributes" to linkedMapOf<String, Any?>().apply {
                item.customAttributes.forEach { (key, value) ->
                    put(key, value)
                }
            },
            "createdAt" to item.createdAt,
            "updatedAt" to item.updatedAt
        )
        item.purchaseDate?.let { payload["purchaseDate"] = it }
        item.purchasePrice?.let { payload["purchasePrice"] = it }
        item.purchaseCurrency?.let { payload["purchaseCurrency"] = it }
        item.purchasePlace?.let { payload["purchasePlace"] = it }
        item.description?.let { payload["description"] = it }
        item.deletedAt?.let { payload["deletedAt"] = it }
        return payload
    }

    private companion object {
        const val FORMAT_VERSION = "1.0"
        const val SCHEMA_VERSION = "1.0"
    }
}

