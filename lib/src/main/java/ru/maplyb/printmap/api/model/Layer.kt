package ru.maplyb.printmap.api.model

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import java.io.Serializable

@kotlinx.serialization.Serializable
data class Layer(
    override val name: String,
    override val selected: Boolean,
    val objects: List<LayerObject>,
    override val header: String? = null,
) : Serializable, Expandable


@kotlinx.serialization.Serializable
sealed interface LayerObject : Serializable {
    val style: MapObjectStyle
    fun updateStyle(newStyle: MapObjectStyle): LayerObject
    fun updateStyleWidth(width: Float): LayerObject

    @kotlinx.serialization.Serializable
    data class Line(
        override val style: MapObjectStyle,
        val pathEffect: String? = "DEFAULT",
        val objects: List<GeoPoint>,
    ) : LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }

        override fun updateStyleWidth(width: Float): LayerObject {
            return this.copy(
                style = this.style.copy(
                    width = width
                )
            )
        }
    }

    @kotlinx.serialization.Serializable
    data class Polygon(
        override val style: MapObjectStyle,
        val alpha: Float = 0f,
        val pathEffect: String? = "DEFAULT",
        val objects: List<GeoPoint>,
    ) : LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }

        override fun updateStyleWidth(width: Float): LayerObject {
            return this.copy(
                style = this.style.copy(
                    width = width
                )
            )
        }
    }

    @kotlinx.serialization.Serializable
    data class Image(
        val path: String,
        val coords: RectangularCoordinates,
        val alpha: Float = 0f,
        override val style: MapObjectStyle,
    ): LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject = this

        override fun updateStyleWidth(width: Float): LayerObject = this
    }

    @kotlinx.serialization.Serializable
    data class Radius(
        override val style: MapObjectStyle,
        val objects: List<GeoPoint>,
    ) : LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }

        override fun updateStyleWidth(width: Float): LayerObject {
            return this.copy(
                style = this.style.copy(
                    width = width
                )
            )
        }
    }

    @kotlinx.serialization.Serializable
    data class Text(
        val coords: GeoPoint,
        val text: String,
        /**0..100*/
        val angle: Float,
        override val style: MapObjectStyle
    ) : LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }

        override fun updateStyleWidth(width: Float): LayerObject {
            return this.copy(
                style = this.style.copy(
                    width = width
                )
            )
        }
    }

    @kotlinx.serialization.Serializable
    data class Object(
        val coords: GeoPoint,
        val res: ObjectRes,
        @ColorInt val nameColor: Int = Color.BLACK,
        val angle: Float,
        val name: String,
        val description: String,
        override val style: MapObjectStyle
    ) : LayerObject {
        override fun updateStyle(newStyle: MapObjectStyle): LayerObject {
            return copy(style = newStyle)
        }

        override fun updateStyleWidth(width: Float): LayerObject {
            return this.copy(
                style = this.style.copy(
                    width = width
                )
            )
        }
    }
}

@kotlinx.serialization.Serializable
sealed interface ObjectRes : Serializable {
    @kotlinx.serialization.Serializable
    data class Storage(val res: String) : ObjectRes

    @kotlinx.serialization.Serializable
    data class Local(@DrawableRes val res: Int) : ObjectRes
}

