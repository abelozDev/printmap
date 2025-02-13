package ru.mapolib.printmap.gui.api

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.util.DestroyLifecycleCallback

internal object DownloadMapManagerImpl : DownloadMapManager {
    private val _state = MutableStateFlow<DownloadMapState>(DownloadMapState.Idle)
    override val state = _state.asStateFlow()
    private var scope = CoroutineScope(SupervisorJob())


    private var preferences: PreferencesDataSource? = null
    var mapPrint: MapPrint? = null

    fun cancelDownloading() {
        mapPrint?.cancelDownloading()
    }

    override suspend fun deleteMap(path: String) {
        mapPrint?.deleteExistedMap(path)
    }

    fun init(activity: Activity): DownloadMapManagerImpl {
        println("DownloadMapManagerImpl init")
        mapPrint = MapPrint.create(activity)
        preferences = PreferencesDataSource.create()
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob())
        }
        scope.launch {
            preferences
                ?.getDownloadStatus(activity)
                ?.collect { downloadStatus ->
                    _state.value = when {
                        downloadStatus.isFinished && downloadStatus.filePath != null -> {
                            DownloadMapState.Finished(downloadStatus.filePath!!)
                        }

                        downloadStatus.errorMessage != null -> {
                            DownloadMapState.Failure(downloadStatus.errorMessage!!)
                        }

                        downloadStatus.progress != null -> {
                            DownloadMapState.Downloading(
                                downloadStatus.progress!!,
                                isOpen = _state.value.isOpen
                            )
                        }

                        else -> {
                            DownloadMapState.Idle
                        }
                    }
                }
        }
        activity.application.registerActivityLifecycleCallbacks(DestroyLifecycleCallback { act ->
            if (act == activity) {
                try {
                    scope.coroutineContext.cancelChildren()
                } catch (_: IllegalStateException) {
                }
            }
        }
        )
        return this
    }

    override fun hide() {
        _state.value = _state.value.hide()
    }

    override fun open() {
        _state.value = _state.value.open()
    }

    suspend fun startFormingAMap(
        maps: List<MapItem>,
        boundingBox: BoundingBox,
        zoom: Int,
        quality: Int
    ) {
        mapPrint?.startFormingAMap(maps, boundingBox, zoom, quality)
    }

    override fun prepareDownloading(
        boundingBox: BoundingBox,
        maps: List<MapItem>,
        zoom: Int
    ) {
        _state.value = DownloadMapState.PrepareDownloading(boundingBox, maps, zoom, true)
    }
}

sealed interface DownloadMapState {
    val isOpen: Boolean
    fun hide(): DownloadMapState
    fun open(): DownloadMapState
    data class PrepareDownloading(
        val boundingBox: BoundingBox,
        val maps: List<MapItem>,
        val zoom: Int,
        override val isOpen: Boolean = false
    ) : DownloadMapState {
        override fun hide(): DownloadMapState {
            return this.copy(isOpen = false)
        }

        override fun open(): DownloadMapState {
            return this.copy(isOpen = true)
        }
    }

    data class Finished(
        val path: String,
        override val isOpen: Boolean = false
    ) : DownloadMapState {
        override fun hide(): DownloadMapState {
            return this.copy(isOpen = false)
        }

        override fun open(): DownloadMapState {
            return this.copy(isOpen = true)
        }
    }

    data class Downloading(
        val progress: Int,
        override val isOpen: Boolean = false
    ) : DownloadMapState {
        override fun hide(): DownloadMapState {
            return this.copy(isOpen = false)
        }

        override fun open(): DownloadMapState {
            return this.copy(isOpen = true)
        }
    }

    data object Idle : DownloadMapState {
        override val isOpen: Boolean
            get() = false

        override fun hide(): DownloadMapState = this
        override fun open(): DownloadMapState = this
    }

    data class Failure(
        val message: String,
        override val isOpen: Boolean = false
    ) : DownloadMapState {
        override fun hide(): DownloadMapState {
            return this.copy(isOpen = false)
        }

        override fun open(): DownloadMapState {
            return this.copy(isOpen = true)
        }
    }
}
