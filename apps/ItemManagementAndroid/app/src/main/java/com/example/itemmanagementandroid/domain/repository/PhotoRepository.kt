package com.example.itemmanagementandroid.domain.repository

import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft

interface PhotoRepository {
    suspend fun listByItem(itemId: String): List<ItemPhoto>
    suspend fun get(photoId: String): ItemPhoto?
    suspend fun add(draft: ItemPhotoDraft): ItemPhoto
    suspend fun remove(photoId: String): Boolean
    suspend fun listDeferredCleanupCandidates(): List<DeferredPhotoCleanupCandidate>
}
