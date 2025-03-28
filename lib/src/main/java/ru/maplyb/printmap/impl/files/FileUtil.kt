package ru.maplyb.printmap.impl.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import ru.maplyb.printmap.impl.domain.model.PageFormat
import kotlin.math.roundToInt
import androidx.core.graphics.scale
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.util.canAllocateBitmap

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
    fun saveBitmapToExternalStorage(bitmap: Bitmap, fileName: String, dpi: Int): String? {
        val file = File(directory, "$fileName.png")
        return try {
            val resizedBitmap = resizeBitmapForDpi(bitmap, dpi)
            FileOutputStream(file).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resizeBitmapForDpi(bitmap: Bitmap, targetDpi: Int): Bitmap {
        val originalDpi = 72f // Стандартный DPI в Android Bitmap
        if (originalDpi == targetDpi.toFloat()) return bitmap
        val scaleFactor = targetDpi / originalDpi

        val matrix = Matrix().apply { postScale(scaleFactor, scaleFactor) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun calculatePagesSize(
        bitmap: Bitmap,
        pageFormat: PageFormat,
        dpi: Int = 72
    ): Int {
        val pageWidth = pageFormat.width
        val pageHeight = pageFormat.height
        val pageWidthInPixel = ((pageWidth / 25.4) * dpi).roundToInt()
        val pageHeightInPixel = ((pageHeight / 25.4) * dpi).roundToInt()
        val scaleFactor = dpi.toFloat() / 72f
        val bitmapHeight = (bitmap.height * scaleFactor).toInt()
        val bitmapWidth = (bitmap.width * scaleFactor).toInt()
        val cols = (bitmapWidth + pageWidthInPixel - 1) / pageWidthInPixel
        val rows = (bitmapHeight + pageHeightInPixel - 1) / pageHeightInPixel
        return cols * rows
    }

    fun saveBitmapToPdf(
        bitmap: Bitmap,
        fileName: String,
        pageFormat: PageFormat,
        dpi: Int
    ): OperationResult<String> {


        val file = File(directory, "$fileName.pdf")
        val document = PdfDocument()

        val pageWidth = pageFormat.width
        val pageHeight = pageFormat.height

        val pageWidthInPixel = ((pageWidth / 25.4) * dpi).roundToInt()
        val pageHeightInPixel = ((pageHeight / 25.4) * dpi).roundToInt()
        val scaleFactor = dpi.toFloat() / 72f
        val newWidth = (bitmap.width * scaleFactor).toInt()
        val newHeight = (bitmap.height * scaleFactor).toInt()
        val otherSize = bitmap.height != newHeight && bitmap.width != newHeight
        if (!canAllocateBitmap(
                newWidth,
                newHeight,
                Bitmap.Config.ARGB_8888
            ) && otherSize
        ) {
            val solving = when {
                dpi > 72 -> "Попробуйте уменьшить DPI"
                pageFormat == PageFormat.A4 -> "Попробуйте использовать лист большего формата"
                else -> "Попробуйте выбрать меньший сертор или изменить зум"
            }
            return OperationResult.Error("Слишком большой файл. $solving")
        }
        val scaledBitmap = bitmap.scale(
            (bitmap.width * scaleFactor).toInt(),
            (bitmap.height * scaleFactor).toInt()
        )

        val cols = (scaledBitmap.width + pageWidthInPixel - 1) / pageWidthInPixel
        val rows = (scaledBitmap.height + pageHeightInPixel - 1) / pageHeightInPixel
        return try {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidthInPixel,
                        pageHeightInPixel,
                        row * cols + col + 1
                    ).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    val srcLeft = col * pageWidthInPixel
                    val srcTop = row * pageHeightInPixel
                    val srcRight = minOf((col + 1) * pageWidthInPixel, scaledBitmap.width)
                    val srcBottom = minOf((row + 1) * pageHeightInPixel, scaledBitmap.height)

                    val croppedBitmap = Bitmap.createBitmap(
                        scaledBitmap,
                        srcLeft,
                        srcTop,
                        srcRight - srcLeft,
                        srcBottom - srcTop
                    )
                    canvas.drawBitmap(croppedBitmap, 0f, 0f, null)
                    document.finishPage(page)
                }
            }

            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            OperationResult.Success(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            OperationResult.Error("Слишком большой файл. Попробуйте уменьшить DPI")
        }
    }

    fun clearDownloadMapStorage() {
        directory.deleteRecursively()
    }

    suspend fun saveTileToPNG(
        byteArray: ByteArray,
        filePath: String,
        alpha: Int,
        quality: Int
    ): String? {
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            if (bitmap != null) {
                val resultBitmap = createBitmap(bitmap.width, bitmap.height)
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
                    FileProvider.getUriForFile(
                        context,
                        "ru.mapolib.printmap.gui.fileprovider",
                        file
                    )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*" // Указываем, что это файл
                    putExtra(Intent.EXTRA_STREAM, uri) // Прикрепляем URI файла
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Разрешаем другим приложениям читать URI
                }

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