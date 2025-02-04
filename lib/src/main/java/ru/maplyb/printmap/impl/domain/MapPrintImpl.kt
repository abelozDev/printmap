package ru.maplyb.printmap.impl.domain

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.model.TileParams
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.TILES_SIZE_TAG
import ru.maplyb.printmap.impl.util.TilesUtil
import ru.maplyb.printmap.impl.util.debugLog
import ru.maplyb.printmap.impl.util.limitSize

internal class MapPrintImpl(private val context: Context) : MapPrint {

    override fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): List<TileParams> {
        return GeoCalculator().calculateTotalTilesCount(bound, zoom)
    }
    override suspend fun startFormingAMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        onResult: (Bitmap?) -> Unit
    ) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
        val visibleMaps = mapList.filter { it.isVisible }
        val tileManager = DownloadTilesManager.create(context)
        /**Скачивание тайлов*/
        val downloadedTiles = tileManager.getTiles(visibleMaps, tiles)

        val resultBitmap = TilesUtil()
            .mergeTilesSortedByCoordinates(
                downloadedTiles,
                tiles.minOf { it.x },
                tiles.maxOf { it.x },
                tiles.minOf { it.y },
                tiles.maxOf { it.y },
                zoom
            )
        debugLog(TILES_SIZE_TAG, "Real size: ${resultBitmap?.byteCount}")

        onResult(resultBitmap/*?.limitSize()*/)


    }

    /** Считаем тестовый размер файла*/
    override suspend fun getPreviewSize(mapList: List<MapItem>, bound: BoundingBox, zoom: Int) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
        val visibleMaps = mapList.filter { it.isVisible }
        val tileManager = DownloadTilesManager.create(context)
        val approximateSize = 255 * 255 * 4 * tiles.size
        debugLog(TILES_SIZE_TAG, "Approximate size: $approximateSize")
    }

}