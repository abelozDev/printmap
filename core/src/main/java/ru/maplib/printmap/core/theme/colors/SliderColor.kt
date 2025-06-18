package ru.maplib.printmap.core.theme.colors

import androidx.compose.material.SliderColors
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PrintMapSliderColor(): SliderColors {
    return SliderDefaults.colors(
        activeTrackColor = Color(0xff1277f3),
        inactiveTrackColor = Color(0xff626166),
        inactiveTickColor = Color(0xffffffff),
        activeTickColor = Color(0xffffffff),
        thumbColor = Color(0xffd9d9d9)
    )
}