package ru.mapolib.printmap.gui.api

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource

object DownloadMapManagerImpl : DownloadMapManager {
    private val _state = MutableStateFlow<DownloadMapState>(DownloadMapState.Idle)
    override val state = _state.asStateFlow()
    private var preferences: PreferencesDataSource? = null
    private var mapPrint: MapPrint? = null
    private var isInit = false

    fun init(activity: Activity): DownloadMapManagerImpl {
        if (!isInit) {
            isInit = true
            mapPrint = MapPrint.create(activity)
            preferences = PreferencesDataSource.create(activity.applicationContext)
            mapPrint?.onMapReady {
                it?.let {
                    _state.value = DownloadMapState.Finished(it)
                }
            }
        }
        return this
    }

    override fun prepareDownloading(
        boundingBox: BoundingBox,
        maps: List<MapItem>,
        zoom: Int
    ) {
        _state.value = DownloadMapState.PrepareDownloading(boundingBox, maps, zoom)
    }
}

sealed interface DownloadMapState {
    data class PrepareDownloading(
        val boundingBox: BoundingBox,
        val maps: List<MapItem>,
        val zoom: Int
    ) : DownloadMapState
    data class Finished(
        val path: String
    ) : DownloadMapState
    data object Downloading : DownloadMapState
    data object Idle : DownloadMapState
    data class Failure(
        val message: String
    ) : DownloadMapState
}