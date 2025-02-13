package ru.mapolib.printmap.gui.presentation.downloaded

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

class MapDownloadedViewModel(
    path: String
): PrintMapViewModel<MapDownloadedEvent, MapDownloadedEffect>() {

    private val _state = MutableStateFlow(
        MapDownloadedUiState(
            image = path
        )
    )
    val state = _state.asStateFlow()

    override fun consumeEvent(action: MapDownloadedEvent) {
        when(action) {
            MapDownloadedEvent.DeleteImage -> {
                deleteExistedMap()
            }
        }
    }

    private fun deleteExistedMap() {
        onEffect(MapDownloadedEffect.DeleteMap(_state.value.image ?: error("Image path is null")))

    }

    companion object {
        fun create(
            path: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MapDownloadedViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MapDownloadedViewModel(
                        path = path
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}