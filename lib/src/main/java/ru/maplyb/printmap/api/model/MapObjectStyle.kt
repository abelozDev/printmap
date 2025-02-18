package ru.maplyb.printmap.api.model

import androidx.annotation.ColorInt

data class MapObjectStyle(
    @ColorInt val color: Int,
    val width: Float
): java.io.Serializable
