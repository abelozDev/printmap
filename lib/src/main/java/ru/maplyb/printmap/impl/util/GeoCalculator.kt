package ru.maplyb.printmap.impl.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateTransform
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.Proj4jException
import org.osgeo.proj4j.ProjCoordinate
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.GeoPointMercator
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.model.TileParams
import kotlin.math.absoluteValue
import kotlin.math.asinh
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

//lat - широта (y)
// lon - долгота (x)

/**
 * Расчеты связанные с картой
 * */
object GeoCalculator {
    const val EARTH_RADIUS = 6_378_140.0
    private val crsFactory = CRSFactory()
    private val crsDegree = crsFactory.createFromName("EPSG:4326") // WGS 84 (широта/долгота)
    private val crsMercator = crsFactory.createFromName("EPSG:3857") // Web Mercator
    private val transformFactory = CoordinateTransformFactory()
    private val transform: CoordinateTransform = transformFactory.createTransform(crsDegree, crsMercator)

    suspend fun calculateTotalSizeCount(
        boundingBox: BoundingBox, zoom: Int
    ): OperationResult<Long> = withContext(Dispatchers.Default) {
        try {
            val (xMin, yMin) = degToNum(boundingBox.latSouth, boundingBox.lonWest, zoom)
            val (xMax, yMax) = degToNum(boundingBox.latNorth, boundingBox.lonEast, zoom)

            val tilesCount =
                ((xMax - xMin).absoluteValue.toLong() + 1) * ((yMax - yMin).absoluteValue.toLong() + 1)
            if (tilesCount > 2500L) {
                return@withContext OperationResult.Error("Слишком большой размер карты. Попробуйте уменьшить размер или зум")
            }
            OperationResult.Success(tilesCount)
        } catch (e: OutOfMemoryError) {
            OperationResult.Error("Слишком большой размер карты. Попробуйте уменьшить размер или зум")
        }
    }

    /**Количество тайлов, размер файла*/
    suspend fun calculateTotalTiles(
        boundingBox: BoundingBox, zoom: Int
    ): OperationResult<List<TileParams>> {
        return withContext(Dispatchers.Default) {
            try {
                val bottomLeft = GeoPoint(boundingBox.latSouth, boundingBox.lonWest)
                val topRight = GeoPoint(boundingBox.latNorth, boundingBox.lonEast)
                val tiles = mutableListOf<TileParams>()
                val (xMin, yMin) = degToNum(bottomLeft.latitude, bottomLeft.longitude, zoom)
                val (xMax, yMax) = degToNum(topRight.latitude, topRight.longitude, zoom)
                val xSorted = if (xMin < xMax) xMin..xMax else xMax..xMin
                val ySorted = if (yMin < yMax) yMin..yMax else yMax..yMin
                for (x in xSorted) {
                    for (y in ySorted) {
                        tiles.add(TileParams(x, y, zoom))
                    }
                }
                OperationResult.Success(tiles)
            } catch (e: OutOfMemoryError) {
                OperationResult.Error("Слишком большой размер карты. Попробуйте уменьшить размер или зум")
            }
        }
    }

