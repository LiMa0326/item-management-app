package com.example.itemmanagementandroid.domain.model

data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val isArchived: Boolean,
    val isSystemDefault: Boolean,
    val createdAt: String,
    val updatedAt: String
)
