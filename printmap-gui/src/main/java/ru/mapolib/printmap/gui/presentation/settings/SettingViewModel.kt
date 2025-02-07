package ru.mapolib.printmap.gui.presentation.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

internal class SettingViewModel(
    private val mapPrint: MapPrint,
    zoom: Int,
    boundingBox: BoundingBox,
    maps: List<MapItem>
) : PrintMapViewModel<SettingEvent, SettingEffect>() {

    private val _state = MutableStateFlow(
        SettingUiState(
            boundingBox = boundingBox,
            zoom = zoom,
            maps = maps,
            tilesCount = 0
        )
    )
    val state = _state.asStateFlow()

    init {
        getTilesCount()
    }

    override fun consumeEvent(action: SettingEvent) {
        when (action) {
            is SettingEvent.UpdateQuality -> {
                //todo пересчитать размер?
                _state.update {
                    it.copy(
                        quality = action.newQuality
                    )
                }
            }

            is SettingEvent.UpdateZoom -> {
                _state.update {
                    it.copy(
                        zoom = action.newZoom
                    )
                }
            }

            SettingEvent.GetTilesCount -> {
                getTilesCount()
            }

            is SettingEvent.UpdateMap -> {
                _state.update {
                    it.copy(
                        maps = it.maps.map { map ->
                            if (map.name == action.map.name) action.map else map
                        }
                    )
                }
            }

            SettingEvent.StartDownloadingMap -> {
                startDownloadingMap()
            }
        }
    }

    private fun startDownloadingMap() {
        viewModelScope.launch {
            mapPrint.startFormingAMap(
                mapList = _state.value.maps,
                bound = _state.value.boundingBox,
                zoom = _state.value.zoom
            )
        }
    }

    private fun getTilesCount() {
        viewModelScope.launch {
            when (val result = mapPrint.getTilesCount(
                _state.value.boundingBox,
                _state.value.zoom
            )
            ) {
                is OperationResult.Error -> {
                    _state.update {
                        it.copy(
                            tilesCount = 0,
                            state = SettingState.Error(
                                message = result.message
                            )
                        )
                    }
                }

                is OperationResult.Success -> {
                    _state.update {
                        it.copy(
                            tilesCount = result.data.count()
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun create(
            activity: Activity,
            zoom: Int,
            boundingBox: BoundingBox,
            maps: List<MapItem>
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
                    val mapPrint = MapPrint.create(activity)
                    @Suppress("UNCHECKED_CAST")
                    return SettingViewModel(
                        mapPrint = mapPrint,
                        zoom = zoom,
                        boundingBox = boundingBox,
                        maps = maps
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}