package com.example.itemmanagementandroid.backup.importing

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class BackupJsonParser {
    fun parse(archivePayload: BackupArchivePayload): ParsedBackupPackage {
        val warnings = mutableListOf<BackupImportWarning>()
        val manifestJson = parseJsonObject(
            payload = archivePayload.manifestJson,
            fileName = "manifest.json"
        )
        val dataJson = parseJsonObject(
            payload = archivePayload.dataJson,
            fileName = "data.json"
        )

        val manifest = BackupImportManifest(
            formatVersion = manifestJson.optTrimmedString("formatVersion")
                ?: throw BackupImportException.InvalidPackage(
                    "manifest.json is missing required field formatVersion."
                ),
            exportMode = manifestJson.optTrimmedString("exportMode")
        )

        val categories = parseCategories(
            categoriesJson = dataJson.optJSONArray("categories"),
            warnings = warnings
        )
        val items = parseItems(
            itemsJson = dataJson.optJSONArray("items"),
            warnings = warnings
        )
        val itemPhotos = parseItemPhotos(
            itemPhotosJson = dataJson.optJSONArray("itemPhotos"),
            warnings = warnings
        )

        val backupData = BackupImportData(
            schemaVersion = dataJson.optTrimmedString("schemaVersion")
                ?: throw BackupImportException.InvalidPackage(
                    "data.json is missing required field schemaVersion."
                ),
            categories = categories,
            items = items,
            itemPhotos = itemPhotos
        )

        return ParsedBackupPackage(
            manifest = manifest,
            data = backupData,
            warnings = warnings
        )
    }

    private fun parseJsonObject(
        payload: ByteArray,
        fileName: String
    ): JSONObject {
        val raw = payload.toString(StandardCharsets.UTF_8)
        return runCatching {
            JSONObject(raw)
        }.getOrElse { exception ->
            throw BackupImportException.InvalidPackage(
                message = "$fileName is not valid JSON.",
                cause = exception
            )
        }
    }

    private fun parseCategories(
        categoriesJson: JSONArray?,
        warnings: MutableList<BackupImportWarning>
    ): List<BackupImportCategory> {
        if (categoriesJson == null) {
            warnings += BackupImportWarning(
                code = "CATEGORIES_MISSING",
                message = "data.json does not contain categories array. Imported as empty."
            )
            return emptyList()
        }

        val categories = mutableListOf<BackupImportCategory>()
        for (index in 0 until categoriesJson.length()) {
            val node = categoriesJson.opt(index)
            val categoryJson = node as? JSONObject
            if (categoryJson == null) {
                warnings += BackupImportWarning(
                    code = "CATEGORY_SKIPPED",
                    message = "categories[$index] is not an object and was skipped."
                )
                continue
            }
            val id = categoryJson.optTrimmedString("id")
            val name = categoryJson.optTrimmedString("name")
            val createdAt = categoryJson.optTrimmedString("createdAt")
            val updatedAt = categoryJson.optTrimmedString("updatedAt")
            if (id == null || name == null || createdAt == null || updatedAt == null) {
                warnings += BackupImportWarning(
                    code = "CATEGORY_SKIPPED",
                    message = "categories[$index] is missing required fields and was skipped."
                )
                continue
            }
            categories += BackupImportCategory(
                id = id,
                name = name,
                sortOrder = categoryJson.optInt("sortOrder", 0),
                isArchived = categoryJson.optBoolean("isArchived", false),
                isSystemDefault = categoryJson.optBoolean("isSystemDefault", false),
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
        return categories
    }

    private fun parseItems(
        itemsJson: JSONArray?,
        warnings: MutableList<BackupImportWarning>
    ): List<BackupImportItem> {
        if (itemsJson == null) {
            warnings += BackupImportWarning(
                code = "ITEMS_MISSING",
                message = "data.json does not contain items array. Imported as empty."
            )
            return emptyList()
        }

        val items = mutableListOf<BackupImportItem>()
        for (index in 0 until itemsJson.length()) {
            val node = itemsJson.opt(index)
            val itemJson = node as? JSONObject
            if (itemJson == null) {
                warnings += BackupImportWarning(
                    code = "ITEM_SKIPPED",
                    message = "items[$index] is not an object and was skipped."
                )
                continue
            }

            val id = itemJson.optTrimmedString("id")
            val categoryId = itemJson.optTrimmedString("categoryId")
            val name = itemJson.optTrimmedString("name")
            val createdAt = itemJson.optTrimmedString("createdAt")
            val updatedAt = itemJson.optTrimmedString("updatedAt")
            if (id == null || categoryId == null || name == null || createdAt == null || updatedAt == null) {
                warnings += BackupImportWarning(
                    code = "ITEM_SKIPPED",
                    message = "items[$index] is missing required fields and was skipped."
                )
                continue
            }

            val tags = parseTags(itemJson.optJSONArray("tags"))
            val customAttributes = parseCustomAttributes(
                customAttributesJson = itemJson.optJSONObject("customAttributes"),
                warnings = warnings,
                itemIndex = index
            )
            items += BackupImportItem(
                id = id,
                categoryId = categoryId,
                name = name,
                purchaseDate = itemJson.optTrimmedString("purchaseDate"),
                purchasePrice = itemJson.optNullableDouble("purchasePrice"),
                purchaseCurrency = itemJson.optTrimmedString("purchaseCurrency"),
                purchasePlace = itemJson.optTrimmedString("purchasePlace"),
                description = itemJson.optTrimmedString("description"),
                tags = tags,
                customAttributes = customAttributes,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = itemJson.optTrimmedString("deletedAt")
            )
        }

        return items
    }

    private fun parseItemPhotos(
        itemPhotosJson: JSONArray?,
        warnings: MutableList<BackupImportWarning>
    ): List<BackupImportItemPhoto> {
        if (itemPhotosJson == null) {
            warnings += BackupImportWarning(
                code = "ITEM_PHOTOS_MISSING",
                message = "data.json does not contain itemPhotos array. Imported as empty."
            )
            return emptyList()
        }

        val photos = mutableListOf<BackupImportItemPhoto>()
        for (index in 0 until itemPhotosJson.length()) {
            val node = itemPhotosJson.opt(index)
            val photoJson = node as? JSONObject
            if (photoJson == null) {
                warnings += BackupImportWarning(
                    code = "ITEM_PHOTO_SKIPPED",
                    message = "itemPhotos[$index] is not an object and was skipped."
                )
                continue
            }

            val id = photoJson.optTrimmedString("id")
            val itemId = photoJson.optTrimmedString("itemId")
            val contentType = photoJson.optTrimmedString("contentType")
            val createdAt = photoJson.optTrimmedString("createdAt")
            if (id == null || itemId == null || contentType == null || createdAt == null) {
                warnings += BackupImportWarning(
                    code = "ITEM_PHOTO_SKIPPED",
                    message = "itemPhotos[$index] is missing required fields and was skipped."
                )
                continue
            }

            photos += BackupImportItemPhoto(
                id = id,
                itemId = itemId,
                contentType = contentType,
                createdAt = createdAt,
                fileName = photoJson.optTrimmedString("fileName"),
                width = photoJson.optNullableInt("width"),
                height = photoJson.optNullableInt("height"),
                kind = photoJson.optTrimmedString("kind")
            )
        }

        return photos
    }

    private fun parseTags(tagsJson: JSONArray?): List<String> {
        if (tagsJson == null) {
            return emptyList()
        }
        val normalizedTags = linkedSetOf<String>()
        for (index in 0 until tagsJson.length()) {
            val value = tagsJson.opt(index)
            if (value is String) {
                value.trim().takeIf(String::isNotEmpty)?.let(normalizedTags::add)
            }
        }
        return normalizedTags.toList()
    }

    private fun parseCustomAttributes(
        customAttributesJson: JSONObject?,
        warnings: MutableList<BackupImportWarning>,
        itemIndex: Int
    ): Map<String, Any> {
        if (customAttributesJson == null) {
            return emptyMap()
        }

        val attributes = linkedMapOf<String, Any>()
        val keys = customAttributesJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val normalizedKey = key.trim()
            if (normalizedKey.isEmpty()) {
                warnings += BackupImportWarning(
                    code = "CUSTOM_ATTRIBUTE_SKIPPED",
                    message = "items[$itemIndex].customAttributes contains blank key and it was skipped."
                )
                continue
            }

            val value = customAttributesJson.opt(key)
            when (value) {
                is String -> attributes[normalizedKey] = value
                is Boolean -> attributes[normalizedKey] = value
                is Int -> attributes[normalizedKey] = value.toLong()
                is Long -> attributes[normalizedKey] = value
                is Float -> attributes[normalizedKey] = value.toDouble()
                is Double -> attributes[normalizedKey] = value
                else -> {
                    warnings += BackupImportWarning(
                        code = "CUSTOM_ATTRIBUTE_SKIPPED",
                        message = "items[$itemIndex].customAttributes.$normalizedKey is not scalar and was skipped."
                    )
                }
            }
        }
        return attributes
    }

    private fun JSONObject.optTrimmedString(key: String): String? {
        if (!has(key)) {
            return null
        }
        val value = opt(key)
        if (value == null || value == JSONObject.NULL) {
            return null
        }
        val normalized = value.toString().trim()
        return normalized.takeIf(String::isNotEmpty)
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key)) {
            return null
        }
        val value = opt(key)
        if (value == null || value == JSONObject.NULL) {
            return null
        }
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key)) {
            return null
        }
        val value = opt(key)
        if (value == null || value == JSONObject.NULL) {
            return null
        }
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }
}
