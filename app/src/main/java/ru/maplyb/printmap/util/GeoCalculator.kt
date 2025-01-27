package ru.maplyb.printmap.util

import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.item.BoundingBox
import ru.maplyb.printmap.item.GeoPoint
import java.io.File
import kotlin.math.asinh
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

class GeoCalculator {

    fun calculateTotalTilesCount(
        boundingBox: BoundingBox, fromZoom: Int, toZoom: Int
    ): Long {
        var totalTileCount = 0L

        val bottomLeft = GeoPoint(boundingBox.latSouth, boundingBox.lonWest)
        val topRight = GeoPoint(boundingBox.latNorth, boundingBox.lonEast)
        for (zoom in fromZoom..toZoom) {
            val leftBottomTiles = degToNum(bottomLeft.latitude, bottomLeft.longitude, zoom)
            val rightTopTiles = degToNum(topRight.latitude, topRight.longitude, zoom)

            val currentTileCount =
                (rightTopTiles.first - leftBottomTiles.first + 1) * (leftBottomTiles.second - rightTopTiles.second + 1)

            totalTileCount += currentTileCount
        }
        return totalTileCount
    }

    fun degToNum(latDeg: Double, lonDeg: Double, zoom: Int): kotlin.Pair<Int, Int> {
        val latRad = Math.toRadians(latDeg)
        val n = 2.0.pow(zoom.toDouble())
        val xTile = ((lonDeg + 180.0) / 360.0 * n).toInt()
        val yTile = ((1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * n).toInt()
        return kotlin.Pair(xTile, yTile)
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


    suspend fun getMetadataMbtiles(path: String): String? {
        return withContext(Dispatchers.IO){
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