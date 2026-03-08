package com.example.itemmanagementandroid.backup.storage

import android.content.Context

class BackupDirectoryPreferenceStore(
    appContext: Context
) {
    private val preferences = appContext.getSharedPreferences(
        PREFERENCE_NAME,
        Context.MODE_PRIVATE
    )

    fun getBackupTreeUri(): String? {
        return preferences.getString(KEY_BACKUP_TREE_URI, null)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    fun setBackupTreeUri(treeUri: String) {
        preferences.edit()
            .putString(KEY_BACKUP_TREE_URI, treeUri)
            .apply()
    }

    private companion object {
        const val PREFERENCE_NAME = "backup_storage_preferences"
        const val KEY_BACKUP_TREE_URI = "backup_tree_uri"
    }
}
