package com.example.itemmanagementandroid.domain.model

data class PhotoImportSummary(
    val importedPhotos: List<ItemPhoto>,
    val failures: List<PhotoImportFailure>
) {
    val successCount: Int
        get() = importedPhotos.size

    val failureCount: Int
        get() = failures.size
}
