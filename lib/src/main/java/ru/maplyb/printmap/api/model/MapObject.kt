package ru.maplyb.printmap.api.model

data class MapObject(
//    val name: String,
//    val isVisible: Boolean,
    val position: GeoPoint
): java.io.Serializable

data class Line(
    val style: MapObjectStyle,
    val objects: List<GeoPoint>
): java.io.Serializable
