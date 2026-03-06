package com.example.itemmanagementandroid.domain.usecase.item

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemListQuery
import com.example.itemmanagementandroid.domain.repository.ItemRepository

class ListItemsUseCase(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(query: ItemListQuery): List<Item> {
        return itemRepository.list(query = query)
    }

    suspend operator fun invoke(includeDeleted: Boolean = false): List<Item> {
        return invoke(
            query = ItemListQuery(includeDeleted = includeDeleted)
        )
    }
}
