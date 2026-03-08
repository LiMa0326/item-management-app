package com.example.itemmanagementandroid.backup.export

enum class ExportMode(
    val wireValue: String
) {
    METADATA_ONLY("metadata_only"),
    THUMBNAILS("thumbnails"),
    FULL("full")
}

