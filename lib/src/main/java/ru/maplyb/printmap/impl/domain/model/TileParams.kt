package ru.maplyb.printmap.impl.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TileParams(
    val x: Int,
    val y: Int,
    val z: Int
)
