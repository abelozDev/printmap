package ru.maplyb.printmap.impl.util

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.model.TileParams
import java.io.File
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

//lat - широта (y)
// lon - долгота (x)

/**
 * Расчеты связанные с картой
 * */
internal class GeoCalculator {

    /**Количество тайлов, размер файла*/
    suspend fun calculateTotalTilesCount(
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

    fun calculateTileY(latitude: Double, zoomLevel: Int): Int =
        floor(
            (1 - ln(
                tan(Math.toRadians(latitude)) +
                        1 / cos(Math.toRadians(latitude))
            ) / Math.PI) / 2.0 * (1 shl zoomLevel)
        ).toInt()

    fun calculateTileX(longitude: Double, zoomLevel: Int): Int =
        floor((longitude + 180.0) / 360.0 * (1 shl zoomLevel)).toInt()

    /*надо получить tile_data (картинка в байтах)*/
    suspend fun getMetadataMbtiles(path: String): String? {
        return withContext(Dispatchers.IO) {
            var bounds: String? = null
            val db: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(File(path), null)

            val query = "SELECT name,value FROM metadata WHERE name = 'bounds';"
            try {
                val cur = db.rawQuery(query, null)
                if (cur.moveToFirst()) {
                    do {
                        bounds = cur.getString(1)
                    } while (cur.moveToNext())
                }
                cur.close()
            } catch (e: android.database.sqlite.SQLiteException) {
                bounds = null
            }
            db.close()
            bounds
        }
    }
}

