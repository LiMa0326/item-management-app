package com.example.itemmanagementandroid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"]
        )
    ],
    indices = [
        Index(value = ["category_id"], name = "idx_items_category_id"),
        Index(value = ["updated_at"], name = "idx_items_updated_at"),
        Index(value = ["purchase_date"], name = "idx_items_purchase_date"),
        Index(value = ["purchase_price"], name = "idx_items_purchase_price"),
        Index(value = ["deleted_at"], name = "idx_items_deleted_at")
    ]
)
data class ItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "purchase_date")
    val purchaseDate: String? = null,
    @ColumnInfo(name = "purchase_price")
    val purchasePrice: Double? = null,
    @ColumnInfo(name = "purchase_currency")
    val purchaseCurrency: String? = null,
    @ColumnInfo(name = "purchase_place")
    val purchasePlace: String? = null,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "tags_json", defaultValue = "'[]'")
    val tagsJson: String = "[]",
    @ColumnInfo(name = "custom_attributes_json", defaultValue = "'{}'")
    val customAttributesJson: String = "{}",
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String? = null
)
