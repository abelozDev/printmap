package ru.maplyb.printmap.api.model

data class BoundingBox(
    val latNorth: Double,
    val latSouth: Double,
    val lonEast: Double,
    val lonWest: Double
): java.io.Serializable