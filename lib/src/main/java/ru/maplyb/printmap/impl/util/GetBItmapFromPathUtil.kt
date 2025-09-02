package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal fun getBitmapFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        context.assets.open(fileName).use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}