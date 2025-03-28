package ru.mapolib.printmap.gui.presentation.downloaded

import android.graphics.Bitmap
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.impl.domain.model.PageFormat
import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

sealed interface MapDownloadedState {
    data object Initial: MapDownloadedState
    data object Progress: MapDownloadedState
    data class Failure(val message: String): MapDownloadedState
}
typealias Dpi = Int
data class MapDownloadedUiState(
    val state: MapDownloadedState = MapDownloadedState.Initial,
    val image: String? = null,
    val bitmap: Bitmap,
    val appName: String,
    val author: String,
    val updateMapProgress: Boolean = false,
    val showLayers: Boolean = true,
    val boundingBox: BoundingBox,
    val exportType: ExportTypes = ExportTypes.PNG(),
    val orientation: ImageOrientation = ImageOrientation.PORTRAIT,
    val dpi: Dpi = 72,
    val name: String = "",
    val layers: List<Layer>
)


val dpiVariants: List<Dpi> = listOf(72,300)


sealed interface ExportTypes {
    val name: String
    data class PDF(
        override val name: String = "PDF",
        val format: PageFormat = PageFormat.A4,
        val pagesSize: Int = 0
    ): ExportTypes
    data class PNG(
        override val name: String = "PNG",
    ): ExportTypes

    companion object {
        val entries = listOf(PDF(), PNG())
    }
}
sealed interface MapDownloadedEvent: PrintMapEvent {
    data object DeleteImage: MapDownloadedEvent
    data object Share: MapDownloadedEvent
    data class UpdateExportType(val type: ExportTypes): MapDownloadedEvent
    data object ShowPolylineChanged: MapDownloadedEvent
    data object ChangeOrientation: MapDownloadedEvent
    data class UpdateLayer(val layer: Layer): MapDownloadedEvent
    data class UpdateName(val name: String): MapDownloadedEvent
    data class SelectDpi(val dpi: Dpi): MapDownloadedEvent
    data class UpdateMapObjectStyle(val layerObject: LayerObject): MapDownloadedEvent
    data object UpdateLayers: MapDownloadedEvent
    data class UpdateState(val state: MapDownloadedState): MapDownloadedEvent
}
sealed interface MapDownloadedEffect: PrintMapEffect {
    data class DeleteMap(val path: String): MapDownloadedEffect
}

enum class ImageOrientation(val description: String) {
    PORTRAIT("Портрет"),
    LANDSCAPE("Ландшафт");

    fun getOther(): ImageOrientation {
        return if (this == PORTRAIT) LANDSCAPE else PORTRAIT
    }
}