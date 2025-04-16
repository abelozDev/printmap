package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Paint
import androidx.annotation.ColorInt

fun defTextPaint(
    context: Context,
    @ColorInt color: Int,
    textSize: Float,
    style: Paint.Style = Paint.Style.FILL,
    strokeWidth: Float = 0f
): Paint {
    return Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
        typeface = gostTypeATypeface(context = context)
        this.color = color
        this.textSize = textSize
        letterSpacing = -0.05f
        this.style = style
        this.strokeWidth = strokeWidth
    }
}