package ru.maplyb.printmap.impl.excel_generator

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.impl.util.converters.WGSToSK42Converter
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private const val EXCEL_REPORT_TAG = "EXCEL_REPORT_TAG"
suspend fun createExcelFromObjects(objects: List<LayerObject.Object>, outputFile: File) {
    val workbook: Workbook = XSSFWorkbook()
    val sheet: Sheet = workbook.createSheet("Objects")

    // Создаем стиль для заголовков
    val headerStyle = workbook.createCellStyle().apply {
        fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        fillPattern = FillPatternType.SOLID_FOREGROUND
        val font = workbook.createFont().apply {
            bold = true
        }
        setFont(font)
    }

    // Создаем заголовки
    val headerRow: Row = sheet.createRow(0)
    val headers = listOf("№", "Координаты", "Название", "Описание")
    headers.forEachIndexed { index, header ->
        val cell = headerRow.createCell(index)
        cell.setCellValue(header)
        cell.cellStyle = headerStyle
    }
    val WGSToSK42Converter = WGSToSK42Converter()
    val mappedObjects = coroutineScope {
        objects
            .chunked(100)
            .flatMap { chunk ->
                chunk.map {
                    async {
                        val convertedCoords = WGSToSK42Converter.wgs84ToSk42(it.coords.latitude, it.coords.longitude)
                        it.copy(
                            coords = GeoPoint(
                                latitude = convertedCoords.first.roundToInt().toDouble(),
                                longitude = convertedCoords.second.roundToInt().toDouble()
                            )
                        )
                    }
                }.awaitAll()
            }
    }

    // Заполняем данными
    mappedObjects.forEachIndexed { index, obj ->
        val row: Row = sheet.createRow(index + 1)

        // Порядковый номер
        row.createCell(0).setCellValue((index + 1).toDouble())

        // Координаты (широта, долгота)
        val coords = "${obj.coords.latitude}, ${obj.coords.longitude}"
        row.createCell(1).setCellValue(coords)

        // Название
        row.createCell(2).setCellValue(obj.name)

        // Описание
        row.createCell(3).setCellValue(obj.description)
    }

    // Включаем перенос текста для всех ячеек с данными
    val wrapStyle = workbook.createCellStyle().apply {
        wrapText = true
        verticalAlignment = VerticalAlignment.TOP
    }

    // Применяем стиль переноса к ячейкам с данными
    for (i in 1..objects.size) {
        val row = sheet.getRow(i)
        for (j in 1..3) {  // Колонки с текстом (координаты, название, описание)
            row.getCell(j)?.cellStyle = wrapStyle
        }
    }

    // Вычисляем динамическую ширину на основе содержимого
    val maxCharsPerColumn = intArrayOf(
        3,  // № - максимум 3 символа
        30, // Координаты - фиксированная ширина
        objects.maxOfOrNull { it.name.length } ?: 10,
        objects.maxOfOrNull { it.description.length } ?: 20
    )

    // Устанавливаем ширину с ограничениями
    maxCharsPerColumn.forEachIndexed { index, maxChars ->
        val width = minOf(maxChars * 256, 15000) // Ограничиваем максимальную ширину
        sheet.setColumnWidth(index, width)
    }

    // Записываем в файл
    FileOutputStream(outputFile).use { outputStream ->
        workbook.write(outputStream)
    }

    workbook.close()
}

// Функция для отправки Excel файла
fun sendExcelFile(context: Context, filePath: String) {
    val file = File(filePath)
    try {
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.printmap.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Экспорт объектов")
                putExtra(Intent.EXTRA_TEXT, "Excel файл с объектами")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Отправить Excel файл"))
        } else {
            println("$EXCEL_REPORT_TAG Файл не найден")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        println("$EXCEL_REPORT_TAG Ошибка при отправке: ${e.message}")
    }
}

// Пример использования: создание и отправка файла
suspend fun exportAndSendExcel(context: Context, objects: List<LayerObject.Object>) {
    try {
        // Создаем файл
        val fileName = "objects_${System.currentTimeMillis()}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)

        createExcelFromObjects(objects, file)

        // Отправляем файл
        sendExcelFile(context, file.absolutePath)

    } catch (e: Exception) {
        e.printStackTrace()
        println("$EXCEL_REPORT_TAG Ошибка при создании файла: ${e.message}")
    }
}

// Альтернатива: только экспорт без отправки
suspend fun exportToExcel(context: Context, objects: List<LayerObject.Object>): String? {
    return try {
        val fileName = "objects_${System.currentTimeMillis()}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)

        createExcelFromObjects(objects, file)

        println("$EXCEL_REPORT_TAG Файл сохранен: ${file.name}")
        file.absolutePath

    } catch (e: Exception) {
        e.printStackTrace()
        println("$EXCEL_REPORT_TAG Ошибка при создании файла: ${e.message}")
        null
    }
}

fun deleteExcelFile(filePath: String?): Boolean {
    return try {
        if (filePath.isNullOrEmpty()) {
            println("$EXCEL_REPORT_TAG Путь к файлу не указан")
            return false
        }

        val file = File(filePath)

        if (!file.exists()) {
            println("$EXCEL_REPORT_TAG Файл не найден")
            return false
        }

        val deleted = file.delete()

        if (deleted) {
            println("$EXCEL_REPORT_TAG Файл успешно удален")
        } else {
            println("$EXCEL_REPORT_TAG Не удалось удалить файл")
        }

        deleted
    } catch (e: Exception) {
        e.printStackTrace()
        println("$EXCEL_REPORT_TAG Ошибка при удалении файла: ${e.message}")
        false
    }
}