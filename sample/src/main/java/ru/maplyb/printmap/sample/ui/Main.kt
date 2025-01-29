package ru.maplyb.printmap.sample.ui

import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox

fun main() {
//    boundingBoxToTiles
    val bbox = BoundingBox(
        latNorth = 85.0511287798066, // Почти самый верхний край карты
        latSouth = 0.0,             // Экватор
        lonEast = 90.0,             // 2 тайла по X (45° * 2)
        lonWest = 0.0               // От нулевого меридиана
    )
    val zoom = 2
    /*MapPrint.create().startFormingAMap(
        emptyList(),
        bbox,
        zoom
    )*/
}