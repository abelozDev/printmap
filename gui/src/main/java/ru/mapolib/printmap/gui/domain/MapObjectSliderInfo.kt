package ru.mapolib.printmap.gui.domain

import ru.maplyb.printmap.api.model.LayerObject

internal data class MapObjectSliderInfo(
    val value: Float,
    val steps: Int,
    val name: String,
    val valueRange: ClosedFloatingPointRange<Float>,
)

internal fun LayerObject.toSliderInfo(value: Float): MapObjectSliderInfo {
   return when(this) {
        is LayerObject.Line -> {
            MapObjectSliderInfo(
                value = value,
                steps = 9,
                valueRange = 1f..20f,
                name = "Линии"
            )
        }
        is LayerObject.Object -> {
            MapObjectSliderInfo(
                value = value,
                steps = 49,
                valueRange = 1f..100f,
                name = "Объекты"
            )
        }
        is LayerObject.Polygon -> {
            MapObjectSliderInfo(
                value = value,
                steps = 9,
                valueRange = 1f..20f,
                name = "Полигоны"
            )
        }
        is LayerObject.Radius -> {
            MapObjectSliderInfo(
                value = value,
                steps = 9,
                valueRange = 1f..20f,
                name = "Радиусы"
            )
        }
        is LayerObject.Text -> {
            MapObjectSliderInfo(
                value = value,
                steps = 32,
                valueRange = 1f..128f,
                name = "Текст"
            )
        }
    }
}
