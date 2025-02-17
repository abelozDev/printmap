package ru.mapolib.printmap.gui.utils

import android.content.Context
import android.graphics.Bitmap
import ru.maplyb.printmap.api.model.DownloadedImage
import ru.maplyb.printmap.impl.util.limitSize

internal fun createBitmaps(
    resultBitmap: Bitmap?,
    context: Context
): Pair<DownloadedImage, DownloadedImage> {
    val detail = resultBitmap?.cropCenter(255 * 2, 255 * 2)
    return DownloadedImage(
        id = 0,
        resultBitmap?.limitSize(context),
        "Превью"
    ) to DownloadedImage(id = 1, detail, "Детальный")
}

internal fun Bitmap.cropCenter(cropWidth: Int, cropHeight: Int): Bitmap {
    val finalCropWidth = minOf(cropWidth, this.width)
    val finalCropHeight = minOf(cropHeight, this.height)
    if (finalCropWidth == this.width && finalCropHeight == this.height) return this
    val x = (this.width - finalCropWidth) / 2
    val y = (this.height - finalCropHeight) / 2
    return Bitmap.createBitmap(this, x, y, finalCropWidth, finalCropHeight)
}
