package com.example.itemmanagementandroid.domain.model

data class ItemDraft(
    val categoryId: String,
    val name: String,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val purchaseCurrency: String? = null,
    val purchasePlace: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val customAttributes: Map<String, Any> = emptyMap()
)
