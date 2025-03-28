package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap

fun canAllocateBitmap(width: Int, height: Int, config: Bitmap.Config): Boolean {
    val bytesPerPixel = when (config) {
        Bitmap.Config.ARGB_8888 -> 4
        Bitmap.Config.RGB_565 -> 2
        Bitmap.Config.ALPHA_8 -> 1
        else -> 4 // По умолчанию считаем как ARGB_8888
    }

    val estimatedSize = width.toLong() * height.toLong() * bytesPerPixel
    val maxMemory = Runtime.getRuntime().maxMemory() // Всего доступно приложению
    val freeMemory = Runtime.getRuntime().freeMemory() // Свободно в данный момент
    val allocatedMemory = Runtime.getRuntime().totalMemory() // Уже выделено

    val availableMemory = maxMemory - allocatedMemory + freeMemory

    return estimatedSize < availableMemory / 2 // Оставляем запас памяти
}