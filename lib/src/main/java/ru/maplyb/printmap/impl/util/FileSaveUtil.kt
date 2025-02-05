package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class FileSaveUtil(private val context: Context) {
    suspend fun saveTileToPNG(byteArray: ByteArray, filePath: String, alpha: Int): String? {
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            if (bitmap != null) {
                val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                val paint = Paint().apply {
                    this.alpha = alpha // Устанавливаем уровень прозрачности
                }

                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                return@withContext withContext(Dispatchers.IO) {
                    val externalFilesDir = context.getExternalFilesDir(null)
                    val file = File(externalFilesDir, filePath)
                    try {
                        FileOutputStream(file).use { fos ->
                            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                        file.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            } else {
                null
            }
        }
    }
    suspend fun deleteTiles(tiles: List<String>) {
        withContext(Dispatchers.IO) {
            tiles.forEach {
                val file = File(it)
                if (file.exists()) file.delete()
            }
        }

    }
    suspend fun getTileSize(byteArray: ByteArray, alpha: Int): Long {
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            if (bitmap != null) {
                val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)
                val paint = Paint().apply {
                    this.alpha = alpha // Устанавливаем уровень прозрачности
                }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                resultBitmap.byteCount.toLong()
            } else 0L
        }
    }
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): String? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        val file = File(picturesDir, "$fileName.png")

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
}