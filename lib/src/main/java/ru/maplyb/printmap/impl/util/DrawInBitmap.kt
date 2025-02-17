package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import kotlin.math.ln
import kotlin.math.sin

class DrawInBitmap {

    fun convertGeoToPixel(
        lat: Double,
        lon: Double,
        zoom: Int,
        boundingBox: BoundingBox,
        tileSize: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Pair<Float, Float> {
        val tilesCount = 1 shl zoom

        // Преобразуем долготу в координаты X (в пикселях)
        val x = (lon - boundingBox.lonWest) / (boundingBox.lonEast - boundingBox.lonWest) * tilesCount

        // Преобразуем широту в координаты Y с использованием проекции Mercator
        val sinLat = sin(Math.toRadians(lat))
        val y = (1.0 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (2.0 * Math.PI)) * tilesCount / 2.0

        // Преобразуем в пиксели, с учетом размера тайла
        val pixelX = (x * tileSize).toFloat()
        val pixelY = (y * tileSize).toFloat()

        // Масштабируем координаты относительно размеров изображения
        val normalizedX = (pixelX / (tileSize * tilesCount)) * bitmapWidth
        val normalizedY = (pixelY / (tileSize * tilesCount)) * bitmapHeight

        // Логируем результаты
        Log.d("GeoToPixel", "GeoCoords: ($lat, $lon), PixelCoords: ($pixelX, $pixelY)")
        Log.d("GeoToPixel", "Normalized PixelCoords: ($normalizedX, $normalizedY)")

        return normalizedX.coerceIn(0f, bitmapWidth.toFloat()) to normalizedY.coerceIn(
            0f,
            bitmapHeight.toFloat()
        )
    }

    fun myConvertGeoToPixel(lat: Double,
                            lon: Double,
                            zoom: Int,
                            boundingBox: BoundingBox,
                            tileSize: Int,
                            bitmapWidth: Int,
                            bitmapHeight: Int): Pair<Float, Float>{

        val tilesCount = 1 shl zoom

        val pointMercator = GeoCalculator().degreeToMercator(GeoPoint(lat, lon))

        // Преобразуем в пиксели, с учетом размера тайла
        val pixelX = (pointMercator.x * tileSize).toFloat()
        val pixelY = (pointMercator.y * tileSize).toFloat()

        // Масштабируем координаты относительно размеров изображения
        val normalizedX = (pixelX / (tileSize * tilesCount)) * bitmapWidth
        val normalizedY = (pixelY / (tileSize * tilesCount)) * bitmapHeight

        // Логируем результаты
        Log.d("GeoToPixel", "GeoCoords: ($lat, $lon), PixelCoords: ($pixelX, $pixelY)")
        Log.d("GeoToPixel", "Normalized PixelCoords: ($normalizedX, $normalizedY)")

        return normalizedX.coerceIn(0f, bitmapWidth.toFloat()) to normalizedY.coerceIn(
            0f,
            bitmapHeight.toFloat()
        )
    }

    //50.38030022353232, 30.226485489123323
    //49.00163585767624, 34.47819411725312
    fun draw(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        lines: List<GeoPoint>,
        zoom: Int,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLUE    // Цвет линии
            strokeWidth = 5f     // Толщина линии
            isAntiAlias = true   // Убираем зазубрины на линиях
        }
        val linesInPixels = lines.map {
            convertGeoToPixel(
                it.latitude,
                it.longitude,
                zoom,
                boundingBox,
                tileSize = 255,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
        }
        for (i in 1..linesInPixels.lastIndex) {
            val start = linesInPixels[i - 1]
            val end = linesInPixels[i]
            // Преобразование GeoPoint в пиксели (понадобится функция преобразования)
            val startX = start.first
            val startY = start.second
            val endX = end.first
            val endY = end.second
            // Рисуем линию между двумя точками
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        return bitmap
    }
}