package ru.maplyb.printmap.impl.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileUtil(private val context: Context) {

    companion object {
        private const val IMAGE_DIRECTORY = "DownloadedMapImages"
    }
    private var _directory: File? = null
    val directory: File
        get() {
            return if (_directory == null) {
                _directory = File(context.getExternalFilesDir(null), IMAGE_DIRECTORY)
                if (!_directory!!.exists()) _directory!!.mkdirs()
                _directory!!
            } else _directory!!
        }

    fun saveBitmapToExternalStorage(bitmap: Bitmap, fileName: String): String? {
        val file = File(directory, "$fileName.png")
        return try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearDownloadMapStorage() {
        directory.deleteRecursively()
    }


    suspend fun saveTileToPNG(byteArray: ByteArray, filePath: String, alpha: Int, quality: Int): String? {
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
                    val file = File(directory, filePath)
                    try {
                        FileOutputStream(file).use { fos ->
                            resultBitmap.compress(Bitmap.CompressFormat.PNG, quality, fos)
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
    fun sendImageAsFile(path: String) {
        val file = File(path)
        try {
            if (file.exists()) {
                val uri =
                    FileProvider.getUriForFile(context, "ru.mapolib.printmap.gui.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*" // Указываем, что это файл
                    putExtra(Intent.EXTRA_STREAM, uri) // Прикрепляем URI файла
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Разрешаем другим приложениям читать URI
                }
                // Открываем Telegram для отправки
                context.startActivity(Intent.createChooser(intent, "Share Image"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
}