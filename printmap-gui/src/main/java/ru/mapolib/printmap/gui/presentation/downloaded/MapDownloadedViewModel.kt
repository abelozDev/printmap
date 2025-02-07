package ru.mapolib.printmap.gui.presentation.downloaded

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.maplyb.printmap.api.domain.MapPrint
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

class MapDownloadedViewModel(
    private val mapPrint : MapPrint
): PrintMapViewModel<MapDownloadedEvent, MapDownloadedEffect>() {

    private val _state = MutableStateFlow(
        MapDownloadedUiState()
    )
    val state = _state.asStateFlow()

    init {
        mapPrint
            .onMapReady { map ->
                _state.update {
                    it.copy(
                        image = map
                    )
                }
            }
    }
    override fun consumeEvent(action: MapDownloadedEvent) {
        when(action) {
            MapDownloadedEvent.DeleteImage -> {
                deleteExistedMap()
            }
        }
    }

    private fun deleteExistedMap() {
        mapPrint.deleteExistedMap()
    }

    companion object {
        fun create(
            activity: Activity,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MapDownloadedViewModel::class.java)) {
                    val mapPrint = MapPrint.create(activity)
                    @Suppress("UNCHECKED_CAST")
                    return MapDownloadedViewModel(
                        mapPrint = mapPrint
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}