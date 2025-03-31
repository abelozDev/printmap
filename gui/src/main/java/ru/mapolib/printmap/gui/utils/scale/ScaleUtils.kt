package ru.mapolib.printmap.gui.utils.scale

import kotlin.math.roundToInt

fun roundScale(
    bitmapWidth: Int,
    scale: Int
): Pair<Int, Float> {
    val pixelsPerSm = pixelsPerCm(72f)
    val segmentLength = bitmapWidth * 0.075f
    /*Масштаб в зависимости от размера линии масштаба*/
    val scaleInSegment = ((segmentLength / pixelsPerSm) * scale).roundToInt()
    val roundedScale = (scaleInSegment / 500.0).roundToInt() * 500
    val segmentLengthWithNewScale = (roundedScale.toDouble() / scale.toDouble()) * pixelsPerSm
    return roundedScale to segmentLengthWithNewScale.toFloat()
}

fun pixelsPerCm(dpi: Float): Float {
    return dpi / 2.54f
}
