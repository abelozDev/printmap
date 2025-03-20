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
import kotlin.math.pow
import kotlin.math.tan

//lat - широта (y)
// lon - долгота (x)

/**
 * Расчеты связанные с картой
 * */
object GeoCalculator {

    suspend fun calculateTotalSizeCount(
        boundingBox: BoundingBox, zoom: Int
    ): OperationResult<Long> = withContext(Dispatchers.Default) {
        try {
            val (xMin, yMin) = degToNum(boundingBox.latSouth, boundingBox.lonWest, zoom)
            val (xMax, yMax) = degToNum(boundingBox.latNorth, boundingBox.lonEast, zoom)

            val tilesCount = ((xMax - xMin).absoluteValue.toLong() + 1) * ((yMax - yMin).absoluteValue.toLong() + 1)
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
    ): Pair<Float, Float> {
        val leftTopPoint =
            degreeToMercator(GeoPoint(boundingBox.latNorth, boundingBox.lonEast))
        val rightBottomPoint =
            degreeToMercator(GeoPoint(boundingBox.latSouth, boundingBox.lonWest))
        val lengthX =
            maxOf(rightBottomPoint.x - leftTopPoint.x, leftTopPoint.x - rightBottomPoint.x)
        val lengthY =
            maxOf(leftTopPoint.y - rightBottomPoint.y, rightBottomPoint.y - leftTopPoint.y)
        val pixelSizeX = lengthX / bitmapWidth
        val pixelSizeY = lengthY / bitmapHeight
        val maxX = minOf(leftTopPoint.x, rightBottomPoint.x)
        val pointsMercator = degreeToMercator(objects)
        val pointPixelX = ((pointsMercator.x - maxX) / pixelSizeX).toFloat()
        val pointPixelY = ((leftTopPoint.y - pointsMercator.y) / pixelSizeY).toFloat()
        return pointPixelX to pointPixelY
    }

    fun convertGeoToPixel(
        objects: List<GeoPoint>,
        boundingBox: BoundingBox,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<Pair<Float, Float>> {
        val leftTopPoint =
            degreeToMercator(GeoPoint(boundingBox.latNorth, boundingBox.lonEast))
        val rightBottomPoint =
            degreeToMercator(GeoPoint(boundingBox.latSouth, boundingBox.lonWest))
        val lengthX =
            maxOf(rightBottomPoint.x - leftTopPoint.x, leftTopPoint.x - rightBottomPoint.x)
        val lengthY =
            maxOf(leftTopPoint.y - rightBottomPoint.y, rightBottomPoint.y - leftTopPoint.y)
        val pixelSizeX = lengthX / bitmapWidth
        val pixelSizeY = lengthY / bitmapHeight
        val maxX = minOf(leftTopPoint.x, rightBottomPoint.x)
        val pointsMercator = degreeToMercator(objects)
        val result = mutableListOf<Pair<Float, Float>>()
        pointsMercator.forEach {
            val pointPixelX = ((it.x - maxX) / pixelSizeX).toFloat()
            val pointPixelY = ((leftTopPoint.y - it.y) / pixelSizeY).toFloat()
            result.add(pointPixelX to pointPixelY)
        }
        return result
    }
    fun degreeToMercator(point: GeoPoint): GeoPointMercator {
        val crsFactory = CRSFactory()
        val crsDegree = crsFactory.createFromName("EPSG:4326") // WGS 84 (широта/долгота)
        val crsMercator = crsFactory.createFromName("EPSG:3857") // Web Mercator

        val transformFactory = CoordinateTransformFactory()
        val transform: CoordinateTransform = transformFactory.createTransform(crsDegree, crsMercator)

        val sourceCoord = ProjCoordinate(point.longitude, point.latitude) // Порядок: долгота, широта
        val targetCoord = ProjCoordinate()

        try {
            transform.transform(sourceCoord, targetCoord)
        } catch (e: Proj4jException) {
            e.printStackTrace()
            // Возвращаем значение по умолчанию или выбрасываем исключение
            throw RuntimeException("Failed to transform coordinates: ${e.message}")
        }

        return GeoPointMercator(targetCoord.x, targetCoord.y)
    }
    fun degreeToMercator(points: List<GeoPoint>): List<GeoPointMercator> {
        val crsFactory = CRSFactory()
        val crsDegree = crsFactory.createFromName("EPSG:4326") // WGS 84 (широта/долгота)
        val crsMercator = crsFactory.createFromName("EPSG:3857") // Web Mercator
        val transformFactory = CoordinateTransformFactory()
        val transform: CoordinateTransform = transformFactory.createTransform(crsDegree, crsMercator)
        val result = mutableListOf<GeoPointMercator>()
        points.forEach {
            val sourceCoord = ProjCoordinate(it.longitude, it.latitude) // Порядок: долгота, широта
            val targetCoord = ProjCoordinate()
            try {
                transform.transform(sourceCoord, targetCoord)
            } catch (e: Proj4jException) {
                e.printStackTrace()
                // Возвращаем значение по умолчанию или выбрасываем исключение
                throw RuntimeException("Failed to transform coordinates: ${e.message}")
            }
            result.add(GeoPointMercator(targetCoord.x, targetCoord.y))
        }
        return result
    }
}

