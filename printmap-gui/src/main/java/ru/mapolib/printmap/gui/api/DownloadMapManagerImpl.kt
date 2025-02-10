package ru.mapolib.printmap.gui.api

import android.app.Activity
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.local.PreferencesDataSource
import ru.maplyb.printmap.impl.util.DestroyLifecycleCallback

internal object DownloadMapManagerImpl : DownloadMapManager {
    private val _state = MutableStateFlow<DownloadMapState>(DownloadMapState.Idle)
    override val state = _state.asStateFlow()
    private val scope = CoroutineScope(Job())
    override suspend fun deleteMap(path: String) {
        mapPrint?.deleteExistedMap(path)
    }

    private var preferences: PreferencesDataSource? = null
    private var mapPrint: MapPrint? = null
    private var isInit = false

    fun init(activity: Activity): DownloadMapManagerImpl {
        if (!isInit) {
            isInit = true
            mapPrint = MapPrint.create(activity)
            preferences = PreferencesDataSource.create()
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
            /*mapPrint?.onMapReady {
                if (it == null) {
                    _state.value = DownloadMapState.Idle
                } else {
                    _state.value = DownloadMapState.Finished(it, true)
                }
            }*/
        }
        activity.application.registerActivityLifecycleCallbacks(DestroyLifecycleCallback { act ->
            if (act == activity) {
                try {
                    scope.cancel()
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
