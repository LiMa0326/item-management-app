package com.example.itemmanagementandroid.data.repository

import com.example.itemmanagementandroid.data.local.dao.ItemPhotoDao
import com.example.itemmanagementandroid.data.local.dao.model.DeferredPhotoCleanupRow
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity
import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.repository.PhotoRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

class PhotoRepositoryImpl(
    private val itemPhotoDao: ItemPhotoDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) : PhotoRepository {
    override suspend fun listByItem(itemId: String): List<ItemPhoto> {
        val normalizedItemId = normalizeRequiredField(
            value = itemId,
            fieldName = "itemId"
        )
        return itemPhotoDao.listByItemOrdered(normalizedItemId).map(::toDomain)
    }

    override suspend fun get(photoId: String): ItemPhoto? {
        val normalizedPhotoId = normalizeRequiredField(
            value = photoId,
            fieldName = "photoId"
        )
        return itemPhotoDao.getById(normalizedPhotoId)?.let(::toDomain)
    }

    override suspend fun add(draft: ItemPhotoDraft): ItemPhoto {
        val normalizedDraft = normalizeDraft(draft)
        val entity = ItemPhotoEntity(
            id = idGenerator(),
            itemId = normalizedDraft.itemId,
            localUri = normalizedDraft.localUri,
            thumbnailUri = normalizedDraft.thumbnailUri,
            contentType = normalizedDraft.contentType,
            width = normalizedDraft.width,
            height = normalizedDraft.height,
            createdAt = nowIsoString()
        )
        itemPhotoDao.insert(entity)
        return toDomain(entity)
    }

    override suspend fun remove(photoId: String): Boolean {
        val normalizedPhotoId = normalizeRequiredField(
            value = photoId,
            fieldName = "photoId"
        )
        return itemPhotoDao.deleteById(normalizedPhotoId) > 0
    }

    override suspend fun listDeferredCleanupCandidates(): List<DeferredPhotoCleanupCandidate> {
        return itemPhotoDao.listDeferredCleanupRows().map(::toDeferredCleanupCandidate)
    }

    private fun nowIsoString(): String = Instant.now(clock).toString()

    private fun toDomain(entity: ItemPhotoEntity): ItemPhoto {
        return ItemPhoto(
            id = entity.id,
            itemId = entity.itemId,
            localUri = entity.localUri,
            thumbnailUri = entity.thumbnailUri,
            contentType = entity.contentType,
            width = entity.width,
            height = entity.height,
            createdAt = entity.createdAt
        )
    }

    private fun toDeferredCleanupCandidate(
        row: DeferredPhotoCleanupRow
    ): DeferredPhotoCleanupCandidate {
        return DeferredPhotoCleanupCandidate(
            photoId = row.photoId,
            itemId = row.itemId,
            localUri = row.localUri,
            thumbnailUri = row.thumbnailUri,
            contentType = row.contentType,
            itemDeletedAt = row.itemDeletedAt
        )
    }

    private fun normalizeDraft(draft: ItemPhotoDraft): ItemPhotoDraft {
        val itemId = normalizeRequiredField(
            value = draft.itemId,
            fieldName = "itemId"
        )
        val localUri = normalizeRequiredField(
            value = draft.localUri,
            fieldName = "localUri"
        )
        val contentType = normalizeRequiredField(
            value = draft.contentType,
            fieldName = "contentType"
        )
        val thumbnailUri = draft.thumbnailUri?.trim()?.takeIf(String::isNotEmpty)
        val width = normalizeDimension(
            value = draft.width,
            fieldName = "width"
        )
        val height = normalizeDimension(
            value = draft.height,
            fieldName = "height"
        )
        return ItemPhotoDraft(
            itemId = itemId,
            localUri = localUri,
            thumbnailUri = thumbnailUri,
            contentType = contentType,
            width = width,
            height = height
        )
    }

    private fun normalizeDimension(value: Int?, fieldName: String): Int? {
        if (value == null) {
            return null
        }
        require(value > 0) {
            "$fieldName must be greater than 0."
        }
        return value
    }

    private fun normalizeRequiredField(value: String, fieldName: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) {
            "$fieldName must not be blank."
        }
        return normalized
    }
}
