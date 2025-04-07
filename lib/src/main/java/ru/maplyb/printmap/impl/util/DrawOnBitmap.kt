package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.PathEffectTypes
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.api.model.ObjectRes
import ru.maplyb.printmap.impl.util.GeoCalculator.convertGeoToPixel
import kotlin.math.sqrt
import androidx.core.graphics.withSave
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.impl.util.GeoCalculator.distanceBetween
import kotlin.math.cos
import kotlin.math.roundToInt

class DrawOnBitmap {

    suspend fun drawLayers(
        context: Context,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        layerObjectsColor: Map<String, Int?>,
        layers: List<Layer>,
    ): Bitmap {
        val canvas = Canvas(bitmap)
        val scaleFactor = sqrt((bitmap.width * bitmap.height).toDouble()).toFloat() / 1000f
        layers
            .flatMap { it.objects }
            .map {
                val color = layerObjectsColor[it::class.simpleName] ?: return@map it
                if (it is LayerObject.Object) {
                    /*У целей меняем цвет имени*/
                    it.copy(nameColor = color)
                } else it.updateStyle(it.style.copy(color = color))
            }
            .forEach { layerObject ->
                when (layerObject) {
                    is LayerObject.Line -> drawLine(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = layerObject,
                        scaleFactor = scaleFactor,
                    )

                    is LayerObject.Polygon -> drawPolygon(
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        objects = layerObject,
                        scaleFactor = scaleFactor,
                    )

                    is LayerObject.Radius -> Unit
                    is LayerObject.Text -> drawTextOnBitmap(
                        canvas = canvas,
                        bitmap = bitmap,
                        boundingBox = boundingBox,
                        text = layerObject,
                        scaleFactor = scaleFactor,
                        context = context
                    )

                    is LayerObject.Object -> drawObjects(
                        bitmap = bitmap,
                        canvas = canvas,
                        boundingBox = boundingBox,
                        context = context,
                        objects = layerObject,
                        scaleFactor = scaleFactor,
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
        coroutineScope {
            ensureActive()
            val linesInPixels = convertGeoToPixel(
                objects.coords,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            ) ?: return@coroutineScope
            val drawable = when (objects.res) {
                is ObjectRes.Local -> ContextCompat.getDrawable(context, objects.res.res)
                is ObjectRes.Storage -> Drawable.createFromPath(objects.res.res)
            } ?: return@coroutineScope

            val bitmapDrawable = objects.style.color?.let {
                val colorFilter = PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN)
                val bitmapDraw = drawable as? BitmapDrawable
                bitmapDraw?.colorFilter = colorFilter
                bitmapDraw
            } ?: drawable as? BitmapDrawable


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
            val textHeight = (objects.style.width * scaleFactor)
            val paint = defTextPaint(
                context = context,
                color = objects.nameColor,
                textSize = textHeight,
            )
            val paintStroke = defTextPaint(
                context = context,
                color = Color.WHITE,
                textSize = textHeight,
                strokeWidth = textHeight / 10f,
                style = Paint.Style.STROKE
            )
            val textLength = paint.measureText(objects.style.name)
            val nameX = centerX - (textLength / 2)
            val nameY = centerY + (scaledHeight / 2) + (textHeight * 0.75f)
            canvas.withSave {
                drawText(objects.style.name, nameX, nameY, paintStroke)
                drawText(objects.style.name, nameX, nameY, paint)
            }
        }
    }


    private suspend fun drawPolygon(
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: LayerObject.Polygon,
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
            if (objects.objects.isEmpty()) return@withContext // Проверка, есть ли точки
            val canvas = Canvas(bitmap)
            val alpha = (objects.alpha / 100.0f).coerceIn(0f, 1f) * 255
            // Кисть для заливки (с учетом прозрачности)
            val fillPaint = Paint().apply {
                color = objects.style.color ?: Color.RED
                isAntiAlias = true
                style = Paint.Style.FILL
                this.alpha = alpha.roundToInt()
            }

            // Кисть для обводки (без прозрачности)
            val basePaint = Paint().apply {
                color = objects.style.color ?: Color.RED
                strokeWidth = (objects.style.width * scaleFactor)
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val firstPaint = Paint(basePaint).apply {
                if (objects.pathEffect != null && objects.pathEffect != "DEFAULT") {
                    pathEffect = PathEffectTypes.valueOf(objects.pathEffect).effect1
                }
            }
            val secondPaint = Paint(basePaint).apply {
                if (objects.pathEffect != null && objects.pathEffect != "DEFAULT") {
                    pathEffect = PathEffectTypes.valueOf(objects.pathEffect).effect2
                }
            }
            val pointsInPixels = convertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            ) ?: return@withContext
            val path = Path().apply {
                moveTo(pointsInPixels.first().first, pointsInPixels.first().second)
                for (i in 1 until pointsInPixels.size) {
                    val (x, y) = pointsInPixels[i]
                    lineTo(x, y)
                }
                close() // Закрываем контур
            }

            // Сначала заливаем, затем рисуем контур
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, firstPaint)
            canvas.drawPath(path, secondPaint)
        }
    }

