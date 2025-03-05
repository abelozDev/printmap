package ru.mapolib.printmap.gui.api

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.FormingMapArgs
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.util.DestroyLifecycleCallback
import ru.maplyb.printmap.impl.util.GeoCalculator

internal object DownloadMapManagerImpl : DownloadMapManager {
    private val _state = MutableStateFlow<DownloadMapState>(DownloadMapState.Idle)
    override val state = _state.asStateFlow()
    private var scope = CoroutineScope(SupervisorJob())


    private var preferences: PreferencesDataSource? = null
    private var mapPrint: MapPrint? = null

    fun cancelDownloading() {
        mapPrint?.cancelDownloading()
    }

    fun dismissFailure(context: Context) {
        scope.launch {
            preferences?.clear(context)
        }
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
                            val args = downloadStatus.filePath!!
                            DownloadMapState.Finished(
                                path = args.path,
                                boundingBox = args.boundingBox,
                                layers = args.layers,
                                isOpen = _state.value.isOpen
                            )
                        }

                        downloadStatus.errorMessage != null -> {
                            DownloadMapState.Failure(
                                downloadStatus.errorMessage!!,
                                isOpen = _state.value.isOpen
                            )
                        }

                        downloadStatus.progress != null -> {
                            DownloadMapState.Downloading(
                                progress = downloadStatus.progress!!,
                                message = downloadStatus.progressMessage ?: "",
                                isOpen = _state.value.isOpen,
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
        if (_state.value is DownloadMapState.PrepareDownloading) {
            _state.value = DownloadMapState.Idle.hide()
        } else {
            _state.value = _state.value.hide()
        }
    }

    override fun open() {
        _state.value = _state.value.open()
    }
    suspend fun startFormingAMap(
        args: FormingMapArgs,
    ) {
        mapPrint?.startFormingAMap(args)
    }

    override fun prepareDownloading(
        author: String,
        boundingBox: BoundingBox,
        maps: List<MapItem>,
        objects: List<Layer>,
        zoom: Int
    ) {
        val modifiedMaps = maps
            .map {
                val clampedValue = it.alpha.coerceIn(0f, 1f)
                it.copy(
                    alpha = (clampedValue * 255)
                )
            }
        _state.value = DownloadMapState.PrepareDownloading(boundingBox, modifiedMaps, zoom, objects, author, isOpen = true)
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
        val objects: List<Layer>,
        val author: String,
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
        val boundingBox: BoundingBox,
        val layers: List<Layer>,
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
