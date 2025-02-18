package ru.mapolib.printmap.gui.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.maplyb.printmap.R
import ru.maplyb.printmap.api.model.BoundingBox
import ru.maplyb.printmap.api.model.MapItem
import ru.maplyb.printmap.api.model.MapObject
import ru.maplyb.printmap.api.model.MapObjectStyle
import ru.maplyb.printmap.api.model.MapType
import ru.mapolib.printmap.gui.utils.formatSize
import kotlin.math.roundToInt

@Composable
internal fun DownloadMapSettingsScreen(
    boundingBox: BoundingBox,
    maps: List<MapItem>,
    zoom: Int,
    objects: Map<MapObjectStyle, List<MapObject>>,
    startFormingAMap: (
        maps: List<MapItem>,
        boundingBox: BoundingBox,
        zoom: Int,
        quality: Int,
        objects: Map<MapObjectStyle, List<MapObject>>,
    ) -> Unit
) {
    if (zoom !in 0..28) throw IllegalArgumentException("zoom must be in 0..28")
    val viewModel = viewModel<SettingViewModel>(
        factory = SettingViewModel.create(
            zoom = zoom,
            boundingBox = boundingBox,
            maps = maps.map {
                it.copy(selected = zoom in it.zoomMin..it.zoomMax)
            },
            objects = objects
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.onEach {
            when (it) {
                is SettingEffect.StartFormingAMap -> startFormingAMap(
                    it.maps,
                    it.boundingBox,
                    it.zoom,
                    it.quality,
                    it.objects
                )
            }
        }
            .launchIn(this)
    }
    var isOpen by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_header),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        if (state.progress) {
            CircularProgressIndicator(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 16.dp),
            )
        } else {
            if (state.state is SettingState.Error) {
                Text(
                    text = stringResource(R.string.file_size_error),
                    color = Color.Red
                )
            } else {
                Text(
                    text = stringResource(R.string.tiles_count, state.tilesCount)
                )
                Spacer(Modifier.height(16.dp))
                state.fileSize?.let {
                    Text(
                        text = stringResource(R.string.file_size, formatSize(it))
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(ru.mapolib.printmap.gui.R.string.zoom)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = state.zoom.toString()
            )
        }
        Slider(
            value = state.zoom.toFloat(),
            onValueChange = {
                viewModel.sendEvent(SettingEvent.UpdateZoom(it.roundToInt()))
            },
            steps = 27,
            valueRange = 0f..28f,
            onValueChangeFinished = {
                viewModel.sendEvent(SettingEvent.GetTilesCount)
            }
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(ru.mapolib.printmap.gui.R.string.quality)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = state.quality.toString()
            )
        }

        Slider(
            value = state.quality.toFloat(),
            onValueChange = {
                viewModel.sendEvent(SettingEvent.UpdateQuality(it.roundToInt()))
            },
            steps = 100,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                //todo
            }
        )
        Row {
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = stringResource(ru.mapolib.printmap.gui.R.string.show_polyline)
            )
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked = state.showPolyline,
                onCheckedChange = {
                    viewModel.sendEvent(SettingEvent.ShowPolylineChanged)
                }
            )
        }
        Row(
            modifier = Modifier
                .clickable {
                    isOpen = !isOpen
                }
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.selected_maps)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        Spacer(Modifier.height(16.dp))
        val offlineMaps by remember(state.maps) {
            mutableStateOf(state.maps.filter { it.type is MapType.Offline })
        }
        val onlineMaps by remember(state.maps) {
            mutableStateOf(state.maps.filter { it.type is MapType.Online })
        }
        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                MapTypesItem(
                    header = stringResource(ru.mapolib.printmap.gui.R.string.offline),
                    maps = offlineMaps,
                    onChange = { map ->
                        viewModel.sendEvent(
                            SettingEvent.UpdateMap(
                                map.copy(
                                    selected = !map.selected
                                )
                            )
                        )
                    }
                )
                MapTypesItem(
                    header = stringResource(ru.mapolib.printmap.gui.R.string.online),
                    maps = onlineMaps,
                    onChange = { map ->
                        viewModel.sendEvent(
                            SettingEvent.UpdateMap(
                                map.copy(
                                    selected = !map.selected
                                )
                            )
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            content = {
                Text(text = stringResource(R.string.download))
            },
            enabled = state.buttonEnabled(),
            onClick = {
                viewModel.sendEvent(SettingEvent.StartDownloadingMap)
            }
        )
    }
}

@Composable
private fun MapTypesItem(
    header: String,
    maps: List<MapItem>,
    onChange: (MapItem) -> Unit
) {
    if (maps.isNotEmpty()) {
        Text(
            modifier = Modifier.padding(
                start = 8.dp,
                bottom = 16.dp
            ),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            text = header,
        )
    }
    maps.forEach { map ->
        Row {
            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = map.name
            )
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked = map.selected,
                onCheckedChange = {
                    onChange(map)
                }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDownloadMapSettingsScreen() {
    DownloadMapSettingsScreen(
        boundingBox = BoundingBox(
            0.0, 0.0, 0.0, 0.0,
        ),
        maps = emptyList(),
        zoom = 10,
        objects = mapOf(),
        { _, _, _, _, _ ->

        }
    )
}