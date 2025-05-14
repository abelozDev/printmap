package ru.maplib.printmap.core.theme.colors

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable

@Composable
fun PrintMapButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = PrintMapColorSchema.colors.buttonBackgroundColor,
        contentColor = PrintMapColorSchema.colors.textColor
    )
}