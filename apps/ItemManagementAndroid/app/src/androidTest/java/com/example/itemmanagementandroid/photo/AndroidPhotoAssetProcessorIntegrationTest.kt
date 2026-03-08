package com.example.itemmanagementandroid.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.itemmanagementandroid.domain.model.ProcessedPhotoAsset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidPhotoAssetProcessorIntegrationTest {
    @Test
    fun processTwentyImages_successAndThumbnailConstraints() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val processor = AndroidPhotoAssetProcessor(
            appContext = context,
            config = PhotoProcessingConfig(
                thumbnailMaxLongSidePx = 1280,
                thumbnailJpegQuality = 85,
                fullImageJpegQuality = 92
            )
        )
        val sourceFiles = createSourceImages(context, count = 20)
        val processedAssets = mutableListOf<ProcessedPhotoAsset>()

        sourceFiles.forEach { sourceFile ->
            val sourceUri = Uri.fromFile(sourceFile).toString()
            val processedAsset = processor.process(sourceUri = sourceUri)
            processedAssets += processedAsset

            val fullFile = fileFromUri(processedAsset.localUri)
            val thumbnailFile = fileFromUri(processedAsset.thumbnailUri)
            assertTrue(fullFile.exists())
            assertTrue(thumbnailFile.exists())
            assertEquals("jpg", fullFile.extension.lowercase())
            assertEquals("jpg", thumbnailFile.extension.lowercase())

            val thumbnailBitmap = BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            assertNotNull(thumbnailBitmap)
            val thumbLongSide = maxOf(thumbnailBitmap.width, thumbnailBitmap.height)
            assertTrue(thumbLongSide <= 1280)
            thumbnailBitmap.recycle()

            val exif = ExifInterface(fullFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            assertTrue(
                orientation == ExifInterface.ORIENTATION_UNDEFINED ||
                    orientation == ExifInterface.ORIENTATION_NORMAL
            )
        }

        assertEquals(20, processedAssets.size)

        processedAssets.forEach { asset ->
            processor.delete(asset)
        }
        sourceFiles.forEach { file -> file.delete() }
    }

    private fun createSourceImages(
        context: Context,
        count: Int
    ): List<File> {
        val sourceDirectory = File(context.cacheDir, "photo-import-source")
        if (!sourceDirectory.exists()) {
            sourceDirectory.mkdirs()
        }
        return List(count) { index ->
            val width = 1800 + (index * 7)
            val height = 1200 + (index * 5)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val file = File(sourceDirectory, "source_$index.jpg")
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            bitmap.recycle()
            file
        }
    }

    private fun fileFromUri(uriString: String): File {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == "file") {
            File(requireNotNull(uri.path) { "Invalid file uri: $uriString" })
        } else {
            File(uriString)
        }
    }
}
