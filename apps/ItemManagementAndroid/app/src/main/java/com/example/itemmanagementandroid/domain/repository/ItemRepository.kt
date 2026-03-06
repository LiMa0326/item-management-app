package com.example.itemmanagementandroid.domain.repository

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft
import com.example.itemmanagementandroid.domain.model.ItemListQuery

interface ItemRepository {
    suspend fun list(query: ItemListQuery): List<Item>

    suspend fun list(includeDeleted: Boolean = false): List<Item> {
        return list(
            query = ItemListQuery(includeDeleted = includeDeleted)
        )
    }

    suspend fun get(itemId: String): Item?
    suspend fun create(draft: ItemDraft): Item
    suspend fun update(itemId: String, draft: ItemDraft): Item
    suspend fun softDelete(itemId: String): Item
    suspend fun restore(itemId: String): Item
}
