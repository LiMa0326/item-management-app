package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class ListItemPhotosUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(itemId: String): List<ItemPhoto> {
        return photoRepository.listByItem(itemId = itemId)
    }
}
