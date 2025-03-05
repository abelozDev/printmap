package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.impl.util.DrawInBitmap
import ru.maplyb.printmap.impl.files.FileUtil
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

class MapDownloadedViewModel(
    private val path: String,
    boundingBox: BoundingBox,
    layers: List<Layer>,
    private val fileUtil: FileUtil,
    private val context: Context
) : PrintMapViewModel<MapDownloadedEvent, MapDownloadedEffect>() {

    private val _state = MutableStateFlow(
        MapDownloadedUiState(
            image = path,
            bitmap = BitmapFactory.decodeFile(path),
            boundingBox = boundingBox,
            layers = layers
        )
    )
    val state = _state.asStateFlow()

    init {
        drawLayers()
        activeRequestCount
            .map {
                it > 0
            }
            .onEach { progress ->
                _state.update {
                    it.copy(
                        updateMapProgress = progress
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    private fun drawLayers() {
        viewModelScope.launch(Dispatchers.Default) {
            println("start on: ${this.hashCode()}")
            doWork {
                val bitmapWithDraw =
                    if (_state.value.layers.isNotEmpty()) {
                        val currentBitmap = BitmapFactory.decodeFile(path)
                        if (!_state.value.showLayers) currentBitmap else {
                            val bitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            val drawLayers = DrawInBitmap()
                            drawLayers.drawLayers(
                                bitmap = bitmap,
                                boundingBox = _state.value.boundingBox,
                                layers = _state.value.layers.filter { it.selected },
                                context = context
                            )
                            bitmap
                        }
                    } else _state.value.bitmap
                if (isActive) {
                    _state.update {
                        it.copy(
                            bitmap = bitmapWithDraw
                        )
                    }
                }
                println("end on: ${this.hashCode()}, ${this.isActive}")
            }
        }
    }

    override fun consumeEvent(action: MapDownloadedEvent) {
        when (action) {
            MapDownloadedEvent.DeleteImage -> {
                deleteExistedMap()
            }

            MapDownloadedEvent.Share -> shareImage()
            MapDownloadedEvent.ShowPolylineChanged -> {
                _state.update {
                    it.copy(
                        showLayers = !_state.value.showLayers
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.UpdateLayer -> {
                val newLayers = _state.value.layers.map { layer ->
                    if (layer.name == action.layer.name) action.layer else layer
                }
                _state.update {
                    it.copy(
                        layers = newLayers,
                        showLayers = !newLayers.all { layer -> !layer.selected }
                    )
                }
                drawLayers()
            }

            is MapDownloadedEvent.UpdateMapObjectStyle -> {
                updateMapObjectsStyle(action.layerObject)
            }

            MapDownloadedEvent.UpdateLayers -> {
                drawLayers()
            }
        }
    }

    private fun updateMapObjectsStyle(layerObject: LayerObject) {
        val type = layerObject.javaClass
        _state.update {
            it.copy(
                layers = it.layers.map { layer ->
                    layer.copy(
                        objects = layer.objects.map {
                            if (it.javaClass == type) {
                                it.updateStyleWidth(
                                    layerObject.style.width
                                )
                            } else it
                        }
                    )
                }
            )
        }
    }

    private fun shareImage() {
        viewModelScope.launch(Dispatchers.Default) {
            doWork(
                onProgress = { progress ->
                    _state.update {
                        it.copy(
                            state = if (progress) MapDownloadedState.Progress else MapDownloadedState.Initial
                        )
                    }
                },
                doOnAsyncBlock = {
                    fileUtil.saveBitmapToExternalStorage(
                        bitmap = _state.value.bitmap,
                        fileName = "${System.currentTimeMillis()}"
                    )?.let {
                        fileUtil.sendImageAsFile(it)
                    }
                }
            )
        }
    }

    private fun deleteExistedMap() {
        fileUtil.clearDownloadMapStorage()
        onEffect(MapDownloadedEffect.DeleteMap(_state.value.image ?: error("Image path is null")))
    }

    companion object {
        fun create(
            path: String,
            boundingBox: BoundingBox,
            layers: List<Layer>,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MapDownloadedViewModel::class.java)) {
                    val saveBitmap = FileUtil(context)
                    @Suppress("UNCHECKED_CAST")
                    return MapDownloadedViewModel(
                        path = path,
                        boundingBox = boundingBox,
                        layers = layers,
                        fileUtil = saveBitmap,
                        context = context
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}