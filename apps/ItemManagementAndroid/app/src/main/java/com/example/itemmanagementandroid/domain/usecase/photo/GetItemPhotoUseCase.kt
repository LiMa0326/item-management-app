package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class GetItemPhotoUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(photoId: String): ItemPhoto? {
        return photoRepository.get(photoId = photoId)
    }
}
