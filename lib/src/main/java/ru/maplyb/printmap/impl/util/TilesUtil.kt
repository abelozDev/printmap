package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun mergeTilesSortedByCoordinates(tilesPaths: List<String>): Bitmap {
        return withContext(Dispatchers.Default) {
            val allCoords = tilesPaths.map {
                extractCoordinates(it)
            }
            val bitmapsWithCoords = tilesPaths.map { path ->
                val (x, y) = extractCoordinates(path)
                val bitmap = BitmapFactory.decodeFile(path)
                Triple(x, y, bitmap)
            }
            val horizontalSize = allCoords.maxOf { it.first } - allCoords.minOf { it.first }
            val verticalSize = allCoords.maxOf { it.second } - allCoords.minOf { it.second }

            val resultWidth = 255 * horizontalSize
            val resultHeight = 255 * verticalSize
            val resultBitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)

            var xOffset = 0
            var yOffset = 0
            for (y in allCoords.minOf { it.second }..allCoords.maxOf { it.second }) {
                for (x in allCoords.minOf { it.first }..allCoords.maxOf { it.first }) {
                    val tile = bitmapsWithCoords.find { it.first == x && it.second == y }?.third
                    if (tile != null) {
                        resultBitmap.apply {
                            val canvas = android.graphics.Canvas(this)
                            canvas.drawBitmap(tile, xOffset.toFloat(), yOffset.toFloat(), null)
                        }
                    }
                    xOffset += 255
                }
                xOffset = 0
                yOffset += 255
            }
            resultBitmap
        }
    }

}