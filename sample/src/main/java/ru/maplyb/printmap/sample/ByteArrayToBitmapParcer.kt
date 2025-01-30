package ru.maplyb.printmap.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory

class ByteArrayToBitmapParser {
    fun pearse(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}