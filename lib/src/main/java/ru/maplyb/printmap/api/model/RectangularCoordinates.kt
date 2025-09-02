package ru.maplyb.printmap.api.model

import kotlinx.serialization.Serializable

@Serializable
data class RectangularCoordinates(
    val topLeft: GeoPoint,
    val topRight: GeoPoint,
    val bottomRight: GeoPoint,
    val bottomLeft: GeoPoint,
): java.io.Serializable
