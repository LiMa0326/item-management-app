package com.example.itemmanagementandroid.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset
import com.example.itemmanagementandroid.domain.repository.PhotoAssetProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidPhotoAssetProcessor(
    private val appContext: Context,
    private val config: PhotoProcessingConfig = PhotoProcessingConfig(),
    private val storage: AppPrivatePhotoStorage = AppPrivatePhotoStorage(appContext)
) : PhotoAssetProcessor {
    override suspend fun process(sourceUri: String): ProcessedPhotoAsset = withContext(Dispatchers.IO) {
        val normalizedSourceUri = sourceUri.trim()
        require(normalizedSourceUri.isNotEmpty()) {
            "sourceUri must not be blank."
        }
        val uri = Uri.parse(normalizedSourceUri)
        var sourceBitmap: Bitmap? = null
        var normalizedBitmap: Bitmap? = null
        var thumbnailBitmap: Bitmap? = null
        var fullFile: File? = null
        var thumbnailFile: File? = null
        try {
            sourceBitmap = decodeBitmap(uri)
            val orientation = readExifOrientation(uri)
            normalizedBitmap = applyExifOrientation(
                sourceBitmap = sourceBitmap,
                orientation = orientation
            )

            fullFile = storage.createFullImageFile()
            writeJpeg(
                bitmap = normalizedBitmap,
                targetFile = fullFile,
                quality = config.fullImageJpegQuality
            )

            thumbnailBitmap = createThumbnailBitmap(
                sourceBitmap = normalizedBitmap,
                maxLongSidePx = config.thumbnailMaxLongSidePx
            )
            thumbnailFile = storage.createThumbnailFile()
            writeJpeg(
                bitmap = thumbnailBitmap,
                targetFile = thumbnailFile,
                quality = config.thumbnailJpegQuality
            )

            ProcessedPhotoAsset(
                sourceUri = normalizedSourceUri,
                localUri = fullFile.toURI().toString(),
                thumbnailUri = thumbnailFile.toURI().toString(),
                contentType = JPEG_CONTENT_TYPE,
                width = normalizedBitmap.width,
                height = normalizedBitmap.height
            )
        } catch (throwable: Throwable) {
            fullFile?.let { runCatching { it.delete() } }
            thumbnailFile?.let { runCatching { it.delete() } }
            throw throwable
        } finally {
            if (thumbnailBitmap != null && thumbnailBitmap !== normalizedBitmap) {
                thumbnailBitmap.recycle()
            }
            if (normalizedBitmap != null && normalizedBitmap !== sourceBitmap) {
                normalizedBitmap.recycle()
            }
            sourceBitmap?.recycle()
        }
    }

    override suspend fun delete(asset: ProcessedPhotoAsset) {
        withContext(Dispatchers.IO) {
            storage.deleteByUri(asset.localUri)
            storage.deleteByUri(asset.thumbnailUri)
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val decoded = appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) {
                "Unable to open input stream for uri: $uri"
            }
            BitmapFactory.decodeStream(input)
        }
        requireNotNull(decoded) {
            "Failed to decode image from uri: $uri"
        }
        return decoded
    }

    private fun readExifOrientation(uri: Uri): Int {
        return runCatching {
            appContext.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    ExifInterface.ORIENTATION_NORMAL
                } else {
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    }

    private fun applyExifOrientation(
        sourceBitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> Unit
        }
        if (matrix.isIdentity) {
            return sourceBitmap
        }
        return Bitmap.createBitmap(
            sourceBitmap,
            0,
            0,
            sourceBitmap.width,
            sourceBitmap.height,
            matrix,
            true
        )
    }

    private fun createThumbnailBitmap(
        sourceBitmap: Bitmap,
        maxLongSidePx: Int
    ): Bitmap {
        require(maxLongSidePx > 0) {
            "maxLongSidePx must be greater than 0."
        }
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val longSide = maxOf(width, height)
        if (longSide <= maxLongSidePx) {
            return sourceBitmap
        }
        val scale = maxLongSidePx.toFloat() / longSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(
            sourceBitmap,
            targetWidth,
            targetHeight,
            true
        )
    }

    private fun writeJpeg(
        bitmap: Bitmap,
        targetFile: File,
        quality: Int
    ) {
        targetFile.outputStream().use { output ->
            val compressed = bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality.coerceIn(0, 100),
                output
            )
            check(compressed) {
                "Failed to compress image into JPEG: ${targetFile.absolutePath}"
            }
        }
    }

    private companion object {
        const val JPEG_CONTENT_TYPE = "image/jpeg"
    }
}
