package ru.maplyb.printmap.impl.service

import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.impl.domain.local.MapPath

internal interface MapResult {
    fun onMapReady(image: MapPath?)
}