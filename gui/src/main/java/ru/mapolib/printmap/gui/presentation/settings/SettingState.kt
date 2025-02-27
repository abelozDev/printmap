package ru.mapolib.printmap.gui.presentation.settings

import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.MapItem
import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

sealed interface SettingState {
    data object Initial : SettingState
    data class Error(val message: String) : SettingState
    data object Progress : SettingState
}

data class SettingUiState(
    val state: SettingState = SettingState.Initial,
    val fileSize: Long? = null,
    val zoom: Int,
    val quality: Int = 100,
    val maps: List<MapItem>,
    val showPolyline: Boolean = true,
    val objects: List<Layer>,
    val tilesCount: Int,
    val boundingBox: BoundingBox,
    val progress: Boolean = false
) {
    fun buttonEnabled(): Boolean {
        return maps.any { it.selected } && state is SettingState.Initial && !progress
    }
}

sealed interface SettingEvent : PrintMapEvent {
    data class UpdateZoom(val newZoom: Int) : SettingEvent
    data object GetTilesCount : SettingEvent
    data class UpdateQuality(val newQuality: Int) : SettingEvent
    data class UpdateMap(val map: MapItem) : SettingEvent
    data object StartDownloadingMap : SettingEvent
    data object ShowPolylineChanged: SettingEvent
}

sealed interface SettingEffect : PrintMapEffect {
    data class StartFormingAMap(
        val maps: List<MapItem>,
        val boundingBox: BoundingBox,
        val zoom: Int,
        val quality: Int,
        val objects: List<Layer>,
        ) : SettingEffect
}