package ru.maplyb.printmap.api.model

import java.io.Serializable

data class Layer(
    override val name: String,
    override val selected: Boolean,
    val objects: List<LayerObject>,
    override val header: String? = null,
): Serializable, Expandable

sealed interface LayerObject: Serializable {
    /*val objects: List<GeoPoint>*/
    val style: MapObjectStyle
    data class Line(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject

    data class Polygon(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject

    data class Radius(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject

    data class Text(
        val coords: GeoPoint,
        val text: String,
        val angle: Float,
        override val style: MapObjectStyle
    ): LayerObject
}