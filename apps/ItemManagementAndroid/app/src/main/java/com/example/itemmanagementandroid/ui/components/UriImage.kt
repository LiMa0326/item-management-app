package com.example.itemmanagementandroid.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun UriImage(
    uri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderText: String = "No Photo"
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, uri, context) {
        value = withContext(Dispatchers.IO) {
            decodeBitmap(
                context = context,
                uriString = uri
            )
        }
    }
    val bitmap = bitmapState.value
    if (bitmap == null) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = placeholderText,
                style = MaterialTheme.typography.labelSmall
            )
        }
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}

private fun decodeBitmap(
    context: android.content.Context,
    uriString: String?
): Bitmap? {
    val normalizedUriString = uriString?.trim().orEmpty()
    if (normalizedUriString.isEmpty()) {
        return null
    }
    val uri = Uri.parse(normalizedUriString)
    return when (uri.scheme?.lowercase()) {
        null, "", "file" -> {
            val file = if (uri.scheme == "file") {
                File(requireNotNull(uri.path) { "Invalid file uri: $normalizedUriString" })
            } else {
                File(normalizedUriString)
            }
            if (!file.exists()) {
                null
            } else {
                BitmapFactory.decodeFile(file.absolutePath)
            }
        }

        "content" -> {
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    null
                } else {
                    BitmapFactory.decodeStream(input)
                }
            }
        }

        else -> null
    }
}
