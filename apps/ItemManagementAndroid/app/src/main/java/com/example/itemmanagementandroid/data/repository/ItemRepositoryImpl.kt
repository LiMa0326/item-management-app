package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.ItemDao
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.repository.json.ItemJsonCodec
import com.example.itemmanagementandroid.data.repository.json.OrgJsonItemJsonCodec
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.repository.ItemRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

class ItemRepositoryImpl(
    private val itemDao: ItemDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val jsonCodec: ItemJsonCodec = OrgJsonItemJsonCodec()
) : ItemRepository {
    override suspend fun list(includeDeleted: Boolean): List<Item> {
        val entities = if (includeDeleted) {
            itemDao.listAllOrdered()
        } else {
            itemDao.listActiveOrdered()
        }
        return entities.map(::toDomain)
    }

    override suspend fun get(itemId: String): Item? {
        return itemDao.getById(itemId)?.let(::toDomain)
    }

    override suspend fun create(draft: ItemDraft): Item {
        val now = nowIsoString()
        val normalizedDraft = normalizeDraft(draft)
        val entity = ItemEntity(
            id = idGenerator(),
            categoryId = normalizedDraft.categoryId,
            name = normalizedDraft.name,
            purchaseDate = normalizedDraft.purchaseDate,
            purchasePrice = normalizedDraft.purchasePrice,
            purchaseCurrency = normalizedDraft.purchaseCurrency,
            purchasePlace = normalizedDraft.purchasePlace,
            description = normalizedDraft.description,
            tagsJson = jsonCodec.encodeTags(normalizedDraft.tags),
            customAttributesJson = jsonCodec.encodeCustomAttributes(normalizedDraft.customAttributes),
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )
        itemDao.insert(entity)
        return toDomain(entity)
    }

    override suspend fun update(itemId: String, draft: ItemDraft): Item {
        val existing = requireItem(itemId)
        val now = nowIsoString()
        val normalizedDraft = normalizeDraft(draft)
        val updated = existing.copy(
            categoryId = normalizedDraft.categoryId,
            name = normalizedDraft.name,
            purchaseDate = normalizedDraft.purchaseDate,
            purchasePrice = normalizedDraft.purchasePrice,
            purchaseCurrency = normalizedDraft.purchaseCurrency,
            purchasePlace = normalizedDraft.purchasePlace,
            description = normalizedDraft.description,
            tagsJson = jsonCodec.encodeTags(normalizedDraft.tags),
            customAttributesJson = jsonCodec.encodeCustomAttributes(normalizedDraft.customAttributes),
            updatedAt = now
        )
        itemDao.update(updated)
        return toDomain(updated)
    }

    override suspend fun softDelete(itemId: String): Item {
        val existing = requireItem(itemId)
        if (existing.deletedAt != null) {
            return toDomain(existing)
        }

        val now = nowIsoString()
        val deleted = existing.copy(
            updatedAt = now,
            deletedAt = now
        )
        itemDao.update(deleted)
        return toDomain(deleted)
    }

    override suspend fun restore(itemId: String): Item {
        val existing = requireItem(itemId)
        if (existing.deletedAt == null) {
            return toDomain(existing)
        }

        val restored = existing.copy(
            updatedAt = nowIsoString(),
            deletedAt = null
        )
        itemDao.update(restored)
        return toDomain(restored)
    }

    private suspend fun requireItem(itemId: String): ItemEntity {
        return requireNotNull(itemDao.getById(itemId)) {
            "Item not found: $itemId"
        }
    }

    private fun nowIsoString(): String = Instant.now(clock).toString()

    private fun toDomain(entity: ItemEntity): Item {
        return Item(
            id = entity.id,
            categoryId = entity.categoryId,
            name = entity.name,
            purchaseDate = entity.purchaseDate,
            purchasePrice = entity.purchasePrice,
            purchaseCurrency = entity.purchaseCurrency,
            purchasePlace = entity.purchasePlace,
            description = entity.description,
            tags = jsonCodec.decodeTags(entity.tagsJson),
            customAttributes = jsonCodec.decodeCustomAttributes(entity.customAttributesJson),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt
        )
    }

    private fun normalizeDraft(draft: ItemDraft): ItemDraft {
        val categoryId = draft.categoryId.trim()
        require(categoryId.isNotEmpty()) {
            "Item categoryId must not be blank."
        }

        val name = draft.name.trim()
        require(name.isNotEmpty()) {
            "Item name must not be blank."
        }

        return ItemDraft(
            categoryId = categoryId,
            name = name,
            purchaseDate = draft.purchaseDate?.trim()?.takeIf(String::isNotEmpty),
            purchasePrice = draft.purchasePrice,
            purchaseCurrency = draft.purchaseCurrency?.trim()?.takeIf(String::isNotEmpty),
            purchasePlace = draft.purchasePlace?.trim()?.takeIf(String::isNotEmpty),
            description = draft.description?.trim()?.takeIf(String::isNotEmpty),
            tags = normalizeTags(draft.tags),
            customAttributes = normalizeCustomAttributes(draft.customAttributes)
        )
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }

    private fun normalizeCustomAttributes(customAttributes: Map<String, Any>): Map<String, Any> {
        val normalized = linkedMapOf<String, Any>()
        customAttributes.forEach { (rawKey, rawValue) ->
            val key = rawKey.trim()
            require(key.isNotEmpty()) {
                "customAttributes key must not be blank."
            }
            normalized[key] = normalizeCustomAttributeValue(rawValue, key)
        }
        return normalized
    }

    private fun normalizeCustomAttributeValue(value: Any, key: String): Any {
        return when (value) {
            is String -> value.trim()
            is Boolean -> value
            is Byte -> value.toLong()
            is Short -> value.toLong()
            is Int -> value.toLong()
            is Long -> value
            is Float -> {
                require(value.isFinite()) { "customAttributes[$key] must be finite." }
                value.toDouble()
            }
            is Double -> {
                require(value.isFinite()) { "customAttributes[$key] must be finite." }
                value
            }
            else -> throw IllegalArgumentException(
                "customAttributes[$key] must be string|number|boolean."
            )
        }
    }
}
