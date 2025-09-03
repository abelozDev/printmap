package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import androidx.core.net.toUri

internal fun getBitmapFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        context.assets.open(fileName).use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
/**
 * Загружает Bitmap по строковому пути.
 * Поддерживает:
 * - абсолютные file-пути: /storage/... или file://...
 * - content-URI: content://...
 *
 * requestedWidth/Height (необязательно): задайте, чтобы уменьшить изображение.
 */
fun loadBitmap(
    context: Context,
    path: String,
    requestedWidth: Int? = null,
    requestedHeight: Int? = null
): Bitmap? {
    return try {
        val isContentUri = path.startsWith("content://")
        val isFileUri = path.startsWith("file://")

        if (isContentUri || isFileUri) {
            val uri = path.toUri()
            decodeBitmapFromStream(context, uri, requestedWidth, requestedHeight)
        } else {
            decodeBitmapFromFile(path, requestedWidth, requestedHeight)
        }
    } catch (_: Throwable) {
        null
    }
}

private fun decodeBitmapFromFile(
    filePath: String,
    reqWidth: Int? = null,
    reqHeight: Int? = null
): Bitmap? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null

        if (reqWidth == null || reqHeight == null) {
            BitmapFactory.decodeFile(filePath)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, bounds)

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            }
            BitmapFactory.decodeFile(filePath, options)
        }
    } catch (_: Throwable) {
        null
    }
}

private fun decodeBitmapFromStream(
    context: Context,
    uri: Uri,
    reqWidth: Int? = null,
    reqHeight: Int? = null
): Bitmap? {
    return try {
        if (reqWidth == null || reqHeight == null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } else {
            // Сначала читаем только размеры
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
    } catch (_: Throwable) {
        null
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}