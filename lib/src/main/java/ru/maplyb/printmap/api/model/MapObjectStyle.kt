package ru.maplyb.printmap.api.model

import androidx.annotation.ColorInt

@kotlinx.serialization.Serializable
data class MapObjectStyle(
    val name: String,
    @ColorInt val color: Int,
    val width: Float
): java.io.Serializable
