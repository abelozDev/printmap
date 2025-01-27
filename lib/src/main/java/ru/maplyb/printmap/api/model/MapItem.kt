package ru.maplyb.printmap.api.model

data class MapItem(
    val name: String,
    val type: MapType,
    val isVisible: Boolean,
    val transparent: Float,
    //todo: Надо ли?
    /*val zoomMin: Int,
    val zoomMax: Int,
    val position: Int*/
)

enum class MapType {
    OFFLINE, ONLINE
}
