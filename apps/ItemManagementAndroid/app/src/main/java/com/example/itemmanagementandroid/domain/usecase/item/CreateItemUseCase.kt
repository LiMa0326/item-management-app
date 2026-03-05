package com.example.itemmanagementandroid.domain.usecase.item

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.repository.ItemRepository

class CreateItemUseCase(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(draft: ItemDraft): Item {
        return itemRepository.create(draft = draft)
    }
}
