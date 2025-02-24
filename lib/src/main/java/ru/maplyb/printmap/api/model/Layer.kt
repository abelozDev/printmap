package ru.maplyb.printmap.api.model

import java.io.Serializable

sealed interface Layer: Serializable, Expandable {
    val style: MapObjectStyle
    override val name: String
    override val selected: Boolean
    data class Line(
        override val style: MapObjectStyle,
        override val selected: Boolean = true,
        override val name: String,
        override val header: String = "Линии",
        val objects: List<GeoPoint>,
    ): Layer

    data class Polygon(
        override val style: MapObjectStyle,
        override val selected: Boolean = true,
        override val name: String,
        override val header: String = "Полигоны",
        val objects: List<GeoPoint>,
    ): Layer

    data class Radius(
        override val name: String,
        override val selected: Boolean = true,
        override val style: MapObjectStyle,
        override val header: String = "Радиусы",
        val objects: List<GeoPoint>,
    ): Layer
}