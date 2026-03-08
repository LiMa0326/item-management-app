package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.model.PhotoImportFailure
import com.example.itemmanagementandroid.domain.model.PhotoImportSummary
import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset
import com.example.itemmanagementandroid.domain.repository.PhotoAssetProcessor

class ImportItemPhotosUseCase(
    private val photoAssetProcessor: PhotoAssetProcessor,
    private val addItemPhotoUseCase: AddItemPhotoUseCase
) {
    suspend operator fun invoke(
        itemId: String,
        sourceUris: List<String>
    ): PhotoImportSummary {
        val normalizedItemId = itemId.trim()
        require(normalizedItemId.isNotEmpty()) {
            "itemId must not be blank."
        }

        val importedPhotos = mutableListOf<ItemPhoto>()
        val failures = mutableListOf<PhotoImportFailure>()

        sourceUris.map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .forEach { sourceUri ->
                var processedAsset: ProcessedPhotoAsset? = null
                runCatching {
                    processedAsset = photoAssetProcessor.process(sourceUri = sourceUri)
                    val asset = requireNotNull(processedAsset)
                    addItemPhotoUseCase(
                        draft = ItemPhotoDraft(
                            itemId = normalizedItemId,
                            localUri = asset.localUri,
                            thumbnailUri = asset.thumbnailUri,
                            contentType = asset.contentType,
                            width = asset.width,
                            height = asset.height
                        )
                    )
                }.onSuccess { createdPhoto ->
                    importedPhotos += createdPhoto
                }.onFailure { throwable ->
                    processedAsset?.let { asset ->
                        runCatching { photoAssetProcessor.delete(asset) }
                    }
                    failures += PhotoImportFailure(
                        sourceUri = sourceUri,
                        reason = throwable.message ?: "Failed to import photo."
                    )
                }
            }

        return PhotoImportSummary(
            importedPhotos = importedPhotos,
            failures = failures
        )
    }
}
