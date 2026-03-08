package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoCover
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset
import com.example.itemmanagementandroid.domain.repository.PhotoAssetProcessor
import com.example.itemmanagementandroid.domain.repository.PhotoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportItemPhotosUseCaseTest {
    private lateinit var fakePhotoRepository: FakePhotoRepository
    private lateinit var fakePhotoAssetProcessor: FakePhotoAssetProcessor
    private lateinit var useCase: ImportItemPhotosUseCase

    @Before
    fun setUp() {
        fakePhotoRepository = FakePhotoRepository()
        fakePhotoAssetProcessor = FakePhotoAssetProcessor()
        useCase = ImportItemPhotosUseCase(
            photoAssetProcessor = fakePhotoAssetProcessor,
            addItemPhotoUseCase = AddItemPhotoUseCase(fakePhotoRepository)
        )
    }

    @Test
    fun invoke_allSuccess_returnsImportedPhotos() = runBlocking {
        val summary = useCase(
            itemId = "item_1",
            sourceUris = listOf(
                "content://photo/a",
                "content://photo/b"
            )
        )

        assertEquals(2, summary.successCount)
        assertEquals(0, summary.failureCount)
        assertEquals(2, fakePhotoRepository.photos.size)
        assertTrue(fakePhotoAssetProcessor.deletedAssets.isEmpty())
    }

    @Test
    fun invoke_partialFailure_collectsFailuresAndCleansTemp() = runBlocking {
        fakePhotoAssetProcessor.failedUris += "content://photo/fail"

        val summary = useCase(
            itemId = "item_1",
            sourceUris = listOf(
                "content://photo/ok",
                "content://photo/fail"
            )
        )

        assertEquals(1, summary.successCount)
        assertEquals(1, summary.failureCount)
        assertEquals("content://photo/fail", summary.failures.first().sourceUri)
        assertEquals(0, fakePhotoAssetProcessor.deletedAssets.size)
    }

    private class FakePhotoRepository : PhotoRepository {
        val photos: MutableList<ItemPhoto> = mutableListOf()
        private var nextPhotoId: Int = 0

        override suspend fun listByItem(itemId: String): List<ItemPhoto> {
            return photos.filter { it.itemId == itemId }
        }

        override suspend fun get(photoId: String): ItemPhoto? {
            return photos.firstOrNull { it.id == photoId }
        }

        override suspend fun add(draft: ItemPhotoDraft): ItemPhoto {
            val created = ItemPhoto(
                id = "photo_${nextPhotoId++}",
                itemId = draft.itemId,
                localUri = draft.localUri,
                thumbnailUri = draft.thumbnailUri,
                contentType = draft.contentType,
                width = draft.width,
                height = draft.height,
                createdAt = "2026-03-08T00:00:00Z"
            )
            photos += created
            return created
        }

        override suspend fun remove(photoId: String): Boolean {
            return photos.removeIf { photo -> photo.id == photoId }
        }

        override suspend fun listDeferredCleanupCandidates(): List<DeferredPhotoCleanupCandidate> {
            return emptyList()
        }

        override suspend fun listCoversByItemIds(itemIds: List<String>): List<ItemPhotoCover> {
            return emptyList()
        }
    }

    private class FakePhotoAssetProcessor : PhotoAssetProcessor {
        val failedUris: MutableSet<String> = mutableSetOf()
        val deletedAssets: MutableList<ProcessedPhotoAsset> = mutableListOf()

        override suspend fun process(sourceUri: String): ProcessedPhotoAsset {
            if (failedUris.contains(sourceUri)) {
                throw IllegalStateException("forced failure")
            }
            val normalized = sourceUri.replace(Regex("[^A-Za-z0-9]"), "_")
            return ProcessedPhotoAsset(
                sourceUri = sourceUri,
                localUri = "file:///tmp/full_$normalized.jpg",
                thumbnailUri = "file:///tmp/thumb_$normalized.jpg",
                contentType = "image/jpeg",
                width = 1000,
                height = 700
            )
        }

        override suspend fun delete(asset: ProcessedPhotoAsset) {
            deletedAssets += asset
        }
    }
}
