package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.ItemPhotoCover
import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class ListItemPhotoCoversUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(itemIds: List<String>): List<ItemPhotoCover> {
        if (itemIds.isEmpty()) {
            return emptyList()
        }
        return photoRepository.listCoversByItemIds(itemIds)
    }
}
