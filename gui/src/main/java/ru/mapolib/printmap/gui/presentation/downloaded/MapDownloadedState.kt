package ru.mapolib.printmap.gui.presentation.downloaded

import android.graphics.Bitmap
import android.graphics.Color
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.Layer
import ru.maplyb.printmap.api.model.LayerObject
import ru.maplyb.printmap.impl.domain.model.PageFormat
import ru.maplyb.printmap.impl.util.draw_on_bitmap.CoordinateSystem
import ru.mapolib.printmap.gui.domain.MapObjectSliderInfo
import ru.mapolib.printmap.gui.presentation.util.PrintMapEffect
import ru.mapolib.printmap.gui.presentation.util.PrintMapEvent

internal sealed interface MapDownloadedState {
    data object Initial: MapDownloadedState
    data object Progress: MapDownloadedState
    data class Failure(val message: String): MapDownloadedState
    data class ChangeLayerColor(val layerObject: LayerObject): MapDownloadedState
    class ChangeCoordinatesGridColor: MapDownloadedState
}

typealias Dpi = Int
typealias CoordinateGridModel = Double

internal data class MapDownloadedUiState(
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
    val dpi: Dpi = dpiVariants.first(),

    val coordinateGrid: CoordinateGridModel = coordinateGridVariants.first(),
    val showCoordinateGrid: Boolean = false,
    val coordinateGridColor: CoordinateGridColor = CoordinateGridColor.default,
    val coordinatesGridSliderInfo: MapObjectSliderInfo = MapObjectSliderInfo(
        value = 5f,
        steps = 19,
        name = "Координатная сетка",
        valueRange = 1f..20f
    ),

    val name: String = "",
    val layerObjectsColor: Map<String, Int?>,
    val layers: List<Layer>,
    val coordinateSystem: CoordinateSystem = CoordinateSystem.SK42,
)

internal data class CoordinateGridColor(
    val color: Int
) {
    companion object {
        val default: CoordinateGridColor = CoordinateGridColor(color = Color.RED)
    }
}

internal val coordinateGridVariants: List<CoordinateGridModel> = listOf(1.0, 0.5, 0.25, 0.125, 0.075, 0.05, 0.025, 0.01, 0.005, 0.0025)
internal val dpiVariants: List<Dpi> = listOf(72,300)

sealed interface ExportTypes {
    val name: String
    data class PDF(
        override val name: String = "PDF",
        val format: PageFormat = PageFormat.A4,
        val pagesSize: Int = 0
    ): ExportTypes {
        override fun toString(): String {
            return name
        }
    }
    data class PNG(
        override val name: String = "PNG",
    ): ExportTypes {
        override fun toString(): String {
            return name
        }
    }

    companion object {
        val entries = listOf(PDF(), PNG())
    }
}

internal sealed interface MapDownloadedEvent: PrintMapEvent {
    data object DeleteImage : MapDownloadedEvent
    data object Share : MapDownloadedEvent
    data class UpdateExportType(val type: ExportTypes) : MapDownloadedEvent
    data object ShowPolylineChanged : MapDownloadedEvent
    data object ChangeOrientation : MapDownloadedEvent
    data class UpdateLayer(val layer: Layer) : MapDownloadedEvent
    data class UpdateName(val name: String) : MapDownloadedEvent
    data class SelectDpi(val dpi: Dpi) : MapDownloadedEvent
    data class UpdateMapObjectStyle(val layerObject: LayerObject) : MapDownloadedEvent
    data object UpdateLayers : MapDownloadedEvent
    data class UpdateState(val state: MapDownloadedState) : MapDownloadedEvent
    data class UpdateColorToObjects(val color: Color?, val layerObject: LayerObject) :
        MapDownloadedEvent

    /*CoordinateGrid*/
    class ChangeCheckCoordinateGrid : MapDownloadedEvent
    data class CoordinateGridSliderValueChangeFinished(val value: Float) : MapDownloadedEvent
    data class SelectCoordinateGrid(val value: Double) : MapDownloadedEvent
    class UpdateCoordinateGridColor(val color: CoordinateGridColor) : MapDownloadedEvent

    /*CoordinateSystem*/
    data class SelectCoordinateSystem(val system: CoordinateSystem): MapDownloadedEvent
}
internal sealed interface MapDownloadedEffect: PrintMapEffect {
    data class DeleteMap(val path: String): MapDownloadedEffect
}

internal enum class ImageOrientation(val description: String) {
    PORTRAIT("Портрет"),
    LANDSCAPE("Ландшафт");

    fun getOther(): ImageOrientation {
        return if (this == PORTRAIT) LANDSCAPE else PORTRAIT
    }
}