package ru.maplyb.printmap.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**Лимитирует размер bitmap в зависимости от доступного разрешения на устройстве*/
internal fun Bitmap.limitSize(): Bitmap {
    val maxSize = getMaxTextureSize()

    val newWidth = width.coerceAtMost(maxSize)
    val newHeight = height.coerceAtMost(maxSize)

    return if (width > maxSize || height > maxSize) {
        Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    } else {
        this
    }
}

private fun getMaxTextureSize(): Int {
    val maxSize = IntArray(1)
    GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
    return maxSize[0]
}
suspend fun ByteArray?.getTileSize(alpha: Int): Long {
    if (this == null) return 0
    return withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(this@getTileSize, 0, this@getTileSize.size)
        if (bitmap != null) {
            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            val paint = Paint().apply {
                this.alpha = alpha
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            resultBitmap.byteCount.toLong()
        } else 0L
    }
}
