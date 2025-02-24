package ru.maplyb.printmap.api.domain

import android.app.Activity
import android.content.Context
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.api.model.Errors
import ru.maplyb.printmap.api.model.FormingMapArgs
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.MapPrintImpl
import ru.maplyb.printmap.impl.domain.local.MapPath
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.domain.model.TileParams

interface MapPrint {

    fun onMapReady(result: (MapPath?) -> Unit)

    fun cancelDownloading()
    suspend fun deleteExistedMap(path: String)
    suspend fun getTilesCount(
        bound: BoundingBox,
        zoom: Int,
    ): OperationResult<List<TileParams>>

    suspend fun startFormingAMap(
        args: FormingMapArgs,
    )
    suspend fun getPreviewSize(
        mapList: List<MapItem>,
        bound: BoundingBox,
        zoom: Int,
    )

    companion object Factory {
        fun create(activity: Activity): MapPrint {
            val prefs = PreferencesDataSource.create()
            return MapPrintImpl(activity, prefs)
        }
    }
}