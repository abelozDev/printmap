package ru.maplyb.printmap.api.model

import android.graphics.Bitmap

data class DownloadedImage(
    val bitmap: Bitmap?,
    val description: String
)