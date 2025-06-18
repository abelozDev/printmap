package ru.maplib.palette

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import ru.maplib.printmap.core.theme.colors.PrintMapColorSchema
import kotlin.math.roundToInt

internal enum class RGBItems(val color: Color) {
    R(Color.Red), G(Color.Green), B(Color.Blue), A(Color.Black)
}

internal fun android.graphics.Color.getRGBColor(color: RGBItems): Float = when(color) {
    RGBItems.R -> this.red() * 255
    RGBItems.G -> this.green() * 255
    RGBItems.B -> this.blue() * 255
    RGBItems.A -> this.alpha() * 255
}
@Composable
internal fun SliderItem(
    item: RGBItems,
    initialValue: Float,
    onValueChangedFinished: (Float) -> Unit
) {
    var sliderValue by remember(initialValue) {
        mutableFloatStateOf(initialValue)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = item.name
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            steps = 254,
            valueRange = 0f..255f,
            onValueChangeFinished = {
                onValueChangedFinished(sliderValue/255)
            },
            colors = SliderDefaults.colors(
                thumbColor = item.color,
                activeTrackColor = item.color.copy(
                    alpha = 0.5f
                ),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            )
        )
        Text(
            color = PrintMapColorSchema.colors.textColor,
            text = sliderValue.roundToInt().toString()
        )
    }

}

@Composable
@Preview
private fun PreviewSliderItem() {
    SliderItem(RGBItems.G, 124f, {})
}