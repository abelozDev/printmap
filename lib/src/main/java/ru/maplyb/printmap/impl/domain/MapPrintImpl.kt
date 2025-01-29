package ru.maplyb.printmap.impl.domain

import android.content.Context
import android.graphics.Bitmap
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.repo.DownloadTilesManager
import ru.maplyb.printmap.impl.util.GeoCalculator
import ru.maplyb.printmap.impl.util.TilesUtil

internal class MapPrintImpl(private val context: Context) : MapPrint {

    override suspend fun startFormingAMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        onResult: (Bitmap?) -> Unit
    ) {
        val tiles = GeoCalculator().calculateTotalTilesCount(bound, zoom)
        val visibleMaps = mapList.filter { it.isVisible }
        val tileManager = DownloadTilesManager.create(context)
        //todo: обработать null
        val downloadedTiles = tileManager.getTiles(visibleMaps, tiles).mapNotNull { it }
        onResult(TilesUtil().mergeTilesSortedByCoordinates(downloadedTiles))
        println("tiles = $tiles")
    }
}