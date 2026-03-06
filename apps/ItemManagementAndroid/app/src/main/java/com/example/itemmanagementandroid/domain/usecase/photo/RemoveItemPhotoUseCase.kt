package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class RemoveItemPhotoUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(photoId: String): Boolean {
        return photoRepository.remove(photoId = photoId)
    }
}
