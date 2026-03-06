package com.example.itemmanagementandroid.domain.model

data class DeferredPhotoCleanupCandidate(
    val photoId: String,
    val itemId: String,
    val localUri: String,
    val thumbnailUri: String?,
    val contentType: String,
    val itemDeletedAt: String,
    val marker: String = ITEM_SOFT_DELETED
) {
    companion object {
        const val ITEM_SOFT_DELETED: String = "ITEM_SOFT_DELETED"
    }
}
