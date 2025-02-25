package ru.maplyb.printmap.api.model

import java.io.Serializable

@kotlinx.serialization.Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
): Serializable