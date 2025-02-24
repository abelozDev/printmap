package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.Layer

class DrawInBitmap {

    suspend fun drawLayers(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: List<Layer>,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        objects.forEach { layer ->
            when(layer) {
                is Layer.Line -> drawLine(
                    canvas = canvas,
                    bitmap = bitmap,
                    boundingBox = boundingBox,
                    objects = layer
                )
                is Layer.Polygon -> drawPolygon(
                    bitmap = bitmap,
                    boundingBox = boundingBox,
                    objects = layer
                )
                is Layer.Radius -> TODO()
            }
        }
        return bitmap
    }

    fun myConvertGeoToPixel(
        objects: List<GeoPoint>,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<Pair<Float, Float>> {
        val leftTopPoint =
            GeoCalculator().degreeToMercator(GeoPoint(boundingBox.latNorth, boundingBox.lonEast))
        val rightBottomPoint =
            GeoCalculator().degreeToMercator(GeoPoint(boundingBox.latSouth, boundingBox.lonWest))
        val lengthX =
            maxOf(rightBottomPoint.x - leftTopPoint.x, leftTopPoint.x - rightBottomPoint.x)
        val lengthY =
            maxOf(leftTopPoint.y - rightBottomPoint.y, rightBottomPoint.y - leftTopPoint.y)
        val pixelSizeX = lengthX / bitmapWidth
        val pixelSizeY = lengthY / bitmapHeight
        val maxX = minOf(leftTopPoint.x, rightBottomPoint.x)
        val pointsMercator = GeoCalculator().degreeToMercator(objects)
        val result = mutableListOf<Pair<Float, Float>>()
        pointsMercator.forEach {
            val pointPixelX = ((it.x - maxX) / pixelSizeX).toFloat()
            val pointPixelY = ((leftTopPoint.y - it.y) / pixelSizeY).toFloat()
            result.add(pointPixelX to pointPixelY)
        }
        return result
    }

    suspend fun drawPolygon(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: Layer.Polygon,
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = objects.style.color    // Цвет линии
                strokeWidth = objects.style.width     // Толщина линии
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val pointsInPixels = myConvertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
            for (i in 1..pointsInPixels.size) {
                val (start, end) = if (i == pointsInPixels.size) {
                    pointsInPixels[0] to pointsInPixels[i-1]
                } else {
                    pointsInPixels[i - 1] to pointsInPixels[i]
                }
                // Преобразование GeoPoint в пиксели (понадобится функция преобразования)
                val startX = start.first
                val startY = start.second
                val endX = end.first
                val endY = end.second
                // Рисуем линию между двумя точками
                canvas.drawLine(startX, startY, endX, endY, paint)
            }
            bitmap
        }
    }

    suspend fun drawLine(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: Layer.Line,
    ) {
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            val paint = Paint().apply {
                color = objects.style.color    // Цвет линии
                strokeWidth = objects.style.width     // Толщина линии
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val linesInPixels = myConvertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
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
        }
    }
}