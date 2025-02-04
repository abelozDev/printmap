package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapType
import ru.maplyb.printmap.impl.domain.model.TileSchema

const val TILES_SIZE_TAG = "TILES_SIZE_TAG"
internal class TilesUtil {

    private fun extractCoordinates(filePath: String): Triple<Int, Int, Int> {
        val regex = """.*_x=(\d+)_y=(\d+)_z=(\d+)\.jpg""".toRegex()
        val matchResult = regex.find(filePath)
        if (matchResult != null) {
            val (x, y, z) = matchResult.destructured
            return Triple(x.toInt(), y.toInt(), z.toInt())
        }
        return Triple(-1, -1, -1)
    }

    suspend fun mergeTilesSortedByCoordinates(
        tilesPaths: Map<MapItem, List<String?>>,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        zoom: Int
    ): Bitmap? {
        if (tilesPaths.isEmpty()) return null
        val horizontalSize = (maxX - minX).coerceAtLeast(1)
        val verticalSize = (maxY - minY).coerceAtLeast(1)

        val resultWidth = 255 * horizontalSize
        val resultHeight = 255 * verticalSize
        val resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val sortedMaps = tilesPaths.keys.sortedBy { it.position }
        return withContext(Dispatchers.Default) {
            sortedMaps.forEach { mapItem ->
                val paths = tilesPaths[mapItem]
                val (newMinY, newMaxY) = if (mapItem.mapType == TileSchema.TMS) {
                    Pair(GeoCalculator().googleXyzToTms(minX, minY, zoom).second, GeoCalculator().googleXyzToTms(maxX, maxY, zoom).second)
                } else {
                    Pair(minY, maxY)
                }
                val bitmapsWithCoords = paths?.mapNotNull { path ->
                    path?.let {
                        val (x, y) = extractCoordinates(it)
                        val bitmap = BitmapFactory.decodeFile(it)
                        val correctedY = if (mapItem.mapType == TileSchema.TMS) {
                            GeoCalculator().googleXyzToTms(x, y, zoom).second
                        } else {
                            y
                        }
                        Triple(x, correctedY, bitmap)
                    }
                }
                val range = if (newMaxY > newMinY) newMinY..newMaxY else newMinY downTo newMaxY
                val paint = Paint().apply {
                    isFilterBitmap = true
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                }
                for (y in range) {
                    for (x in minX..maxX) {
                        val tile =
                            bitmapsWithCoords?.find { it.first == x && it.second == y }?.third
                        val xOffset = (x - minX) * 255
                        val yOffset = if ((newMaxY > newMinY)) (y - newMinY) * 255 else (newMinY - y ) * 255
                        if (tile != null) {
                            canvas.drawBitmap(tile, xOffset.toFloat(), yOffset.toFloat(), paint)
                        }
                    }
                }
            }
            resultBitmap
        }
    }
}