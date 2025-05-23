package ru.mapolib.printmap.gui.presentation.settings

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
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.OperationResult
import ru.maplyb.printmap.impl.domain.use_case.GetTilesCountAndFileSizeUseCase
import ru.mapolib.printmap.gui.presentation.util.PrintMapViewModel

internal class SettingViewModel(
    private val getTilesCountAndFileSizeUseCase: GetTilesCountAndFileSizeUseCase,
    zoom: Int,
    boundingBox: BoundingBox,
    maps: List<MapItem>,
    objects: List<Layer>,
) : PrintMapViewModel<SettingEvent, SettingEffect>() {

    private val _state = MutableStateFlow(
        SettingUiState(
            boundingBox = boundingBox,
            zoom = zoom,
            maps = maps,
            tilesCount = 0,
            objects = objects
            )
    )
    val state = _state.asStateFlow()

    init {
        activeRequestCount
            .map {
                it > 0
            }
            .onEach {
                _state.update { state ->
                    state.copy(
                        progress = it,
                    )
                }
            }
            .launchIn(viewModelScope)

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
            is SettingEvent.UpdateMaps -> {
                _state.update {
                    it.copy(
                        maps = it.maps.map { map ->
                            if (action.maps.any { it.name == map.name }) {
                                action.maps.first { it.name == map.name }
                            } else {
                                map
                            }
                        }
                    )
                }
            }

            SettingEvent.StartDownloadingMap -> {
                startDownloadingMap()
            }

            SettingEvent.ShowPolylineChanged -> {
                _state.update {
                    it.copy(
                        showPolyline = !it.showPolyline
                    )
                }
            }
        }
    }

    private fun startDownloadingMap() {
        viewModelScope.launch {
            onEffect(
                SettingEffect.StartFormingAMap(
                    maps = _state.value.maps.filter { it.selected && it.isVisible },
                    boundingBox = _state.value.boundingBox,
                    zoom = _state.value.zoom,
                    quality = _state.value.quality,
                    objects = _state.value.objects
                )
            )
        }
    }

    private fun getTilesCount() {
        viewModelScope.launch {
            doWork {
                when (val result = getTilesCountAndFileSizeUseCase(
                    _state.value.boundingBox,
                    _state.value.zoom
                )) {
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
                                tilesCount = result.data.first,
                                fileSize = result.data.second,
                                state = SettingState.Initial
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun create(
            zoom: Int,
            boundingBox: BoundingBox,
            maps: List<MapItem>,
            objects: List<Layer>,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    val getTilesCountUseCase = GetTilesCountAndFileSizeUseCase()
                    return SettingViewModel(
                        getTilesCountAndFileSizeUseCase = getTilesCountUseCase,
                        zoom = zoom,
                        boundingBox = boundingBox,
                        maps = maps,
                        objects = objects
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

}