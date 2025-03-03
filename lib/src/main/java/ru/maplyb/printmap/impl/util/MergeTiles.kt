package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.GeoPoint
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.model.TileSchema

internal class MergeTiles {

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
        author: String,
        boundingBox: BoundingBox,
        tiles: List<TileParams>,
        tilesPaths: Map<MapItem, List<String?>>,
        zoom: Int
    ): Result<Bitmap?> {
        return runCatching {
            val minX: Int = tiles.minOf { it.x }
            val maxX: Int = tiles.maxOf { it.x }
            val minY: Int = tiles.minOf { it.y }
            val maxY: Int = tiles.maxOf { it.y }
            val horizontalSize = (maxX + 1 - minX).coerceAtLeast(1)
            val verticalSize = (maxY + 1 - minY).coerceAtLeast(1)

            val resultWidth = 256 * horizontalSize
            val resultHeight = 256 * verticalSize
            val resultBitmap =
                Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val sortedMaps = tilesPaths.keys.sortedBy { it.position }
            withContext(Dispatchers.Default) {
                sortedMaps.forEach { mapItem ->
                    val paths = tilesPaths[mapItem]
                    val (newMinY, newMaxY) = if (mapItem.mapType == TileSchema.TMS) {
                        Pair(
                            GeoCalculator.googleXyzToTms(minX, minY, zoom).second,
                            GeoCalculator.googleXyzToTms(maxX, maxY, zoom).second
                        )
                    } else {
                        Pair(minY, maxY)
                    }
                    val bitmapsWithCoords = paths?.mapNotNull { path ->
                        path?.let {
                            val (x, y) = extractCoordinates(it)
                            val bitmap = BitmapFactory.decodeFile(it)
                            val correctedY = if (mapItem.mapType == TileSchema.TMS) {
                                GeoCalculator.googleXyzToTms(x, y, zoom).second
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

                            val yOffset =
                                if (newMaxY > newMinY) (y - newMinY) * 255 else (newMinY - y) * 255

                            if (tile != null) {
                                canvas.drawBitmap(tile, xOffset.toFloat(), yOffset.toFloat(), paint)
                            }
                        }
                    }
                }
                val croppedBitmap = cropBitmapToCurrentBoundingBox(
                    bitmap = resultBitmap,
                    tiles = tiles,
                    zoom = zoom,
                    boundingBox = boundingBox
                )
                addWatermark(croppedBitmap, author)
            }
        }
    }

    private fun cropBitmapToCurrentBoundingBox(
        bitmap: Bitmap,
        tiles: List<TileParams>,
        zoom: Int,
        boundingBox: BoundingBox
    ): Bitmap {
        val currentBoundingBox =
            GeoCalculator.tilesToBoundingBox(tiles, zoom)
        val (xMin, yMin) = GeoCalculator.convertGeoToPixel(
            objects = GeoPoint(
                boundingBox.latNorth,
                boundingBox.lonWest
            ),
            boundingBox = currentBoundingBox,
            bitmapHeight = bitmap.height,
            bitmapWidth = bitmap.width
        )
        val (xMax, yMax) = GeoCalculator.convertGeoToPixel(
            objects = GeoPoint(
                boundingBox.latSouth,
                boundingBox.lonEast
            ),
            boundingBox = currentBoundingBox,
            bitmapHeight = bitmap.height,
            bitmapWidth = bitmap.width
        )

        val widthPx = (xMax - xMin).toInt()
        val heightPx = (yMax - yMin).toInt()

        return Bitmap.createBitmap(bitmap, xMin.toInt(), yMin.toInt(), widthPx, heightPx)
    }

    private fun addWatermark(
        bitmap: Bitmap,
        author: String
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textSize = mutableBitmap.width * 0.025f

        val paint = Paint().apply {
            color = Color.BLACK
            this.textSize = textSize
            isAntiAlias = true
            alpha = (255 / 1.75).toInt()
            setShadowLayer(textSize * 0.6f, textSize * 0.4f, textSize * 0.4f, Color.WHITE)
        }

        val textHeight = paint.descent() - paint.ascent()
        val x = mutableBitmap.width * 0.02f
        val y = mutableBitmap.height - textHeight / 2

        canvas.drawText(author, x, y, paint)

        return mutableBitmap
    }

}