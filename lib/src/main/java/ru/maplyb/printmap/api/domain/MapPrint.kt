package ru.maplyb.printmap.api.domain

import android.content.Context
import android.graphics.Bitmap
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.MapPrintImpl
import ru.maplyb.printmap.impl.domain.model.TileParams

interface MapPrint {

    /*fun ShowMapPrintDialog(mapList: List<MapItem>, bound: BoundingBox, zoom: Int)*/
    fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): List<TileParams>
    suspend fun startFormingAMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        onResult: (Bitmap?) -> Unit
    )

    companion object Factory {
        fun create(context: Context): MapPrint = MapPrintImpl(context)
    }
}