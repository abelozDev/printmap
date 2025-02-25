package ru.maplyb.printmap.api.model

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(
    val latNorth: Double,
    val latSouth: Double,
    val lonEast: Double,
    val lonWest: Double
): java.io.Serializable