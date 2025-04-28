package ru.mapolib.printmap.gui.core.ui

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun PrintMapTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors().copy(
        focusedContainerColor = Color.Gray,
        errorContainerColor = Color.Gray,
        disabledContainerColor = Color.Gray,
        unfocusedContainerColor = Color.Gray,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color.White,
        focusedTextColor = Color.White,
        errorTextColor = Color.White,
        focusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )
}