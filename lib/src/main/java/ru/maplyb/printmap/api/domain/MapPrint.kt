package ru.maplyb.printmap.api.domain

import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.impl.domain.MapPrintImpl

interface MapPrint {

    /*fun ShowMapPrintDialog(mapList: List<MapItem>, bound: BoundingBox, zoom: Int)*/

    fun startFormingAMap(mapList: List<MapItem>, bound: BoundingBox, zoom: Int)


    companion object Factory {
        fun create(): MapPrint = MapPrintImpl()
    }
}