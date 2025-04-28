package ru.maplib.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.graphics.toColor
import androidx.core.graphics.toColorInt

@Composable
internal fun CustomColorSelector(
    initialColor: android.graphics.Color,
    selectColor: (android.graphics.Color) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        val hexColor by remember(initialColor) {
            mutableStateOf(String.format("#%08X", initialColor.toArgb()))
        }
        var editColor by remember(initialColor) {
            mutableStateOf(hexColor)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(initialColor.toArgb())),
            contentAlignment = Alignment.Center,
        ) {
            BasicTextField(
                maxLines = 1,
                singleLine = true,
                textStyle = TextStyle.Default.copy(
                    color = getContrastColor(Color(initialColor.toArgb())),
                ),
                value = editColor,
                onValueChange = { input ->
                    println("input: $input")
                    if (input.length > 9) return@BasicTextField
                    if (input.matches(Regex("#[A-Fa-f0-9]{0,8}"))) {
                        editColor = input
                    }
                    if (editColor.length == 9) {
                        selectColor(input.toColorInt().toColor())
                    }
                },
            )
        }
        LazyColumn(
            modifier = Modifier.padding(16.dp),
        ) {
            items(RGBItems.entries.size) {
                val item = RGBItems.entries[it]
                SliderItem(
                    item = item,
                    initialValue = initialColor.getRGBColor(item),
                    onValueChangedFinished = {
                        selectColor(
                            android.graphics.Color.argb(
                                if (item == RGBItems.A) it else initialColor.alpha(),
                                if (item == RGBItems.R) it else initialColor.red(),
                                if (item == RGBItems.G) it else initialColor.green(),
                                if (item == RGBItems.B) it else initialColor.blue()
                            ).toColor()
                        )
                    }
                )
            }
        }
    }
}

fun getContrastColor(color: Color): Color {

    val r = color.toArgb().red / 255.0
    val g = color.toArgb().green / 255.0
    val b = color.toArgb().blue / 255.0

    // Формула яркости (Perceived luminance)
    val luminance = 0.299 * r + 0.587 * g + 0.114 * b

    // Если яркость высокая (цвет светлый) → вернуть черный, иначе белый
    return if (luminance > 0.5) Color.Black else Color.White
}

@Composable
@Preview
private fun PreviewCustomColorSelector() {
    CustomColorSelector(
        initialColor = android.graphics.Color.RED.toColor(), {}
    )
}