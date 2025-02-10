package ru.mapolib.printmap.gui.presentation.downloaded

import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

sealed interface MapDownloadedState {
    data object Initial: MapDownloadedState
}

data class MapDownloadedUiState(
    val state: MapDownloadedState = MapDownloadedState.Initial,
    val image: String? = null
)

sealed interface MapDownloadedEvent: PrintMapEvent {
    data object DeleteImage: MapDownloadedEvent
}
sealed interface MapDownloadedEffect: PrintMapEffect {
    data class DeleteMap(val path: String): MapDownloadedEffect
}