package ru.maplib.printmap.print_gui.utils

internal fun formatSize(bytes: Int): String {
    val units = arrayOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }

    return "%.2f %s".format(size, units[unitIndex])
}