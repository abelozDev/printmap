package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.api.model.ObjectRes
import ru.maplyb.printmap.impl.util.GeoCalculator.convertGeoToPixel
import kotlin.math.sqrt

class DrawInBitmap {

    suspend fun drawLayers(
        context: Context,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        layers: List<Layer>,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        val scaleFactor = sqrt((bitmap.width * bitmap.height).toDouble()).toFloat() / 1000f
        layers
            .flatMap { it.objects }
            .forEach { objects ->
                when (objects) {
                    is LayerObject.Line -> drawLine(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = objects,
                        scaleFactor = scaleFactor
                    )

                    is LayerObject.Polygon -> drawPolygon(
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = objects,
                        scaleFactor = scaleFactor
                    )

                    is LayerObject.Radius -> TODO()
                    is LayerObject.Text -> drawTextOnBitmap(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        text = objects,
                        scaleFactor = scaleFactor
                    )

                    is LayerObject.Object -> drawObjects(
                        bitmap = bitmap,
                        canvas = canvas,
                        boundingBox = boundingBox,
                        context = context,
                        objects = objects,
                        scaleFactor = scaleFactor
                    )
                }
            }
        return bitmap
    }

    private suspend fun drawObjects(
        bitmap: Bitmap,
        canvas: Canvas,
        boundingBox: BoundingBox,
        context: Context,
        objects: LayerObject.Object,
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
        val linesInPixels = convertGeoToPixel(
            objects.coords,
            boundingBox,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height
        )
        val drawable = when(objects.res) {
            is ObjectRes.Local -> ContextCompat.getDrawable(context, objects.res.res)
            is ObjectRes.Storage -> Drawable.createFromPath(objects.res.res)
        } ?: return@withContext

        val bitmapDrawable = drawable as? BitmapDrawable
        val realWidth = bitmapDrawable?.bitmap?.width ?: drawable.intrinsicWidth
        val realHeight = bitmapDrawable?.bitmap?.height ?: drawable.intrinsicHeight
        val scaleFactor1 = (objects.style.width / 25f) * scaleFactor
        val scaledWidth = (realWidth * scaleFactor1).toInt()
        val scaledHeight = (realHeight * scaleFactor1).toInt()
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

    }



    private suspend fun drawPolygon(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: LayerObject.Polygon,
        scaleFactor: Float
    ): Bitmap {
        return withContext(Dispatchers.Default) {
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                color = objects.style.color    // Цвет линии
                strokeWidth = (objects.style.width * scaleFactor)
                isAntiAlias = true   // Убираем зазубрины на линиях
            }
            val pointsInPixels = GeoCalculator.convertGeoToPixel(
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
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
            val paint = Paint().apply {
                color = objects.style.color
                strokeWidth = (objects.style.width * scaleFactor)
                isAntiAlias = true
                style = Paint.Style.STROKE

            }

            val linesInPixels = convertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )

            val path = Path() // Используем Path для сглаживания
            var firstPoint = true

            for (i in 1..linesInPixels.lastIndex) {
                val start = linesInPixels[i - 1]
                val end = linesInPixels[i]

                // Если это первая точка, начинаем путь
                if (firstPoint) {
                    path.moveTo(start.first, start.second)
                    firstPoint = false
                }

                // Добавляем линии к пути
                path.lineTo(end.first, end.second)
            }

            // Рисуем весь путь за один раз
            canvas.drawPath(path, paint)
        }
    }

    suspend fun drawTextOnBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        text: LayerObject.Text,
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
            val linesInPixels = convertGeoToPixel(
                listOf(text.coords),
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )
            val paint = Paint().apply {
                color = text.style.color
                this.textSize = (text.style.width * scaleFactor)
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
}