package ru.mapolib.printmap.gui.halpers.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

internal fun sendImageAsFile(context: Context, path: String) {
    val file = File(path)
    try {
        if (file.exists()) {
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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