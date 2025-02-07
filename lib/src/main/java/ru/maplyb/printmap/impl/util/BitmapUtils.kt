package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.window.layout.WindowMetricsCalculator
import java.io.File
import java.io.FileOutputStream

fun Bitmap.limitSize(context: Context): Bitmap {
    val maxSize = getMaxBitmapSize(context)

    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
        // Если изображение шире, ограничиваем по ширине
        newWidth = maxSize
        newHeight = (newWidth / aspectRatio).toInt()
    } else {
        // Если изображение выше, ограничиваем по высоте
        newHeight = maxSize
        newWidth = (newHeight * aspectRatio).toInt()
    }

    return if (width > maxSize || height > maxSize) {
        Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    } else {
        this
    }
}

private fun getMaxBitmapSize(context: Context): Int {
    val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val currentBounds = windowMetrics.bounds // E.g. [0 0 1350 1800]
    val screenWidth = currentBounds.width()
    val screenHeight = currentBounds.height()

    val maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024
    val memoryLimit = when {
        maxMemory >= 512 -> 4096
        maxMemory >= 256 -> 2048
        else -> 1024
    }
    return minOf(screenWidth * 2, screenHeight * 2, memoryLimit)
}

fun getBitmapFromPath(path: String): Bitmap? {
    return try {
        BitmapFactory.decodeFile(path)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun ByteArray?.getTileSize(alpha: Int): Long {
    if (this == null) return 0
    return withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(this@getTileSize, 0, this@getTileSize.size)
        if (bitmap != null) {
            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                this.alpha = alpha
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap.byteCount.toLong()
        } else 0L
    }
}
fun saveBitmapToExternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
    val file = File(context.getExternalFilesDir(null), "$fileName.png")
    return try {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

}
