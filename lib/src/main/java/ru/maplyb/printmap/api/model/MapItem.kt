package ru.maplyb.printmap.api.model

data class MapItem(
    val name: String,
    val type: MapType,
    val isVisible: Boolean,
    val alpha: Float,
    val position: Int
    //todo: Надо ли?
    /*val zoomMin: Int,
    val zoomMax: Int*/
)

sealed class MapType {
    abstract val path: String
    data class Offline(override val path: String): MapType()
    data class Online(override val path: String): MapType()
}
