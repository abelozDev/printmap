package ru.maplyb.printmap.impl.data.local

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.model.TileSchema
import ru.maplyb.printmap.impl.domain.repo.DataSource
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.debugLog
import ru.maplyb.printmap.impl.util.getTileSize
import java.io.ByteArrayOutputStream

internal class LocalDataSource : DataSource {

    override suspend fun getTilesApproximateSize(
        path: String,
        tiles: List<TileParams>,
        alpha: Int,
        schema: TileSchema
    ): Long {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        val geoCalculator = GeoCalculator

        val formattedList = if (schema == TileSchema.TMS) {
            tiles.map { (x, y, z) ->
                val (newX, newY) = geoCalculator.googleXyzToTms(x, y, z)
                TileParams(newX, newY, z)
            }
        } else {
            tiles
        }
        var result = 0
        var oneTileByteArray: ByteArray? = null
        val query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1;"

       //val query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?;"
        try {
            formattedList.forEach { (x, y, z) ->
                val args = arrayOf(z.toString(), x.toString(), y.toString())
                db.rawQuery(query, args).use { cursor ->
                    if (cursor.moveToFirst()) {
                        if (oneTileByteArray == null) {
                            oneTileByteArray = cursor.getBlob(0)
                        }
                        result++
                    }
                }
            }
        } catch (e: android.database.sqlite.SQLiteException) {
            println("Error while fetching tile data: ${e.printStackTrace()}")
            e.printStackTrace()
        } finally {
            db.close()
        }

        return oneTileByteArray.getTileSize(alpha) * result
    }


    override suspend fun getTile(
        path: String,
        x: Int,
        y: Int,
        z: Int,
        schema: TileSchema
    ): ByteArray? {
        return getTileDataMbtiles(path, x, y, z, schema)
    }

    private suspend fun getTileDataMbtiles(
        path: String,
        x: Int,
        y: Int,
        z: Int,
        schema: TileSchema
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            var tileData: ByteArray? = null
            val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            val (newX, newY) = if (schema == TileSchema.TMS) GeoCalculator.googleXyzToTms(
                x,
                y,
                z
            ) else x to y
            val query =
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?;"
            val args = arrayOf(z.toString(), newX.toString(), newY.toString())
            try {
                val cursor = db.rawQuery(query, args)
                if (cursor.moveToFirst()) {
                    tileData = cursor.getBlob(0)
                }
                cursor.close()
            } catch (e: android.database.sqlite.SQLiteException) {
                println("Error while fetching tile data: ${e.printStackTrace()}")
                e.printStackTrace()
            } finally {
                db.close()
            }
            tileData
        }
    }

    override suspend fun getSchema(path: String): TileSchema {
        val db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        var schema: String? = null
        val query = "SELECT name,value FROM metadata WHERE name = 'scheme';"
        try {
            val cur = db.rawQuery(query, null)
            if (cur.moveToFirst()) {
                do {
                    schema = cur.getString(1)
                } while (cur.moveToNext())
            }
            cur.close()
        } catch (e: android.database.sqlite.SQLiteException) {
            schema = null
        }
        db.close()
        return when (schema) {
            TileSchema.TMS.name -> TileSchema.TMS
            TileSchema.GOOGLE.name -> TileSchema.GOOGLE
            else -> TileSchema.TMS
        }
    }


}

fun saveTileToDatabase(
    database: SQLiteDatabase, xOfXYZ: Int, yOfXYZ: Int, zoomLevel: Int, bitmap: Bitmap,
) {
    val boas = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, boas)
    val blob = boas.toByteArray()
    val insertSql =
        "INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
    //todo: Пока закомментировал, т.к. нет OsmMapTools
//    val yTMS = OsmMapTools.xyzToTmsY(yOfXYZ, zoomLevel)
//    val args = arrayOf(zoomLevel, xOfXYZ, yTMS, blob)
//    database.execSQL(insertSql, args)
}