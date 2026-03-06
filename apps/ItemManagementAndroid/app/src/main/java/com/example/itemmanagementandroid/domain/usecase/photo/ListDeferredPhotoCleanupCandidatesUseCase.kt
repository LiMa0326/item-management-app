package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.DeferredPhotoCleanupCandidate
import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class ListDeferredPhotoCleanupCandidatesUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(): List<DeferredPhotoCleanupCandidate> {
        return photoRepository.listDeferredCleanupCandidates()
    }
}
