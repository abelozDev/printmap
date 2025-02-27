package ru.maplyb.printmap.api.model

import androidx.annotation.DrawableRes
import java.io.Serializable

@kotlinx.serialization.Serializable
data class Layer(
    override val name: String,
    override val selected: Boolean,
    val objects: List<LayerObject>,
    override val header: String? = null,
): Serializable, Expandable


@kotlinx.serialization.Serializable
sealed interface LayerObject: Serializable {
    val style: MapObjectStyle
    fun updateStyle(newStyle: MapObjectStyle): LayerObject
    @kotlinx.serialization.Serializable
    data class Line(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }
    }

    @kotlinx.serialization.Serializable
    data class Polygon(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }
    }
    @kotlinx.serialization.Serializable
    data class Radius(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }
    }
    @kotlinx.serialization.Serializable
    data class Text(
        val coords: GeoPoint,
        val text: String,
        val angle: Float,
        override val style: MapObjectStyle
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }
    }
    @kotlinx.serialization.Serializable
    data class Object(
        val coords: GeoPoint,
        @DrawableRes val res: Int,
        val angle: Float,
        override val style: MapObjectStyle
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }
    }
}

