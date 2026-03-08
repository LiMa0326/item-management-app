package com.example.itemmanagementandroid.domain.repository

import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset

interface PhotoAssetProcessor {
    suspend fun process(sourceUri: String): ProcessedPhotoAsset

    suspend fun delete(asset: ProcessedPhotoAsset)
}
