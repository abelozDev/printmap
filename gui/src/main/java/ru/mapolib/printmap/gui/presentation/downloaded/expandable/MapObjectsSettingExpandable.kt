package ru.mapolib.printmap.gui.presentation.downloaded.expandable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.model.LayerObject
import ru.mapolib.printmap.gui.domain.MapObjectSliderInfo
import ru.mapolib.printmap.gui.domain.toSliderInfo
import kotlin.math.roundToInt

@Composable
fun MapObjectsSettingExpandable(
    layersObjects: List<LayerObject>,
    layerObjectsColors: Map<String, Int?>,
    changeObjectStyle: (LayerObject) -> Unit,
    onColorChangeClicked: (LayerObject) -> Unit,
    changeStyleFinished: () -> Unit
) {
    var isOpen by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .clickable {
                isOpen = !isOpen
            }
    ) {
        Text(
            text = stringResource(R.string.map_objects_settings)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
    Spacer(Modifier.height(16.dp))
    AnimatedVisibility(
        visible = isOpen,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val groupedObjects = remember(layersObjects) {
            listOf(
                layersObjects.filterIsInstance<LayerObject.Text>(),
                layersObjects.filterIsInstance<LayerObject.Object>(),
                layersObjects.filterIsInstance<LayerObject.Line>(),
                layersObjects.filterIsInstance<LayerObject.Polygon>(),
                layersObjects.filterIsInstance<LayerObject.Radius>()
            )
        }
        Column {
            groupedObjects
                .asSequence()
                .mapNotNull { it.firstOrNull() }
                .forEach {
                    MapObjectsSettingSlider(
                        slideInfo = it.toSliderInfo(it.style.width),
                        onValueChanged = { newValue ->
                            changeObjectStyle(it.updateStyle(it.style.copy(width = newValue.toFloat())))
                        },
                        onValueChangedFinished = changeStyleFinished,
                        selectedColor = layerObjectsColors[it::class.simpleName!!],
                        onColorChangeClicked = {
                            onColorChangeClicked(it)
                        }
                    )
                }
        }
    }
}

@Composable
private fun MapObjectsSettingSlider(
    slideInfo: MapObjectSliderInfo,
    selectedColor: Int?,
    onColorChangeClicked: () -> Unit,
    onValueChanged: (Int) -> Unit,
    onValueChangedFinished: () -> Unit
) {

    Column {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slideInfo.name
            )
            Spacer(Modifier.width(8.dp))
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
            Spacer(Modifier.weight(1f))
            Text(
                text = "Размер: ${ slideInfo.value.roundToInt() }/${ slideInfo.valueRange.endInclusive.roundToInt() }"
            )
        }

        Slider(
            value = slideInfo.value,
            onValueChange = {
                onValueChanged(it.roundToInt())
            },
            steps = slideInfo.steps,
            valueRange = slideInfo.valueRange,
            onValueChangeFinished = {
                onValueChangedFinished()
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PreviewMapObjectsSettingSlider() {
    MapObjectsSettingSlider(
        slideInfo = MapObjectSliderInfo(
            0f,
            1,
            "a",
            0f..1f
        ),
        selectedColor = null,
        onColorChangeClicked = {},
        onValueChanged = {},
        onValueChangedFinished = {}
    )
}
@Composable
@Preview
private fun PreviewMapObjectsSettingExpandable() {
    MapObjectsSettingExpandable(
        layersObjects = emptyList(),
        layerObjectsColors = mapOf(),
        changeObjectStyle = {},
        onColorChangeClicked = {},
        changeStyleFinished = {}
    )
}