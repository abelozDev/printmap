package ru.maplyb.printmap.api.utils

import android.content.Context
import android.graphics.Bitmap
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.impl.util.limitSize

internal fun createBitmaps(resultBitmap: Bitmap?, context: Context): Pair<DownloadedImage, DownloadedImage> {
    val detail = resultBitmap?.cropCenter(255 * 2, 255 * 2)
    return DownloadedImage(resultBitmap?.limitSize(context), "Превью") to DownloadedImage(detail, "Детальный")
}
internal fun Bitmap.cropCenter(cropWidth: Int, cropHeight: Int): Bitmap {
    val x = (this.width - cropWidth) / 2
    val y = (this.height - cropHeight) / 2
    return Bitmap.createBitmap(this, x, y, cropWidth, cropHeight)
}