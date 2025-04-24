package ru.maplyb.printmap.impl.util.draw_on_bitmap

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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import ru.maplyb.printmap.api.model.GeoPoint
import kotlin.math.roundToInt
import androidx.core.graphics.withTranslation
import ru.maplyb.printmap.LatLon
import ru.maplyb.printmap.getGeodesicLine
import ru.maplyb.printmap.impl.util.converters.WGSToSK42Converter
import ru.maplyb.printmap.impl.util.defTextPaint
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// Добавляем enum для систем координат
enum class CoordinateSystem {
    WGS84,
    SK42
}

// В начале метода drawScaleLines добавим структуру для хранения точек пересечения
data class GridIntersection(
    val x: Float,  // координата X на экране
    val y: Float,  // координата Y на экране
    var sk42X: Int,  // координата X в СК-42
    var sk42Y: Int   // координата Y в СК-42
)

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
            canvas.withTranslation(centerX, centerY) {
                rotate(objects.angle)
                drawable.setBounds(
                    -scaledWidth / 2,
                    -scaledHeight / 2,
                    scaledWidth / 2,
                    scaledHeight / 2
                )
                drawable.draw(this)
            }
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
        stepDegrees: Double,
        stepMeters: Double,
        context: Context,
        color: Int,
        bitmap: Bitmap,
        boundingBox: BoundingBox,
        width: Float,
        coordinateSystem: CoordinateSystem = CoordinateSystem.WGS84
    ) {
        return coroutineScope {
            val paint = defTextPaint(
                context = context,
                color = color,
                strokeWidth = width,
                textSize = 0f
            )
            val canvas = Canvas(bitmap)
            val converterToSk = WGSToSK42Converter()
            val textPaint = defTextPaint(
                context = context,
                color = Color.YELLOW,
                textSize = 25f
            )

            when (coordinateSystem) {
                CoordinateSystem.WGS84 -> {
                    // Существующая логика для WGS84
                    val allLons = getLonLinesByDistance(
                        startLon = boundingBox.lonWest,
                        endLon = boundingBox.lonEast,
                        stepDegrees
                    ).map {
                        getGeodesicLine(
                            startLat = boundingBox.latNorth,
                            startLon = it,
                            endLat = boundingBox.latSouth,
                            endLon = it,
                            stepMeters = 1000.0
                        )
                    }

                    drawWGS84Lines(
                        canvas = canvas,
                        allLons = allLons,
                        boundingBox = boundingBox,
                        bitmap = bitmap,
                        paint = paint,
                        textPaint = textPaint,
                        isVertical = true
                    )

                    val allLats = latsList(
                        startLat = boundingBox.latNorth,
                        endLat = boundingBox.latSouth,
                        stepDegrees
                    ).map {
                        getGeodesicLine(
                            startLat = it,
                            startLon = boundingBox.lonWest,
                            endLat = it,
                            endLon = boundingBox.lonEast,
                            stepMeters = 1000.0
                        )
                    }

                    drawWGS84Lines(
                        canvas = canvas,
                        allLons = allLats,
                        boundingBox = boundingBox,
                        bitmap = bitmap,
                        paint = paint,
                        textPaint = textPaint,
                        isVertical = false
                    )
                }

                CoordinateSystem.SK42 -> {
                    // Преобразуем границы карты в СК-42 для горизонтальных линий
                    // Добавляем буфер к границам для гарантии полного покрытия
                    val bufferDegrees = 0.5  // Буфер в градусах
                    // рисуем вертикальные линии
                    val westZone = ((boundingBox.lonWest - bufferDegrees + 6.0) / 6.0).toInt()
                    val eastZone = ((boundingBox.lonEast + bufferDegrees + 6.0) / 6.0).toInt()

                    for (zone in westZone..eastZone) {
                        val zoneCentralMeridian = zone * 6 - 3
                        val zoneWestLon = zoneCentralMeridian - 3.0
                        val zoneEastLon = zoneCentralMeridian + 3.0
                        val visibleWestLon = (boundingBox.lonWest - bufferDegrees).coerceIn(zoneWestLon, zoneEastLon)
                        val visibleEastLon = (boundingBox.lonEast + bufferDegrees).coerceIn(zoneWestLon, zoneEastLon)

                        if (visibleWestLon >= visibleEastLon) continue

                        val (_, northLon) = converterToSk.wgs84ToSk42(boundingBox.latNorth + bufferDegrees, visibleWestLon)
                        val (_, southLon) = converterToSk.wgs84ToSk42(boundingBox.latSouth - bufferDegrees, visibleEastLon)

                        val startX = (northLon / stepMeters).toInt() * stepMeters
                        val endX = (southLon / stepMeters).toInt() * stepMeters

                        var currentX = startX
                        while (currentX <= endX) {
                            val points = mutableListOf<Pair<Double, Double>>()

                            // Увеличиваем количество точек для более плавных линий
                            val latStep = (boundingBox.latNorth - boundingBox.latSouth) / 100.0
                            var currentLat = boundingBox.latNorth + bufferDegrees
                            var first: String? = null
                            while (currentLat >= boundingBox.latSouth - bufferDegrees) {
                                // Преобразуем текущую точку в СК-42 и обратно
                                val (sk42Lat, _) = converterToSk.wgs84ToSk42(currentLat, visibleWestLon)
                                val (wgsLat, wgsLon) = converterToSk.sk42ToWgs84(sk42Lat, currentX, zone)
                                if (wgsLon in zoneWestLon..zoneEastLon) {
                                    points.add(wgsLat to wgsLon)
                                    if (first == null) {
                                        first = "${currentX.roundToInt()}"
                                    }
                                }
                                currentLat -= latStep
                            }

                            if (points.size >= 2) {
                                val pixelPoints = convertGeoToPixel(
                                    points.map { GeoPoint(it.first, it.second) },
                                    boundingBox,
                                    bitmapWidth = bitmap.width,
                                    bitmapHeight = bitmap.height
                                )

                                if (pixelPoints != null) {
                                    // Рисуем линию
                                    for (i in 0 until pixelPoints.lastIndex) {
                                        canvas.drawLine(
                                            pixelPoints[i].first,
                                            pixelPoints[i].second,
                                            pixelPoints[i + 1].first,
                                            pixelPoints[i + 1].second,
                                            paint
                                        )
                                    }
                                    val coordText = "$first"
                                    pixelPoints.firstOrNull {
                                        it.first > 0f && it.second > 0f
                                    }?.let {
                                        canvas.drawText(coordText, it.first + 5f, it.second + 30f, textPaint)
                                    }
                                }
                            }
                            currentX += stepMeters
                        }
                    }
                    val (northLat, _) = converterToSk.wgs84ToSk42(boundingBox.latNorth + bufferDegrees, boundingBox.lonWest)
                    val (southLat, _) = converterToSk.wgs84ToSk42(boundingBox.latSouth - bufferDegrees, boundingBox.lonWest)

                    // Округляем координаты до ближайшей сетки для широты
                    val startY = ceil(northLat / stepMeters).toInt() * stepMeters
                    val endY = floor(southLat / stepMeters).toInt() * stepMeters
                    // рисуем горизонтальные линии
                    var currentY = startY
                    while (currentY >= endY) {
                        val points = mutableListOf<Pair<Double, Double>>()

                        // Увеличиваем частоту точек для более плавных линий
                        val lonStep = 0.05  // уменьшенный шаг в градусах
                        var currentLon = boundingBox.lonWest - bufferDegrees
                        var first: Int? = null

                        while (currentLon <= boundingBox.lonEast + bufferDegrees) {
                            // Определяем зону для текущей точки
                            val zone = ((currentLon + 6.0) / 6.0).toInt()

                            // Преобразуем в СК-42
                            val (_, sk42Lon) = converterToSk.wgs84ToSk42(0.0, currentLon)  // широта не важна

                            // Преобразуем обратно в WGS84
                            val (wgsLat, wgsLon) = converterToSk.sk42ToWgs84(currentY, sk42Lon, zone)

                            // Добавляем точку с расширенными границами
                            if (wgsLon >= (boundingBox.lonWest - bufferDegrees) &&
                                wgsLon <= (boundingBox.lonEast + bufferDegrees)) {
                                points.add(wgsLat to wgsLon)
                                if (first == null) {
                                    first = (currentY + 1000).roundToInt()
                                }
                            }

                            currentLon += lonStep
                        }

                        // Сортируем точки по долготе для правильного порядка отрисовки
                        val sortedPoints = points.sortedBy { it.second }

                        if (sortedPoints.size >= 2) {
                            val pixelPoints = convertGeoToPixel(
                                sortedPoints.map { GeoPoint(it.first, it.second) },
                                boundingBox,
                                bitmapWidth = bitmap.width,
                                bitmapHeight = bitmap.height
                            )

                            if (pixelPoints != null) {
                                for (i in 0 until pixelPoints.lastIndex) {
                                    canvas.drawLine(
                                        pixelPoints[i].first,
                                        pixelPoints[i].second,
                                        pixelPoints[i + 1].first,
                                        pixelPoints[i + 1].second,
                                        paint
                                    )
                                }
                                val coordText = "$first"
                                pixelPoints.firstOrNull {
                                    it.first > 0f && it.second > 0f
                                }?.let {
                                    canvas.drawText(coordText, it.first, it.second + 20f, textPaint)
                                }
                            }
                        }
                        currentY -= stepMeters
                    }
                }
            }
        }
    }

    // Вспомогательная функция для отрисовки линий WGS84
    private fun drawWGS84Lines(
        canvas: Canvas,
        allLons: List<List<LatLon>>,
        boundingBox: BoundingBox,
        bitmap: Bitmap,
        paint: Paint,
        textPaint: Paint,
        isVertical: Boolean
    ) {
        val linesToPixel = allLons.map { lines ->
                    convertGeoToPixel(
                lines.map {
                            GeoPoint(
                                latitude = it.lat,
                                longitude = it.lon
                            )
                        },
                        boundingBox,
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height
                    )!!
                }

        for (j in 0..linesToPixel.lastIndex) {
            val item = linesToPixel[j]
            val coords = allLons[j][0]

            // Форматируем и отображаем координату
            val coordText = if (isVertical) {
                String.format(null, "%.5f", coords.lon)
            } else {
                String.format(null, "%.5f", coords.lat)
            }

            // Отрисовываем текст
            if (isVertical) {
                canvas.drawText(coordText, item[0].first, item[0].second + 20f, textPaint)
            } else {
                canvas.drawText(coordText, item[0].first, item[0].second, textPaint)
            }

            // Рисуем линии
                    for (i in 0..<item.lastIndex) {
                        val start = item[i]
                        val end = item[i + 1]
                        canvas.drawLine(
                            start.first,
                            start.second,
                            end.first,
                            end.second,
                            paint
                        )
            }
        }
    }

    //Точка пересечения
    private fun findIntersection(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        x3: Double,
        y3: Double,
        x4: Double,
        y4: Double
    ): GeoPoint? {
        val denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)

        // Если знаменатель равен нулю, отрезки параллельны
        if (denom == 0.0) return null

        val t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom
        val u = ((x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)) / denom

        // Если t и u находятся в пределах отрезков (0 <= t <= 1 и 0 <= u <= 1), то отрезки пересекаются
        if (t in 0f..1f && u in 0f..1f) {
            val ix = x1 + t * (x2 - x1)
            val iy = y1 + t * (y2 - y1)
            return GeoPoint(ix, iy)
        }
        return null
    }

    private fun latsList(
        startLat: Double,
        endLat: Double,
        step: Double
    ): List<Double> {
        val roundedStartLat = roundCoordToNearestStep(startLat, /*step*/)
        val count = ((roundedStartLat - endLat) / step).toInt()
        return (1..count).map { roundedStartLat - it * step }
    }
    private fun getLonLinesByDistance(
        startLon: Double,
        endLon: Double,
        step: Double
    ): List<Double> {
        val roundedStartLon = roundCoordToNearestStep(endLon, /*step*/)
        val count = ((roundedStartLon - startLon) / step).toInt()
        val result = (1..count).map { roundedStartLon - it * step }
        return result
    }


    private fun getLatLinesByDistance(
        latSouth: Double,
        heightMeters: Double,
        stepMeters: Int
    ): List<Double> {
        val metersPerDegreeLat = 111_000.0
        val stepLat = stepMeters / metersPerDegreeLat
        val count = (heightMeters / stepMeters).toInt()
        return (1..count).map { latSouth + it * stepLat }
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