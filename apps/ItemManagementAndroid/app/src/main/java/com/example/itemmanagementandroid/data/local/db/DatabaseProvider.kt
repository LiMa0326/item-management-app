package com.example.itemmanagementandroid.data.local.db

import android.content.Context

object DatabaseProvider {
    @Volatile
    private var instance: ItemManagementDatabase? = null

    fun get(context: Context): ItemManagementDatabase {
        return instance ?: synchronized(this) {
            instance ?: ItemManagementDatabase.build(context).also { database ->
                instance = database
            }
        }
    }
}
