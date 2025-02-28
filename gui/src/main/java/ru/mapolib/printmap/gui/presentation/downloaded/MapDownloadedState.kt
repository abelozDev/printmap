package ru.mapolib.printmap.gui.presentation.downloaded

import android.graphics.Bitmap
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

sealed interface MapDownloadedState {
    data object Initial: MapDownloadedState
    data object Progress: MapDownloadedState
}

data class MapDownloadedUiState(
    val state: MapDownloadedState = MapDownloadedState.Initial,
    val image: String? = null,
    val bitmap: Bitmap,
    val updateMapProgress: Boolean = false,
    val showLayers: Boolean = true,
    val boundingBox: BoundingBox,
    val layers: List<Layer>
)

sealed interface MapDownloadedEvent: PrintMapEvent {
    data object DeleteImage: MapDownloadedEvent
    data object Share: MapDownloadedEvent
    data object ShowPolylineChanged: MapDownloadedEvent
    data class UpdateLayer(val layer: Layer): MapDownloadedEvent
    data class UpdateMapObjectStyle(val layerObject: LayerObject): MapDownloadedEvent
    data object UpdateLayers: MapDownloadedEvent
}
sealed interface MapDownloadedEffect: PrintMapEffect {
    data class DeleteMap(val path: String): MapDownloadedEffect
}