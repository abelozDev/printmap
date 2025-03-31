package ru.maplyb.printmap.impl.util

import android.content.Context
import android.graphics.Typeface

fun timesNewRomanTypeface(context: Context): Typeface = Typeface.createFromAsset(context.assets, "fonts/Times New Roman.ttf")
fun gostTypeATypeface(context: Context): Typeface = Typeface.createFromAsset(context.assets, "fonts/GOST type A Standard.ttf")
