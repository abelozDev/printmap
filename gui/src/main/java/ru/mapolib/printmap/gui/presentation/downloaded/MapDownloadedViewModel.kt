package ru.mapolib.printmap.gui.presentation.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.impl.util.DrawInBitmap
import ru.maplyb.printmap.impl.util.FileUtil
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

class MapDownloadedViewModel(
    path: String,
    boundingBox: BoundingBox,
    layers: List<Layer>,
    private val fileUtil: FileUtil,
    private val context: Context
): PrintMapViewModel<MapDownloadedEvent, MapDownloadedEffect>() {

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
        activeRequestCount
            .map {
                it > 0
            }
            .onEach { progress ->
                _state.update {
                    it.copy(
                        progress = progress
                    )
                }
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            drawLayers()
        }
    }

    private suspend fun drawLayers() {
        doWork {
            val bitmapWithDraw = if (_state.value.layers.isNotEmpty()) {
                val bitmap = _state.value.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val drawLayers = DrawInBitmap()
                drawLayers.drawLayers(
                    bitmap = bitmap,
                    boundingBox = _state.value.boundingBox,
                    layers = _state.value.layers,
                    context = context
                )
                bitmap // Возвращаем измененный bitmap
            } else _state.value.bitmap

            _state.update {
                it.copy(
                    bitmap = bitmapWithDraw
                )
            }
        }
    }

    override fun consumeEvent(action: MapDownloadedEvent) {
        when(action) {
            MapDownloadedEvent.DeleteImage -> {
                deleteExistedMap()
            }

            MapDownloadedEvent.Share -> shareImage()
        }
    }
    private fun shareImage() {
        fileUtil.saveBitmapToExternalStorage(
            bitmap = _state.value.bitmap,
            fileName = "${System.currentTimeMillis()}"
        )?.let {
            fileUtil.sendImageAsFile(it)
        }
    }
    private fun deleteExistedMap() {
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