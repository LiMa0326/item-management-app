package com.example.itemmanagementandroid.domain.usecase.item

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.repository.ItemRepository

class GetItemUseCase(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(itemId: String): Item? {
        return itemRepository.get(itemId = itemId)
    }
}
