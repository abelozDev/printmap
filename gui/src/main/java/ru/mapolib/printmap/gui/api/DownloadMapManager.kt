package ru.mapolib.printmap.gui.api

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapObjectStyle

interface DownloadMapManager {

    val state: StateFlow<DownloadMapState>

    suspend fun deleteMap(path: String)
    fun prepareDownloading(
        author: String,
        boundingBox: BoundingBox,
        maps: List<MapItem>,
        objects: List<Layer>,
        zoom: Int
    )
    fun hide()
    fun open()

    companion object {
        fun create(activity: Activity): DownloadMapManager {
            return DownloadMapManagerImpl.init(activity)
        }
    }
}