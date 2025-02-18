package ru.maplyb.printmap.api.model

import java.io.Serializable

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
): Serializable