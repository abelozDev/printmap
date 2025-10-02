package ru.mapolib.printmap.gui.api

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem

interface DownloadMapManager {

    val state: StateFlow<DownloadMapState>

    suspend fun deleteMap(path: String)
    fun prepareDownloading(
        appName: String,
        author: String? = null,
        boundingBox: BoundingBox,
        maps: List<MapItem>,
        objects: List<Layer>,
        zoom: Int
    )
    fun hide()
    fun open()
    suspend fun updateDownloadStatusReportPath(context: Context, path: String?)

    companion object {
        fun create(activity: Activity): DownloadMapManager {
            return DownloadMapManagerImpl.init(activity)
        }
    }
}