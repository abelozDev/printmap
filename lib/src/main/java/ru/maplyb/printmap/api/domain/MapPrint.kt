package ru.maplyb.printmap.api.domain

import android.app.Activity
import android.content.Context
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.MapPrintImpl
import ru.maplyb.printmap.impl.domain.model.TileParams

interface MapPrint {

    fun onMapReady(result: (List<DownloadedImage>) -> Unit)

    fun init(context: Context)
    /*fun ShowMapPrintDialog(mapList: List<MapItem>, bound: BoundingBox, zoom: Int)*/
    fun deleteExistedMap()
    fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): List<TileParams>

    suspend fun startFormingAMap(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
        /*onResult: (List<DownloadedImage>) -> Unit*/
    )
    suspend fun getPreviewSize(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
    )

    companion object Factory {
        fun create(activity: Activity): MapPrint = MapPrintImpl(activity)
    }
}