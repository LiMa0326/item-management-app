package com.example.itemmanagementandroid.domain.repository

import com.example.itemmanagementandroid.domain.model.Item
import com.example.itemmanagementandroid.domain.model.ItemDraft

interface ItemRepository {
    suspend fun list(includeDeleted: Boolean = false): List<Item>
    suspend fun get(itemId: String): Item?
    suspend fun create(draft: ItemDraft): Item
    suspend fun update(itemId: String, draft: ItemDraft): Item
    suspend fun softDelete(itemId: String): Item
    suspend fun restore(itemId: String): Item
}
