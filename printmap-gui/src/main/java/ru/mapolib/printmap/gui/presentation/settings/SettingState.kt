package ru.mapolib.printmap.gui.presentation.settings

import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

sealed interface SettingState {
    data object Initial: SettingState
    data class Error(val message: String): SettingState
}

data class SettingUiState(
    val state: SettingState = SettingState.Initial,
    val fileSize: Int? = null,
    val zoom: Int,
    val quality: Int = 100,
    val maps: List<MapItem>,
    val tilesCount: Int,
    val boundingBox: BoundingBox
)

sealed interface SettingEvent: PrintMapEvent {
    data class UpdateZoom(val newZoom: Int): SettingEvent
    data object GetTilesCount: SettingEvent
    data class UpdateQuality(val newQuality: Int): SettingEvent
    data class UpdateMap(val map: MapItem): SettingEvent
    data object StartDownloadingMap: SettingEvent
}
sealed interface SettingEffect: PrintMapEffect