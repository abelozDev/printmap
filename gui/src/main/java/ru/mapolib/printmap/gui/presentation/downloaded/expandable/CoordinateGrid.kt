package ru.mapolib.printmap.gui.presentation.downloaded.expandable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.maplyb.printmap.impl.util.draw_on_bitmap.CoordinateSystem
import ru.mapolib.printmap.gui.domain.MapObjectSliderInfo
import ru.mapolib.printmap.gui.presentation.components.PrintmapPopup
import ru.mapolib.printmap.gui.presentation.downloaded.coordinateGridVariants
import kotlin.math.roundToInt

@Composable
internal fun CoordinateGrid(
    sliderInfo: MapObjectSliderInfo,
    selectedCoordinateGrid: Double,
    selectedCoordSystem: CoordinateSystem,
    checked: Boolean,
    selectedColor: Int?,
    onCheckedChanged: (Boolean) -> Unit,
    onSliderValueChangedFinished: (Float) -> Unit,
    onColorChangeClicked: () -> Unit,
    selectCoordinateGrid: (Double) -> Unit,
    selectCoordinateSystem: (CoordinateSystem) -> Unit
) {
    var sliderState by remember {
        mutableFloatStateOf(sliderInfo.value)
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "Координатная сетка"
            )
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChanged
            )
        }
        AnimatedVisibility(
            visible = checked,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrintmapPopup(
                        name = "Система координат",
                        items = CoordinateSystem.entries.map { it.name },
                        selected = selectedCoordSystem.name,
                        update = {
                            selectCoordinateSystem(CoordinateSystem.valueOf(it))
                        }
                    )

                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrintmapPopup(
                        name = "Масштаб",
                        items = coordinateGridVariants,
                        selected = selectedCoordinateGrid,
                        update = {
                            selectCoordinateGrid(it)
                        }
                    )

                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .size(24.dp)
                            .background(
                                if (selectedColor == null) {
                                    Color.Gray
                                } else Color(selectedColor)
                            )
                            .clickable {
                                onColorChangeClicked()
                            }
                    )
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = sliderInfo.value,
                        onValueChange = {
                            sliderState = it
                        },
                        steps = sliderInfo.steps,
                        valueRange = sliderInfo.valueRange,
                        onValueChangeFinished = {
                            onSliderValueChangedFinished(sliderState)
                        }
                    )
                    Text(
                        text = "${sliderState.roundToInt()}/${sliderInfo.valueRange.endInclusive.toInt()}",
                    )
                }
            }
        }
    }
}




@Composable
@Preview
private fun PreviewCoordinateGrid() {
    CoordinateGrid(
        checked = true,
        selectedColor = null,
        sliderInfo = MapObjectSliderInfo(
            value = 5f,
            steps = 9,
            valueRange = 0f..10f,
            name = "Координатная сетка"
        ),
        selectedCoordinateGrid = coordinateGridVariants[0],
        onCheckedChanged = {},
        onSliderValueChangedFinished = {},
        onColorChangeClicked = {},
        selectCoordinateGrid = {},
        selectCoordinateSystem = {},
        selectedCoordSystem = CoordinateSystem.SK42
    )
}