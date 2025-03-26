package ru.maplyb.printmap.api.model

import java.io.Serializable

data class FormingMapArgs(
    val mapList: List<MapItem>,
    val bound: BoundingBox,
    val layers: List<Layer>,
    val zoom: Int,
    val quality: Int,
    val author: String,
    val appName: String
): Serializable