    /**Из широты-долготы в x,y*/
    fun degToNum(latDeg: Double, lonDeg: Double, zoom: Int): kotlin.Pair<Int, Int> {
        val latRad = Math.toRadians(latDeg)
        val n = 2.0.pow(zoom.toDouble())
        val xTile = ((lonDeg + 180.0) / 360.0 * n).toInt()
        val yTile = ((1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * n).toInt()
        return xTile to yTile
    }

    fun tilesToBoundingBox(tiles: List<TileParams>, zoom: Int): BoundingBox {
        // Находим минимальные и максимальные значения x и y
        val xMin = tiles.minOf { it.x }
        val yMin = tiles.minOf { it.y }
        val xMax = tiles.maxOf { it.x }
        val yMax = tiles.maxOf { it.y }

        // Преобразуем крайние значения в географические координаты
        val bottomLeft = numToDeg(xMin, yMax + 1, zoom) // Добавляем 1 к yMax для нижней границы
        val topRight = numToDeg(xMax + 1, yMin, zoom) // Добавляем 1 к xMax для правой границы

        return BoundingBox(
            latNorth = topRight.latitude,
            lonWest = bottomLeft.longitude,
            latSouth = bottomLeft.latitude,
            lonEast = topRight.longitude
        )
    }

    fun numToDeg(x: Int, y: Int, zoom: Int): GeoPoint {
        val n = 1 shl zoom // 2^zoom
        val lon = x / n.toDouble() * 360.0 - 180.0
        val latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n.toDouble())))
        val lat = Math.toDegrees(latRad)
        return GeoPoint(lat, lon)
    }


    fun boundingBoxToTiles(bbox: BoundingBox, zoom: Int): List<Pair<Int, Int>> {
        val (xMin, yMin) = degToNum(bbox.latSouth, bbox.lonWest, zoom)
        val (xMax, yMax) = degToNum(bbox.latNorth, bbox.lonEast, zoom)
        println("xMin = $xMin, yMin = $yMin, xMax = $xMax, yMax = $yMax")
        val tiles = mutableListOf<Pair<Int, Int>>()
        val xSorted = if (xMin < xMax) xMin..xMax else xMax..xMin
        val ySorted = if (yMin < yMax) yMin..yMax else yMax..yMin
        for (x in xSorted) {
            for (y in ySorted) {
                tiles.add(x to y)
            }
        }
        return tiles
    }

    fun googleXyzToTms(x: Int, y: Int, zoom: Int): Pair<Int, Int> {
        val n = 2.0.pow(zoom.toDouble()).toInt()
        val yTms = n - 1 - y
        return x to yTms
    }

    fun convertGeoToPixel(
        objects: GeoPoint,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Pair<Float, Float>? {
        val leftTopPoint =
            degreeToMercator(GeoPoint(boundingBox.latNorth, boundingBox.lonEast)).getOrNull() ?: return null
        val rightBottomPoint =
            degreeToMercator(GeoPoint(boundingBox.latSouth, boundingBox.lonWest)).getOrNull() ?: return null
        val lengthX =
            maxOf(rightBottomPoint.x - leftTopPoint.x, leftTopPoint.x - rightBottomPoint.x)
        val lengthY =
            maxOf(leftTopPoint.y - rightBottomPoint.y, rightBottomPoint.y - leftTopPoint.y)
        val pixelSizeX = lengthX / bitmapWidth
        val pixelSizeY = lengthY / bitmapHeight
        val maxX = minOf(leftTopPoint.x, rightBottomPoint.x)
        val pointsMercator = degreeToMercator(objects).getOrNull() ?: return null
        val pointPixelX = ((pointsMercator.x - maxX) / pixelSizeX).toFloat()
        val pointPixelY = ((leftTopPoint.y - pointsMercator.y) / pixelSizeY).toFloat()
        return pointPixelX to pointPixelY
    }
    fun degreeToMercator(point: GeoPoint): Result<GeoPointMercator> {
        val sourceCoord = ProjCoordinate(point.longitude, point.latitude) // Порядок: долгота, широта
        val targetCoord = ProjCoordinate()
        return try {
            transform.transform(sourceCoord, targetCoord)
            Result.success(GeoPointMercator(targetCoord.x, targetCoord.y))
        } catch (e: Proj4jException) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    fun convertGeoToPixel(
        objects: List<GeoPoint>,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<Pair<Float, Float>>? {
        return try {
            val leftTopPoint =
                degreeToMercator(GeoPoint(boundingBox.latNorth, boundingBox.lonEast)).getOrNull() ?: return null
            val rightBottomPoint =
                degreeToMercator(GeoPoint(boundingBox.latSouth, boundingBox.lonWest)).getOrNull() ?: return null
            val lengthX =
                maxOf(rightBottomPoint.x - leftTopPoint.x, leftTopPoint.x - rightBottomPoint.x)
            val lengthY =
                maxOf(leftTopPoint.y - rightBottomPoint.y, rightBottomPoint.y - leftTopPoint.y)
            val pixelSizeX = lengthX / bitmapWidth
            val pixelSizeY = lengthY / bitmapHeight
            val maxX = minOf(leftTopPoint.x, rightBottomPoint.x)
            val pointsMercator = degreeToMercator(objects).getOrNull() ?: return null
            val result = mutableListOf<Pair<Float, Float>>()
            pointsMercator.forEach {
                val pointPixelX = ((it.x - maxX) / pixelSizeX).toFloat()
                val pointPixelY = ((leftTopPoint.y - it.y) / pixelSizeY).toFloat()
                result.add(pointPixelX to pointPixelY)
            }
            result
        } catch (e: RuntimeException) {
            null
        }
    }



    fun degreeToMercator(points: List<GeoPoint>): Result<List<GeoPointMercator>> {
        val result = mutableListOf<GeoPointMercator>()
        points.forEach {
            val sourceCoord = ProjCoordinate(it.longitude, it.latitude) // Порядок: долгота, широта
            val targetCoord = ProjCoordinate()
            try {
                transform.transform(sourceCoord, targetCoord)
            } catch (e: Proj4jException) {
                e.printStackTrace()
                // Возвращаем значение по умолчанию или выбрасываем исключение
                return Result.failure(e)
            }
            result.add(GeoPointMercator(targetCoord.x, targetCoord.y))
        }
        return Result.success(result)
    }


    /*Разница в метрах*/
    fun distanceBetween(aX: Double, aY: Double, bX: Double, bY: Double): Double {
        val lat1 = Math.toRadians(aX)
        val lng1 = Math.toRadians(aY)
        val lat2 = Math.toRadians(bX)
        val lng2 = Math.toRadians(bY)

        val latDistance = lat2 - lat1
        val lngDistance = lng2 - lng1

        val a = sin(latDistance / 2).pow(2) + cos(lat1) * cos(lat2) * sin(lngDistance / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS * c // в метрах
    }
}

