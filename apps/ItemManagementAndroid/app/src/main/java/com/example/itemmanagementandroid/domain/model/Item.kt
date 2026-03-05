package com.example.itemmanagementandroid.domain.model

data class Item(
    val id: String,
    val categoryId: String,
    val name: String,
    val purchaseDate: String?,
    val purchasePrice: Double?,
    val purchaseCurrency: String?,
    val purchasePlace: String?,
    val description: String?,
    val tags: List<String>,
    val customAttributes: Map<String, Any>,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?
)
