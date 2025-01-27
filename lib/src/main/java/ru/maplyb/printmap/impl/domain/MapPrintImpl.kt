package ru.maplyb.printmap.impl.domain

import ru.maplyb.printmap.api.domain.MapPrint
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem

internal class MapPrintImpl: MapPrint {
    override fun startFormingAMap(mapList: List<MapItem>, bound: BoundingBox, zoom: Int) {
        println("MapPrintTag Map is forming")
    }
}