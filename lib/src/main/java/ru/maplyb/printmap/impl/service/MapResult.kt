package ru.maplyb.printmap.impl.service

import ru.maplyb.printmap.api.model.DownloadedImage

internal interface MapResult {
    fun onMapReady(images: List<DownloadedImage>)
}