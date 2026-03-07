package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.ItemDao
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.repository.json.ItemJsonCodec
import com.example.itemmanagementandroid.data.repository.json.OrgJsonItemJsonCodec
import com.example.itemmanagementandroid.domain.model.DuplicateItemNameException
import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.model.ItemListSortOption
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
    override suspend fun list(query: ItemListQuery): List<Item> {
        val entities = if (query.includeDeleted) {
            itemDao.listAllOrdered()
        } else {
            itemDao.listActiveOrdered()
        }

        val filtered = if (query.categoryId == null) {
            entities
        } else {
            entities.filter { entity -> entity.categoryId == query.categoryId }
        }

        return filtered
            .map(::toDomain)
            .sortedWith(comparatorFor(query.sortOption))
    }

    override suspend fun get(itemId: String): Item? {
        return itemDao.getById(itemId)?.let(::toDomain)
    }

    override suspend fun create(draft: ItemDraft): Item {
        val now = nowIsoString()
        val normalizedDraft = normalizeDraft(draft)
        requireUniqueNameForCreate(normalizedDraft.name)
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
        requireUniqueNameForUpdate(
            name = normalizedDraft.name,
            itemId = itemId
        )
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

    private suspend fun requireUniqueNameForCreate(name: String) {
        if (itemDao.countActiveByNormalizedName(name) > 0) {
            throw DuplicateItemNameException()
        }
    }

    private suspend fun requireUniqueNameForUpdate(name: String, itemId: String) {
        if (itemDao.countActiveByNormalizedNameExcludingId(name, itemId) > 0) {
            throw DuplicateItemNameException()
        }
    }

    private fun comparatorFor(sortOption: ItemListSortOption): Comparator<Item> {
        return Comparator { left, right ->
            val primary = when (sortOption) {
                ItemListSortOption.RECENTLY_ADDED -> right.createdAt.compareTo(left.createdAt)
                ItemListSortOption.RECENTLY_UPDATED -> right.updatedAt.compareTo(left.updatedAt)
                ItemListSortOption.PURCHASE_DATE -> compareNullableDescending(
                    left = left.purchaseDate,
                    right = right.purchaseDate
                )
                ItemListSortOption.PURCHASE_PRICE -> compareNullableDescending(
                    left = left.purchasePrice,
                    right = right.purchasePrice
                )
            }
            if (primary != 0) {
                primary
            } else {
                compareStableFallback(left = left, right = right)
            }
        }
    }

    private fun <T : Comparable<T>> compareNullableDescending(
        left: T?,
        right: T?
    ): Int {
        if (left == null && right == null) {
            return 0
        }
        if (left == null) {
            return 1
        }
        if (right == null) {
            return -1
        }
        return right.compareTo(left)
    }

    private fun compareStableFallback(
        left: Item,
        right: Item
    ): Int {
        val updatedCompare = right.updatedAt.compareTo(left.updatedAt)
        if (updatedCompare != 0) {
            return updatedCompare
        }

        val createdCompare = right.createdAt.compareTo(left.createdAt)
        if (createdCompare != 0) {
            return createdCompare
        }

        return left.id.compareTo(right.id)
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
