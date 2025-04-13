package ru.maplyb.printmap.impl.util.draw_on_bitmap

import kotlin.math.roundToInt

internal fun roundCoordToNearestStep(value: Double): Double {
    val round = (value * 100).roundToInt() / 100.0
    return round
}