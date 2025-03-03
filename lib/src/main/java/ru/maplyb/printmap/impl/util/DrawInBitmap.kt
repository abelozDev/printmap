package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.api.model.ObjectRes

class DrawInBitmap {

    suspend fun drawLayers(
        context: Context,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        layers: List<Layer>,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        layers
            .flatMap { it.objects }
            .forEach { objects ->
                when (objects) {
                    is LayerObject.Line -> drawLine(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = objects
                    )

                    is LayerObject.Polygon -> drawPolygon(
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = objects
                    )

                    is LayerObject.Radius -> TODO()
                    is LayerObject.Text -> drawTextOnBitmap(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        text = objects
                    )

                    is LayerObject.Object -> drawObjects(
                        bitmap = bitmap,
                        canvas = canvas,
                        boundingBox = boundingBox,
                        context = context,
                        objects = objects
                    )
                }
            }
        return bitmap
    }

    private fun drawObjects(
        bitmap: Bitmap,
        canvas: Canvas,
        boundingBox: BoundingBox,
        context: Context,
        objects: LayerObject.Object,
    ) {
        val linesInPixels = convertGeoToPixel(
            objects.coords,
            boundingBox,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height
        )
        val drawable = when(objects.res) {
            is ObjectRes.Local -> ContextCompat.getDrawable(context, objects.res.res)
            is ObjectRes.Storage -> Drawable.createFromPath(objects.res.res)
        } ?:return

        val bitmapDrawable = drawable as? BitmapDrawable
        val realWidth = bitmapDrawable?.bitmap?.width ?: drawable.intrinsicWidth
        val realHeight = bitmapDrawable?.bitmap?.height ?: drawable.intrinsicHeight
        val scaleFactor = objects.style.width / 25f
        val scaledWidth = (realWidth * scaleFactor).toInt()
        val scaledHeight = (realHeight * scaleFactor).toInt()
        val centerX = linesInPixels.first
        val centerY = linesInPixels.second
        canvas.save()
        canvas.translate(centerX, centerY)

        canvas.rotate(objects.angle)
        drawable.setBounds(
            -scaledWidth / 2,
            -scaledHeight / 2,
            scaledWidth / 2,
            scaledHeight / 2
        )
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun convertGeoToPixel(
        objects: GeoPoint,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Pair<Float, Float> {
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
        val pointPixelX = ((pointsMercator.x - maxX) / pixelSizeX).toFloat()
        val pointPixelY = ((leftTopPoint.y - pointsMercator.y) / pixelSizeY).toFloat()
        return pointPixelX to pointPixelY
    }

    private fun convertGeoToPixel(
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

    private suspend fun drawPolygon(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: LayerObject.Polygon,
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = objects.style.color    // Цвет линии
                strokeWidth = objects.style.width     // Толщина линии
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val pointsInPixels = convertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
            for (i in 1..pointsInPixels.size) {
                val (start, end) = if (i == pointsInPixels.size) {
                    pointsInPixels[0] to pointsInPixels[i - 1]
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

    private suspend fun drawLine(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: LayerObject.Line,
    ) {
        withContext(Dispatchers.Default) {
            val paint = Paint().apply {
                color = objects.style.color    // Цвет линии
                strokeWidth = objects.style.width     // Толщина линии
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val linesInPixels = convertGeoToPixel(
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

    private fun drawTextOnBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        text: LayerObject.Text,
    ) {
        val linesInPixels = convertGeoToPixel(
            listOf(text.coords),
            boundingBox,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height
        )
        val paint = Paint().apply {
            color = text.style.color
            this.textSize = text.style.width
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }

        canvas.save()
        canvas.translate(linesInPixels.first().first, linesInPixels.first().second)
        canvas.rotate(text.angle)
        canvas.drawText(text.text, 0f, 0f, paint)
        canvas.restore()
    }
}