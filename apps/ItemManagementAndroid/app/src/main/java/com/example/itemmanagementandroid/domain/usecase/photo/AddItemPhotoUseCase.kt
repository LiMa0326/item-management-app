package com.example.itemmanagementandroid.domain.usecase.photo

import com.example.itemmanagementandroid.domain.model.ItemPhoto
import com.example.itemmanagementandroid.domain.model.ItemPhotoDraft
import com.example.itemmanagementandroid.domain.repository.PhotoRepository

class AddItemPhotoUseCase(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(draft: ItemPhotoDraft): ItemPhoto {
        return photoRepository.add(draft = draft)
    }
}
