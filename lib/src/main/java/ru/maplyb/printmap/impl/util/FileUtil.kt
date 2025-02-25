package ru.maplyb.printmap.impl.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class FileUtil(private val context: Context) {
    fun saveBitmapToExternalStorage(bitmap: Bitmap, fileName: String): String? {
        val file = File(context.getExternalFilesDir(null), "$fileName.png")
        return try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
}