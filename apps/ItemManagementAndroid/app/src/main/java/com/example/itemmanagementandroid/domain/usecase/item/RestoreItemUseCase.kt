package com.example.itemmanagementandroid.domain.usecase.item

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.repository.ItemRepository

class RestoreItemUseCase(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(itemId: String): Item {
        return itemRepository.restore(itemId = itemId)
    }
}
