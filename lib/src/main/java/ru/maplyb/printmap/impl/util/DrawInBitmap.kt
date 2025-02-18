package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.MapObject
import ru.maplyb.printmap.api.model.MapObjectStyle

class DrawInBitmap {
    fun myConvertGeoToPixel(
        lat: Double,
        lon: Double,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Pair<Float, Float> {

        val pointMercator = GeoCalculator().degreeToMercator(GeoPoint(lat, lon))
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
        val pointPixelX = ((pointMercator.x - maxX) / pixelSizeX).toFloat()
        val pointPixelY = ((leftTopPoint.y - pointMercator.y) / pixelSizeY).toFloat()

        return pointPixelX to pointPixelY
    }


    fun draw(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: Map<MapObjectStyle, List<MapObject>>,
        zoom: Int,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        objects.forEach { (style, objects) ->
            val paint = Paint().apply {
                color = style.color    // Цвет линии
                strokeWidth = style.width     // Толщина линии
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val linesInPixels = objects.map {
                myConvertGeoToPixel(
                    it.position.latitude,
                    it.position.longitude,
                    boundingBox,
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
        }
            return bitmap
    }
}