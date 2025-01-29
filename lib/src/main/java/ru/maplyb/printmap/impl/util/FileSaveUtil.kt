package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileSaveUtil(private val context: Context) {
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
}