    suspend fun drawScaleLines(
        stepMeters: Int = 1000,
        context: Context,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        width: Float,
    ): Bitmap {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                val paint = defTextPaint(
                    context = context,
                    color = Color.RED,
                    strokeWidth = width,
                    textSize = 0f
                )
                val canvas = Canvas(bitmap)
                val widthGeoMetr = distanceBetween(
                    boundingBox.latNorth,
                    boundingBox.lonWest,
                    boundingBox.latNorth,
                    boundingBox.lonEast
                )
                val heightGeoMetr = distanceBetween(
                    boundingBox.latNorth,
                    boundingBox.lonWest,
                    boundingBox.latSouth,
                    boundingBox.lonWest
                )
                val midLat = (boundingBox.latNorth + boundingBox.latSouth) / 2
                val latLines = getLatLinesByDistance(
                    boundingBox.latSouth,
                    boundingBox.latNorth,
                    heightGeoMetr,
                    stepMeters
                )
                val lonLines = getLonLinesByDistance(
                    boundingBox.lonWest,
                    boundingBox.lonEast,
                    midLat,
                    widthGeoMetr,
                    stepMeters
                )
                val startLatPoint = convertGeoToPixel(
                    latLines.map {
                        GeoPoint(
                            latitude = it,
                            longitude = boundingBox.lonWest
                        )
                    },
                    boundingBox,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                ) ?: return@withContext bitmap
                val endLatPoint = convertGeoToPixel(
                    latLines.map {
                        GeoPoint(
                            latitude = it,
                            longitude = boundingBox.lonEast
                        )
                    },
                    boundingBox,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                ) ?: return@withContext bitmap
                startLatPoint
                    .zip(endLatPoint).forEach { (start, end) ->
                        canvas.drawLine(
                            start.first,
                            start.second,
                            end.first,
                            end.second,
                            paint
                        )
                    }

                val startLonPoint = convertGeoToPixel(
                    lonLines.map {
                        GeoPoint(
                            latitude = boundingBox.latNorth,
                            longitude = it
                        )
                    },
                    boundingBox,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                ) ?: return@withContext bitmap
                val endLonPoint = convertGeoToPixel(
                    lonLines.map {
                        GeoPoint(
                            latitude = boundingBox.latSouth,
                            longitude = it
                        )
                    },
                    boundingBox,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                ) ?: return@withContext bitmap
                startLonPoint.zip(endLonPoint).forEach { (start, end) ->
                    canvas.drawLine(
                        start.first,
                        start.second,
                        end.first,
                        end.second,
                        paint
                    )
                }
                bitmap
            }
        }
    }

    fun getLatLinesByDistance(
        latSouth: Double,
        latNorth: Double,
        heightMeters: Double,
        stepMeters: Int
    ): List<Double> {
        val metersPerDegreeLat = 111_000.0
        val stepLat = stepMeters / metersPerDegreeLat
        val count = (heightMeters / stepMeters).toInt()

        return (1..count).map { latSouth + it * stepLat }
    }

    fun getLonLinesByDistance(
        lonWest: Double,
        lonEast: Double,
        latAt: Double, // средняя широта
        widthMeters: Double,
        stepMeters: Int
    ): List<Double> {
        val metersPerDegreeLon = 111_320.0 * cos(Math.toRadians(latAt))
        val stepLon = stepMeters / metersPerDegreeLon
        val count = (widthMeters / stepMeters).toInt()

        return (1..count).map { lonWest + it * stepLon }
    }

    private fun getInnerIntervals(
        start: Double,
        end: Double,
        steps: Int
    ): List<Double> {
        check(start < end) { "start must by less than end" }
        if (steps == 0) return emptyList()
        val step = (end - start) / steps
        val result = mutableListOf<Double>()
        for (i in 1..steps) {
            result.add(start + (step * i))
        }
        return result
    }


    private suspend fun drawLine(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        objects: LayerObject.Line,
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
            val basePaint = Paint().apply {
                color = objects.style.color ?: Color.RED
                strokeWidth = (objects.style.width * scaleFactor)
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val linesInPixels = convertGeoToPixel(
                objects.objects,
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            ) ?: return@withContext

            val firstPaint = Paint(basePaint).apply {
                if (objects.pathEffect != null && objects.pathEffect != "DEFAULT") {
                    pathEffect = PathEffectTypes.valueOf(objects.pathEffect).effect1
                }
            }
            val secondPaint = Paint(basePaint).apply {
                if (objects.pathEffect != null && objects.pathEffect != "DEFAULT") {
                    pathEffect = PathEffectTypes.valueOf(objects.pathEffect).effect2
                }
            }

            val path = Path()
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
            canvas.drawPath(path, firstPaint)
            canvas.drawPath(path, secondPaint)
        }
    }

    private suspend fun drawTextOnBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        context: Context,
        text: LayerObject.Text,
        scaleFactor: Float
    ) {
        withContext(Dispatchers.Default) {
            val linesInPixels = convertGeoToPixel(
                listOf(text.coords),
                boundingBox,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            ) ?: return@withContext
            val textSize = (text.style.width * scaleFactor)
            val paint = defTextPaint(
                context = context,
                color = text.style.color ?: Color.RED,
                textSize = textSize
            )
            canvas.save()
            canvas.translate(linesInPixels.first().first, linesInPixels.first().second)
            canvas.rotate(text.angle)
            canvas.drawText(text.text, 0f, 0f, paint)
            canvas.restore()
        }
    }
}