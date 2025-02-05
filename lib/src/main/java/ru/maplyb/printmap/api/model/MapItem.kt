package ru.maplyb.printmap.api.model

import ru.maplyb.printmap.impl.domain.model.TileSchema
import java.io.Serializable

data class MapItem(
    val name: String,
    val type: MapType,
    val isVisible: Boolean,
    val alpha: Float,
    val position: Int,
    val mapType: TileSchema = TileSchema.GOOGLE
    //todo: Надо ли?
    /*val zoomMin: Int,
    val zoomMax: Int*/
): Serializable

sealed class MapType: Serializable {
    abstract val path: String
    data class Offline(override val path: String): MapType()
    data class Online(override val path: String): MapType()
}

