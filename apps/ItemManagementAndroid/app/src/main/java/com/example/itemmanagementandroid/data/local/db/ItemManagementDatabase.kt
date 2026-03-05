package com.example.itemmanagementandroid.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.itemmanagementandroid.data.local.dao.CategoryDao
import com.example.itemmanagementandroid.data.local.entity.CategoryEntity
import com.example.itemmanagementandroid.data.local.entity.ItemEntity
import com.example.itemmanagementandroid.data.local.entity.ItemPhotoEntity

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ItemPhotoEntity::class
    ],
    version = DatabaseVersions.CURRENT,
    exportSchema = true
)
abstract class ItemManagementDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME: String = "item_management.db"

        fun build(context: Context): ItemManagementDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ItemManagementDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*DatabaseMigrations.ALL)
                .build()
        }
    }
}
